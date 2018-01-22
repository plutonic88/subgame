package solvers;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;

/**
 * "Solve" a game by returning a uniform random strategy
 */

public class RandomSolver implements GameSolver {

  public RandomSolver() {
  }

  public String getName() {
    return "Uniform Random";
  }

  public String getDescription() {
    return "Random Solver\nAlways returns the uniform mixed strategy\n";
  }

  public boolean isStochastic() {
    return false;
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
   * Returns a distribution of actions to take
   *
   * @param emg    Empirical game observation
   * @param player The player to select an action for.
   */
  public MixedStrategy solveGame(EmpiricalMatrixGame emg, int player) {
    // new mixed strategies default to uniform mixture
    return new MixedStrategy(emg.getNumActions(player));
  }

  /**
   * Returns a distribution of actions to take
   *
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    // new mixed strategies default to uniform mixture
    return new MixedStrategy(gameObs.getNumActions(player));
  }
}
