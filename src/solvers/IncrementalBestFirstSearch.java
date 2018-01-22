package solvers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import deviation.AvePayoffDeviationTestOrder;
import deviation.DeviationTestOrder;
import deviation.RandomDeviationTestOrder;
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

public class IncrementalBestFirstSearch implements GameSolver, GameOutcomePredictor, IncrementalGameOutcomePredictor {

  private String name;

  public enum DecisionMode {
    LES, ENS, RDS,
    STABILITY_RANDOM, STABILITY_MIN_EPSILON, STABILITY_MAX_TESTED,
    STABILITY_MAX_AVE_PAYOFF, STABILITY_MAX_MIN_PAYOFF
  }

  public enum CandidateOrdering {
    STABILITY_RANDOM, STABILITY_MIN_EPSILON, STABILITY_MAX_TESTED,
    STABILITY_MAX_AVE_PAYOFF, STABILITY_MAX_MIN_PAYOFF
  }

  public enum DeviationOrdering {
    RANDOM, BEST_AVE_PAYOFF
  }

  private EpsilonNashSolver ens = null;
  private ReplicatorDynamicsSolver rds = null;
  private QRESolver les = null;

  private DecisionMode decisionMode = DecisionMode.STABILITY_RANDOM;
  private CandidateOrdering candidateOrdering = CandidateOrdering.STABILITY_RANDOM;
  private DeviationOrdering deviationOrdering = DeviationOrdering.RANDOM;

  // whether or not this solver is stochastic or not (use for fully-observable benchmark case)
  private boolean isStochastic = true;

  /**
   * Saved state; much of this is necessary to continue with additional observations
   */
  private int nPlayers = 0;
  private int[] nActs = null;
  private MixedStrategy strategy = null;
  private EmpiricalMatrixGame eGame = null;
  private IncrementalStabilityAnalysis stabilityAnalysis = null;
  private OutcomeDistribution predictedOutcome = null;
  private boolean confirmedEquilibrium; // indicates that we have found an equilibrium
  private int samplesToConfirmEquilibrium; // number of samples necessary to confirm an equilibrium
  private List<int[]> potentialDeviations = new ArrayList<int[]>();
  private DeviationTestOrder deviationTestOrder = new RandomDeviationTestOrder();

