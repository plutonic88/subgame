package solvers;

import java.util.Arrays;
import java.util.Set;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;
import subgame.EGAUtils;
import support.ProfileInfo;

/**
 * A set of tools for sampling the payoff matrix, analyzing the stability of profiles,
 * and selecting strategies based on this analysis
 */

public class IncrementalRandomSampling implements GameSolver, GameOutcomePredictor, IncrementalGameOutcomePredictor {

  private String name;

  public enum DecisionMode {
    ENS, LES, RDS,
    STABILITY_RANDOM, STABILITY_MIN_EPSILON, STABILITY_MAX_TESTED,
    STABILITY_MAX_AVE_PAYOFF, STABILITY_MAX_MIN_PAYOFF
  }

  private IncrementalRandomSampling.DecisionMode decisionMode = IncrementalRandomSampling.DecisionMode.STABILITY_RANDOM;

  private EpsilonNashSolver ens = null;
  private ReplicatorDynamicsSolver rds = null;
  private QRESolver les = null;

  // whether or not this solver is stochastic or not (use for fully-observable benchmark case)
  private boolean isStochastic = true;

  /**
   * Saved state; much of this is necessary to continue with additional observations
   */
  private int nPlayers = 0;
  private int[] nActs = null;
  private MixedStrategy strategy = null;
  private EmpiricalMatrixGame eGame = null;
  private IncrementalStabilityAnalysis stabilityAnalysis;
  private OutcomeDistribution predictedOutcome = null;
  private boolean confirmedEquilibrium; // whether or not we have confirmed an equilibrium yet
  private int samplesToConfirmEquilibrium; // number of samples necessary to confirm an equilibrium

  public IncrementalRandomSampling() {
    updateName();
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = EGAUtils.getSB();
    sb.append("IncrementalRandomSampling\n");
    sb.append("Decision mode: ").append(decisionMode).append("\n");
    return EGAUtils.returnSB(sb);
  }

  public DecisionMode getDecisionMode() {
    return decisionMode;
  }

  public void setDecisionMode(DecisionMode decisionMode) {
    this.decisionMode = decisionMode;
    switch (decisionMode) {
      case ENS:
        ens = new EpsilonNashSolver();
        break;
      case RDS:
        rds = new ReplicatorDynamicsSolver();
        break;
      case LES:
        les = new QRESolver();
        break;
      default:
        // do nothing
    }
  }

  public EpsilonNashSolver getEns() {
    return ens;
  }

  public ReplicatorDynamicsSolver getRds() {
    return rds;
  }

  public QRESolver getLes() {
    return les;
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

  private void updateName() {
    name = "RS";
  }

  public int getSamplesToConfirmEquilibrium() {
    return samplesToConfirmEquilibrium;
  }

  public OutcomeDistribution predictOutcome(GameObserver gameObs) {
    // set up all of the necessary data structures
    initialize(gameObs);
    computeOutcomePrediction(gameObs);
    return predictedOutcome;
  }

  public OutcomeDistribution incrementalPredictOutcome(GameObserver gameObs) {
    if (eGame == null) {
      initialize(gameObs);
    }
    computeOutcomePrediction(gameObs);
    return predictedOutcome;
  }

  /**
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   * @return a mixed strategy to play
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    // set up all of the necessary data structures
    initialize(gameObs);
    computeOutcomePrediction(gameObs);
    computeStrategy(player);
    return strategy;
  }

  /**
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   * @return a mixed strategy to play
   */
  public MixedStrategy incrementalSolveGame(GameObserver gameObs, int player) {
    if (eGame == null) {
      initialize(gameObs);
    }
    computeOutcomePrediction(gameObs);
    computeStrategy(player);
    return strategy;
  }

  // select the best response to the current outcome prediction
  private void computeStrategy(int player) {
    strategy = new MixedStrategy(nActs[player], 0d);
    double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, predictedOutcome, false);
    strategy.setBestResponse(stratPayoffs);
//    System.out.println("Predicted outcome: " + predictedOutcome);
//    System.out.println("Strategy: " + strategy);
  }

