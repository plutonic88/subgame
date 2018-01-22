package solvers;

import java.util.Arrays;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import observers.GameObserver;

/**
 * Returns the best response to the uniform distribution
 * Uniform mixture over pure strategies with equal expected payoffs
 */

public final class BRUSolver implements GameSolver, GameOutcomePredictor {

  private ExplorationUtil explorationUtil = new ExplorationUtil();

  private ExplorationUtil.SamplingMode samplingMode = ExplorationUtil.SamplingMode.ALL_EVEN_PLUS_RANDOM;
  private int samplesPerProfile = 1;

  private int nPlayers = 0;
  private int[] nActs = null;
  private double[] stratPayoffs = null;
  private int[] numSampledProfiles = null;
  private EmpiricalMatrixGame eGame = null;
  private OutcomeIterator itr = null;

  public BRUSolver() {
  }

  public BRUSolver(ExplorationUtil.SamplingMode mode) {
    this.samplingMode = mode;
  }

  public boolean isStochastic() {
    return true;
  }

  public String getName() {
    return "BRU";
  }

  // returns the description
  public String getDescription() {
    return "BRU Solver\nPlays a best response to the uniform mixture of opponent strategies.\nSampling mode: " +
           samplingMode +
           "\n";
  }

  // always predicts the centroid
  public OutcomeDistribution predictOutcome(EmpiricalMatrixGame emg) {
    return new OutcomeDistribution(emg.getNumActions());
  }

  // always predicts the centroid
  public OutcomeDistribution predictOutcome(GameObserver gameObs) {
    return new OutcomeDistribution(gameObs.getNumActions());
  }

  /**
   * Returns a strategy to play
   *
   * @param emg    Empirical Observation of a game
   * @param player The player to select an action for.
   */
  public MixedStrategy solveGame(EmpiricalMatrixGame emg, int player) {
    initialize(emg, player);
    return computeStrategy(player);
  }

  /**
   * Returns a strategy to play
   *
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    initialize(gameObs, player);
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, player, samplesPerProfile);
    return computeStrategy(player);
  }

  private MixedStrategy computeStrategy(int player) {
    // compute total payoff to each strategy, assuming uniform distribution of
    // opponent play (equivalent ordering to expected payoff)
    itr.reset();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      if (eGame.getNumSamples(outcome) > 0) {
        stratPayoffs[outcome[player]] += eGame.getPayoff(outcome, player);
        numSampledProfiles[outcome[player]]++;
      }
    }

    // compute averages
    for (int i = 0; i < stratPayoffs.length; i++) {
      if (numSampledProfiles[i] > 0) {
        stratPayoffs[i] /= numSampledProfiles[i];
      } else {
        stratPayoffs[i] = Double.NEGATIVE_INFINITY;
      }
    }

    MixedStrategy strategy = new MixedStrategy(nActs[player], 0d);
    strategy.setBestResponse(stratPayoffs);
    return strategy;
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   */
  private void initialize(GameObserver gameObs, int player) {
    // check to see if we are set up for a game of this dimension
    if (gameObs.getNumPlayers() == nPlayers &&
        Arrays.equals(gameObs.getNumActions(), nActs)) {
      reset();
      return;
    }

    nPlayers = gameObs.getNumPlayers();
    nActs = gameObs.getNumActions().clone();
    stratPayoffs = new double[nActs[player] + 1];
    numSampledProfiles = new int[nActs[player] + 1];
    eGame = new EmpiricalMatrixGame(nPlayers, nActs);
    itr = new OutcomeIterator(gameObs);
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   */
  private void initialize(EmpiricalMatrixGame emg, int player) {
    nPlayers = emg.getNumPlayers();
    nActs = emg.getNumActions().clone();
    stratPayoffs = new double[nActs[player] + 1];
    numSampledProfiles = new int[nActs[player] + 1];
    eGame = new EmpiricalMatrixGame(emg);
    itr = new OutcomeIterator(emg);
  }

  /**
   * Reset all private data structures to analyze a new game
   */
  private void reset() {
    eGame.clear();

    // initialize payoff counters
    Arrays.fill(stratPayoffs, 0d);
    Arrays.fill(numSampledProfiles, 0);
    stratPayoffs[0] = Double.NEGATIVE_INFINITY;
  }

//  private void sampleEvenly(GameObserver go, EmpiricalMatrixGame eg, int player) {
//    // first, sample all profiles evenly as many times as possible
//    int numSamplesForAllProfiles = go.numObsLeft() / eg.getNumProfiles();
//    SolverUtils.sampleAllProfiles(go, eg, numSamplesForAllProfiles);
//
//    // next, sample all actions evenly, as many times as possible
//    int[] outcome = new int[go.getNumPlayers()];
//    int numSamplesForAllActions = go.numObsLeft() / go.getNumActions(player);
//    for (int i = 1; i <= go.getNumActions(player); i++) {
//      int numSampled = 0;
//      outcome[player] = i;
//      while (numSampled < numSamplesForAllActions) {
//        for (int pl = 0; pl < go.getNumPlayers(); pl++) {
//          if (pl == player) continue;
//          outcome[pl] = rand.nextInt(go.getNumActions(pl)) + 1;
//        }
//        // check if we have already sampled this opponent outcome
//        if (eg.getNumSamples(outcome) > 0) continue;
//
//        eg.addSample(outcome, go.getSample(outcome));
//        numSampled++;
//      }
//    }
//
//    // use any remaining samples randomly
//    SolverUtils.sampleRandomlyWithReplacement(go, eg, go.numObsLeft(), 1);
//  }

}