  public IncrementalBestFirstSearch() {
    updateName();
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = EGAUtils.getSB();
    sb.append("IncrementalBestFirstSearch\n");
    sb.append("Decision Mode: ").append(decisionMode).append("\n");
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

  public CandidateOrdering getCandidateOrdering() {
    return candidateOrdering;
  }

  public void setCandidateOrdering(CandidateOrdering candidateOrdering) {
    this.candidateOrdering = candidateOrdering;
  }

  public DeviationOrdering getDeviationOrdering() {
    return deviationOrdering;
  }

  public void setDeviationOrdering(DeviationOrdering deviationOrdering) {
    this.deviationOrdering = deviationOrdering;
    deviationTestOrder = null;
    switch (deviationOrdering) {
      case RANDOM:
        deviationTestOrder = new RandomDeviationTestOrder();
        break;
      case BEST_AVE_PAYOFF:
        deviationTestOrder = new AvePayoffDeviationTestOrder();
        break;
      default:
        throw new RuntimeException("Invalid deviationOrdering: " + deviationOrdering);
    }
  }

  public DeviationTestOrder getDeviationTestOrder() {
    return deviationTestOrder;
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
    name = "BFS";
  }

  public int getSamplesToConfirmEquilibrium() {
    return samplesToConfirmEquilibrium;
  }

  public OutcomeDistribution predictOutcome(GameObserver gameObs) {
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

    // very first sample; just pick a random profile
    if (eGame.getTotalNumSamples() == 0 && gameObs.numObsLeft() > 0) {
      int[] randomProfile = eGame.getRandomProfile();
      sampleAndUpdate(randomProfile, gameObs);
      //System.out.println("Randomly sampled first profile: " + Arrays.toString(randomProfile));
    }

    // sample using BFS ordering
    int nProfiles = eGame.getNumProfiles();
    while (gameObs.numObsLeft() > 0 &&
           eGame.getTotalNumSamples() < nProfiles) {

      int[] candidateProfile = selectCandidate();
      //System.out.println("Candidate profile: " + Arrays.toString(candidateProfile));

      int[] deviationToSample = selectDeviation(candidateProfile);

      // no valid deviation from a candidate; sample a random profile
      if (deviationToSample == null) {
        //System.out.println("No valid candidate deviations; selecting RANDOMLY!");
        deviationToSample = new int[nPlayers];
        eGame.getRandomProfile(deviationToSample);
        while (eGame.getNumSamples(deviationToSample) > 0) {
          eGame.getRandomProfile(deviationToSample);
        }
      }

      //System.out.println("Profile to sample: " + Arrays.toString(deviationToSample));
      sampleAndUpdate(deviationToSample, gameObs);

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
        throw new RuntimeException("Invalid decision mode: " + decisionMode);
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
      System.out.println("WARNING: no equilibrium candidates! (BFS)");
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
      System.out.println("WARNING: pi == null! (BFS): " + decisionMode + " " + equilibriumCandidates.size());
      predictedOutcome.setCentroid();
      return;
    }

    if (predictedOutcome == null) {
      System.out.println("NULL predicted outcome! (BFS)");
      predictedOutcome = new OutcomeDistribution(nActs);
    }

    predictedOutcome.setPureOutcome(pi.outcome);
  }

  // selects the next deviation to sample
  // null if no unsampled deviations; in this case we have a confirmed equilibrium
  private int[] selectDeviation(int[] candidateProfile) {
    //System.out.println("CANDIDATE profile: " + Arrays.toString(candidateProfile));

    List<Point> order = deviationTestOrder.getDeviationTestOrder(candidateProfile);
    for (Point anOrder : order) {
      int[] devProfile = candidateProfile.clone();
      devProfile[anOrder.x] = anOrder.y;
      //System.out.println("deviation: " + Arrays.toString(devProfile));
      if (eGame.getNumSamples(devProfile) == 0) {
        //System.out.println("SELECTED deviation: " + Arrays.toString(devProfile));
        return devProfile;
      }
    }
    return null;
  }

  private int[] selectCandidate() {
    double bestEpsilon = stabilityAnalysis.getMinEpsilonCandidate(0);

    //System.out.println("best candidate epsilon: " + bestEpsilon);

    Set<ProfileInfo> equilibriumCandidates =
            stabilityAnalysis.getEpsilonBoundedCandidateProfiles(0, Math.max(0, bestEpsilon));
    ProfileInfo pi = null;

    if (equilibriumCandidates.size() <= 0) {
      System.out.println("WARNING: no equilibrium candidates! (IRS)");
      return stabilityAnalysis.getMostStableCandidate(0).outcome;
    }

//    if (equilibriumCandidates.size() > 1) {
//    System.out.println("Selecting candidate BFS");
//    System.out.println("ordering: " + candidateOrdering);
//    for (ProfileInfo cand : equilibriumCandidates) {
//      System.out.println("candidate: " + Arrays.toString(cand.outcome) + " max benefit: " + cand.maxBenefit +
//       " tested: " + cand.numDeviationsSampled + " payoffs: " + Arrays.toString(cand.payoffs));
//    }
//    }

    switch (candidateOrdering) {
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
        throw new RuntimeException("Invalid candidateOrdering: " + candidateOrdering);
    }

//    if (equilibriumCandidates.size() > 1) {
//    System.out.println("Selected: " + Arrays.toString(pi.outcome));
//    }


    if (pi == null) {
      System.out.println(
              "WARNING: pi == null! (BFS, select candidate): " + decisionMode + " " + equilibriumCandidates.size());
      System.out.println("best eps: " + bestEpsilon);
      System.out.println("Candidate: " + equilibriumCandidates);
      return stabilityAnalysis.getMostStableCandidate(0).outcome;
    }

    return pi.outcome;
  }

  // gets the sample and updates the analysis
  private void sampleAndUpdate(int[] profileToSample, GameObserver gameObs) {
    //System.out.println("Sampling profile: " + Arrays.toString(profileToSample));
    double[] samplePayoffs = gameObs.getSample(profileToSample);
    eGame.addSample(profileToSample, samplePayoffs);
    stabilityAnalysis.updateOutcome(profileToSample, eGame);
    deviationTestOrder.update(profileToSample, samplePayoffs);
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
    deviationTestOrder.initialize(nPlayers, nActs);
  }

  // reset the data structures to analyze a new game
  public void reset(GameObserver gameObs) {
    eGame.clear();
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());
    stabilityAnalysis = new IncrementalStabilityAnalysis(eGame);
    predictedOutcome.setCentroid();
    confirmedEquilibrium = false;
    samplesToConfirmEquilibrium = -1;
    deviationTestOrder.reset();
  }

}
