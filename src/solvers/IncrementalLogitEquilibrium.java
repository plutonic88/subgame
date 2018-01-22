package solvers;

import java.util.Arrays;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;
import subgame.EGAUtils;

/**
 * A set of tools for sampling the payoff matrix, analyzing the stability of profiles,
 * and selecting strategies based on this analysis
 */

public class IncrementalLogitEquilibrium implements GameSolver, GameOutcomePredictor {

  private String name;

  private QRESolver les = new QRESolver();

  private double[] samplingThresholds = {0.1d, 0.2d, 0.3d, 0.4d, 0.5d, 0.6d, 0.7d, 0.8d, 0.9d, 1d};
  private double[] samplingLambdas = {0d, 1d, 2d, 4d, 6d, 8d, 10d, 12.5d, 15d, 20d};
  private double finalLambda = 10d;

  private int nextUpdate = -1;
  private int nextWindow = 0;

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
  private OutcomeDistribution samplingDistribution = null;

  public IncrementalLogitEquilibrium() {
    updateName();
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = EGAUtils.getSB();
    sb.append("IncrementalLogitEquilibrium\n");
    return EGAUtils.returnSB(sb);
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
    name = "ILES";
  }

  public double[] getSamplingThresholds() {
    return samplingThresholds;
  }

  public void setSamplingThresholds(double[] samplingThresholds) {
    this.samplingThresholds = samplingThresholds;
  }

  public double[] getSamplingLambdas() {
    return samplingLambdas;
  }

  public void setSamplingLambdas(double[] samplingLambdas) {
    this.samplingLambdas = samplingLambdas;
  }

  public double getFinalLambda() {
    return finalLambda;
  }

  public void setFinalLambda(double finalLambda) {
    this.finalLambda = finalLambda;
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

    if (nextUpdate < 0) {
      nextUpdate = (int) Math.ceil(samplingThresholds[0] * eGame.getNumProfiles());
      nextWindow++;
    }

    //System.out.println("Computing outcome prediction. Samples: " + gameObs.getNumObs());

    int nProfiles = eGame.getNumProfiles();
    while (gameObs.numObsLeft() > 0 &&
           eGame.getTotalNumSamples() < nProfiles) {

      // if we hit a threshold, update the sampling distribution 
      if (eGame.getTotalNumSamples() >= nextUpdate) {
        les.setLambda(samplingLambdas[nextWindow]);
        samplingDistribution = les.predictOutcome(eGame);
        nextUpdate = (int) Math.ceil(samplingThresholds[nextWindow] * eGame.getNumProfiles());
        nextWindow++;
      }

      int[] profileToSample = null;
      int cnt = 0;
      boolean foundSample = false;

      while (!foundSample && cnt <= 20) {
        profileToSample = samplingDistribution.sampleDistribution();
        if (eGame.getNumSamples(profileToSample) < 1) {
          foundSample = true;
        }
        cnt++;
      }

      // if we have not found a valid sample using the given distribution,
      // select this sample randomly
      if (!foundSample) {
        //System.out.println("No valid sample generated; sampling uniform random.");
        profileToSample = new int[nPlayers];
        eGame.getRandomProfile(profileToSample);
        while (eGame.getNumSamples(profileToSample) > 0) {
          eGame.getRandomProfile(profileToSample);
        }
      }

      sampleAndUpdate(profileToSample, gameObs);
      //System.out.println("Randomly sampled profile: " + Arrays.toString(randomProfile));
    }

    // predict the final outcome
    les.setLambda(finalLambda);
    predictedOutcome = les.predictOutcome(eGame);

    //System.out.println("Predicted outcome: " + predictedOutcome);
    //System.out.println("Done with this iteration; no more samples.");
    //System.out.println("Samples: " + gameObs.getNumObs());
  }


  // gets the sample and updates the analysis
  private void sampleAndUpdate(int[] profileToSample, GameObserver gameObs) {
    //System.out.println("Sampling profile: " + Arrays.toString(profileToSample));
    double[] samplePayoffs = gameObs.getSample(profileToSample);
    eGame.addSample(profileToSample, samplePayoffs);
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
    samplingDistribution = new OutcomeDistribution(nActs);
    nextUpdate = -1;
    nextWindow = 0;
  }

  // reset the data structures to analyze a new game
  public void reset(GameObserver gameObs) {
    eGame.clear();
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());
    stabilityAnalysis = new IncrementalStabilityAnalysis(eGame);
    predictedOutcome.setCentroid();
    samplingDistribution.setCentroid();
    nextUpdate = -1;
    nextWindow = 0;
  }

}
