/**
 * 
 */
package solvers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import games.Game;
import games.MatrixGame;

/**
 * @author Asa
 * Class for performing iterated elimination of dominated strategies (IEDS) on
 * matrix games.
 */
public class IEDSMatrixGames {

  /* argument value for initProfile */
  public static final int NO_FIXED_PLAYER = -1;

  /* return values for dominates */
  public static final int STRICTLY_DOMINATED_BY = -2;
  public static final int WEAKLY_DOMINATED_BY = -1;
  public static final int EQUIVALENT = 0;
  public static final int WEAKLY_DOMINATES = 1;
  public static final int STRICTLY_DOMINATES = 2;
  public static final int INCOMPARABLE = 3;

  /**
   * Initializes an action profile according to a given array of iterators
   * over actions. For use with nextProfile.
   * 
   * @param game
   *            the game from which strategies are being taken
   * @param player
   *            the index of the player whose strategy to leave fixed, or else
   *            NO_FIXED_PLAYER
   * @param profile
   *            the action profile to initialize
   * @param remainingIts
   *            array containing, for each player, an iterator over actions
   */
  private static void initProfile(Game game, int player, int[] profile,
      Iterator<Integer>[] remainingIts) {
    for (int p = 0; p < game.getNumPlayers(); p++) {
      if (p != player) {
        profile[p] = remainingIts[p].next();
      }
    }
  }

  /**
   * Modifies a profile to the "next" one to examine, for iterating through
   * opponent profiles.
   * 
   * @TODO: Make this an iterator?
   * @param game
   *            the game strategy profiles for which are being considered
   * @param player
   *            the index of the player whose strategy to leave fixed, or else
   *            NO_FIXED_PLAYER
   * @param profile
   *            the current strategy profile to increment
   * @param remaining
   *            array of sets containing, for each player, the strategies to
   *            loop over
   * @param remainingIts
   *            iterators tracking the next strategy of each player in the
   *            iteration
   * @return the modified profile, starting with action 1 and ending with n
   *         for every opponent, or null if the input was already maxed (or
   *         null)
   */
  private static int[] nextProfile(Game game, int player,
      int[] profile, Set<Integer>[] remaining,
      Iterator<Integer>[] remainingIts) {
    if (profile == null) {
      return null;
    }
    /*
     * loops through strategies for each player p, with the highest-indexed
     * player given the outermost loop example, for player=2, where each
     * player has three pure strategies:
     * (0,0,X,0)->(1,0,X,0)->(2,0,X,0)->(0,
     * 1,X,0)->...->(1,2,X,2)->(2,2,X,2)->null->null
     */
    int p = 0;
    while (true) {
      /* skip the fixed player */
      if (p == player) {
        p++;
        if (p == game.getNumPlayers()) {
          return null;
        }
      }
      /*
       * reset opponents with maxed strategies before incrementing the
       * lowest unmaxed one ("carrying")
       */
      if (!remainingIts[p].hasNext()) {
        remainingIts[p] = remaining[p].iterator();
        assert (remainingIts[p].hasNext());
        profile[p] = remainingIts[p].next();
        /* advance to the next player during the next pass */
        p++;
        /* we've exhausted every combination of strategies */
        if (p == game.getNumPlayers()) {
          return null;
        }
      }
      /* found a player whose strategy we can advance */
      else {
        profile[p] = remainingIts[p].next();
        return profile;
      }
    }
  }

