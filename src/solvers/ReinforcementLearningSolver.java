package solvers;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.nf;
import static subgame.EGAUtils.rand;
import static subgame.EGAUtils.returnSB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import games.MixedStrategy;
import observers.GameObserver;

/**
 * Uses a simple variation of reinforcement learning to find a strategy
 * to play the game (uniform mixture for equal values)
 */
public class ReinforcementLearningSolver {

  // TODO: extend this to support full game solver interface again

  // size of population of learners
  public static final int DEFAULT_POPULATION_SIZE = 1;

  // if this is > 0, value functions are intialized randomly between -range/2 and +range/2
  public static final double INIT_VALUE_FUNCTIONS_RANGE = 0d;

  // maximum iterations, specified as a muliplier on the number of observations
  public static final double MAX_ITERATIONS_MULTIPLIER = 20d;

  // control the size of the population for each player (number of value functions)
  // these are selected randomly as necessary
  public int populationSize = DEFAULT_POPULATION_SIZE;
  public int opponentPopulationSize = DEFAULT_POPULATION_SIZE;

  // The learning rate for updates
  private double alpha;

  // starting alpha
  public double alphaStart;

  // ending alpha target
  public double alphaEnd;

  // The decay rate for alpha
  public double alphaDecay;

  // The exploration rate
  private double epsilon;

  // starting epsilon
  public double epsilonStart;

  // ending alpha target
  public double epsilonEnd;

  // The decay rate for epsilon
  public double epsilonDecay;

  // Store this for efficiency
  private int iteration = 0;
  private int numPlayers = 0;
  private int[] numActs = null;
  private int[] numValueFunctions = null;
  //private EmpiricalMatrixGame eGame;
  private final List<Integer> candidateActs = new ArrayList<Integer>();
  private final List<List<ValueFunction>> valueFunctions = new ArrayList<List<ValueFunction>>();
  private ValueFunction[] vfArr = null;
  private int[] outcome = null;
  private int maxIterations = 0;

  private String name;

  // default initialization
  private ReinforcementLearningSolver() {
  }

  // Inialization with no decay
  public ReinforcementLearningSolver(double alphaStart, double epsilonStart) {
    this.alpha = alphaStart;
    this.alphaStart = alphaStart;
    this.alphaEnd = Double.NEGATIVE_INFINITY;
    this.alphaDecay = -1.0d;
    this.epsilon = epsilonStart;
    this.epsilonStart = epsilonStart;
    this.epsilonEnd = Double.NEGATIVE_INFINITY;
    this.epsilonDecay = -1.0d;
    updateName();
  }

  // Inialization with decay based on start/end targets
  public ReinforcementLearningSolver(double alphaStart, double alphaEnd, double epsilonStart, double epsilonEnd) {
    this.alpha = alphaStart;
    this.alphaStart = alphaStart;
    this.alphaEnd = alphaEnd;
    this.alphaDecay = -1.0d;
    this.epsilon = epsilonStart;
    this.epsilonStart = epsilonStart;
    this.epsilonEnd = epsilonEnd;
    this.epsilonDecay = -1.0d;
    updateName();
  }

  public void setPopulationSize(int pop) {
    this.populationSize = pop;
  }

  public void setOpponentPopulationSize(int pop) {
    this.opponentPopulationSize = pop;
  }

  public void setAlphaStart(double alphaStart) {
    this.alphaStart = alphaStart;
    updateName();
  }

  public void setAlphaEnd(double alphaEnd) {
    this.alphaEnd = alphaEnd;
    updateName();
  }

  public void setAlphaDecay(double alphaDecay) {
    this.alphaDecay = alphaDecay;
  }

  public void setEpsilonStart(double epsilonStart) {
    this.epsilonStart = epsilonStart;
    updateName();
  }

  public void setEpsilonEnd(double epsilonEnd) {
    this.epsilonEnd = epsilonEnd;
    updateName();
  }

