package games;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends the game object to a symmetric game
 */

public abstract class SymmetricGame extends Game {

  protected final int nSymmetricProfiles;
  protected final int nSymmetricActions;

  /**
   * Constructor for a new symmetric matrix game.
   * All players must have the same number of actions (by definition)
   */
  public SymmetricGame(int numPlayers, int numActions) {
    super(numPlayers, numActions);
    this.nSymmetricActions = numActions;

    // number of profiles in a symmetric game is (N+S-1) choose (N)
    nSymmetricProfiles = getNChooseR(nPlayers + nSymmetricActions - 1, nPlayers);
  }

  /**
   * The number of symmetric outcome profiles
   */
  public int getNumSymmetricProfiles() {
    return nSymmetricProfiles;
  }

  /**
   * The number of symmetric actions (equal for all players)
   */
  public int getNumSymmetricActions() {
    return nSymmetricActions;
  }

  /**
   * Returns a map representing the counts of each action in the given outcome
   */
  public Map<Integer, Integer> createCounts(int[] outcome) {
    Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
    for (int anOutcome : outcome) {
      if (counts.containsKey(anOutcome)) {
        counts.put(anOutcome, counts.get(anOutcome) + 1);
      } else {
        counts.put(anOutcome, 1);
      }
    }
    return counts;
  }

  /**
   * Returns a map representing the average payoff for each action in the given outcome profile/payoff vector
   */
  public Map<Integer, Double> createSymmetricPayoffs(int[] outcome, Map<Integer, Integer> counts, double[] payoffs) {
    Map<Integer, Double> symmetricPayoffs = new HashMap<Integer, Double>();

    // find the total payoff for each action
    for (int pl = 0; pl < outcome.length; pl++) {
      if (symmetricPayoffs.containsKey(outcome[pl])) {
        symmetricPayoffs.put(outcome[pl], symmetricPayoffs.get(outcome[pl]) + payoffs[pl]);
      } else {
        symmetricPayoffs.put(outcome[pl], payoffs[pl]);
      }
    }

    // take the average (divide by the counts)
    for (Map.Entry<Integer, Double> entry : symmetricPayoffs.entrySet()) {
      symmetricPayoffs.put(entry.getKey(), entry.getValue() / counts.get(entry.getKey()));
    }
    return symmetricPayoffs;
  }

  public Map<Integer, Double> createSymmetricPayoffs(Map<Integer, Integer> counts, double[] payoffs) {
    Map<Integer, Double> symmetricPayoffs = new HashMap<Integer, Double>();
    int cnt = 0;
    for (int a : counts.keySet()) {
      symmetricPayoffs.put(a, payoffs[cnt]);
      cnt++;
    }
    return symmetricPayoffs;
  }

  /**
   * Maps the symmetric payoffs into an asymmetric version for the given outcome
   */
  public double[] mapSymmetricPayoffs(int[] outcome, Map<Integer, Double> actionPayoffs) {
    double[] tmpPayoffs = new double[outcome.length];
    for (int pl = 0; pl < outcome.length; pl++) {
      tmpPayoffs[pl] = actionPayoffs.get(outcome[pl]);
    }
    return tmpPayoffs;
  }

  /**
   * Returns the "canonical" version of this profile (maps all permutations into the same profile)
   */
//   public int[] getCanonicalOutcome(int[] outcome) {
//     int[] canonicalOutcome = outcome.clone();
//     Arrays.sort(canonicalOutcome);
//     return canonicalOutcome;
//   }

  /**
   * Get an iterator for looping over the outcomes of this game
   */
  public SymmetricOutcomeIterator symmetricIterator() {
    return new SymmetricOutcomeIterator(this);
  }

  /*
   * Static method that comptutes n choose r
   */
  public static int getNChooseR(int n, int r) {
    long nFact = getFactorial(n);
    long rFact = getFactorial(r);
    long nminusrFact = getFactorial(n - r);
    return (int) (nFact / (rFact * nminusrFact));
  }

  /*
   * Static method for computing factorials
   */
  private static long getFactorial(int n) {
    long fact = 1;
    for (long i = n; i > 1; i--) {
      fact *= i;
    }
    return fact;
  }
}
