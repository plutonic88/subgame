package solvers;

import static subgame.EGAUtils.rand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import games.EmpiricalMatrixGame;
import games.Game;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import observers.GameObserver;
import support.ActionData;
import support.ActionDataInteger;

/**
 * Utility functions for game solvers
 */

public class SolverUtils {

  // static class
  private SolverUtils() {
  }

  /**
   * Normalize the vector a so that the elements sum to 1
   *
   * @param a vector to normalize
   */
  public static void normalize(double[] a) {
    double sum = 0d;
    for (double b : a) {
      sum += b;
    }
    for (double b : a) {
      b /= sum;
    }
  }

  /**
   * Computes a ranking of the actions on the given metric
   *
   * @param scores the scores of each action on a given metric
   * @return rank ordering of the actions, given the scores
   */
  public static ActionDataInteger computeRankings(ActionData scores) {
    int nPlayers = scores.getNumPlayers();
    int[] nActs = scores.getNumActions();
    ActionDataInteger ranks = new ActionDataInteger(nActs, 1);

    for (int pl = 0; pl < nPlayers; pl++) {
      List<Double> playerScores = scores.get(pl);
      for (int a = 1; a <= nActs[pl]; a++) {
        for (int a2 = a + 1; a2 <= nActs[pl]; a2++) {
          if (playerScores.get(a) > playerScores.get(a2)) {
            ranks.increment(pl, a2);
          } else if (playerScores.get(a) < playerScores.get(a2)) {
            ranks.increment(pl, a);
          }
        }
      }
    }
    return ranks;
  }

  /**
   * Count the number of appearances of each strategy in the given set of profiles
   */
  public static ActionDataInteger countStrategyAppearances(Set<int[]> profiles, int nPlayers, int[] nActs) {
    ActionDataInteger counts = new ActionDataInteger(nActs);
    for (int[] outcome : profiles) {
      for (int pl = 0; pl < nPlayers; pl++) {
        counts.set(pl, outcome[pl], counts.get(pl, outcome[pl]) + 1);
      }
    }
    return counts;
  }

  /**
   * Computes the maximum benefit to deviating to any pure strategy from this outcome
   *
   * @param g                   The game
   * @param outcomeDistribution The outcome
   * @return maximum benefit to deviating (epsilon)
   */
  public static double computeOutcomeStability(Game g, OutcomeDistribution outcomeDistribution) {
    double[] payoffs = computeOutcomePayoffs(g, outcomeDistribution, false);
    ActionData pureStrategyPayoffs = computePureStrategyPayoffs(g, outcomeDistribution, false);
    return computeStability(payoffs, pureStrategyPayoffs);
  }

  /**
   * Computes the maximum benefit to deviating, given the outcome payoffs and pure strategy payoffs
   *
   * @param payoffs             payoffs to each player for a given outcome
   * @param pureStrategyPayoffs payoffs to each players pure strategies for the given outcome
   * @return maximum benefit to deviating (epsilon)
   */
  public static double computeStability(double[] payoffs, ActionData pureStrategyPayoffs) {
    if (payoffs.length != pureStrategyPayoffs.getNumPlayers()) {
      System.err.println("Payoffs and pure strategy payoffs have different lengths in computing stability!");
      return Double.POSITIVE_INFINITY;
    }

    double epsilon = Double.NEGATIVE_INFINITY;
    int[] nActs = pureStrategyPayoffs.getNumActions();
    for (int pl = 0; pl < payoffs.length; pl++) {
      for (int a = 1; a <= nActs[pl]; a++) {
        epsilon = Math.max(epsilon, pureStrategyPayoffs.get(pl, a) - payoffs[pl]);
      }
    }
    return epsilon;
  }

  /**
   * Wrapper with default setting for ignoreUnsampled
   *
   * @param g                   the game
   * @param outcomeDistribution distribution over outcomes
   * @return vector of payoffs to each player in the given outcome
   */
  public static double[] computeOutcomePayoffs(Game g, OutcomeDistribution outcomeDistribution) {
    return computeOutcomePayoffs(g, outcomeDistribution, false);
  }