  public void setEpsilonDecay(double epsilonDecay) {
    this.epsilonDecay = epsilonDecay;
  }

  // allows decay to be set based on a target start and end value, given number of observations
  private void computeAlphaDecay(int numObs) {
    alphaDecay = Math.log(alphaEnd / alphaStart) / (double) numObs;
  }

  // allows decay to be set based on a target start and end value, given number of observations
  private void computeEpsilonDecay(int numObs) {
    epsilonDecay = Math.log(epsilonEnd / epsilonStart) / (double) numObs;
  }

  public boolean isStochastic() {
    return true;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns a description of the solver
   */
  public String getDescription() {
    StringBuilder sb = getSB();
    sb.append("Reinforcement Learning Solver\n");
    sb.append("Apply basic reinforcement learning to learn a strategy.\n");
    sb.append("Value function init range: " + INIT_VALUE_FUNCTIONS_RANGE + "\n");
    sb.append("Max iterations multiplier: " + MAX_ITERATIONS_MULTIPLIER + "\n");
    sb.append("Population size: ").append(populationSize).append("\n");
    sb.append("Opponent population size: ").append(opponentPopulationSize).append("\n");
    sb.append("Alpha (start, end, decay): ").append(alphaStart).append(" ").append(alphaEnd).append(" ")
            .append(nf.format(alphaDecay)).append("\n");
    sb.append("Epsilon (start, end, decay): ").append(epsilonStart).append(" ").append(epsilonEnd).append(" ")
            .append(nf.format(epsilonDecay)).append("\n");
    return returnSB(sb);
  }

  private void updateName() {
    name = "RL " + nf.format(alphaStart) + " " + nf.format(alphaEnd) +
           " " + nf.format(epsilonStart) + " " + nf.format(epsilonEnd);
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   */
  private void initialize(GameObserver gameObs, int player) {
    // check to see if we are set up for a game of this dimension
    if (gameObs.getNumPlayers() == numPlayers &&
        Arrays.equals(gameObs.getNumActions(), numActs)) {
      reset(gameObs);
      return;
    }

    numPlayers = gameObs.getNumPlayers();
    numActs = gameObs.getNumActions().clone();
    //eGame = new EmpiricalMatrixGame(nPlayers, nActs);
    vfArr = new ValueFunction[numPlayers];
    outcome = new int[numPlayers];
    numValueFunctions = new int[numPlayers];
    for (int pl = 0; pl < numPlayers; pl++) {
      numValueFunctions[pl] = (pl == player ? populationSize : opponentPopulationSize);
    }

    // set up the set of value functions
    for (int pl = 0; pl < numPlayers; pl++) {
      List<ValueFunction> tmp = new ArrayList<ValueFunction>();
      for (int i = 0; i < numValueFunctions[pl]; i++) {
        tmp.add(new ValueFunction(numActs[pl]));
      }
      valueFunctions.add(tmp);
    }

    reset(gameObs);
  }

  /**
   * Reset all private data structures to analyze a new game
   */
  private void reset(GameObserver gameObs) {
    //eGame.clear();
    iteration = 0;

    // compute decay rates, if these are based on target end values
    maxIterations = (int) Math.round(MAX_ITERATIONS_MULTIPLIER * (double) gameObs.numObsLeft());
    if (alphaEnd > 0) {
      computeAlphaDecay(gameObs.numObsLeft());
    }
    if (epsilonEnd > 0) {
      computeEpsilonDecay(gameObs.numObsLeft());
    }

    // reset the learning/exploration rates
    alpha = alphaStart;
    epsilon = epsilonStart;

    // reset value functions....
    for (int pl = 0; pl < numPlayers; pl++) {
      List<ValueFunction> tmp = valueFunctions.get(pl);
      for (int i = 0; i < numValueFunctions[pl]; i++) {
        tmp.get(i).reset();
      }
      valueFunctions.add(tmp);
    }
  }

  /**
   * Returns a distribution of actions to take
   *
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    initialize(gameObs, player);

    // run the RL algorithm
    int startingNumObs = gameObs.numObsLeft();
    int numObsSoFar;
    int lastNumObsSoFar = 0;

    while (gameObs.numObsLeft() > 0 && iteration < maxIterations) {
      for (int pl = 0; pl < numPlayers; pl++) {
        vfArr[pl] = valueFunctions.get(pl).get(rand.nextInt(numValueFunctions[pl]));
        outcome[pl] = vfArr[pl].chooseAction();
      }

      double[] payoffs = gameObs.getSample(outcome);
      for (int pl = 0; pl < numPlayers; pl++) {
        vfArr[pl].updateValueFunction(outcome[pl], payoffs[pl]);
      }

      // update the learning/exploration rates
      // these are based on observations used, rathe than iterations (more predictable indicator of progress)
      numObsSoFar = startingNumObs - gameObs.numObsLeft();
      if (numObsSoFar != lastNumObsSoFar) {
        alpha = alphaStart * (Math.pow(Math.E, alphaDecay * numObsSoFar));
        epsilon = epsilonStart * (Math.pow(Math.E, epsilonDecay * numObsSoFar));
        lastNumObsSoFar = numObsSoFar;
      }

      iteration++;
    }

    System.out.println("RL solver. Iterations: " + iteration + " obs left: " + gameObs.numObsLeft());

    // for now, just randomly select a value function to use for this player
    // could combine into a single mixed strategy, or some other method
    ValueFunction tmp = valueFunctions.get(player).get(rand.nextInt(numValueFunctions[player]));
    return tmp.getMixedStrategy();
  }


  /**
   * Inner class for representing a value function
   */
  private class ValueFunction {
    public double[] values;
    private int numActs;

    public ValueFunction(int numActs) {
      if (numActs < 1) {
        throw new RuntimeException(
                "ReinformcementLearningSolver: Attempting to create value function with < 1 action.");
      }
      this.numActs = numActs;
      values = new double[numActs + 1];
    }

    // reset all values using the initialization procedure
    public void reset() {
      if (INIT_VALUE_FUNCTIONS_RANGE != 0) {
        initRandom(INIT_VALUE_FUNCTIONS_RANGE);
      } else {
        initZero();
      }
    }

    // initialize all values to 0
    public void initZero() {
      Arrays.fill(values, 0d);
    }

    // set random values over some range, centered on 0
    public void initRandom(double range) {
      for (int i = 0; i < values.length; i++) {
        values[i] = (rand.nextDouble() * range) - (range / 2d);
      }
    }

    /**
     * Perform the learning update, given an observation
     */
    public void updateValueFunction(int act, double payoff) {
      values[act] += alpha * (payoff - values[act]);
    }

    /**
     * Choose an action based on this value function and e-greedy selection
     */
    public int chooseAction() {
      // choose randomly with probability epsilon
      if (rand.nextDouble() < epsilon) {
        candidateActs.clear();
        candidateActs.add(rand.nextInt(numActs) + 1);
      }

      // otherwise, select the action with the highest value
      // randomize between actions with the same value
      else {
        candidateActs.clear();
        candidateActs.add(1);
        double max = values[1];
        for (int i = 2; i < values.length; i++) {
          if (values[i] > max) {
            max = values[i];
            candidateActs.clear();
            candidateActs.add(i);
          } else if (values[i] == max) {
            candidateActs.add(i);
          }
        }
        Collections.shuffle(candidateActs);
      }
      return candidateActs.get(0);
    }

    /**
     * Create a mixed strategy from a value function
     * All actions with the maximum value are played with equal probability
     */
    public MixedStrategy getMixedStrategy() {
      if (values.length < 1) return null;
      MixedStrategy strategy = new MixedStrategy(numActs, 0d);
      strategy.setBestResponse(values);
      return strategy;
    }
  }
}
