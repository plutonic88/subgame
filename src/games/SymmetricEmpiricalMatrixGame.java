package games;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Symmetric version of the empirical game matrix class
 */
public class SymmetricEmpiricalMatrixGame extends SymmetricGame {

  // This is the default payoff returned when there are no samples for a profile
  private double DEFAULT_PAYOFF = Double.NEGATIVE_INFINITY;

  // Controls whether or not this game keeps explicit lists of all samples or just summary information
  private boolean RECORD_SAMPLES = false;

  // the total number of samples for all profiles
  private int totalSamples;

  // store the payoffs in a hash table for now
  // this is a map from <action,count> to the payoff objects
  private final Map<Map<Integer,Integer>, SymmetricEmpiricalPayoffs> payoffs;

  /**
   * Constructor for a new symmetric matrix game.
   * All players must have the same number of actions (by definition)
   */
  public SymmetricEmpiricalMatrixGame(int numPlayers, int numActions) {
    super(numPlayers, numActions);

    // Profiles are stored in a hash table for now
    payoffs = new HashMap<Map<Integer,Integer>, SymmetricEmpiricalPayoffs>(nSymmetricProfiles, 0.5f);
    init();
  }

  /**
   * Initializes the payoff matrix, sample counts, etc.
   */
  protected void init() {
    totalSamples = 0;

    // loop through combinations, adding all possible payoffs
    SymmetricOutcomeIterator itr = symmetricIterator();
    while (itr.hasNext()) {
      payoffs.put(createCounts(itr.next()), new SymmetricEmpiricalPayoffs());
    }
  }

  /**
   * Add a sample payoff to the game
   */
  public void addSample(int[] outcome, double[] payoffsToAdd) {
    Map<Integer,Integer> counts = createCounts(outcome);
    payoffs.get(counts).addSample(createSymmetricPayoffs(outcome, counts, payoffsToAdd));
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
    SymmetricOutcomeIterator itr = symmetricIterator();
    while (itr.hasNext()) {
      payoffs.get(createCounts(itr.next())).setRecordSamples(value);
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
    return payoffs.get(createCounts(outcome)).getNumSamples();
  }

  /**
   * Returns the number of profiles that have been sampled at least the number of times specified
   */
  public int getNumProfilesSampled(int bound) {
    int cnt = 0;
    SymmetricOutcomeIterator itr = symmetricIterator();
    while (itr.hasNext()) {
      if ( payoffs.get(createCounts(itr.next())).getNumSamples() >= bound) cnt++;
    }
    return cnt;
  }

  /**
   * Returns the standard deviation for the payoffs to a player for an outcome
   */
//   public double getStdDev(int[] outcome, int player) {
//     return payoffs.get(createCounts(outcome)).getStdDev(player);
//   }

  /**
   * Returns the standard deviations for all actions
   */
  public Map<Integer,Double> getStdDevs(int[] outcome) {
    return payoffs.get(createCounts(outcome)).getStdDevs();
  }

  /**
   * Get the maximum and minimum payoffs for the game over all players: [max, min]
   * OVERRIDING to address sampling
   */
  public double[] getExtremePayoffs() {
    double max = Double.NEGATIVE_INFINITY;
    double min = Double.POSITIVE_INFINITY;

    SymmetricOutcomeIterator itr = symmetricIterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      SymmetricEmpiricalPayoffs ep = payoffs.get(createCounts(outcome));
      if (ep.getNumSamples() <= 0) { continue; }
      double[] payoffs = mapSymmetricPayoffs(outcome, ep.getMeanPayoffs());
      for (double tmp : payoffs) {
        max = Math.max(max, tmp);
        min = Math.min(min, tmp);
      }
    }
    return new double[]{max, min};
  }

  /**
   * Get the maximum and minimum payoffs for a particular player
   * OVERRIDING to address sampling
   */
  public double[] getExtremePayoffs(int player) {
    double max = Double.NEGATIVE_INFINITY;
    double min = Double.POSITIVE_INFINITY;

    SymmetricOutcomeIterator itr = symmetricIterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      SymmetricEmpiricalPayoffs ep = payoffs.get(createCounts(outcome));
      if (ep.getNumSamples() <= 0) { continue; }
      double payoff = ep.getMeanPayoff(outcome[player]);
      max = Math.max(max, payoff);
      min = Math.min(min, payoff);
    }
    return new double[]{max, min};
  }

  /**
   * Return the mean payoff for a particular player for a particular outcome
   * OVERRIDING for efficiency
   *
   * NOTE: NOT recommended for symmetric game (player/action mapping is confusing)
   */
  public double getPayoff(int[] outcome, int player) {
    Map<Integer,Integer> counts = createCounts(outcome);
    SymmetricEmpiricalPayoffs ep = payoffs.get(counts);

    // override the default payoffs from the EmpiricalPayoffs class
    if (ep.getNumSamples() <= 0) {
      System.err.println("Warning: returning default payoff in for empirical matrix game.\n");
      return DEFAULT_PAYOFF;
    }
    return ep.getMeanPayoff(outcome[player]);
  }

  /**
   * Return the mean payoff for all players, given an outcome
   */
  public double[] getPayoffs(int[] outcome) {
    Map<Integer,Integer> counts = createCounts(outcome);
    SymmetricEmpiricalPayoffs ep = payoffs.get(counts);

    // override the default payoffs from the EmpiricalPayoffs class
    if (ep.getNumSamples() <= 0) {
      System.err.println("Warning: returning default payoff in for empirical matrix game.\n");
      double[] tmp = new double[nPlayers];
      Arrays.fill(tmp, DEFAULT_PAYOFF);
      return tmp;
    }
    return mapSymmetricPayoffs(outcome, ep.getMeanPayoffs());
  }
}
