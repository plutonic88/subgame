package deviation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Apr 2, 2008
 * Time: 5:13:26 PM
 */
public class RandomDeviationTestOrder implements DeviationTestOrder {

  int nPlayers;
  int[] nActs;

  public RandomDeviationTestOrder() {
  }

  // do nothing
  public void update(int[] profile, double[] payoffs) {
  }

  public List<Point> getDeviationTestOrder(int[] profile) {
    List<Point> testOrder = new ArrayList<Point>();
    for (int pl = 0; pl < nPlayers; pl++) {
      for (int a = 1; a <= nActs[pl]; a++) {
        if (a == profile[pl]) continue;
        testOrder.add(new Point(pl, a));
      }
    }
    Collections.shuffle(testOrder);
    //System.out.println("Order: " + testOrder);
    return testOrder;
  }

  public List<Integer> getDeviationTestOrder(int[] profile, int player) {
    List<Integer> testOrder = new ArrayList<Integer>();
    for (int a = 1; a <= nActs[player]; a++) {
      if (a == profile[player]) continue;
      testOrder.add(a);
    }
    Collections.shuffle(testOrder);
    //System.out.println("Order: " + testOrder);
    return testOrder;
  }

  public void initialize(int nPlayers, int[] nActs) {
    this.nPlayers = nPlayers;
    this.nActs = nActs.clone();
  }

  public void reset() {
    // do nothing
  }

}