  /**
   * Compares two pure strategies to determine dominance.
   * 
   * @param game
   *            the matrix game from which the strategies are taken
   * @param player
   *            the player whose strategies to compare
   * @param strat1
   *            index of one strategy to compare
   * @param strat2
   *            index of the other strategy
   * @param remaining
   *            array of strategies still under consideration for each player
   * @return constant comparing the two strategies: STRICTLY_DOMINATED_BY if
   *         strat2 strictly dominates strat1 WEAKLY_DOMINATED_BY if strat2
   *         weakly dominates strat1 EQUIVALENT if the strategies have equal
   *         payoffs for all opponent profiles WEAKLY_DOMINATES if strat1
   *         weakly dominates strat2 STRICTLY_DOMINATES if strat1 strictly
   *         dominates strat2 INCOMPARABLE if none of the above holds
   */
  public static int dominates(Game game, int player, int strat1,
      int strat2, Set<Integer>[] remaining) {
    /*
     * track whether strat1 can dominate/weakly dominate strat2, and
     * vice-versa
     */
    boolean oneDomTwo = true;
    boolean oneWeakTwo = true;
    boolean twoDomOne = true;
    boolean twoWeakOne = true;

    double value1;
    double value2;
    int players = game.getNumPlayers();

    Iterator<Integer>[] remainingIts = new Iterator[players];
    for (int p = 0; p < players; p++) {
      remainingIts[p] = remaining[p].iterator();
    }

    int[] opponentProfile = new int[players];
    initProfile(game, player, opponentProfile, remainingIts);

    while (opponentProfile != null) {
      opponentProfile[player] = strat1;
      value1 = game.getPayoff(opponentProfile, player);
      opponentProfile[player] = strat2;
      value2 = game.getPayoff(opponentProfile, player);
      if (value1 == value2) {
        /*
         * tied score for this opponent profile, so cannot have strict
         * domination
         */
        oneDomTwo = twoDomOne = false;
      } else if (value1 < value2) {
        oneDomTwo = oneWeakTwo = false;
      } else if (value2 < value1) {
        twoDomOne = twoWeakOne = false;
      }
      /* if we can decide the actions are incomparable, return */
      if (!(oneDomTwo || oneWeakTwo || twoDomOne || twoWeakOne)) {
        return INCOMPARABLE;
      }
      opponentProfile = nextProfile(game, player, opponentProfile,
          remaining, remainingIts);
    }
    if (oneDomTwo) {
      return STRICTLY_DOMINATES;
    }
    if (twoDomOne) {
      return STRICTLY_DOMINATED_BY;
    }
    if (oneWeakTwo && twoWeakOne) {
      return EQUIVALENT;
    }
    if (oneWeakTwo) {
      return WEAKLY_DOMINATES;
    }
    if (twoWeakOne) {
      return WEAKLY_DOMINATED_BY;
    }
    assert (false); /* if none of the above holds, we return in the loop */
    return INCOMPARABLE;
  }

