package generate;

import static subgame.Parameters.GAME_FILES_PATH;
import static subgame.Parameters.GAMUT_GAME_EXTENSION;

import java.io.File;

import games.MatrixGame;
import games.OutcomeIterator;
import output.SimpleOutput;
import util.DistributionSampler;

/**
 * Generate strategic-form games with payoffs drawn from particular distributions
 *
 * This variation has several advantages over Gamut:
 *   - allows one-player games
 *   - supports more than just the uniform distribution
 *   - is faster, since it doesn't require calling gamut, parsing files, etc in all cases
 */

public class RandomGenerator {

  // static class
  private RandomGenerator() {
  }

  /**
   * Create a new random game using the given distribution
   */
  public static MatrixGame generateRandomGame(int nPlayers, int[] nActions,
                                              DistributionSampler sampler) {
    MatrixGame g = new MatrixGame(nPlayers, nActions);
    OutcomeIterator itr = g.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      double[] payoffs = new double[nPlayers];
      for (int pl = 0; pl < nPlayers; pl++) {
        payoffs[pl] = sampler.getSampleDouble();
      }
      g.setPayoffs(outcome, payoffs);
    }
    return g;
  }

  /**
   * Create a new using the given distribution
   * Write the game to the given location
   */
  public static MatrixGame generateRandomGame(String gameClassName, String gameName,
                                              int nPlayers, int[] nActions,
                                              DistributionSampler sampler) {
    File dir = new File(GAME_FILES_PATH + gameClassName);
    if (!dir.exists()) { dir.mkdirs(); }

    MatrixGame g = generateRandomGame(nPlayers, nActions, sampler);
    SimpleOutput.writeGame(GAME_FILES_PATH + gameClassName + "/" + gameName, g);
    return g;
  }

  /**
   * Generate a set of random games with N samples
   * If some samples already exist in given directory, only generate the additional games necessary
   *
   * Assumes games are stored as:
   * GAME_FILES_PATH/gameClassName/#.gamut
   */
  public static void generateRandomGameSet(String gameClassName, int numSamples,
                                           int nPlayers, int[] nActions,
                                           DistributionSampler sampler) {
    File dir = new File(GAME_FILES_PATH + gameClassName);
    int cnt = 0;

    // make the directory if necessary, otherwise count the number of existing samples
    if (!dir.exists()) {
      dir.mkdirs();
    } else {
      String[] gameFiles = dir.list();
      for (String str : gameFiles) {
        // not a gamut game file
        if (!str.endsWith(GAMUT_GAME_EXTENSION)) {
          continue;
        }

        // extract the game number and record the maximum number
        int i = Integer.parseInt(str.split("\\.")[0]);
        if (i > cnt) { cnt = i; }
      }
    }

    // create any additional samples necessary, numbering from 1 to numSamples
    while(cnt < numSamples) {
      cnt++;
      generateRandomGame(gameClassName, cnt + GAMUT_GAME_EXTENSION, nPlayers, nActions, sampler);
    }
  }
}
