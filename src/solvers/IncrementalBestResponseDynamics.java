package solvers;

import java.util.Arrays;
import java.util.List;

import deviation.AvePayoffDeviationTestOrder;
import deviation.DeviationTestOrder;
import deviation.RandomDeviationTestOrder;
import games.DeviationIterator;
import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;
import subgame.EGAUtils;
import util.GenericTensor;

/**
 * A set of tools for sampling the payoff matrix, analyzing the stability of profiles,
 * and selecting strategies based on this analysis
 */

public class IncrementalBestResponseDynamics
        implements GameSolver, GameOutcomePredictor, IncrementalGameOutcomePredictor {

  private static final int NO_BR_FLAG = -1;
  private static final int NO_SAMPLES_FLAG = -2;

  private String name;

  public enum Solver {
    ENS, LES, RDS, CURRENT_PROFILE,
//    STABILITY_RANDOM, STABILITY_MIN_EPSILON, STABILITY_MAX_TESTED,
//    STABILITY_MAX_AVE_PAYOFF, STABILITY_MAX_MIN_PAYOFF
  }

  public enum DeviationOrdering {
    RANDOM, BEST_AVE_PAYOFF
  }

  private IncrementalBestResponseDynamics.Solver finalSolver = IncrementalBestResponseDynamics.Solver.CURRENT_PROFILE;
  private DeviationOrdering deviationOrdering = DeviationOrdering.RANDOM;

  private EpsilonNashSolver ens = null;
  private ReplicatorDynamicsSolver rds = null;
  private QRESolver les = null;

  // improvement required for finding a better response
  private double aspirationLevel = Double.POSITIVE_INFINITY;

  // set a lower bound on the number of deviations tested
  private int minDeviationsTested = 5;

  // whether or not this solver is stochastic or not (use for fully-observable benchmark case)
  private boolean isStochastic = true;

  /**
   * Saved state; much of this is necessary to continue with additional observations
   */
  private int nPlayers = 0;
  private int[] nActs = null;
  private MixedStrategy strategy = null;
  private EmpiricalMatrixGame eGame = null;
  private OutcomeDistribution predictedOutcome = null;
  private int currentPlayer;       // track current player we are finding a BR for
  private int lastPlayerWithBR;    // track last player with a valid BR (stopping condition)
  private int samplesToConfirmEquilibrium; // number of samples necessary to confirm an equilibrium
  private int[] mostStableConfirmed;
  private double confirmedStability;
  private int[] currentOutcome;
  private GenericTensor<Boolean> tabu; // tracks whether the profiles are on the "tabu" list
  private DeviationTestOrder deviationTestOrder = new RandomDeviationTestOrder();

  public IncrementalBestResponseDynamics() {
    updateName();
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = EGAUtils.getSB();
    sb.append("IncrementalBestResponseDynamics\n");
    sb.append("Final Solver: ").append(finalSolver).append("\n");
    return EGAUtils.returnSB(sb);
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

  public IncrementalBestResponseDynamics.Solver getFinalSolver() {
    return finalSolver;
  }

  public void setFinalSolver(IncrementalBestResponseDynamics.Solver finalSolver) {
    this.finalSolver = finalSolver;
    switch (finalSolver) {
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

  public DeviationTestOrder getDeviationTestOrder() {
    return deviationTestOrder;
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

  public double getAspirationLevel() {
    return aspirationLevel;
  }

  public void setAspirationLevel(double aspirationLevel) {
    this.aspirationLevel = aspirationLevel;
  }

  public int getMinDeviationsTested() {
    return minDeviationsTested;
  }

  public void setMinDeviationsTested(int minDeviationsTested) {
    this.minDeviationsTested = minDeviationsTested;
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
    name = "BRD";
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
  public MixedStrategy continueSolveGame(GameObserver gameObs, int player) {
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
  }

  private void computeOutcomePrediction(GameObserver gameObs) {

    // find best responses while we have samples remaining
    // if we find a profile with no BR, restart randomly
    int nProfiles = eGame.getNumProfiles();
    while (gameObs.numObsLeft() > 0 &&
           eGame.getTotalNumSamples() < nProfiles) {

      int newBR = computeBetterResponse(currentPlayer, gameObs);

      // not enough samples to compute the next BR
      if (newBR == NO_SAMPLES_FLAG) {
        break;
      }

      // no BR for this player; try another player
      // if no players are left, we have found an equilibrium
      else if (newBR == NO_BR_FLAG) {
        if (lastPlayerWithBR == currentPlayer) {
          checkStability();
          selectRandomUnexploredProfile(gameObs);
          updateCurrentPlayer();
        } else {
          updateCurrentPlayer();
        }
      }

      // we have found a new BR
      else {
        // flag errors
        if (newBR < 0 || newBR > nActs[currentPlayer]) {
          throw new RuntimeException("Invalid BR in IBRD: " + currentPlayer + " " + newBR);
        }

        // update the current profile and tabu list
        tabu.setValue(true, currentOutcome);
        currentOutcome[currentPlayer] = newBR;
        lastPlayerWithBR = currentPlayer;
        updateCurrentPlayer();
      }
    }

    // update "global" prediction based on all data so far
    updateGlobalOutcomePrediction();

    //System.out.println("Done with this iteration; no more samples.");
    //System.out.println("Samples: " + gameObs.getNumObs());
  }


  // gets the sample and updates the analysis
  private void sampleAndUpdate(int[] profileToSample, GameObserver gameObs) {
    double[] samplePayoffs = gameObs.getSample(profileToSample);
    eGame.addSample(profileToSample, samplePayoffs);
    deviationTestOrder.update(profileToSample, samplePayoffs);
  }

  private void updateCurrentPlayer() {
    if (currentPlayer == nPlayers - 1) {
      currentPlayer = 0;
    } else {
      currentPlayer++;
    }
  }

  // check whether current outcome is an equilibrium 
  private void checkStability() {
    double maxDeviation = Double.NEGATIVE_INFINITY;

    DeviationIterator itr = new DeviationIterator(currentOutcome, nActs);
    while (itr.hasNext()) {
      int[] devOutcome = itr.next();

      // this shouldn't happen
      if (eGame.getNumSamples(devOutcome) <= 0) {
        System.out.println("BRD: checking equilibrium with unsampled deviations!");
        System.out.println("Current: " + Arrays.toString(currentOutcome));
        System.out.println("Unsampled: " + Arrays.toString(devOutcome));
        return;
      }

      double devBenefit = eGame.getPayoff(devOutcome, itr.getDeviatingPlayer()) -
                          eGame.getPayoff(currentOutcome, itr.getDeviatingPlayer());

      maxDeviation = Math.max(maxDeviation, devBenefit);

      // terminate early; not better than one we have already confirmed
      if (maxDeviation >= confirmedStability) return;
    }

    if (maxDeviation < confirmedStability) {
      confirmedStability = maxDeviation;
      mostStableConfirmed = currentOutcome.clone();
      if (samplesToConfirmEquilibrium < 0 &&
          confirmedStability <= 0) {
        samplesToConfirmEquilibrium = eGame.getTotalNumSamples();
      }
    }
  }

  // selects and samples a new profile randomly from the set of unexplored profiles
  private void selectRandomUnexploredProfile(GameObserver gameObs) {
    if (eGame.getTotalNumSamples() >= eGame.getNumProfiles() ||
        gameObs.numObsLeft() <= 0) return;

    eGame.getRandomProfile(currentOutcome);
    while (eGame.getNumSamples(currentOutcome) > 0) {
      eGame.getRandomProfile(currentOutcome);
    }

    sampleAndUpdate(currentOutcome, gameObs);
  }

  // finds a better response to the current outcome for the given player, sampling if necessary
  private int computeBetterResponse(int player, GameObserver go) {
    int[] devOutcome = currentOutcome.clone();
    double aspirationPayoff = eGame.getPayoff(currentOutcome, player) + aspirationLevel;
    double bestPayoff = eGame.getPayoff(currentOutcome, player);
    int bestResponse = currentOutcome[player];

    ///System.out.println("Finding BR: " + Arrays.toString(currentOutcome));

    // get the order in which to test best responses
    List<Integer> order = deviationTestOrder.getDeviationTestOrder(currentOutcome, player);

//    if (order.size() < nActs[player]-1) {
//      System.out.println("To few actions in order listing: " + order.size());
//    }

    int itr = 0;
    for (int a : order) {
      // skip current action
      if (a == currentOutcome[player]) continue;
      devOutcome[player] = a;

      //System.out.println("Testing dev: " + Arrays.toString(devOutcome));

      // check whether this profile is "tabu"
      if (tabu.getValue(devOutcome)) {
        //System.out.println("TABU");
        continue;
      }

      // sample if necessary; flag not enough samples
      if (eGame.getNumSamples(devOutcome) == 0) {
        if (go.numObsLeft() <= 0) return NO_SAMPLES_FLAG;
        sampleAndUpdate(devOutcome, go);
        //System.out.println("SAMPLED");
      }

      // check BR
      double devPayoff = eGame.getPayoff(devOutcome, player);

//      if (devPayoff > aspirationPayoff) {
//        //System.out.println("Aspiration met.");
//        return a;
//      } else

      if (devPayoff > bestPayoff) {
        //System.out.println("Br, but not aspiration level");
        bestPayoff = devPayoff;
        bestResponse = a;
      }

      itr++;
      if (itr > minDeviationsTested &&
          bestPayoff > aspirationPayoff) {
        return bestResponse;
      }

    }

    if (bestResponse != currentOutcome[player]) {
      //System.out.println("return non-aspiration BR");
      return bestResponse;
    }
    //System.out.println("NO BR");
    return NO_BR_FLAG;
  }

  private void updateGlobalOutcomePrediction() {

    switch (finalSolver) {
      case ENS:
        predictedOutcome = ens.predictOutcome(eGame);
        break;
      case RDS:
        predictedOutcome = rds.predictOutcome(eGame);
        break;
      case LES:
        predictedOutcome = les.predictOutcome(eGame);
        break;
      case CURRENT_PROFILE:
        if (mostStableConfirmed != null) {
          predictedOutcome.setPureOutcome(mostStableConfirmed);
        } else {
          predictedOutcome.setPureOutcome(currentOutcome);
        }
        break;
      default:
        throw new RuntimeException("Invalid final solver: " + finalSolver);
    }

    //System.out.println("Updated global outcome prediction:");
    //System.out.println(predictedOutcome);
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
    currentPlayer = 0;
    lastPlayerWithBR = nPlayers - 1;
    samplesToConfirmEquilibrium = -1;
    mostStableConfirmed = null;
    confirmedStability = Double.POSITIVE_INFINITY;
    currentOutcome = new int[nPlayers];
    predictedOutcome = new OutcomeDistribution(nActs);
    tabu = new GenericTensor<Boolean>(nActs);
    for (int i = 0; i < tabu.size(); i++) {
      tabu.setValue(false, i);
    }
    deviationTestOrder.initialize(nPlayers, nActs);

    // this must be last
    selectRandomUnexploredProfile(gameObs);
  }

  // reset the data structures to analyze a new game
  public void reset(GameObserver gameObs) {
    eGame.clear();
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());
    currentPlayer = 0;
    lastPlayerWithBR = nPlayers - 1;
    samplesToConfirmEquilibrium = -1;
    mostStableConfirmed = null;
    confirmedStability = Double.POSITIVE_INFINITY;
    predictedOutcome = new OutcomeDistribution(nActs);
    for (int i = 0; i < tabu.size(); i++) {
      tabu.setValue(false, i);
    }
    deviationTestOrder.reset();

    // this must be last
    selectRandomUnexploredProfile(gameObs);
  }

}
