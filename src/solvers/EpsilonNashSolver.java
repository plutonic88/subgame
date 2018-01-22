package solvers;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.nf;
import static subgame.EGAUtils.returnSB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;
import support.ProfileInfo;

/**
 * A set of tools for sampling the payoff matrix, analyzing the stability of profiles,
 * and selecting strategies based on this analysis
 */

public class EpsilonNashSolver implements GameSolver, GameOutcomePredictor {

  private ExplorationUtil explorationUtil = new ExplorationUtil();

  // STABLE_SET    Plays the strategy distribution given by a set of the most stable profiles
  public enum DecisionMode {
    STABLE_SET, BOLTZMANN, BR_BOLTZMANN
  }

  private ExplorationUtil.SamplingMode samplingMode = ExplorationUtil.SamplingMode.ALL_EVEN_PLUS_RANDOM;
  private DecisionMode decisionMode = DecisionMode.STABLE_SET;

  // If true, the decision procedures distinguish between profiles with negative benefits to deviating
  // If false, the benefit to deviating is bounded from below by 0, so all equilibria are equivalent
  private boolean useNegativeBenefits = true;

  // Temperature parameter used in boltmann distribution calculations
  // +INFINITY implies random actions; lim->0 implies full weight on most stable solutions
  private double temperature = Double.POSITIVE_INFINITY;

  // determines number of samples to make in a batch when sampling
  private int samplesPerProfile = 1;

  // This can be used to imposed a lower bound on the number of deviations sampled before a
  // profile is included in the solution computation
  //
  // This is relative to the average number of deviations sampled for profiles we have data for:
  // bound = floor(ave - (relativeDeviationSamplingBound * ave))
  // 1.0 implies no bound
  private double relativeDeviationSamplingBound = 1.0;

  // Absolute version of the deviation sampling bound
  private int absoluteDeviationSamplingBound = 0;

  // This can be used to set an upper bound on the epsilon values that will be included in the set of stable profiles
  // this is expressed as a multiplier on the range of payoffs in the game:   (max_payoff-minPayoff * stableSetEpsilonBound)
  // NEGATIVE INFINITY implies that *only* the minimum benefit to deviating set is chosen
  // 0 implies the set of all nash equilibria
  private double stableSetEpsilonBound = Double.NEGATIVE_INFINITY;

  private String name;

  // whether or not this solver is stochastic or not (use for fully-observable benchmark case)
  private boolean isStochastic = true;

  /**
   * Cached data for efficiency
   */
  private int numPlayers = 0;
  private int[] numActs = null;
  MixedStrategy strategy = null;
  EmpiricalMatrixGame eGame = null;

  public EpsilonNashSolver() {
    updateName();
  }

  public EpsilonNashSolver(ExplorationUtil.SamplingMode samplingMode) {
    this.samplingMode = samplingMode;
    updateName();
  }

  public EpsilonNashSolver(ExplorationUtil.SamplingMode samplingMode, int samplesPerProfile) {
    this.samplingMode = samplingMode;
    this.samplesPerProfile = samplesPerProfile;
    updateName();
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = getSB();
    sb.append("Epsilon Nash Solver\n");
    sb.append("Plays according to the most stable profiles in the selected subgame.\n");
    sb.append("Sampling mode: ").append(samplingMode).append("\n");
    sb.append("Sample per profile: ").append(samplesPerProfile).append("\n");
    sb.append("Decision mode: ").append(decisionMode).append("\n");
    sb.append("Use negative benefits: ").append(useNegativeBenefits).append("\n");
    sb.append("Temperature: ").append(temperature).append("\n");
    sb.append("Relative deviation bound: ").append(relativeDeviationSamplingBound).append("\n");
    sb.append("Absolute deviation bound: ").append(absoluteDeviationSamplingBound).append("\n");
    sb.append("Epsilon bound: ").append(stableSetEpsilonBound).append("\b");
    return returnSB(sb);
  }

  public boolean isUseNegativeBenefits() {
    return useNegativeBenefits;
  }

  public void setUseNegativeBenefits(boolean useNegativeBenefits) {
    this.useNegativeBenefits = useNegativeBenefits;
  }

  public int getSamplesPerProfile() {
    return samplesPerProfile;
  }

  public void setSamplesPerProfile(int samplesPerProfile) {
    this.samplesPerProfile = samplesPerProfile;
  }

  public void setSamplingMode(ExplorationUtil.SamplingMode mode) {
    this.samplingMode = mode;
  }

  public void setDecisionMode(DecisionMode mode) {
    this.decisionMode = mode;
    updateName();
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
    updateName();
  }

  public int getAbsoluteDeviationSamplingBound() {
    return absoluteDeviationSamplingBound;
  }

