package generate;

import static subgame.Parameters.GAME_FILES_PATH;
import static subgame.Parameters.GAMUT_GAME_EXTENSION;
import static subgame.Parameters.GAMUT_PATH;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import games.MatrixGame;
import parsers.GamutParser;

/**
 * Utility class that generates different types of games using gamut
 */

public class GamutGenerator {

  // static class
  private GamutGenerator() {
  }

  /**
   * Generate a set of random games with N samples
   * If some samples already exist in given directory, only generate the additional games necessary
   * <p/>
   * Assumes games are stored as:
   * GAME_FILES_PATH/gameClassName/#.gamut
   */
  public static void generateRandomGameSet(String gameClassName, int numSamples, int nPlayers, int[] nActions,
                                           int minPayoff, int maxPayoff) {

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
        String s = str.split("\\.")[0];

        int i = Integer.parseInt(s);
        if (i > cnt) {
          cnt = i;
        }
      }
    }

    // create any additional samples necessary, numbering from 1 to numSamples
    while (cnt < numSamples) {
      cnt++;
      generateRandomGame(gameClassName, cnt + GAMUT_GAME_EXTENSION, nPlayers, nActions, minPayoff, maxPayoff);
    }
  }

  /**
   * This function generates a game object. Note that this uses a temporary file and does not
   * save the game to disk.
   */
  public static MatrixGame generateRandomGameObject(int nPlayers, int[] nActions, int minPayoff, int maxPayoff) {
    String tmpFileName = "tmp.gamut";

    // generate the game
    generateRandomGame(GAME_FILES_PATH + tmpFileName, nPlayers, nActions, minPayoff, maxPayoff);

    // read it in
    MatrixGame g = GamutParser.readGamutGame(GAME_FILES_PATH + tmpFileName);

    // delete the temporary file
    File tmpFile = new File(GAME_FILES_PATH + tmpFileName);
    tmpFile.delete();

    return g;
  }

  /**
   * Invoke gamut to create a new instance of a random game with the given Parameters
   */
  public static void generateRandomGame(String gameClassName, String gameName, int nPlayers, int[] nActions,
                                        int minPayoff, int maxPayoff) {
      //String path = GAME_FILES_PATH + gameClassName + "/" + gameName;
      //Oscar is detecting operating system
      String path = "";
      if(System.getProperty("os.name").toLowerCase().contains("win"))
          path =  GAME_FILES_PATH + gameClassName + "\\" + gameName;
      else
          path = GAME_FILES_PATH + gameClassName + "/" + gameName;
    generateRandomGame(path, nPlayers, nActions, minPayoff, maxPayoff);
  }

  /**
   * Invoke gamut to create a new instance of a random game with the given Parameters
   */
  public static void generateRandomGame(String gamePath, int nPlayers, int[] nActions, int minPayoff, int maxPayoff) {
    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add("\""+GAMUT_PATH+"\"");
    cmd.add("-g");
    cmd.add("RandomGame");
    cmd.add("-normalize");
    cmd.add("-min_payoff");
    cmd.add(Integer.toString(minPayoff));
    cmd.add("-max_payoff");
    cmd.add(Integer.toString(maxPayoff));
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-players");
    cmd.add(Integer.toString(nPlayers));
    cmd.add("-actions");
    for (int pl = 0; pl < nPlayers; pl++)
      cmd.add(Integer.toString(nActions[pl]));
System.out.println("java -jar gamut.jar -g RandomGame -normalize -min_payoff "+minPayoff+" -max_payoff "+maxPayoff+" -f "+gamePath+" -players "+nPlayers+" -actions "+nActions[0]+ " "+ nActions[1]);

    /*try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    }*/
  }
  public static void generateZeroSumGame(String gamePath, int[] nActions) {
      //creates zerosum 2 player game [-100,100] payoffs
    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add("\""+GAMUT_PATH+"\"");
    cmd.add("-g");
    cmd.add("RandomZeroSum");
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-actions");
    for (int pl = 0; pl < 2; pl++)
      cmd.add(Integer.toString(nActions[pl]));
    System.out.println("java -jar gamut.jar -g RandomZeroSum  -f "+gamePath+" -actions "+nActions[0]+ " "+ nActions[1]);

    /*try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    } */
  }

  /**
   * Invoke gamut to create a new instance of a covariant game with the given Parameters
   */
  public static void generateRandomLEGGame(String gamePath,
                                           int nPlayers, int nActions,
                                           int minPayoff, int maxPayoff) {

    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add(GAMUT_PATH);
    cmd.add("-random_params");
    cmd.add("-g");
    cmd.add("RandomLEG");
    cmd.add("-normalize");
    cmd.add("-min_payoff");
    cmd.add(Integer.toString(minPayoff));
    cmd.add("-max_payoff");
    cmd.add(Integer.toString(maxPayoff));
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-players");
    cmd.add(Integer.toString(nPlayers));
    cmd.add("-actions");
    cmd.add(Integer.toString(nActions));
    cmd.add("-graph");
    cmd.add("RandomGraph");
    cmd.add("-graph_params");
    cmd.add("[");
    cmd.add("-nodes");
    cmd.add(Integer.toString(nActions));
    cmd.add("-sym_edges");
    cmd.add(Integer.toString(1));
    cmd.add("-reflex_ok");
    cmd.add(Integer.toString(0));
    cmd.add("]");
    cmd.add("-func");
    cmd.add("PolyFunction");
    cmd.add("-func_params");
    cmd.add("[");
    cmd.add("-degree");
    cmd.add(Integer.toString(2));
    cmd.add("-coef_min");
    cmd.add(Integer.toString(-3));
    cmd.add("-coef_max");
    cmd.add(Integer.toString(3));
    cmd.add("]");

    try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    }
  }

  // create "congestion" games using gamut
  // note: facilities max out at 5 (31 pure strategies per player)
  public static void generateCongestionGame(String gamePath,
                                            int nPlayers, int facilities,
                                            int minPayoff, int maxPayoff) {

    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add(GAMUT_PATH);
    cmd.add("-g");
    cmd.add("CongestionGame");
    cmd.add("-random_params");
    cmd.add("-normalize");
    cmd.add("-min_payoff");
    cmd.add(Integer.toString(minPayoff));
    cmd.add("-max_payoff");
    cmd.add(Integer.toString(maxPayoff));
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-players");
    cmd.add(Integer.toString(nPlayers));
    cmd.add("-facilities");
    cmd.add(Integer.toString(facilities));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    }
  }

  // create "supermodular" games using gamut
  // contains: Cournot Duopoly, Betrand Oligopoly, Arms Race
  public static void generateSupermodularGame(String gamePath,
                                              int nPlayers, int actions,
                                              int minPayoff, int maxPayoff) {

    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add(GAMUT_PATH);
    cmd.add("-g");
    cmd.add("SupermodularGames");
    cmd.add("-random_params");
    cmd.add("-normalize");
    cmd.add("-min_payoff");
    cmd.add(Integer.toString(minPayoff));
    cmd.add("-max_payoff");
    cmd.add(Integer.toString(maxPayoff));
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-players");
    cmd.add(Integer.toString(nPlayers));
    cmd.add("-actions");
    cmd.add(Integer.toString(actions));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    }
  }

  // create "compactly representable" games using gamut
  // contains:  BidirectionalLEG, PolymatrixGame, UniformLEG, RandomLEG, CoordinationGame, GraphicalGame-Road
  public static void generateCompactGame(String gamePath,
                                         int nPlayers, int actions,
                                         int minPayoff, int maxPayoff) {

    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add(GAMUT_PATH);
    cmd.add("-g");
    cmd.add("CompactlyRepresentable");
    cmd.add("-random_params");
    cmd.add("-normalize");
    cmd.add("-min_payoff");
    cmd.add(Integer.toString(minPayoff));
    cmd.add("-max_payoff");
    cmd.add(Integer.toString(maxPayoff));
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-players");
    cmd.add(Integer.toString(nPlayers));
    cmd.add("-actions");
    cmd.add(Integer.toString(actions));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    }
  }

  /**
   * Invoke gamut to create a new instance of a covariant game with the given Parameters
   */
  public static void generateCovariantGame(String gamePath,
                                           int nPlayers, int[] nActions,
                                           int minPayoff, int maxPayoff,
                                           double covariance) {

    List<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-jar");
    cmd.add(GAMUT_PATH);
    cmd.add("-g");
    cmd.add("CovariantGame");
    cmd.add("-r");
    cmd.add(Double.toString(covariance));
    cmd.add("-normalize");
    cmd.add("-min_payoff");
    cmd.add(Integer.toString(minPayoff));
    cmd.add("-max_payoff");
    cmd.add(Integer.toString(maxPayoff));
    cmd.add("-f");
    cmd.add(gamePath);
    cmd.add("-players");
    cmd.add(Integer.toString(nPlayers));
    cmd.add("-actions");
    for (int pl = 0; pl < nPlayers; pl++)
      cmd.add(Integer.toString(nActions[pl]));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb = pb.redirectErrorStream(true);
      Process proc = pb.start();
      InputStream is = proc.getInputStream();
      proc.waitFor();

      while (is.available() > 0) {
        System.out.write(is.read());
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception while generating game using gamut: " + e.getMessage());
    }
  }
}
