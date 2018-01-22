package games;

import java.util.ArrayList;
import java.util.Arrays;

import util.Entropy;

/**
 * Utility functions for manipulating games
 */

public class GameUtils {

  // static class
  private GameUtils() {
  }

  /**
   * Create a symmetric version of the given game by averaging the payoffs across the
   * possible permutations.
   *
   * NOTE: the game must have symmetric actions sets to do this (equal numbers of actions for each player)
   * Returns null if this condition is not met
   */
  public static SymmetricEmpiricalMatrixGame symmetrifyGame(Game game) {
    int[] allActions = game.getNumActions();
    int nActions = allActions[0];

    // check to make sure that the number of actions is the same for all players
    for (int i = 1; i < allActions.length; i++) {
      if (allActions[i] != nActions) return null;
    }

    SymmetricEmpiricalMatrixGame newGame = new SymmetricEmpiricalMatrixGame(game.getNumPlayers(), nActions);

    OutcomeIterator itr = game.iterator();
    while(itr.hasNext()) {
      int[] outcome = itr.next();
      newGame.addSample(outcome, game.getPayoffs(outcome));
    }
    return newGame;
  }
    // computes the exploitability of the given mixed strategy for the given player in the game
    public static double computeExploitability(Game game, MixedStrategy strategy, int player) {
        double worstCase = Double.POSITIVE_INFINITY;
        OutcomeIterator itr = game.iterator();
        while(itr.hasNext()) {
            int[] tmp = itr.next();
            int[] outcome = Arrays.copyOf(tmp, tmp.length);
            double payoff = 0;
            //System.out.println(player);
            for (int action = 1; action <= game.getNumActions(player); action++) {
              outcome[player] = action;
              payoff += strategy.getProb(action) * game.getPayoff(outcome, player);
            }
            if (payoff < worstCase) {
              worstCase = payoff;
            }
        }
        return worstCase;
    }
    public static boolean weaklyDominates(Game game, int player, int action1, int action2,ArrayList<Integer>ignore){
        double payoffs1 = 0.0;
        double payoffs2 = 0.0;
        int[] outcome = new int[2];
        for(int i =1;i<=game.getNumActions(player);i++){
            if(!ignore.contains(new Integer(i-1))){
                if(player==0){
                outcome[0]=action1+1;
                outcome[1]=i;
                }
                else{
                    outcome[0]=i;
                    outcome[1]=action1+1;
                }
                payoffs1 = game.getPayoff(outcome,player);
                if(player==0){
                outcome[0]=action2+1;
                outcome[1]=i;
                }
                else{
                    outcome[0]=i;
                    outcome[1]=action2+1;
                }
                payoffs2 = game.getPayoff(outcome,player);
                if(payoffs1<payoffs2)
                    return false;
            }
        }
        return true;//action1 weakly dominates action2
    }
    public static boolean equivalent(Game game, int player, int action1, int action2,ArrayList<Integer>ignore){
        double payoffs1 = 0.0;
        double payoffs2 = 0.0;
        int[] outcome = new int[2];
        for(int i =1;i<=game.getNumActions(player);i++){
            if(!ignore.contains(new Integer(i-1))){
                if(player==0){
                outcome[0]=action1+1;
                outcome[1]=i;
                }
                else{
                    outcome[0]=i;
                    outcome[1]=action1+1;
                }
                payoffs1 = game.getPayoff(outcome,player);
                if(player==0){
                outcome[0]=action2+1;
                outcome[1]=i;
                }
                else{
                    outcome[0]=i;
                    outcome[1]=action2+1;
                }
                payoffs2 = game.getPayoff(outcome,player);
                if(payoffs1!=payoffs2)
                    return false;
            }
        }
        return true;//action1 weakly dominates action2
    }

    /**
     * Method to calculate stability values
     * @param solverPayoffs the payoffs for the solver
     * @return stability matrix
     */
    public static double[] getStability(double[][] solverPayoffs) {
        double[] stabilities = new double[solverPayoffs.length];
        Arrays.fill(stabilities, Double.NEGATIVE_INFINITY);
        for (int strat = 0;  strat < solverPayoffs.length; strat++) {
            double base = solverPayoffs[strat][strat];
            for (int row = 0; row < solverPayoffs.length; row++) {
                if (row == strat)
                    continue;
                double btd = solverPayoffs[row][strat] - base;
                stabilities[strat] = Math.max(stabilities[strat], btd);
            }
        }
        return stabilities;
      }

    /**
     * Finds the number of actions in a strategy that have a probability greater than some threshold
     * @param ms Mixed Strategy
     * @param threshold probability threshold
     * @return
     */
    public static double computeSupport(MixedStrategy ms, double threshold){
        int support = 0;
        double[] probs = ms.getProbs();
        for(int i = 1; i < probs.length; i++)
            if(probs[i] > threshold)
                support++;
        return (double)support / ((double)probs.length-1);
    }

    /**
     * Computers the entropy of a strategy
     * entro = - E[ p_i * log_2(p_i) ]
     *
     */
    public static double computeEntropy(MixedStrategy ms){
        /*double entro = 0;
        double[] probs = ms.getProbs();
        for(int i = 0; i < probs.length; i++)
            entro += probs[i] * Math.log(probs[i]);

        return entro * -1;*/
        return Entropy.calculateEntropy(removeFirstElement(ms.getProbs()));
    }

    /**
     * Makes an array without the first element
     * @param a original array
     * @return smaller array
     */
    private static double[] removeFirstElement(double[] a){
        double[] b = new double[a.length-1];
        for(int i = 0; i < b.length; i++)
            b[i] = a[i+1];
        return b;
    }
}