  public void setAbsoluteDeviationSamplingBound(int absoluteDeviationSamplingBound) {
    this.absoluteDeviationSamplingBound = absoluteDeviationSamplingBound;
  }

  public double getRelativeDeviationSamplingBound() {
    return relativeDeviationSamplingBound;
  }

  public void setRelativeDeviationSamplingBound(double bound) {
    this.relativeDeviationSamplingBound = bound;
  }

  public double getStableSetEpsilonBound() {
    return stableSetEpsilonBound;
  }

  public void setStableSetEpsilonBound(double bound) {
    this.stableSetEpsilonBound = bound;
    updateName();
  }

  public void setStochastic(boolean isStochastic) {
    this.isStochastic = isStochastic;
  }

  public boolean isStochastic() {
    return isStochastic;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OutcomeDistribution predictOutcome(EmpiricalMatrixGame emg) {
    // set up all of the necessary data structures
    initialize(emg, 0);
    StabilityAnalysis stability = new StabilityAnalysis(eGame, 0);

    switch (decisionMode) {
      case STABLE_SET:
        return computeStableSetPrediction(stability);
      case BOLTZMANN:
        return stability.getBoltzmannOutcome(temperature,
                                             absoluteDeviationSamplingBound,
                                             useNegativeBenefits);
      case BR_BOLTZMANN:
        return stability.getBoltzmannOutcome(temperature,
                                             absoluteDeviationSamplingBound,
                                             useNegativeBenefits);
      default:
        throw new RuntimeException("Error in epsilon nash solver. Invalid decision mode: " + decisionMode);
    }
  }

  public OutcomeDistribution predictOutcome(GameObserver gameObs) {
    // set up all of the necessary data structures
    initialize(gameObs, 0);

    // sample the game matrix
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, 0, samplesPerProfile);

    StabilityAnalysis stability = new StabilityAnalysis(eGame, 0);

    switch (decisionMode) {
      case STABLE_SET:
        return computeStableSetPrediction(stability);
      case BOLTZMANN:
        return stability.getBoltzmannOutcome(temperature,
                                             absoluteDeviationSamplingBound,
                                             useNegativeBenefits);
      case BR_BOLTZMANN:
        return stability.getBoltzmannOutcome(temperature,
                                             absoluteDeviationSamplingBound,
                                             useNegativeBenefits);
      default:
        throw new RuntimeException("Error in epsilon nash solver. Invalid decision mode: " + decisionMode);
    }
  }

  /**
   * @param emg    An observed empirical game
   * @param player The player to select an action for.
   * @return a mixed strategy to play
   */
  public MixedStrategy solveGame(EmpiricalMatrixGame emg, int player) {
    // set up all of the necessary data structures
    initialize(emg, player);

    // compute the strategy to play for this matrix and player
    computeStrategy(player);

    return strategy;
  }

  /**
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   * @return a mixed strategy to play
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    // set up all of the necessary data structures
    initialize(gameObs, player);

    // sample the game matrix
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, player, samplesPerProfile);

    // compute the strategy to play for this matrix and player
    computeStrategy(player);

    return strategy;
  }

  private void computeStrategy(int player) {
    // make a decision based on the available information
    StabilityAnalysis stability = new StabilityAnalysis(eGame, 0);

    // compute a bound specifying the minimum number of deviations that must be sampled for
    // a profile to participate in the analysis
    // this is relative to the average number sampled
    double aveDeviationsSampled = stability.getAveDeviationsSampled();
    if (absoluteDeviationSamplingBound == 0 && relativeDeviationSamplingBound != 1d) {
      absoluteDeviationSamplingBound =
              (int) Math.floor(aveDeviationsSampled -
                               (relativeDeviationSamplingBound * aveDeviationsSampled));
    }

    OutcomeDistribution dist;
    switch (decisionMode) {
      case STABLE_SET:
        computeStableSetStrategy(stability, player);
        break;
      case BOLTZMANN:
        dist = stability.getBoltzmannOutcome(temperature,
                                             absoluteDeviationSamplingBound,
                                             useNegativeBenefits);
        //System.out.println("Boltzmann distribution (" + temperature + "):" + dist.toString());

        strategy.setProbs(dist.getMarginalDistribution(player));
        //System.out.println("Dist strategy (" + temperature + "): " + strategy.toString());
        break;
      case BR_BOLTZMANN:
        dist = stability.getBoltzmannOutcome(temperature,
                                             absoluteDeviationSamplingBound,
                                             useNegativeBenefits);
        double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, dist, false);
        strategy.setBestResponse(stratPayoffs);
        //System.out.println("BR to dist strategy (" + temperature + "): " + strategy.toString());
        break;
      default:
        throw new RuntimeException("Error in epsilon nash solver. Invalid decision mode: " + decisionMode);
    }
  }

  private void updateName() {
    if (decisionMode == DecisionMode.STABLE_SET) {
      name = "e-Nash SET " + nf.format(stableSetEpsilonBound);
    } else if (decisionMode == DecisionMode.BOLTZMANN) {
      name = "e-Nash RAW " + nf.format(temperature);
    } else if (decisionMode == DecisionMode.BR_BOLTZMANN) {
      name = "e-Nash BR " + nf.format(temperature);
    } else {
      name = "e-Nash UNKNOWN";
    }
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   *
   * @param gameObs obervation of the game
   * @param player  the player
   */
  private void initialize(GameObserver gameObs, int player) {
    // check to see if we are set up for a game of this dimension
    if (gameObs.getNumPlayers() == numPlayers &&
        Arrays.equals(gameObs.getNumActions(), numActs)) {
      reset(player);
      return;
    }

    numPlayers = gameObs.getNumPlayers();
    numActs = gameObs.getNumActions().clone();
    strategy = new MixedStrategy(numActs[player], 0d);
    eGame = new EmpiricalMatrixGame(numPlayers, numActs);
  }

