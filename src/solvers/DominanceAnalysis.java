package solvers;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.nf;
import static subgame.EGAUtils.returnSB;

import java.util.ArrayList;
import java.util.List;

import games.EmpiricalMatrixGame;
import games.OutcomeIterator;

/**
 * Utilities to analyze dominance relationships in games
 * <p/>
 * TODO: Add some error checking
 */

public final class DominanceAnalysis {

  private static final int DEFAULT_SAMPLE_THRESHOLD = 0;

  // The threshold for determining whether to consider profiles sampled
  // or unsampled. Unsampled do not count towards the analysis
  private int sampleThreshold = DEFAULT_SAMPLE_THRESHOLD;
  private final int nPlayers;
  private final int[] nActs;

  // the actual comparison data
  private final ArrayList<PairedComparison[][]> comparisons = new ArrayList<PairedComparison[][]>();

  /**
   * Create a new dominance analysis object, running the analysis for the given game
   * <p/>
   * TODO: optimize for symmetry
   */
  public DominanceAnalysis(EmpiricalMatrixGame game, int sampleThreshold) {
    this.sampleThreshold = sampleThreshold;
    nPlayers = game.getNumPlayers();
    nActs = game.getNumActions();

    for (int pl = 0; pl < nPlayers; pl++) {
      int numActs = nActs[pl];
      PairedComparison[][] comp = new PairedComparison[numActs + 1][numActs + 1];

      for (int i = 1; i <= numActs; i++) {
        for (int j = 1; j <= numActs; j++) {
          comp[i][j] = compare(pl, i, j, game);
        }
      }
      comparisons.add(comp);
    }
  }

  /**
   * Returns the dominant strategy for this player if one exists, or -1 if one does not
   */
  public int findDominant(int player, boolean strict) {
    PairedComparison[][] tmp = comparisons.get(player);

    for (int i = 1; i < tmp.length; i++) {
      for (int j = 1; j < tmp[i].length; j++) {
        // skip comparison with itself
        if (i == j) continue;

        // not enough samples to confirm that i dominates j
        if (tmp[i][j].numSampled != tmp[i][j].numComparisons) break;

        // i does not dominate j
        if (strict) {
          if (tmp[i][j].maxBenefit >= 0d) break;
        } else {
          if (tmp[i][j].maxBenefit > 0d) break;
        }

        // we have checked that i dominates all other strategies
        if (j == tmp[i].length - 1) return i;
      }
    }
    return -1;
  }

  /**
   * Returns a list of all dominated actions for the given player
   */
  public List<Integer> findDominated(int player, boolean strict) {
    PairedComparison[][] tmp = comparisons.get(player);
    List<Integer> dominated = new ArrayList<Integer>();

    for (int i = 1; i < tmp.length; i++) {
      for (int j = 1; j < tmp[i].length; j++) {
        // skip comparison with itself
        if (i == j) continue;

        if (dominates(player, i, j, strict)) {
          dominated.add(i);
          break;
        }
      }
    }
    return dominated;
  }

  /**
   * Returns a count of the number of other actions that dominate each action
   */
  public List<Integer> dominatedCounts(int player, boolean strict) {
    PairedComparison[][] tmp = comparisons.get(player);
    List<Integer> counts = new ArrayList<Integer>();
    counts.add(Integer.MIN_VALUE);

    for (int i = 1; i < tmp.length; i++) {
      int tmpCnt = 0;
      for (int j = 1; j < tmp[i].length; j++) {
        // skip comparison with itself
        if (i == j) continue;
        if (dominates(player, i, j, strict)) {
          tmpCnt++;
        }
      }
      counts.add(tmpCnt);
    }
    return counts;
  }

