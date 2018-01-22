package solvers;

import games.MixedStrategy;
import observers.GameObserver;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Sep 2, 2007
 * Time: 11:00:41 PM
 */
public interface GameSolver {
  /**
   * @return a brief string describing the solver
   */
  String getName();

  /**
   * @return a vervose string describing this particular solver, including parameter settings, etc.
   */
  String getDescription();

  /**
   * @return a boolean indicating whether or not this method is stochastic
   *         This should be true if the strategy returned for any player may change
   *         for multiple invocations on an identical game instance
   */
  boolean isStochastic();

  /**
   * Returns a strategy to play for the given player
   *
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   * @return a distribution of actions to take
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player);
}
