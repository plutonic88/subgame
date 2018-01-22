package solvers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import observers.GameObserver;
import subgame.EGAUtils;
import support.ProfileInfo;

/**
 * A set of tools for sampling the payoff matrix, analyzing the stability of profiles,
 * and selecting strategies based on this analysis
 */

public class IncrementalSubgameAnalysis implements GameSolver, GameOutcomePredictor, IncrementalGameOutcomePredictor {

  // this can be used to ignore the contribution of outcomes with very low probability in the
  // best response computation (which may save both time and samples)
  private static final double BR_PROB_LOWER_BOUND = 0;

  private static final int NO_BR_FLAG = -1;
  private static final int NO_SAMPLES_FLAG = -2;

  private String name;

  // TODO: I am somewhat concerned about using the "default" payoffs model for structured games, since
  // TODO: this is no longer correct; try something else?
  public enum FinalData {
    FULL, SUBGAME
  }

  public enum Solver {
    ENS, LES, RDS, MOST_STABLE
  }

  private FinalData finalData = FinalData.FULL;
  private Solver interimSolver = Solver.LES;
  private Solver finalSolver = Solver.LES;

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
  private OutcomeDistribution currentSubgameOutcome = null;
  private OutcomeDistribution predictedOutcome = null;
  private List<int[]> subgameMembers;
  private List<List<Integer>> subgameLists;
  private int currentPlayer;       // track current player we are finding a BR for
  private int lastPlayerWithBR;    // track last player with a valid BR (stopping condition)
  private OutcomeDistribution mostStableConfirmed;
  private double confirmedStability;
  private int samplesToConfirmEquilibrium; // number of samples necessary to confirm an equilibrium
  private boolean terminatedSamplingSubgame; // flag indicates we ran out of samples after finding a BR
  private int[] startingProfile;

  private int maxSubgameSize;
  private int initialSubgameSize = 1;
  private int maxSubgameIncreaseRate = 2; // number of BR iterators for each player before increasing
  private int iteration = 0;

  private IncrementalStabilityAnalysis stabilityAnalysis;
  private boolean preferPureInterimEquilibria = true;

