package deviation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import subgame.EGAUtils;
import support.ActionData;
import support.ActionDataInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Apr 2, 2008
 * Time: 5:13:26 PM
 */
public class AvePayoffDeviationTestOrder implements DeviationTestOrder {

  private static final double LARGE_NEG = -10000000000d;
  private static final double LARGE_POS = 1000000000d;

  private double initialValue = LARGE_POS;
  private double minTotalSamples = 0; // use random ordering until this number of samples is reached
  private double minTotalSamplesPerAction = 0d;

  int nPlayers;
  int[] nActs;

  ActionData totalPayoffs;
  ActionDataInteger nSamples;
  int totalSamples;

  SortedMap<Double, Integer> sorter = new TreeMap<Double, Integer>();
  SortedMap<Double, Point> sorterPoints = new TreeMap<Double, Point>();

  public AvePayoffDeviationTestOrder() {
  }

  public void setPosInitialValue() {
    initialValue = LARGE_POS;
  }

  public void setNegInitialValue() {
    initialValue = LARGE_NEG;
  }

  public double getMinTotalSamples() {
    return minTotalSamples;
  }

  public void setMinTotalSamples(double minTotalSamples) {
    this.minTotalSamples = minTotalSamples;
    this.minTotalSamplesPerAction = 0d; // make sure that this setting isn't unset automatically
  }

  public double getMinTotalSamplesPerAction() {
    return minTotalSamplesPerAction;
  }

  public void setMinTotalSamplesPerAction(double minTotalSamplesPerAction) {
    this.minTotalSamplesPerAction = minTotalSamplesPerAction;
  }

  // track the "average" payoff associated with each pure strategy
  public void update(int[] profile, double[] payoffs) {
    for (int pl = 0; pl < nPlayers; pl++) {
      double oldVal = totalPayoffs.get(pl, profile[pl]);
      totalPayoffs.set(pl, profile[pl], oldVal + payoffs[pl]);
      int oldSamples = nSamples.get(pl, profile[pl]);
      nSamples.set(pl, profile[pl], oldSamples + 1);
    }
    totalSamples += nPlayers;
  }

  public List<Point> getDeviationTestOrder(int[] profile) {

    // use random ordering until we have enough samples
    if (totalSamples < minTotalSamples) {
      List<Point> testOrder = new ArrayList<Point>();
      for (int pl = 0; pl < nPlayers; pl++) {
        for (int a = 1; a <= nActs[pl]; a++) {
          if (a == profile[pl]) continue;
          testOrder.add(new Point(pl, a));
        }
      }
      Collections.shuffle(testOrder);
      return testOrder;
    }

    sorterPoints.clear();
    for (int pl = 0; pl < nPlayers; pl++) {
      for (int a = 1; a <= nActs[pl]; a++) {
        if (a == profile[pl]) continue;
        double avePayoff;
        if (nSamples.get(pl, a) > 0) {
          avePayoff = totalPayoffs.get(pl, a) / nSamples.get(pl, a);
        } else {
          avePayoff = initialValue + EGAUtils.rand.nextDouble();
        }

        //System.out.println("ave payoff (" + pl + "," + a +"): " + avePayoff);

        while (sorter.containsKey(avePayoff)) {
          avePayoff += EGAUtils.rand.nextDouble() / 1000;
        }
        sorterPoints.put(avePayoff, new Point(pl, a));

        //System.out.println("Test: " + avePayoff + " " + a);
      }
    }

    Collection<Point> tmp = sorterPoints.values();
    List<Point> testOrder = new ArrayList<Point>();
    for (Point aTmp : tmp) {
      testOrder.add(aTmp);
    }
    Collections.reverse(testOrder);

    //System.out.println("Order: " + testOrder);
    return testOrder;
  }

  public List<Integer> getDeviationTestOrder(int[] profile, int player) {

    // use random ordering if not enough samples
    if (totalSamples < minTotalSamples) {
      List<Integer> testOrder = new ArrayList<Integer>();
      for (int a = 1; a <= nActs[player]; a++) {
        if (a == profile[player]) continue;
        testOrder.add(a);
      }
      Collections.shuffle(testOrder);
      return testOrder;
    }

    sorter.clear();
    for (int a = 1; a <= nActs[player]; a++) {
      if (a == profile[player]) continue;
      double avePayoff;
      if (nSamples.get(player, a) > 0) {
        avePayoff = totalPayoffs.get(player, a) / nSamples.get(player, a);
      } else {
        avePayoff = initialValue + EGAUtils.rand.nextDouble();
      }

      while (sorter.containsKey(avePayoff)) {
        avePayoff += EGAUtils.rand.nextDouble() / 1000;
      }
      sorter.put(avePayoff, a);
      //System.out.println("Test: " + avePayoff + " " + a);
    }

    Collection<Integer> tmp = sorter.values();
    List<Integer> testOrder = new ArrayList<Integer>();
    for (Integer aTmp : tmp) {
      testOrder.add(aTmp);
    }
    Collections.reverse(testOrder);

    //System.out.println("Order: " + testOrder);
    return testOrder;
  }

  public void initialize(int nPlayers, int[] nActs) {
    this.nPlayers = nPlayers;
    this.nActs = nActs.clone();
    totalPayoffs = new ActionData(nActs, 0d);
    nSamples = new ActionDataInteger(nActs, 0);
    totalSamples = 0;
    if (minTotalSamplesPerAction > 0) {
      int totActs = 0;
      for (int pl = 0; pl < nPlayers; pl++) {
        totActs += nActs[pl];
      }
      minTotalSamples = Math.ceil(totActs * minTotalSamplesPerAction);
    }
  }

  public void reset() {
    totalPayoffs.setZeros();
    nSamples.setZeros();
    totalSamples = 0;
  }

}