  /**
   * Performs iterated elimination of dominated strategies non-destructively
   * on the given game.
   * 
   * @param game
   *            the game to simplify
   * @return simplified game containing no dominated strategies for any player
   */
  public static Game IEDS(Game game) 
  {
    int players = game.getNumPlayers();
    /*
     * remaining[player] contains indices of strategies for player that have
     * not yet been eliminated
     */
    Set<Integer>[] remaining = new HashSet[players];
    for (int player = 0; player < players; player++) 
    {
      remaining[player] = new HashSet<Integer>();
      for (int i = 1; i <= game.getNumActions(player); i++) 
      {
        remaining[player].add(i);
      }
    }

    int cmp;
    /*
     * tracks whether at least one strategy has been eliminated this
     * pass--if not, we can stop
     */
    boolean proceed = true;
    while (proceed) {
      proceed = false;
      for (int player = 0; player < players; player++) {
        /*
         * after finding one dominated strategy for one player, advance
         * to the next player
         */
        OUT: for (int strat1 : remaining[player]) {
          for (int strat2 : remaining[player]) {
            if (strat1 == strat2) {
              continue;
            }
            cmp = dominates(game, player, strat1, strat2, remaining);
            switch (cmp) {
            case STRICTLY_DOMINATED_BY:
            case WEAKLY_DOMINATED_BY:
            case EQUIVALENT:
              remaining[player].remove(strat1);
              proceed = true;
              break OUT;
            case WEAKLY_DOMINATES:
            case STRICTLY_DOMINATES:
              remaining[player].remove(strat2);
              proceed = true;
              break OUT;
            }
          }
        }
      }
    }

    int[] numActions = new int[players];
    for (int player = 0; player < players; player++) {
      numActions[player] = remaining[player].size();
    }
    MatrixGame ret = new MatrixGame(players, numActions);
    /*
     * new game should have same dimensions, but with action indices in
     * 0, ..., k-1 (despite the deleted columns)
     */
    Set<Integer>[] retRemaining = new HashSet[players];
    for (int p = 0; p < players; p++) {
      retRemaining[p] = new HashSet<Integer>();
      for (int i = 1; i <= remaining[p].size(); i++) {
        retRemaining[p].add(i);
      }
    }
    Iterator<Integer>[] remainingIts = new Iterator[players];
    Iterator<Integer>[] retIts = new Iterator[players];
    for (int p = 0; p < players; p++) {
      remainingIts[p] = remaining[p].iterator();
      retIts[p] = retRemaining[p].iterator();
    }
    int[] outcome = new int[players];
    int[] retOutcome = new int[players];
    initProfile(game, NO_FIXED_PLAYER, outcome, remainingIts);
    initProfile(game, NO_FIXED_PLAYER, retOutcome, retIts);
    double oldPayoff;

    /* for every remaining profile, copy the payoff from the original game */
    while (outcome != null) {
      for (int player = 0; player < players; player++) {
        oldPayoff = game.getPayoff(outcome, player);
        ret.setPayoff(retOutcome, player, oldPayoff);
      }
      outcome = nextProfile(game, NO_FIXED_PLAYER, outcome, remaining,
          remainingIts);
      retOutcome = nextProfile(ret, NO_FIXED_PLAYER, retOutcome,
          retRemaining, retIts);
    }
    return ret;
  }
  
  
  

  /**
   * Performs iterated elimination of dominated strategies non-destructively
   * on the given game.
   * 
   * @param game
   *            the game to simplify
   * @return simplified game containing no dominated strategies for any player
   */
  public static MatrixGame IEDS(MatrixGame game, Set<Integer>[] remaining, Set<Integer>[] removed) 
  {
    int players = game.getNumPlayers();
    /*
     * remaining[player] contains indices of strategies for player that have
     * not yet been eliminated
     */
   // Set<Integer>[] remaining = new HashSet[players];
    for (int player = 0; player < players; player++) 
    {
      remaining[player] = new HashSet<Integer>();
      for (int i = 1; i <= game.getNumActions(player); i++) 
      {
        remaining[player].add(i);
      }
    }

    int cmp;
    /*
     * tracks whether at least one strategy has been eliminated this
     * pass--if not, we can stop
     */
    boolean proceed = true;
    while (proceed) {
      proceed = false;
      for (int player = 0; player < players; player++) {
        /*
         * after finding one dominated strategy for one player, advance
         * to the next player
         */
        OUT: for (int strat1 : remaining[player]) {
          for (int strat2 : remaining[player]) {
            if (strat1 == strat2) {
              continue;
            }
            cmp = dominates(game, player, strat1, strat2, remaining);
            switch (cmp) {
            case STRICTLY_DOMINATED_BY:
            case WEAKLY_DOMINATED_BY:
            case EQUIVALENT:
              remaining[player].remove(strat1);
              removed[player].add(strat1);
              proceed = true;
              break OUT;
            case WEAKLY_DOMINATES:
            case STRICTLY_DOMINATES:
              remaining[player].remove(strat2);
              removed[player].add(strat2);
              proceed = true;
              break OUT;
            }
          }
        }
      }
    }

    int[] numActions = new int[players];
    for (int player = 0; player < players; player++) {
      numActions[player] = remaining[player].size();
    }
    MatrixGame ret = new MatrixGame(players, numActions);
    /*
     * new game should have same dimensions, but with action indices in
     * 0, ..., k-1 (despite the deleted columns)
     */
    Set<Integer>[] retRemaining = new HashSet[players];
    for (int p = 0; p < players; p++) {
      retRemaining[p] = new HashSet<Integer>();
      for (int i = 1; i <= remaining[p].size(); i++) {
        retRemaining[p].add(i);
      }
    }
    Iterator<Integer>[] remainingIts = new Iterator[players];
    Iterator<Integer>[] retIts = new Iterator[players];
    for (int p = 0; p < players; p++) {
      remainingIts[p] = remaining[p].iterator();
      retIts[p] = retRemaining[p].iterator();
    }
    int[] outcome = new int[players];
    int[] retOutcome = new int[players];
    initProfile(game, NO_FIXED_PLAYER, outcome, remainingIts);
    initProfile(game, NO_FIXED_PLAYER, retOutcome, retIts);
    double oldPayoff;

    /* for every remaining profile, copy the payoff from the original game */
    while (outcome != null) {
      for (int player = 0; player < players; player++) {
        oldPayoff = game.getPayoff(outcome, player);
        ret.setPayoff(retOutcome, player, oldPayoff);
      }
      outcome = nextProfile(game, NO_FIXED_PLAYER, outcome, remaining,
          remainingIts);
      retOutcome = nextProfile(ret, NO_FIXED_PLAYER, retOutcome,
          retRemaining, retIts);
    }
  
    return ret;
  }
  
  
  

