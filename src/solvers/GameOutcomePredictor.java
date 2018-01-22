package solvers;

import games.OutcomeDistribution;
import observers.GameObserver;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Sep 2, 2007
 * Time: 11:04:57 PM
 */
public interface GameOutcomePredictor {

  /**
   * Returns the predicted distribution over outcomes
   *
   * @param gameObs game observer
   * @return outcome distribution
   */
  public OutcomeDistribution predictOutcome(GameObserver gameObs);

}
