package solvers;


import java.util.ArrayList;
import java.util.Arrays;

import Log.Logger;
import games.Game;
import games.MixedStrategy;
import games.OutcomeIterator;


public class MinEpsilonBoundSolver {

  public static ArrayList<MixedStrategy> getPSNE(Game game) {
    double bestEpsilon = Double.POSITIVE_INFINITY;
    int[] bestOutcome = new int[game.getNumPlayers()];

    OutcomeIterator itr = new OutcomeIterator(game);
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      double epsilon = Double.NEGATIVE_INFINITY;
      
      for (int pl = 0; pl < game.getNumPlayers(); pl++) {
        double payoff = game.getPayoff(outcome, pl);
        
       // Logger.logit("\n \n Player "+pl+"'s Action Profile("+ outcome[0]+","+outcome[1]+")'s payoff "+payoff+" and deviations for the following profiles :");
        
        
        for (int a = 1; a < game.getNumActions(pl); a++) {
          if (outcome[pl] == a) continue;
          int tmp = outcome[pl];
          outcome[pl] = a;
          double btd = game.getPayoff(outcome, pl) - payoff;
          
          
          Logger.logit("\n Deviation to Outcome ("+ outcome[0]+","+ outcome[1]+") EPsilon(deviation) "+ btd);
          epsilon = Math.max(epsilon, btd);
          Logger.logit("\n Deviation to Outcome ("+ outcome[0]+","+ outcome[1]+") EPsilon(deviation) changed to "+ epsilon);
          
          
          outcome[pl] = tmp;
         // epsilon = Math.max(epsilon, btd);
        }
      }
      if (epsilon < bestEpsilon) {
        bestEpsilon = epsilon;
        Logger.logit("\n Best Epsilon yet "+ bestEpsilon);
        bestOutcome = Arrays.copyOf(outcome, outcome.length);
      }
    }

    ArrayList<MixedStrategy> strategies = new ArrayList<MixedStrategy>();
    for (int pl = 0; pl < bestOutcome.length; pl++) {
      MixedStrategy ms = new MixedStrategy(game.getNumActions(pl));
      ms.setZeros();
      ms.setProb(bestOutcome[pl], 1d);
      strategies.add(ms);
    }
    return strategies;
  }

  public static ArrayList<MixedStrategy> getMinEpsilonBoundProfile(Game game, Game upperBounds) {
    double bestEpsilon = Double.POSITIVE_INFINITY;
    int[] bestOutcome = new int[game.getNumPlayers()];

    OutcomeIterator itr = new OutcomeIterator(game);
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      double epsilon = Double.NEGATIVE_INFINITY;

      for (int pl = 0; pl < game.getNumPlayers(); pl++) {
    	//  Logger.logit("\n Player "+pl);
    	  
        double payoff = game.getPayoff(outcome, pl);
        //Logger.logit("\n \n Player "+pl+"'s Action Profile("+ outcome[0]+","+outcome[1]+")'s payoff "+payoff+" and deviations for the following profiles :");
        
        for (int a = 1; a <= game.getNumActions(pl); a++) 
        {
          //if (outcome[pl] == a) continue;
          int tmp = outcome[pl];
          outcome[pl] = a;
          double btd = upperBounds.getPayoff(outcome, pl) - payoff;
          
         // Logger.logit("\n Deviation to Outcome ("+ outcome[0]+","+ outcome[1]+") EPsilon(deviation) "+ btd);
          epsilon = Math.max(epsilon, btd);
          //Logger.logit("\n Deviation to Outcome ("+ outcome[0]+","+ outcome[1]+") EPsilon(deviation) changed to "+ epsilon);
          outcome[pl] = tmp;
         
          
        }
      }
      if (epsilon < bestEpsilon) {
        bestEpsilon = epsilon;
        
       // Logger.logit("\n Best Epsilon yet "+ bestEpsilon);
        
        bestOutcome = Arrays.copyOf(outcome, outcome.length);
      }
    }

    ArrayList<MixedStrategy> strategies = new ArrayList<MixedStrategy>();
    for (int pl = 0; pl < bestOutcome.length; pl++) {
      MixedStrategy ms = new MixedStrategy(game.getNumActions(pl));
      ms.setZeros();
      ms.setProb(bestOutcome[pl], 1d);
      strategies.add(ms);
    }
    return strategies;
  }
}