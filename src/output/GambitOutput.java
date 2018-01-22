package output;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import games.Game;
import games.OutcomeIterator;

/**
 * Outputs a game in the Gambit .nfg file format.
 * <p/>
 * The format works with Gambit version 0.97.0.3 which is the
 * current version.  Could eventually need to be updated to work
 * with later versions of gambit if the file format is modified.
 * <p/>
 * Note: derived from gamut version
 */

public class GambitOutput {

  // testing this to see if gambit issues are related to very high-precision numbers/rounding errors
  public static final NumberFormat gambit_nf = new DecimalFormat("0.00000");

  // static class
  private GambitOutput() {
  }

  /**
   * Write the game to the given file name in the gambit .nfg format
   *
   * @param fileName the file name to write to
   * @param g        the game to write to the file
   */
  public static void writeGame(String fileName, Game g) {
    try {
      writeGame(new PrintWriter(new File(fileName)), g);
    } catch (IOException e) {
      throw new RuntimeException("Error writing gambit game to file: " + e.getMessage());
    }
  }

  /**
   * Write the game to the given printwriter in the gambit .nfg format
   *
   * @param out the outpute stream to write to
   * @param g   the game to write
   */
  public static void writeGame(PrintWriter out, Game g) {
    out.print(gameToGambitString(g));
    out.flush();
  }

  public static String gameToGambitString(Game g) {
    StringBuilder sb = getSB();

    // The first line of every .nfg file starts with NFG 1
    // D denotes decimal payoffs
    // the description of the game follows
    sb.append("NFG 1 D \"").append(g.getDescription()).append("\" { ");

    // Next print the players in a format like
    //       { "Player1", "Player2", "Player3" }
    for (int i = 1; i <= g.getNumPlayers(); i++) {
      sb.append("\"Player").append(i).append("\" ");
    }
    sb.append("} ");

    // Still on the first line, print out the number of
    // actions for each player in a format like
    //       {2, 3, 4}
    sb.append("{ ");
    for (int i = 0; i < g.getNumPlayers(); i++) {
      sb.append(g.getNumActions(i)).append(" ");
    }
    sb.append("} \n\n");

    // Now print the payoffs in a row
    OutcomeIterator itr = new OutcomeIterator(g);
    while (itr.hasNext()) {
      int[] acts = itr.next();
      for (int i = 0; i < g.getNumPlayers(); i++) {
        sb.append(gambit_nf.format(g.getPayoff(acts, i))).append(" ");
      }
    }
    sb.append("\n");

    return returnSB(sb);
  }
}