  /**
   * Check whether strategy 1 dominates strategy 2
   */
  public boolean dominates(int player, int act1, int act2, boolean strict) {
    // check that strategies are not equal
    if (act1 == act2) return false;

    PairedComparison tmp = comparisons.get(player)[act2][act1];

    // make sure everything is sampled
    if (tmp.numSampled != tmp.numComparisons) return false;

    // check for beneficial deviations
    if (strict) {
      if (tmp.maxBenefit < 0d) {
        return true;
      }
    } else {
      if (tmp.maxBenefit <= 0d) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the total number of possible opponent contexts for a
   * deviation from strategy 1 to strategy 2
   */
  public int getNumDeviations(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    return tmp.numComparisons;
  }

  /**
   * Returns the number of possible deviations from strategy 1 to strategy 2
   * that are sampled, given the threshold number of samples
   */
  public int getNumSampledDeviations(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    return tmp.numSampled;
  }

  /**
   * Returns the percentage of possible deviations from strategy 1 to strategy 2
   * that are sampled, given the threshold number of samples
   */
  public double getPercentSampledDeviations(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    if (tmp.numComparisons > 0) {
      return (double) tmp.numSampled / (double) tmp.numComparisons;
    } else {
      return 0d;
    }
  }

  /**
   * Returns the number of beneficial deviations from strategy 1 to strategy 2
   */
  public int getNumBeneficialDeviations(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    return tmp.numBeneficial;
  }

  /**
   * Returns the percentage of sampled deviations from strategy 1 to strategy 2 that were beneficial
   */
  public double getPercentBeneficialDeviations(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    if (tmp.numSampled > 0) {
      return (double) tmp.numBeneficial / (double) tmp.numSampled;
    } else {
      return 0d;
    }
  }

  /**
   * Returns the maximum (sampled) benefit to deviating from stategy 1 to strategy 2
   * Note that this may be negative
   */
  public double getMaxDeviationBenefit(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    return tmp.maxBenefit;
  }

  /**
   * Returns the average (sampled) benefit to deviating from stategy 1 to strategy 2
   * Note that this may be negative
   */
  public double getAveDeviationBenefit(int player, int act1, int act2) {
    PairedComparison tmp = comparisons.get(player)[act1][act2];
    if (tmp.numSampled > 0) {
      return tmp.totBenefit / tmp.numSampled;
    } else {
      return 0d;
    }
  }

  /**
   * Returns a string representation of the number of actions that dominate each action
   */
  public String outputDominatedCounts(int player, boolean strict) {
    StringBuilder sb = getSB();
    List<Integer> counts = dominatedCounts(player, strict);

    sb.append("Player ").append(player).append(": ");
    for (int tmp : counts) {
      sb.append(tmp).append(" ");
    }
    sb.append("\n");
    return returnSB(sb);
  }

  /**
   * Generates a string representation of the boolean dominance relationships
   */
  public String outputBooleanDominance(boolean strict, boolean symmetric) {
    StringBuilder sb = getSB();

    // There is a matrix of relationships for each player
    for (int pl = 0; pl < nPlayers; pl++) {
      PairedComparison[][] tmp = comparisons.get(pl);

      sb.append("Dominance Relationships ");
      if (!symmetric) {
        sb.append("for player ").append(pl).append("\n");
      }
      sb.append("---------------------------------------------\n");

      for (int i = 1; i < tmp.length; i++) {
        for (int j = 1; j < tmp[i].length; j++) {
          if (tmp[i][j].numSampled != tmp[i][j].numComparisons) {
            sb.append("? ");
          } else if (strict && tmp[i][j].maxBenefit < 0d) {
            sb.append("1 ");
          } else if (!strict && tmp[i][j].maxBenefit <= 0d) {
            sb.append("1 ");
          } else {
            sb.append("0 ");
          }
        }
        sb.append("\n");
      }
      sb.append("\n");

      if (symmetric) break;
    }
    return returnSB(sb);
  }

  /**
   * Generates a string representation of the maximum benefits to deviating
   */
  public String outputMaxDeviationBenefits(boolean symmetric) {
    StringBuilder sb = getSB();

    // There is a matrix of relationships for each player
    for (int pl = 0; pl < nPlayers; pl++) {
      PairedComparison[][] tmp = comparisons.get(pl);

      sb.append("Maximum deviation benefits ");
      if (!symmetric) {
        sb.append("for player ").append(pl);
      }
      sb.append("\n");
      sb.append("-------------------------------------------------\n");

      for (int i = 1; i < tmp.length; i++) {
        for (int j = 1; j < tmp[i].length; j++) {
          if (tmp[i][j].numSampled <= 0) {
            sb.append("? ");
          } else {
            sb.append("[").append(i).append(",").append(j).append("] ");
            sb.append(nf.format(tmp[i][j].maxBenefit)).append(" ");
          }
        }
        sb.append("\n");
      }
      sb.append("\n");
      if (symmetric) break;
    }
    return returnSB(sb);
  }

  /**
   * Generates a string representation of the average benefits to deviating
   */
  public String outputAverageDeviationBenefits(boolean symmetric) {
    StringBuilder sb = getSB();

    // There is a matrix of relationships for each player
    for (int pl = 0; pl < comparisons.size(); pl++) {
      PairedComparison[][] tmp = comparisons.get(pl);

      sb.append("Average deviation benefits ");
      if (!symmetric) {
        sb.append("for player ").append(pl);
      }
      sb.append("\n");
      sb.append("-------------------------------------------------\n");

      for (int i = 1; i < tmp.length; i++) {
        for (int j = 1; j < tmp[i].length; j++) {
          sb.append("[").append(i).append(",").append(j).append("] ");
          if (tmp[i][j].numSampled <= 0) {
            sb.append("? ");
          } else {
            double ave = tmp[i][j].totBenefit / (double) tmp[i][j].numSampled;
            sb.append(nf.format(ave)).append(" ");
          }
        }
        sb.append("\n");
      }
      sb.append("\n");
      if (symmetric) break;
    }
    return returnSB(sb);
  }

  /**
   * Returns a string representation for the partial ranking defined by the dominance relationship
   */
  public String outputPartialRankings(boolean symmetric) {
    StringBuilder sb = getSB();
    for (int pl = 0; pl < nPlayers; pl++) {
      sb.append(outputPartialRanking(pl));
      sb.append("\n");
      if (symmetric) break;
    }
    return returnSB(sb);
  }

  /**
   * Returns a string representation for the partial ranking defined by the dominance relationship
   */
  public String outputPartialRanking(int player) {
    List<List<Integer>> ranking = computePartialRanking(player);
    StringBuilder sb = getSB();

    sb.append("Partial ranking of strategies for player: ").append(player).append("\n");
    sb.append("--------------------------------------------------\n");

    for (List<Integer> equivalenceSet : ranking) {
      for (int j = 0; j < equivalenceSet.size() - 1; j++) {
        sb.append(equivalenceSet.get(j)).append(", ");
      }
      sb.append(equivalenceSet.get(equivalenceSet.size() - 1)).append("\n");
    }
    return returnSB(sb);
  }

  /**
   * Computes a partial ranking of the strategies for a player, based on the dominance relationships
   */
  public List<List<Integer>> computePartialRanking(int player) {
    List<List<Integer>> ranking = new ArrayList<List<Integer>>();
    PairedComparison[][] cmp = comparisons.get(player);

    // initialize the ranking with the first strategy
    List<Integer> tmp = new ArrayList<Integer>();
    tmp.add(1);
    ranking.add(tmp);

    // insert each strategy into the ranking
    for (int strat = 2; strat < cmp.length; strat++) {

      // loop through the equivalence sets
      for (int i = 0; i < ranking.size(); i++) {

        List<Integer> equivalenceSet = ranking.get(i);

        boolean dominatesAll = true;
        boolean dominatedByAll = true;

        // figure out the relationship with this equivalance set
        for (int strat2 : equivalenceSet) {
          PairedComparison pc = cmp[strat][strat2];
          if (pc.numSampled < pc.numComparisons || pc.maxBenefit >= 0d) {
            dominatesAll = false;
          }

          pc = cmp[strat2][strat];
          if (pc.numSampled < pc.numComparisons || pc.maxBenefit >= 0d) {
            dominatedByAll = false;
          }
        }

        // dominated by all strategies in the current set
        // keep going, unless this is the last existing set
        if (dominatedByAll) {
          if (i == ranking.size() - 1) {
            tmp = new ArrayList<Integer>();
            tmp.add(strat);
            ranking.add(tmp);
            break;
          }
        }

        // dominates everything in this set, but dominated by all above it;
        // create a new equivalance set for this strategy
        else if (dominatesAll) {
          tmp = new ArrayList<Integer>();
          tmp.add(strat);
          ranking.add(i, tmp);
          break;
        }

        // relationship with strategies in this set not defined by dominance;
        // include it in the equilvalance class
        else {
          equivalenceSet.add(strat);
          break;
        }
      }
    }
    return ranking;
  }

  /**
   * Compute the actual comparisons of two strategies based on the possible deviations
   */
  private PairedComparison compare(int player, int act1, int act2, EmpiricalMatrixGame game) {

    // create an iterator to loop through the possible outcomes of other players's actions
    int[] numActsRestricted = new int[nPlayers - 1];
    for (int pl = 0; pl < nPlayers; pl++) {
      if (pl < player) {
        numActsRestricted[pl] = nActs[pl];
      } else if (pl > player) {
        numActsRestricted[pl - 1] = nActs[pl];
      }
    }

    OutcomeIterator itr = new OutcomeIterator(nPlayers - 1, numActsRestricted);
    PairedComparison cmp = new PairedComparison();

    int[] outcome1 = new int[nPlayers];
    int[] outcome2 = new int[nPlayers];
    outcome1[player] = act1;
    outcome2[player] = act2;

    // loop over each outcome of opponent actions and run the comparisons
    while (itr.hasNext()) {
      int[] outcome = itr.next();

      // increment the total number of comparisons
      cmp.numComparisons++;

      // create the unrestricted profiles
      for (int pl = 0; pl < nPlayers; pl++) {
        if (pl < player) {
          outcome1[pl] = outcome[pl];
          outcome2[pl] = outcome[pl];
        } else if (pl > player) {
          outcome1[pl] = outcome[pl - 1];
          outcome2[pl] = outcome[pl - 1];
        }
      }

      int samples1 = game.getNumSamples(outcome1);
      int samples2 = game.getNumSamples(outcome2);

      // skip any profiles that do not have enough samples
      if (samples1 < sampleThreshold || samples2 < sampleThreshold) {
        continue;
      }

      double[] payoffs1 = game.getPayoffs(outcome1);
      double[] payoffs2 = game.getPayoffs(outcome2);
      double diff = payoffs2[player] - payoffs1[player];

      // increment number of sampled comparisons
      cmp.numSampled++;

      // beneficial deviation
      if (diff > 0) {
        cmp.numBeneficial++;
      }

      // set the maximum benefit to deviating
      if (diff > cmp.maxBenefit) {
        cmp.maxBenefit = diff;
      }

      // add to the total
      cmp.totBenefit += diff;
    }
    return cmp;
  }

  /**
   * Encapsulates a comparison of two possible actions for a player,
   * based on deviating from one to another in all possible opponent contexts
   * <p/>
   * Always assumes a deviation from strategy 1 to strategy 2 when computing max
   */
  private static class PairedComparison {
    int numComparisons = 0;
    int numSampled = 0;
    int numBeneficial = 0;
    double maxBenefit = Double.NEGATIVE_INFINITY;
    double totBenefit = 0d;
  }
}