  /**
   * Given a game and a distribution over outcomes, compute the expected payoffs for each player
   *
   * @param g                   the game
   * @param outcomeDistribution distribution over outcomes
   * @param ignoreUnsampled     for empirical games, do not include the payoffs for any unsampled profiles
   * @return vector of payoffs to each player in the given outcome
   */
  public static double[] computeOutcomePayoffs(Game g, OutcomeDistribution outcomeDistribution,
                                               boolean ignoreUnsampled) {
    int nPlayers = g.getNumPlayers();
    double[] payoffs = new double[nPlayers];

    // loop through outcomes to compute the expected payoffs
    OutcomeIterator itr = g.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();

      // check to make sure this profiles has been sampled
      if (ignoreUnsampled &&
          g instanceof EmpiricalMatrixGame) {
        if (((EmpiricalMatrixGame) g).getNumSamples(outcome) <= 0) {
          continue;
        }
      }

      
      double prob =0;
      try{
       prob = outcomeDistribution.getProb(outcome);
      }
      catch(Exception ex)
      {
    	  System.out.println(ex.toString());
      }
      
      
      
      
      if (prob > 0) {
        // add in this component of the payoffs
        double[] outcomePayoffs = g.getPayoffs(outcome);
        for (int pl = 0; pl < nPlayers; pl++) {
          payoffs[pl] += prob * outcomePayoffs[pl];
        }
      }
    }
    return payoffs;
  }

  /**
   * Wrapper with default value for ingnoreUnsampled
   *
   * @param g                   the game
   * @param outcomeDistribution distribution over outcomes
   * @return the payoffs for playing each pure strategy in the game in the context of the outcome distribution
   */
  public static ActionData computePureStrategyPayoffs(Game g, OutcomeDistribution outcomeDistribution) {
    return computePureStrategyPayoffs(g, outcomeDistribution, false);
  }

  /**
   * Compute the payoffs to all pure strategies for the given outcome distributions
   * To compute the payoffs to each player's possible strategies we first maginalize the
   * distribution over that player and then compute the expected payoff wrt the opponent action profile
   *
   * @param g                   the game
   * @param outcomeDistribution distribution over outcomes
   * @param ignoreUnsampled     for empirical games, do not include the payoffs for any unsampled profiles
   * @return the payoffs for playing each pure strategy in the game in the context of the outcome distribution
   */
  public static ActionData computePureStrategyPayoffs(Game g, OutcomeDistribution outcomeDistribution,
                                                      boolean ignoreUnsampled) {
    int nPlayers = g.getNumPlayers();
    int[] nActs = g.getNumActions();
    int[] outcome = new int[nPlayers];
    ActionData expectedPayoffs = new ActionData(nActs);
    List<Integer> restrictedPlayers = new ArrayList<Integer>();

    // compute the payoff for each player's pure strategies
    for (int pl = 0; pl < nPlayers; pl++) {
      restrictedPlayers.clear();
      restrictedPlayers.add(pl);
      OutcomeDistribution conditional = outcomeDistribution.getConditionalDistribution(restrictedPlayers);

      // loop over all of the restricted outcomes, averaging the outcomes
      OutcomeIterator itr = conditional.iterator();
      while (itr.hasNext()) {
        int[] conditionalOutcome = itr.next();

        double prob = conditional.getProb(conditionalOutcome);
        if (prob <= 0) continue;

        // create the non-marginal outcomes to get the payoffs
        for (int pl2 = 0; pl2 < nPlayers; pl2++) {
          if (pl2 < pl) outcome[pl2] = conditionalOutcome[pl2];
          else if (pl2 > pl) outcome[pl2] = conditionalOutcome[pl2 - 1];
        }

        // average in the payoffs for each possible action
        for (int a = 1; a <= nActs[pl]; a++) {
          outcome[pl] = a;

          // check to make sure this profiles has been sampled
          if (g instanceof EmpiricalMatrixGame &&
              ignoreUnsampled) {
            if (((EmpiricalMatrixGame) g).getNumSamples(outcome) <= 0) {
              continue;
            }
          }

          double[] payoffs = g.getPayoffs(outcome);
          double oldValue = expectedPayoffs.get(pl, a);
          expectedPayoffs.set(pl, a, oldValue + (prob * payoffs[pl]));
        }
      }
    }
    return expectedPayoffs;
  }

  /**
   * Wrapper with default value for ignoreUnsampled
   *
   * @param g                   the game
   * @param player              player
   * @param outcomeDistribution distribution over outcomes
   * @return the payoffs for playing each pure strategy in the game in the context of the outcome distribution
   */
  public static double[] computePureStrategyPayoffs(Game g, int player,
                                                    OutcomeDistribution outcomeDistribution) {
    return computePureStrategyPayoffs(g, player, outcomeDistribution, false);
  }

  /**
   * Compute the payoffs for a given players pure strategies for the given outcome distributions
   * To compute the payoffs to the player's possible strategies we first maginalize the
   * distribution over that player and then compute the expected payoff wrt the opponent action profile
   *
   * @param g                   the game
   * @param player              player
   * @param outcomeDistribution distribution over outcomes
   * @param ignoreUnsampled     for empirical games, do not include the payoffs for any unsampled profiles
   * @return the payoffs for playing each pure strategy in the game in the context of the outcome distribution
   */
  public static double[] computePureStrategyPayoffs(Game g, int player,
                                                    OutcomeDistribution outcomeDistribution,
                                                    boolean ignoreUnsampled) {
    int nPlayers = g.getNumPlayers();
    int[] nActs = g.getNumActions();
    int[] outcome = new int[nPlayers];
    double[] payoffs = new double[nActs[player] + 1];

    List<Integer> restrictedPlayers = new ArrayList<Integer>();
    restrictedPlayers.add(player);
    OutcomeDistribution conditional = outcomeDistribution.getConditionalDistribution(restrictedPlayers);

    // loop over all of the restricted outcomes, averaging the outcomes
    OutcomeIterator itr = conditional.iterator();
    while (itr.hasNext()) {
      int[] conditionalOutcome = itr.next();
      double prob = conditional.getProb(conditionalOutcome);
      if (prob <= 0) continue;

      // create the non-marginal outcomes to get the payoffs
      for (int pl2 = 0; pl2 < nPlayers; pl2++) {
        if (pl2 < player) outcome[pl2] = conditionalOutcome[pl2];
        else if (pl2 > player) outcome[pl2] = conditionalOutcome[pl2 - 1];
      }

      // average in the payoffs for each possible action
      for (int a = 1; a <= nActs[player]; a++) {
        outcome[player] = a;

        // check to make sure this profiles has been sampled
        if (g instanceof EmpiricalMatrixGame &&
            ignoreUnsampled) {
          if (((EmpiricalMatrixGame) g).getNumSamples(outcome) <= 0) {
            continue;
          }
        }

        payoffs[a] += prob * g.getPayoffs(outcome)[player];
      }
    }
    return payoffs;
  }

  /**
   * Compute the average benefit to deviating from each action to the set of other possible actions
   */
  public static ActionData computeAveDeviationBenefit(ActionData pureStrategyPayoffs) {
    int nPlayers = pureStrategyPayoffs.getNumPlayers();
    int[] nActions = pureStrategyPayoffs.getNumActions();
    ActionData deviationBenefits = new ActionData(nActions);

    for (int pl = 0; pl < nPlayers; pl++) {
      double tot = 0;
      for (int a = 1; a <= nActions[pl]; a++) {
        tot += pureStrategyPayoffs.get(pl, a);
      }

      double nSamples = nActions[pl] - 1;
      for (int a = 1; a <= nActions[pl]; a++) {
        double aPayoff = pureStrategyPayoffs.get(pl, a);
        double aveNotA = (tot - aPayoff) / nSamples;
        deviationBenefits.set(pl, a, aveNotA - aPayoff);
      }
    }
    return deviationBenefits;
  }

  /**
   * Compute the benefit of deviating to each possible action, given a vector of baseline payoffs
   */
  public static ActionData computeDeviationBenefit(ActionData pureStrategyPayoffs, double[] baselinePayoffs) {
    int nPlayers = pureStrategyPayoffs.getNumPlayers();
    int[] nActions = pureStrategyPayoffs.getNumActions();
    ActionData deviationBenefits = new ActionData(nActions);

    for (int pl = 0; pl < nPlayers; pl++) {
      for (int a = 1; a <= nActions[pl]; a++) {
        double aPayoff = pureStrategyPayoffs.get(pl, a);
        deviationBenefits.set(pl, a, baselinePayoffs[pl] - aPayoff);
      }
    }
    return deviationBenefits;
  }

  /**
   * Compute the benefit for deviating from each pure strategy to the best pure strategy
   */
  public static ActionData computeMaxDeviationBenefit(ActionData pureStrategyPayoffs) {
    int nPlayers = pureStrategyPayoffs.getNumPlayers();
    int[] nActions = pureStrategyPayoffs.getNumActions();
    ActionData deviationBenefits = new ActionData(nActions);

    for (int pl = 0; pl < nPlayers; pl++) {
      double maxPayoff = Double.NEGATIVE_INFINITY;
      for (int a = 1; a <= nActions[pl]; a++) {
        maxPayoff = Math.max(maxPayoff, pureStrategyPayoffs.get(pl, a));
      }

      for (int a = 1; a <= nActions[pl]; a++) {
        double aPayoff = pureStrategyPayoffs.get(pl, a);
        deviationBenefits.set(pl, a, aPayoff - maxPayoff);
      }
    }
    return deviationBenefits;
  }

  /**
   * Collect N samples for all profiles in the game
   */
  public static void sampleAllProfiles(GameObserver go, EmpiricalMatrixGame eGame, int numSamples) {
    OutcomeIterator itr = new OutcomeIterator(go);
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      for (int i = 0; i < numSamples; i++) {
        double[] result = go.getSample(outcome);
        eGame.addSample(outcome, result);
      }
    }
  }

  /**
   * Collect N samples for all profiles in a subgame
   * Returns false if we run out of samples
   */
  public static boolean sampleSubgame(GameObserver go, EmpiricalMatrixGame eGame,
                                      int numSamples, List<List<Integer>> subGameActions) {
    int nPlayers = subGameActions.size();
    int[] nActions = new int[nPlayers];
    for (int pl = 0; pl < nPlayers; pl++) {
      nActions[pl] = subGameActions.get(pl).size();
      if (nActions[pl] <= 0) return true;
    }

    OutcomeIterator itr = new OutcomeIterator(nPlayers, nActions);
    int[] outcome = new int[nPlayers];
    while (itr.hasNext()) {
      int[] subGameOutcome = itr.next();
      for (int pl = 0; pl < nPlayers; pl++) {
        outcome[pl] = subGameActions.get(pl).get(subGameOutcome[pl] - 1);
      }
      for (int i = 0; i < numSamples; i++) {
        eGame.addSample(outcome, go.getSample(outcome));
        if (go.numObsLeft() <= 0) return false;
      }
      if (go.numObsLeft() <= 0) return false;
    }
    return true;
  }

  /**
   * Collect extra samples for each profile to bring it up to the desired number of samples
   * Returns false if we run out of samples
   */
  public static boolean sampleSubgameToMinimum(GameObserver go, EmpiricalMatrixGame eGame,
                                               int desiredSamples, List<List<Integer>> subGameActions) {
    int nPlayers = subGameActions.size();
    int[] nActions = new int[nPlayers];
    for (int pl = 0; pl < nPlayers; pl++) {
      nActions[pl] = subGameActions.get(pl).size();
      if (nActions[pl] <= 0) return true;
    }

    OutcomeIterator itr = new OutcomeIterator(nPlayers, nActions);
    int[] outcome = new int[nPlayers];
    while (itr.hasNext()) {
      int[] subGameOutcome = itr.next();
      for (int pl = 0; pl < nPlayers; pl++) {
        outcome[pl] = subGameActions.get(pl).get(subGameOutcome[pl] - 1);
      }
      int numNeeded = desiredSamples - eGame.getNumSamples(outcome);
      for (int i = 0; i < numNeeded; i++) {
        if (go.numObsLeft() <= 0) return false;
        eGame.addSample(outcome, go.getSample(outcome));
      }
      if (go.numObsLeft() <= 0) return false;
    }
    return true;
  }

  /**
   * Collect N samples for uniform random profiles (with replacement)
   */
  public static void sampleRandomlyWithReplacement(GameObserver go, EmpiricalMatrixGame eGame, int totalSamples,
                                                   int samplesPerProfile) {
    int nPlayers = go.getNumPlayers();
    int[] nActions = go.getNumActions();
    int[] outcome = new int[nPlayers];

    int bound = go.getNumObs() + totalSamples;
    while (go.getNumObs() < bound) {
      for (int player = 0; player < nPlayers; player++) {
        outcome[player] = rand.nextInt(nActions[player]) + 1;
      }

      for (int i = 0; i < samplesPerProfile; i++) {
        double[] result = go.getSample(outcome);
        eGame.addSample(outcome, result);
        if (go.getNumObs() == bound) break;
      }
    }
  }

  /**
   * "Without replacement" here translates to "without exceeding the given samples per profile limit"
   */
  public static void sampleRandomlyWithoutReplacement(GameObserver go, EmpiricalMatrixGame eGame, int totalSamples,
                                                      int samplesPerProfile) {
    int nPlayers = go.getNumPlayers();
    int[] nActions = go.getNumActions();
    int[] outcome = new int[nPlayers];

    int bound = go.getNumObs() + totalSamples;
    int maxGameSamples = eGame.getNumProfiles() * samplesPerProfile;
    while (go.getNumObs() < bound &&
           eGame.getTotalNumSamples() < maxGameSamples) {
      for (int player = 0; player < nPlayers; player++) {
        outcome[player] = rand.nextInt(nActions[player]) + 1;
      }

      for (int i = eGame.getNumSamples(outcome); i < samplesPerProfile; i++) {
        double[] result = go.getSample(outcome);
        eGame.addSample(outcome, result);
        if (go.getNumObs() == bound) break;
      }
    }
  }
}