  private void initialize(EmpiricalMatrixGame emg, int player) {
    numPlayers = emg.getNumPlayers();
    numActs = emg.getNumActions();
    strategy = new MixedStrategy(numActs[player], 0d);
    eGame = new EmpiricalMatrixGame(emg);
  }

  /**
   * Reset all private data structures to analyze a new game
   *
   * @param player the player
   */
  private void reset(int player) {
    eGame.clear();
    strategy = new MixedStrategy(numActs[player], 0d);
  }

  /**
   * Compute a strategy based on playing the stable strategy set
   * Given bounds on the minimum number of deviations sampled and the maximum benefit to deviating,
   * compute the set of most appealing profiles and play according to this distribution
   *
   * @param stability stability analysis
   * @param player    the player
   */
  private void computeStableSetStrategy(StabilityAnalysis stability, int player) {

    ProfileInfo mostStableProfile = stability.getMostStableProfile(absoluteDeviationSamplingBound);
    if (mostStableProfile == null) {
      System.err.println("Error in epsilon nash solver. Empty set of most stable profiles.");
      return;
    }

    double minBenefitToDeviating = mostStableProfile.maxBenefit;
    double[] maxAndMinPayoffs = eGame.getExtremePayoffs();
    double payoffRange = maxAndMinPayoffs[0] - maxAndMinPayoffs[1];
    double benefitToDeviatingBound = Math.max(minBenefitToDeviating, payoffRange * stableSetEpsilonBound);
    TreeSet<ProfileInfo> profileSet = stability.getEpsilonBoundedProfileSet(absoluteDeviationSamplingBound,
                                                                            benefitToDeviatingBound);

//    if (profileSet.size() > 1) {
//      System.out.println("Computing stable set strategy, set size: " + profileSet.size());
//    }

    // count number of instances of each action in the min profile set
    for (ProfileInfo pi : profileSet) {
//      if (pi.maxBenefit > 0) {
//        System.out.println("Most stable not Nash: " + pi.maxBenefit);
//      }
      int tmpStrat = pi.outcome[player];
      double tmpProb = strategy.getProb(tmpStrat);
      strategy.setProb(tmpStrat, tmpProb + 1d);
    }

    // normalize to create a mixed strategy
    strategy.normalize();
  }


  private OutcomeDistribution computeStableSetPrediction(StabilityAnalysis stability) {

    ProfileInfo mostStableProfile = stability.getMostStableProfile(absoluteDeviationSamplingBound);
    if (mostStableProfile == null) {
      System.err.println("Error in epsilon nash solver. Empty set of most stable profiles.");
      return null;
    }

    double minBenefitToDeviating = mostStableProfile.maxBenefit;
    double[] maxAndMinPayoffs = eGame.getExtremePayoffs();
    double payoffRange = maxAndMinPayoffs[0] - maxAndMinPayoffs[1];
    double benefitToDeviatingBound = Math.max(minBenefitToDeviating, payoffRange * stableSetEpsilonBound);
    TreeSet<ProfileInfo> profileSet = stability.getEpsilonBoundedProfileSet(absoluteDeviationSamplingBound,
                                                                            benefitToDeviatingBound);

//    if (profileSet.size() > 1) {
//      System.out.println("Computing stable set strategy, set size: " + profileSet.size());
//    }

    Collection<int[]> profileOutcomes = new ArrayList<int[]>();
    for (ProfileInfo pi : profileSet) {
      profileOutcomes.add(pi.outcome);
    }

    OutcomeDistribution dist = new OutcomeDistribution(numActs, profileOutcomes);
    return dist;
  }

}