  public IncrementalSubgameAnalysis() {
    updateName();
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = EGAUtils.getSB();
    sb.append("IncrementalSubgameAnalysis\n");
    sb.append("Interim Solver: ").append(interimSolver).append("\n");
    sb.append("Final Solver: ").append(finalSolver).append("\n");
    sb.append("Final Data: ").append(finalData).append("\n");
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

  public FinalData getFinalData() {
    return finalData;
  }

  public void setFinalData(FinalData finalData) {
    this.finalData = finalData;
  }

  public Solver getInterimSolver() {
    return interimSolver;
  }

  public void setInterimSolver(Solver interimSolver) {
    this.interimSolver = interimSolver;
    switch (interimSolver) {
      case ENS:
        if (ens == null) {
          ens = new EpsilonNashSolver();
        }
        break;
      case RDS:
        if (rds == null) {
          rds = new ReplicatorDynamicsSolver();
        }
        break;
      case LES:
      case MOST_STABLE:
        if (les == null) {
          les = new QRESolver();
        }
        break;
      default:
        // do nothing
    }
  }

  public Solver getFinalSolver() {
    return finalSolver;
  }

  public void setFinalSolver(Solver finalSolver) {
    this.finalSolver = finalSolver;
    switch (finalSolver) {
      case ENS:
        if (ens == null) {
          ens = new EpsilonNashSolver();
        }
        break;
      case RDS:
        if (rds == null) {
          rds = new ReplicatorDynamicsSolver();
        }
        break;
      case LES:
      case MOST_STABLE:
        if (les == null) {
          les = new QRESolver();
        }
        break;
      default:
        // do nothing
    }
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
    name = "ISA";
  }

  public int getSamplesToConfirmEquilibrium() {
    return samplesToConfirmEquilibrium;
  }

  public int getInitialSubgameSize() {
    return initialSubgameSize;
  }

  public void setInitialSubgameSize(int initialSubgameSize) {
    this.initialSubgameSize = initialSubgameSize;
  }

  public int getMaxSubgameIncreaseRate() {
    return maxSubgameIncreaseRate;
  }

  public void setMaxSubgameIncreaseRate(int maxSubgameIncreaseRate) {
    this.maxSubgameIncreaseRate = maxSubgameIncreaseRate;
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

    // if the last iteration terminated sampling the subgame, finish and update the outcome
    if (terminatedSamplingSubgame) {
      //System.out.println("Filling in terminated subgame.");
      sampleAndUpdateSubgame(gameObs);
    }

    int nProfiles = eGame.getNumProfiles();
    while (gameObs.numObsLeft() > 0 &&
           eGame.getTotalNumSamples() < nProfiles) {

      // if we have already found the equilibrium, finish by sampling randomly
      if (confirmedStability <= 0) {
        sampleRandomly(gameObs);
        continue;
      }

      // otherwise, try to find a BR and update the subgame
      boolean foundBR = findBR(gameObs);

      // for now, this can go here; may need to move it when adding check for mixtures
      checkStability();

      if (foundBR) {
        if (validSubgame()) {
          sampleAndUpdateSubgame(gameObs);
        } else {
          updateInitialOutcomePrediction();
        }

//        System.out.println("Updated subgame matrix: ");
//        for (int pl = 0; pl < nPlayers; pl++) {
//          System.out.println("Player " + pl);
//          System.out.println(Arrays.toString(subgameMembers.get(pl)));
//          System.out.println(subgameLists.get(pl));
//        }
      }
    }

    // update "global" prediction based on all data so far
    updateGlobalOutcomePrediction();

//    System.out.println("Done with this iteration; no more samples.");
//    System.out.println("Samples: " + gameObs.getNumObs());
  }

  private boolean validSubgame() {
    for (int pl = 0; pl < nPlayers; pl++) {
      if (subgameLists.get(pl).size() <= 0) return false;
    }
    return true;
  }

  private void updateCurrentPlayer() {
    if (currentPlayer == nPlayers - 1) {
      currentPlayer = 0;
      iteration++;
      if (iteration % maxSubgameIncreaseRate == 0) {
        maxSubgameSize++;
      }
    } else {
      currentPlayer++;
    }
    //System.out.println("Updated player: " + currentPlayer + " iteration: " + iteration + " subgamesize: " + maxSubgameSize);
  }

  private void sampleRandomly(GameObserver gameObs) {
    int nProfiles = eGame.getNumProfiles();
    int[] randomProfile = new int[nPlayers];

    //System.out.println("Sampling randomly; equilibrium found.");
    while (gameObs.numObsLeft() > 0 &&
           eGame.getTotalNumSamples() < nProfiles) {

      // randomly select an unsampled profile
      eGame.getRandomProfile(randomProfile);
      while (eGame.getNumSamples(randomProfile) > 0) {
        eGame.getRandomProfile(randomProfile);
      }

      sampleAndUpdate(randomProfile, gameObs);
      //System.out.println("Randomly sampled profile: " + Arrays.toString(randomProfile));
    }
  }

  private boolean findBR(GameObserver gameObs) {
    while (true) {
      int newBR = computeBestResponse(currentPlayer, currentSubgameOutcome, gameObs);
      //System.out.println("New BR computed. pl: " + currentPlayer + " br: " + newBR);

      // not enough samples to compute the next BR
      if (newBR == NO_SAMPLES_FLAG) {
        //System.out.println("BR: Not enough samples.");
        return false;
      }

      // no BR for this player; try another player
      // if no players are left, we have found an equilibrium
      else if (newBR == NO_BR_FLAG) {
        //System.out.println("No BR for player: " + currentPlayer);
        if (lastPlayerWithBR == currentPlayer) {
          //System.out.println("No player with BR; EQUILIBRIUM FOUND.");
          confirmedStability = 0d;
          mostStableConfirmed = new OutcomeDistribution(currentSubgameOutcome);
          samplesToConfirmEquilibrium = eGame.getTotalNumSamples();
          return false;
        } else {
          updateCurrentPlayer();
        }
      }

      // we have found a new BR
      else {
        // flag errors
        if (newBR < 0 || newBR > nActs[currentPlayer]) {
          throw new RuntimeException("Invalid BR in ISA: " + currentPlayer + " " + newBR);
        }

        //System.out.println("Found BR: " + newBR);
        addBRtoSubgame(currentPlayer, newBR);
        lastPlayerWithBR = currentPlayer;
        updateCurrentPlayer();
        return true;
      }
    }
  }

  // checks to see if we have found a more stable outcome
  private void checkStability() {
    // check all confirmed pure strategy outcomes
    ProfileInfo mostStablePure = stabilityAnalysis.getMostStableProfile(eGame.getNumPossibleDeviations());
    if (mostStablePure != null &&
        mostStablePure.maxBenefit < confirmedStability) {
      if (mostStableConfirmed == null) {
        mostStableConfirmed = new OutcomeDistribution(nActs);
      }
      mostStableConfirmed.setPureOutcome(mostStablePure.outcome);

      if (confirmedStability > 0 && mostStablePure.maxBenefit <= 0) {
        samplesToConfirmEquilibrium = eGame.getTotalNumSamples();
      }
      confirmedStability = mostStablePure.maxBenefit;
    }
  }

  // adds a new best-response to the subgame, accounting for size restrictions
  private void addBRtoSubgame(int player, int action) {
    List<Integer> playerList = subgameLists.get(player);

    // if we are at the limit, remove the first subgame strategy added
    if (playerList.size() >= maxSubgameSize) {
      //System.out.println("Subgame too large, removing earliest action: " + playerList.get(0));
      subgameMembers.get(player)[playerList.get(0)] = 0;
      playerList.remove(0);
    }

    // add the new action to the end of the subgame list
    subgameMembers.get(player)[action] = 1;
    playerList.add(action);
  }

  // samples the subgame and updates the prediction
  private void sampleAndUpdateSubgame(GameObserver gameObs) {
    if (SolverUtils.sampleSubgameToMinimum(gameObs, eGame, 1, subgameLists)) {
      updateSubgameOutcomePrediction();
      terminatedSamplingSubgame = false;
    } else {
      terminatedSamplingSubgame = true;
    }
  }

  // gets the sample and updates the analysis
  private void sampleAndUpdate(int[] profileToSample, GameObserver gameObs) {
    //System.out.println("Sampling profile: " + Arrays.toString(profileToSample));
    double[] samplePayoffs = gameObs.getSample(profileToSample);
    eGame.addSample(profileToSample, samplePayoffs);
    stabilityAnalysis.updateOutcome(profileToSample, eGame);
  }

  // updates the predicted outcome before we have a valid subgame defined
  private void updateInitialOutcomePrediction() {
    int[] tmpOutcome = startingProfile.clone();
    for (int pl = 0; pl < nPlayers; pl++) {
      if (subgameLists.get(pl).size() > 0) {
        tmpOutcome[pl] = subgameLists.get(pl).get(0);
      }
    }
    currentSubgameOutcome.setPureOutcome(tmpOutcome);
    //System.out.println("Updated INITIAL outcome prediction: " + Arrays.toString(tmpOutcome));
  }

  // updates the predicted outcome for the current subgame
  private void updateSubgameOutcomePrediction() {

    // create a new game representing the current subgame
    int[] newNumActs = new int[nPlayers];
    int maxNumActs = 0;
    for (int pl = 0; pl < nPlayers; pl++) {
      newNumActs[pl] = subgameLists.get(pl).size();
      maxNumActs = Math.max(maxNumActs, newNumActs[pl]);
    }

    // if there is only one outcome, just choose the outcome
    if (maxNumActs == 1) {
      int[] tmpOutcome = new int[nPlayers];
      for (int pl = 0; pl < nPlayers; pl++) {
        tmpOutcome[pl] = subgameLists.get(pl).get(0);
      }
      currentSubgameOutcome.setPureOutcome(tmpOutcome);
      return;
    }

    // create the temporary subgame data structure
    EmpiricalMatrixGame subGame = new EmpiricalMatrixGame(eGame, subgameLists, newNumActs);

    // first, do a preliminary screen to see if we can find a pure equilibrium of the subgame
    // this should both reduce the cost of BR computations and speed up this step significantly
    if (preferPureInterimEquilibria) {
      IncrementalStabilityAnalysis subGameStability = new IncrementalStabilityAnalysis(subGame, 1);
      subGameStability.updateAllOutcomes(subGame);

      Set<ProfileInfo> equilibriumCandidates =
              subGameStability.getEpsilonBoundedProfileSet(0, 0d);

      ProfileInfo pi = null;
      if (equilibriumCandidates.size() > 0) {
        pi = SelectCandidateEquilibrium.selectHighestAvePayoff(equilibriumCandidates);
      }

      if (pi != null) {

        int[] fullGameOutcome = new int[nPlayers];
        for (int pl = 0; pl < nPlayers; pl++) {
          fullGameOutcome[pl] = subgameLists.get(pl).get(pi.outcome[pl] - 1);
        }
        currentSubgameOutcome.setPureOutcome(fullGameOutcome);
        return;
      }
    }


    OutcomeDistribution tmpSubgameOutcome;
    switch (interimSolver) {
      case ENS:
        tmpSubgameOutcome = ens.predictOutcome(subGame);
        break;
      case RDS:
        tmpSubgameOutcome = rds.predictOutcome(subGame);
        break;
      case LES:
        tmpSubgameOutcome = les.predictOutcome(subGame);
        break;
      case MOST_STABLE:
        double tmpLambda = les.getLambda();
        // TODO: FIX THIS (make more general)
        les.setLambda(1000);
        tmpSubgameOutcome = les.predictOutcome(subGame);
        les.setLambda(tmpLambda);
        break;
      default:
        throw new RuntimeException("Invalid interim solver: " + interimSolver);
    }

    currentSubgameOutcome.setAll(0);
    int[] fullGameOutcome = new int[nPlayers];
    OutcomeIterator itr = tmpSubgameOutcome.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      double prob = tmpSubgameOutcome.getProb(outcome);

      for (int pl = 0; pl < nPlayers; pl++) {
        fullGameOutcome[pl] = subgameLists.get(pl).get(outcome[pl] - 1);
      }
      currentSubgameOutcome.setProb(fullGameOutcome, prob);
    }

    //System.out.println("Updated subgame outcome prediction:");
    //System.out.println(currentSubgameOutcome);
  }