  /**
   * For basic testing of IEDS on a two-player game.
   * 
   * @TODO: generalize to n-player games
   * @TODO: automatically check payoffs (actions may be permuted arbitrarily
   *        during simplification, complicating this)
   * @param rowActions
   *            : number of actions for the row player
   * @param colActions
   *            : number of actions for the column player
   * @param rowPayoffs
   *            : payoffs for the row player
   * @param colPayoffs
   *            : payoffs for the column player
   * @param expRowActions
   *            : number of actions for the row player in the correctly
   *            reduced game
   * @param expColActions
   *            : number of actions for the column player in the correctly
   *            reduced game
   * @return true for pass (all results match expected values), false for
   *         fail; more detail is printed to stdout
   */
  public static boolean testIEDS(int rowActions, int colActions,
      double[][] rowPayoffs, double[][] colPayoffs, int expRowActions,
      int expColActions) {
    int numPlayers = 2;

    /* initialize a new game according to the given payoff matrices */
    MatrixGame game1 = new MatrixGame(numPlayers, new int[] { rowActions,
        colActions });
    for (int i = 0; i < rowActions; i++) {
      for (int j = 0; j < colActions; j++) {
        /* MatrixGame uses 1-based indexing */
        game1.setPayoffs(new int[] { i + 1, j + 1 }, new double[] {
            rowPayoffs[i][j], colPayoffs[i][j] });
      }
    }

    /* check number of players (should match original game) */
    Game ret = IEDS(game1);
    if (numPlayers != ret.getNumPlayers()) {
      System.out.printf(
          "FAILED: result has %d players instead of %d\n\n",
          ret.getNumPlayers(), numPlayers);
      return false;
    }

    /* print payoff matrices */
    int[] dim = ret.getNumActions();
    int[] expDim = new int[] { expRowActions, expColActions };
    for (int player = 0; player < numPlayers; player++) {
      for (int i = 1; i <= dim[0]; i++) {
        for (int j = 1; j <= dim[1]; j++) {
          System.out.print(ret.getPayoff(new int[] { i, j }, player));
        }
        System.out.println();
      }
      System.out.println();
    }

    /* check dimensions */
    if (!Arrays.equals(expDim, dim)) {
      System.out
          .printf("FAILED: result has dimensions (%d, %d) instead of (%d, %d)\n\n",
              dim[0], dim[1], expDim[0], expDim[1]);
      return false;
    }

    System.out.println("PASSED!\n");
    return true;
  }
}