package solvers;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.util.Arrays;
import java.util.List;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Sep 2, 2007
 * Time: 11:23:43 PM
 */
public class ReplicatorDynamicsSolver implements GameSolver, GameOutcomePredictor {

  public static final int DEFAULT_MAX_ITERATIONS = 1000;

  private ExplorationUtil explorationUtil = new ExplorationUtil();

  // RAW:  play the RDE as given
  // BR:  play a best response to the the RDE distribution
  public enum DecisionMode {
    RAW, BR
  }

  // selects the method for sampling the game matrix
  private ExplorationUtil.SamplingMode samplingMode = ExplorationUtil.SamplingMode.ALL_EVEN_PLUS_RANDOM;
  private int samplesPerProfile = 1;

  private DecisionMode decisionMode = DecisionMode.BR;

  // The maximum number of iterations to run.
  private int maxIteration;

  // Parameterize the space by predicting the RD equilibrium with probability (1-delta)
  // and the uniform distribution with probability delta
  private double delta;

  // whether or not this solver is stochastic or not (use for fully-observable benchmark case)
  private boolean isStochastic = true;

  private String name;
  private int numPlayers = 0;
  private int[] numActs = null;
  private List<MixedStrategy> strategies;
  private OutcomeDistribution outcomeDistribution = null;
  private MixedStrategy strategy = null;
  private EmpiricalMatrixGame eGame = null;
  private ReplicatorDynamics replicatorDynamics = null;

  public ReplicatorDynamicsSolver() {
    this(DEFAULT_MAX_ITERATIONS, 0d);
  }

  public ReplicatorDynamicsSolver(double delta) {
    this(DEFAULT_MAX_ITERATIONS, delta);
  }

  public ReplicatorDynamicsSolver(int maxIteration, double delta) {
    this.maxIteration = maxIteration;
    this.delta = delta;
    this.name = "RD Solver (" + delta + ")";
    this.replicatorDynamics = new ReplicatorDynamics(maxIteration);
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = getSB();
    sb.append("Replicator Dynamics Solver\n");
    sb.append("Plays according to the equilibrium estimate produced by replicator dynamics\n");
    sb.append("Sampling mode: ").append(samplingMode).append("\n");
    sb.append("Decision mode: ").append(decisionMode).append("\n");
    sb.append("Sample per profile: ").append(samplesPerProfile).append("\n");
    sb.append("Max iterations: ").append(maxIteration).append("\n");
    sb.append("Delta: ").append(delta).append("\n");
    return returnSB(sb);
  }

  public ExplorationUtil.SamplingMode getSamplingMode() {
    return samplingMode;
  }

  public void setSamplingMode(ExplorationUtil.SamplingMode samplingMode) {
    this.samplingMode = samplingMode;
  }

  public int getSamplesPerProfile() {
    return samplesPerProfile;
  }

  public void setSamplesPerProfile(int samplesPerProfile) {
    this.samplesPerProfile = samplesPerProfile;
  }

  public DecisionMode getDecisionMode() {
    return decisionMode;
  }

  public void setDecisionMode(DecisionMode decisionMode) {
    this.decisionMode = decisionMode;
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

  public int getMaxIteration() {
    return maxIteration;
  }

  public void setMaxIteration(int maxIteration) {
    this.maxIteration = maxIteration;
    replicatorDynamics.setMaxIteration(maxIteration);
  }

  public double getDelta() {
    return delta;
  }

  public void setDelta(double delta) {
    this.delta = delta;
    this.name = "RD Solver (" + delta + ")";
  }

  public OutcomeDistribution predictOutcome(EmpiricalMatrixGame emg) {
    initialize(emg, 0);
    strategies = replicatorDynamics.run(eGame);
    outcomeDistribution.setMixedStrategies(strategies);
    if (delta > 0) {
      outcomeDistribution.mixWithUniform(delta);
    }
    return outcomeDistribution;
  }

  public OutcomeDistribution predictOutcome(GameObserver gameObs) {
    initialize(gameObs, 0);
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, 0, samplesPerProfile);
    strategies = replicatorDynamics.run(eGame);
    outcomeDistribution.setMixedStrategies(strategies);
    if (delta > 0) {
      outcomeDistribution.mixWithUniform(delta);
    }
    return outcomeDistribution;
  }

  /**
   * @param emg    Empirical game observation
   * @param player The player to select an action for.
   * @return the mixed strategy to play
   */
  public MixedStrategy solveGame(EmpiricalMatrixGame emg, int player) {
    initialize(emg, player);
    computeStrategy(player);
    return strategy;
  }

  /**
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   * @return the mixed strategy to play
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    initialize(gameObs, player);
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, player, samplesPerProfile);
    computeStrategy(player);
    return strategy;
  }

  private void computeStrategy(int player) {
    strategies = replicatorDynamics.run(eGame);

    switch (decisionMode) {
      case RAW:
        strategy.setProbs(strategies.get(player).getProbs());
        if (delta > 0) {
          strategy.mixWithUniform(delta);
        }
        break;
      case BR:
        outcomeDistribution.setMixedStrategies(strategies);
        if (delta > 0) {
          outcomeDistribution.mixWithUniform(delta);
        }
        double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, outcomeDistribution, false);
        strategy.setBestResponse(stratPayoffs);
        break;
      default:
        new RuntimeException("Error in replicator dynamics solver: Unknown decision mode: " + decisionMode);
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
    outcomeDistribution = new OutcomeDistribution(numActs);
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   *
   * @param emg    obervation of the game
   * @param player the player
   */
  private void initialize(EmpiricalMatrixGame emg, int player) {
    numPlayers = emg.getNumPlayers();
    numActs = emg.getNumActions().clone();
    strategy = new MixedStrategy(numActs[player], 0d);
    eGame = new EmpiricalMatrixGame(emg);
    outcomeDistribution = new OutcomeDistribution(numActs);
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
}