  private void updateGlobalOutcomePrediction() {
    if (finalSolver == Solver.MOST_STABLE) {
      if (confirmedStability <= 0) {
        predictedOutcome = mostStableConfirmed;
      } else {
        predictedOutcome = currentSubgameOutcome;
      }
    }

    // just run the solver on the final game matrix
    if (finalData == FinalData.FULL) {
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
        default:
          throw new RuntimeException("Invalid final solver: " + finalSolver);
      }
    }

    // we have already solved for this and mapped it into a full game outcome; just copy it over
    else if (finalData == FinalData.SUBGAME) {

      // update one more time if we need to use a different solver for the final analysis
      if (finalSolver != interimSolver) {
        Solver tmp = interimSolver;
        interimSolver = finalSolver;
        updateSubgameOutcomePrediction();
        interimSolver = tmp;
      }
      predictedOutcome = currentSubgameOutcome;
    }

//    System.out.println("Updated global outcome prediction:");
//    System.out.println(predictedOutcome);
  }

  /**
   * Compute the best response for the player, sampling as necessary
   * Only strategies not in the subgame already are considered
   * Returns -1 if there is no beneficial deviation
   * Returns -2 if we run out of samples
   *
   * @param player player
   * @param od     distribution over outcomes
   * @param go     the game observer
   * @return the best response; -1 if cannot determine
   */
  private int computeBestResponse(int player, OutcomeDistribution od, GameObserver go) {
    int[] outcome = new int[nPlayers];
    double[] payoffs = new double[nActs[player] + 1];

    // create the outcome distribution, conditional on this player's choice
    List<Integer> restrictedPlayers = new ArrayList<Integer>();
    restrictedPlayers.add(player);
    OutcomeDistribution conditional = od.getConditionalDistribution(restrictedPlayers);

    // loop over all of the restricted outcomes, averaging the outcomes
    OutcomeIterator itr = conditional.iterator();
    while (itr.hasNext()) {
      int[] conditionalOutcome = itr.next();
      double prob = conditional.getProb(conditionalOutcome);
      if (prob <= BR_PROB_LOWER_BOUND) continue;

      // create the non-marginal outcomes to get the payoffs
      for (int pl2 = 0; pl2 < nPlayers; pl2++) {
        if (pl2 < player) outcome[pl2] = conditionalOutcome[pl2];
        else if (pl2 > player) outcome[pl2] = conditionalOutcome[pl2 - 1];
      }

      // average in the payoffs for each possible action
      for (int a = 1; a <= nActs[player]; a++) {
        outcome[player] = a;

        // if necessary, sample the necessary profile
        if (eGame.getNumSamples(outcome) <= 0) {
          if (go.numObsLeft() <= 0) return NO_SAMPLES_FLAG;
          sampleAndUpdate(outcome, go);
          //System.out.println("Sampling: " + Arrays.toString(outcome));
        }
        payoffs[a] += prob * eGame.getPayoffs(outcome)[player];
      }
    }

    // find the best payoff and return that action
    int bestResponse = 1;
    double bestPayoff = payoffs[1];
    for (int a = 2; a <= nActs[player]; a++) {
      if (payoffs[a] > bestPayoff) {
        bestResponse = a;
        bestPayoff = payoffs[a];
      }
    }

//    System.out.println("Computing BR for player " + currentPlayer);
//    System.out.println("Payoffs" + Arrays.toString(payoffs));

    if (subgameMembers.get(player)[bestResponse] == 1) return NO_BR_FLAG;
    return bestResponse;
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   *
   * @param gameObs obervation of the game
   */
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
    currentSubgameOutcome = new OutcomeDistribution(nActs, 0);
    startingProfile = eGame.getRandomProfile();
    currentSubgameOutcome.setPureOutcome(startingProfile);
    currentPlayer = 0;
    lastPlayerWithBR = nPlayers - 1;
    mostStableConfirmed = null;
    confirmedStability = Double.POSITIVE_INFINITY;
    samplesToConfirmEquilibrium = -1;
    terminatedSamplingSubgame = false;

    subgameMembers = new ArrayList<int[]>();
    subgameLists = new ArrayList<List<Integer>>();
    for (int pl = 0; pl < nPlayers; pl++) {
      subgameMembers.add(new int[nActs[pl] + 1]);
      subgameLists.add(new LinkedList<Integer>());
    }

    maxSubgameSize = initialSubgameSize;
    iteration = 0;

    stabilityAnalysis = new IncrementalStabilityAnalysis(eGame);

//    System.out.println("Initial outcome: ");
//    System.out.println(currentSubgameOutcome);
  }

  // reset the data structures to analyze a new game
  public void reset(GameObserver gameObs) {
    eGame.clear();
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());
    startingProfile = eGame.getRandomProfile();
    currentSubgameOutcome.setPureOutcome(startingProfile);
    currentPlayer = 0;
    lastPlayerWithBR = nPlayers - 1;
    mostStableConfirmed = null;
    confirmedStability = Double.POSITIVE_INFINITY;
    samplesToConfirmEquilibrium = -1;
    terminatedSamplingSubgame = false;

    for (int[] actList : subgameMembers) {
      Arrays.fill(actList, 0);
    }
    for (List<Integer> actList : subgameLists) {
      actList.clear();
    }

    maxSubgameSize = initialSubgameSize;
    iteration = 0;

    stabilityAnalysis = new IncrementalStabilityAnalysis(eGame);

//    System.out.println("Initial outcome: ");
//    System.out.println(currentSubgameOutcome);
  }

}
