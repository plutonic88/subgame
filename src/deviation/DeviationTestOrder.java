package deviation;

import java.awt.Point;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Sep 2, 2007
 * Time: 11:00:41 PM
 */
public interface DeviationTestOrder {

  // add the sampled payoffs for this profile to the test order heuristic
  public void update(int[] profile, double[] payoffs);

  // get a list of <player,action> pairs specifying an order over all player's actions
  public List<Point> getDeviationTestOrder(int[] profile);

  // get the order in which to test deviations from this profile for the given player
  public List<Integer> getDeviationTestOrder(int[] profile, int player);

  // reset the data to analyze a new game
  public void reset();

  // initialize to study a particular game
  public void initialize(int nPlayers, int[] nActs);

}
