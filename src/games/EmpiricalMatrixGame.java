package games;

import java.util.Arrays;
import java.util.List;

import util.GenericTensor;

/**
 * This class is used to track a game instances as it is revealed by sampling/observation
 * <p/>
 * Right now, only observations of full payoff vectors are supported (no isolated observations of individual's payoffs)
 */

public class EmpiricalMatrixGame extends Game {

  // This is the default payoff returned when there are no samples for a profile
  private double DEFAULT_PAYOFF = Double.NEGATIVE_INFINITY;

  // Controls whether or not this game keeps explicit lists of all samples or just summary information
  private boolean RECORD_SAMPLES = false;

  // the total number of samples for all profiles
  private int totalSamples;

  // the actual payoff data
  private final GenericTensor<EmpiricalPayoffs> payoffs;

  /**
   * Constructor for an empirical matrix
   */
  public EmpiricalMatrixGame(int numPlayers, int[] numActions) {
    super(numPlayers, numActions);
    payoffs = new GenericTensor<EmpiricalPayoffs>(nActions);
    init();
  }

  /**
   * Create an "empirical" representation of a game from another game representation
   * Defaults to one "sample" per profile
   */
  public EmpiricalMatrixGame(Game game) {
    super(game.getNumPlayers(), game.getNumActions());
    payoffs = new GenericTensor<EmpiricalPayoffs>(nActions);
    init();

    OutcomeIterator itr = game.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      addSample(outcome, game.getPayoffs(outcome));
    }
  }

  /**
   * Copy the given empirical game exactly
   */
  public EmpiricalMatrixGame(EmpiricalMatrixGame game) {
    super(game.getNumPlayers(), game.getNumActions());
    payoffs = new GenericTensor<EmpiricalPayoffs>(nActions);
    this.DEFAULT_PAYOFF = game.DEFAULT_PAYOFF;
    this.RECORD_SAMPLES = game.RECORD_SAMPLES;
    init();

    OutcomeIterator itr = game.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      payoffs.getValue(outcome).setEmpiricalPayoffs(game.getEmpiricalpayoffs(outcome));
    }
  }

  /**
   * Creates a restricted version of the original game containing only the specified actions (which are re-labeled)
   * TODO: redo this in a more general way
   */
  public EmpiricalMatrixGame(EmpiricalMatrixGame sourceGame, List<List<Integer>> actionSets, int[] newNumActions) {
    super(sourceGame.getNumPlayers(), newNumActions);
    payoffs = new GenericTensor<EmpiricalPayoffs>(nActions);
    this.DEFAULT_PAYOFF = sourceGame.DEFAULT_PAYOFF;
    this.RECORD_SAMPLES = sourceGame.RECORD_SAMPLES;
    totalSamples = 0;

    // copy all of the payoff/sampling information from the original game
    OutcomeIterator itr = this.iterator();
    int[] sourceOutcome = new int[nPlayers];
    while (itr.hasNext()) {
      int[] outcome = itr.next();

      // map this outcome into the original (larger) game
      for (int pl = 0; pl < nPlayers; pl++) {
        sourceOutcome[pl] = actionSets.get(pl).get(outcome[pl] - 1);
      }

      // insert a copy of the payoffs into this new game
      payoffs.setValue(new EmpiricalPayoffs(sourceGame.payoffs.getValue(sourceOutcome)), outcome);
      totalSamples += sourceGame.getNumSamples(sourceOutcome);
    }
  }

  /**
   * Initializes the payoff matrix, sample counts, etc.
   */
  protected void init() {
    totalSamples = 0;

    // we need to initialize all of the payoff objects because the tensor does not
    // have access to this information
    for (int i = 0; i < payoffs.size(); i++) {
      payoffs.setValue(new EmpiricalPayoffs(nPlayers), i);
    }
  }

  /**
   * Clears all of the payoff information so the data structure can be re-used
   */
  public void clear() {
    totalSamples = 0;
    for (int i = 0; i < payoffs.size(); i++) {
      payoffs.getValue(i).reset();
    }
  }

  /**
   * Add a sample payoff to the game
   */
  public void addSample(int[] outcome, double[] payoffsToAdd) {
    payoffs.getValue(outcome).addSample(payoffsToAdd);
    totalSamples++;
  }

  /**
   * Check whether this game records full sample information or not
   */
  public boolean getRecordSamples() {
    return RECORD_SAMPLES;
  }

  /**
   * Use this function to turn on explicit sample recording for this game
   * Keep in mind that this is quite expensive
   */
  public void setRecordSamples(boolean value) {
    RECORD_SAMPLES = value;
    for (int i = 0; i < payoffs.size(); i++) {
      payoffs.getValue(i).setRecordSamples(value);
    }
  }

  /**
   * Sets the default payoff to return for profiles with no samples
   */
  public void setDefaultPayoff(double value) {
    DEFAULT_PAYOFF = value;
  }

  /**
   * Check the default payoff value for unsampled profiles
   */
  public double getDefaultPayoff() {
    return DEFAULT_PAYOFF;
  }

  /**
   * Returns the total number of samples for this game
   */
  public int getTotalNumSamples() {
    return totalSamples;
  }

  /**
   * Returns the number of samples observed for this outcome
   */
  public int getNumSamples(int[] outcome) {
    return payoffs.getValue(outcome).getNumSamples();
  }

  /**
   * Returns the number of profiles that have been sampled at least the number of times specified
   */

  public int getNumProfilesSampled(int bound) {
    int cnt = 0;
    for (int i = 0; i < payoffs.size(); i++) {
      if (payoffs.getValue(i).getNumSamples() >= bound) cnt++;
    }
    return cnt;
  }

  /**
   * Returns the standard deviation for the payoffs to a player for an outcome
   */
  public double getStdDev(int[] outcome, int player) {
    return payoffs.getValue(outcome).getStdDev(player);
  }

  /**
   * Returns the standard deviations for all players
   */
  public double[] getStdDevs(int[] outcome) {
    return payoffs.getValue(outcome).getStdDevs();
  }

  /**
   * Get the maximum and minimum payoffs for the game over all players: [max, min]
   * OVERRIDING to address sampling
   */
  public double[] getExtremePayoffs() {
    double max = Double.NEGATIVE_INFINITY;
    double min = Double.POSITIVE_INFINITY;

    OutcomeIterator itr = iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      EmpiricalPayoffs ep = payoffs.getValue(outcome);
      if (ep.getNumSamples() <= 0) {
        continue;
      }
      double[] payoffs = ep.getMeanPayoffs();
      for (double tmp : payoffs) {
        max = Math.max(max, tmp);
        min = Math.min(min, tmp);
      }
    }

    return new double[] {max, min};
  }

  /**
   * Get the maximum and minimum payoffs for a particular player
   * OVERRIDING to address sampling
   */
  public double[] getExtremePayoffs(int player) {
    double max = Double.NEGATIVE_INFINITY;
    double min = Double.POSITIVE_INFINITY;

    OutcomeIterator itr = iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      EmpiricalPayoffs ep = payoffs.getValue(outcome);
      if (ep.getNumSamples() <= 0) {
        continue;
      }
      double payoff = ep.getMeanPayoff(player);
      max = Math.max(max, payoff);
      min = Math.min(min, payoff);
    }

    return new double[] {max, min};
  }

  /**
   * Return the mean payoff for a particular player for a particular outcome
   * OVERRIDING for efficiency
   */
  public double getPayoff(int[] outcome, int player) {
    EmpiricalPayoffs ep = payoffs.getValue(outcome);

    // override the default payoffs from the EmpiricalPayoffs class
    if (ep.getNumSamples() <= 0) {
//      System.err.println("Warning: returning default payoff in empirical matrix game: " +
//                         Arrays.toString(outcome) + "\n");
      return DEFAULT_PAYOFF;
    }
    return ep.getMeanPayoff(player);
  }


  public EmpiricalPayoffs getEmpiricalpayoffs(int[] outcome) {
    return payoffs.getValue(outcome);
  }

  /**
   * Return the mean payoff for all players, given an outcome
   */
  public double[] getPayoffs(int[] outcome) {
    EmpiricalPayoffs ep = payoffs.getValue(outcome);

    // override the default payoffs from the EmpiricalPayoffs class
    if (ep.getNumSamples() <= 0) {
//      System.err.println("Warning: returning default payoff in empirical matrix game: " +
//                         Arrays.toString(outcome) + "\n");

      double[] tmp = new double[nPlayers];
      Arrays.fill(tmp, DEFAULT_PAYOFF);
      return tmp;
    }
    return ep.getMeanPayoffs();
  }
}