  private void computeOutcomePrediction(GameObserver gameObs) {

    //System.out.println("Computing outcome prediction. Samples: " + gameObs.getNumObs());

    int nProfiles = eGame.getNumProfiles();
    int[] randomProfile = new int[nPlayers];
    while (gameObs.numObsLeft() > 0 &&
           eGame.getTotalNumSamples() < nProfiles) {

      // randomly select an unsampled profile
      eGame.getRandomProfile(randomProfile);
      while (eGame.getNumSamples(randomProfile) > 0) {
        eGame.getRandomProfile(randomProfile);
      }

      sampleAndUpdate(randomProfile, gameObs);
      //System.out.println("Randomly sampled profile: " + Arrays.toString(randomProfile));

      // record when we have found an equilibrium
      if (!confirmedEquilibrium &&
          stabilityAnalysis.getConfirmedEquilibrium()) {
        confirmedEquilibrium = true;
        samplesToConfirmEquilibrium = eGame.getTotalNumSamples();
      }
    }

    switch (decisionMode) {
      case ENS:
        predictedOutcome = ens.predictOutcome(eGame);
        break;
      case RDS:
        predictedOutcome = rds.predictOutcome(eGame);
        break;
      case LES:
        predictedOutcome = les.predictOutcome(eGame);
        break;
      case STABILITY_RANDOM:
      case STABILITY_MIN_EPSILON:
      case STABILITY_MAX_TESTED:
      case STABILITY_MAX_AVE_PAYOFF:
      case STABILITY_MAX_MIN_PAYOFF:
        stableSetPredictions();
        break;
      default:
        throw new RuntimeException("Invalid decisionMode: " + decisionMode);
    }

    //System.out.println("Predicted outcome: " + predictedOutcome);
    //System.out.println("Done with this iteration; no more samples.");
    //System.out.println("Samples: " + gameObs.getNumObs());
  }

  private void stableSetPredictions() {
    if (confirmedEquilibrium) {
      predictedOutcome.setPureOutcome(stabilityAnalysis.getMostStableProfile(eGame.getNumPossibleDeviations()).outcome);
      return;
    }

    double bestEpsilon = stabilityAnalysis.getMinEpsilonBound(0);
    Set<ProfileInfo> equilibriumCandidates =
            stabilityAnalysis.getEpsilonBoundedProfileSet(0, Math.max(0, bestEpsilon));
    ProfileInfo pi = null;

    if (equilibriumCandidates.size() <= 0) {
      System.out.println("WARNING: no equilibrium candidates! (IRS)");
      predictedOutcome.setCentroid();
      return;
    }

    switch (decisionMode) {
      case STABILITY_RANDOM:
        pi = SelectCandidateEquilibrium.selectRandom(equilibriumCandidates);
        break;
      case STABILITY_MIN_EPSILON:
        pi = SelectCandidateEquilibrium.selectMinEpsilonBound(equilibriumCandidates);
        break;
      case STABILITY_MAX_TESTED:
        pi = SelectCandidateEquilibrium.selectMaxDeviationsTested(equilibriumCandidates);
        break;
      case STABILITY_MAX_AVE_PAYOFF:
        pi = SelectCandidateEquilibrium.selectHighestAvePayoff(equilibriumCandidates);
        break;
      case STABILITY_MAX_MIN_PAYOFF:
        pi = SelectCandidateEquilibrium.selectHighestMinPayoff(equilibriumCandidates);
        break;
      default:
        throw new RuntimeException("Invalid decisionMode: " + decisionMode);
    }

    if (pi == null) {
      System.out.println("WARNING: NULL candidate selection");
      predictedOutcome.setCentroid();
      return;
    }

    if (predictedOutcome == null) {
      System.out.println("NULL predicted outcome! (IRS)");
      predictedOutcome = new OutcomeDistribution(nActs);
    }

    predictedOutcome.setPureOutcome(pi.outcome);
  }

  // gets the sample and updates the analysis
  private void sampleAndUpdate(int[] profileToSample, GameObserver gameObs) {
    //System.out.println("Sampling profile: " + Arrays.toString(profileToSample));
    double[] samplePayoffs = gameObs.getSample(profileToSample);
    eGame.addSample(profileToSample, samplePayoffs);
    stabilityAnalysis.updateOutcome(profileToSample, eGame);
  }

  public void initialize(GameObserver gameObs) {
    // check to see if we are set up for a game of this dimension
    if (eGame != null &&
        gameObs.getNumPlayers() == nPlayers &&
        Arrays.equals(gameObs.getNumActions(), nActs)) {
      reset(gameObs);
    } else {
      initialSetup(gameObs);
    }
  }

  // first time setup; declare new data structures
  public void initialSetup(GameObserver gameObs) {
    nPlayers = gameObs.getNumPlayers();
    nActs = gameObs.getNumActions().clone();

    eGame = new EmpiricalMatrixGame(nPlayers, nActs);
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());
    stabilityAnalysis = new IncrementalStabilityAnalysis(eGame);
    predictedOutcome = new OutcomeDistribution(nActs);
    confirmedEquilibrium = false;
    samplesToConfirmEquilibrium = -1;
  }

  // reset the data structures to analyze a new game
  public void reset(GameObserver gameObs) {
    eGame.clear();
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());
    stabilityAnalysis = new IncrementalStabilityAnalysis(eGame);
    predictedOutcome.setCentroid();
    confirmedEquilibrium = false;
    samplesToConfirmEquilibrium = -1;
  }

}
