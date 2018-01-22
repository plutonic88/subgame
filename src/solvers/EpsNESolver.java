package solvers;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import games.Game;
import games.MixedStrategy;
import games.OutcomeIterator;
import subgame.Parameters;

/**
 * Created by IntelliJ IDEA.
 * User: Oscar-XPS
 * Date: 10/10/13
 * Time: 5:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class EpsNESolver {
  // finds the outcome profile with the smallest epsilon value, where
  // epsilon is the maximum benefit to deviating from the profile for any player.
  // any profile with an epsilon <= 0 is a pure strategy Nash equilibrium
  // Returns the action taken by the specified player
  // the value of epsilon is also calculated, but not returned

  public static MixedStrategy findMostStableOutcome(Game game, int player) {

    double epsilon = Double.POSITIVE_INFINITY;
    int playerAction = 0;

    OutcomeIterator itr = game.iterator();
    while(itr.hasNext()) {
      int[] tmp = itr.next();

      double maxBTD = Double.NEGATIVE_INFINITY;
      for (int pl = 0; pl < game.getNumPlayers(); pl++) {
        int[] outcome = Arrays.copyOf(tmp, tmp.length);
        double base = game.getPayoff(outcome, pl);

        for (int action = 1; action <= game.getNumActions(pl); action++) {
          if (action == tmp[pl]) continue; // skip if this is the original action
          outcome[pl] = action;
          double currBTD = game.getPayoff(outcome, pl) - base;
          maxBTD = Math.max (currBTD, maxBTD);
        }
      }
      if (epsilon > maxBTD) {
        epsilon = maxBTD;
        playerAction = tmp[player];
      }
    }
    //StrategyHolder.getInstance().setEps(epsilon);//keep running tally of epsilons
    File log = new File(Parameters.GAME_FILES_PATH+"epsilon.txt");
    try{
        PrintWriter pw = new PrintWriter(new FileWriter(log, true));
        pw.append(epsilon+" ");
        pw.close();
    }catch(Exception e){e.printStackTrace();}
    double[] strat = new double[game.getNumActions(player)+1];
    strat[playerAction] = 1.0;
    return new MixedStrategy(strat);
    //return playerAction;
  }


}
