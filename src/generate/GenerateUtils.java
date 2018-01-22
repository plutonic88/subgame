package generate;

import games.MatrixGame;
import games.OutcomeIterator;
import util.DistributionSampler;

/**
 * Utility functions for game generation
 */

public class GenerateUtils {

  // static class
  private GenerateUtils() {
  }

  /**
   * Add noise to the given game, using the given distribution
   */
  public static void addNoise(MatrixGame mg, DistributionSampler sampler) {
    OutcomeIterator itr = mg.iterator();
    while(itr.hasNext()) {
      int[] outcome = itr.next();
      double[] payoffs = mg.getPayoffs(outcome);
      for (int pl = 0; pl < mg.getNumPlayers(); pl++) {
        payoffs[pl] += sampler.getSampleDouble();
      }
      mg.setPayoffs(outcome, payoffs);
    }
  }
}
