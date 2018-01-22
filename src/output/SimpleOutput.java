package output;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

import games.Game;
import games.OutcomeIterator;

/**
 * A simple output format
 */

public class SimpleOutput {

  // static class
  private SimpleOutput() {
  }

  /**
   * Write the game to the given file name in the gamut SimpleOutput format
   */
  public static void writeGame(String fileName, Game g) {
    try {
      writeGame(new PrintWriter(new File(fileName)), g);
    } catch (IOException e) {
      throw new RuntimeException("Error writing gamut game to file: " + e.getMessage());
    }
  }

  /**
   * Write the game to the given printwriter in the gamut SimpleOutput format
   */
  public static void writeGame(PrintWriter out, Game g) {
    StringBuilder sb = getSB();

    sb.append("# Game output in GAMUT SimpleOutput format, v1.0.1\n");
    sb.append(commentString(g.getDescription(), "# "));
    sb.append("\n");

    sb.append("# Players:\t").append(g.getNumPlayers()).append("\n");
    sb.append("# Actions:\t");

    for (int i = 0; i < g.getNumPlayers(); i++) {
      sb.append(g.getNumActions(i)).append((i == g.getNumPlayers() - 1 ? "\n" : " "));
    }

    OutcomeIterator itr = new OutcomeIterator(g);
    while (itr.hasNext()) {
      int[] acts = itr.next();
      sb.append(itr).append(" :\t[ ");

      for (int i = 0; i < g.getNumPlayers(); i++) {
        sb.append(g.getPayoff(acts, i)).append(" ");
      }
      sb.append("]\n");
    }
    out.print(returnSB(sb));
    out.flush();
  }

  /**
   * Properly adds and formats comments to the output in the comment
   * format specified.
   *
   * @param str     the string to add comments to
   * @param comment the string to use as a comment marker at the
   *                beginning of a line
   */
  private static String commentString(String str, String comment) {
    return comment + str.replaceAll("\n", "\n" + comment);
  }
  public static void RemoveActions(PrintWriter out, Game g,HashSet<Integer> p1,HashSet<Integer> p2) {
    StringBuilder sb = getSB();

    sb.append("# Game output in GAMUT SimpleOutput format, v1.0.1\n");
    sb.append(commentString(g.getDescription(), "# "));
    sb.append("\n");

    sb.append("# Players:\t").append(g.getNumPlayers()).append("\n");
    sb.append("# Actions:\t");

    for (int i = 0; i < g.getNumPlayers(); i++) {
      sb.append(g.getNumActions(i)).append((i == g.getNumPlayers() - 1 ? "\n" : " "));
    }
        int p1count=0;
        int p2count=0;
        OutcomeIterator itr = new OutcomeIterator(g);
        while (itr.hasNext()) {
            int[] acts = itr.next();
            int[] outcome = itr.getOutcome();//oscar
            if(p1.contains(outcome[0]) && p2.contains(outcome[1]))//oscar
            {
                sb.append("[").append(outcome[0]-p1count).append(" ").append(outcome[1]-p2count).append("] :\t[ ");
                for (int i = 0; i < g.getNumPlayers(); i++) {
                sb.append(g.getPayoff(acts, i)).append(" ");
            }
            sb.append("]\n");
        }
    }
    out.print(returnSB(sb));
    out.flush();
    }
}
