package games;

import java.util.HashMap;
import java.util.Map;

/**
 * Class implements a symmetric normal form game
 * Profiles are stored in a hash table instead of a tensor
 * <p/>
 * TODO: Is there a way to index combinations? This could be used
 * to make the storage (and possibly lookups) more efficient here...
 */

public class SymmetricMatrixGame extends SymmetricGame {

  // store the payoffs in a hash table for now
  // this is a map from <action,count> to the payoff objects
  private final Map<Map<Integer, Integer>, SymmetricPayoffs> payoffs;

  /**
   * Constructor for a new symmetric matrix game.
   * All players must have the same number of actions (by definition)
   */
  public SymmetricMatrixGame(int numPlayers, int numActions) {
    super(numPlayers, numActions);

    // Profiles are stored in a hash table for now
    payoffs = new HashMap<Map<Integer, Integer>, SymmetricPayoffs>(nSymmetricProfiles, 0.5f);
    init();
  }

  /**
   * Create the initial payoff objects in the data structure
   */
  private void init() {
    // loop through combinations, adding all possible payoffs
    SymmetricOutcomeIterator itr = symmetricIterator();
    while (itr.hasNext()) {
      payoffs.put(createCounts(itr.next()), new SymmetricPayoffs());
    }
  }

  /**
   * Returns the payoffs for an outcome
   * All permutations of the same symmetric outcome are mapped to the "canonical" value (sorted outcome)
   *
   * @param outcome an array containing the actions chosen by each player
   */
  public double[] getPayoffs(int[] outcome) {
    return mapSymmetricPayoffs(outcome, payoffs.get(createCounts(outcome)).getPayoffs());
  }

  /**
   * Sets the payoff for a player for a given outcome.
   * All permutations of the same symmetric outcome are mapped to the "canonical" value (sorted outcome)
   * NOTE: This method is not recommended for symmetric games, since "players" do not really have a consistent interpretation
   *
   * @param outcome an array containing the actions chosen by each player
   * @param player  the player whose payoff should be returned.
   * @param value   the amount of the payoff
   */
  public void setPayoff(int[] outcome, int player, double value) {
    Map<Integer, Integer> counts = createCounts(outcome);
    payoffs.get(counts).setPayoff(outcome[player], value);
  }

  /**
   * Sets the payoffs for all players
   * All permutations of the same symmetric outcome are mapped to the "canonical" value (sorted outcome)
   *
   * @param outcome action choices for each player
   * @param values  payoff values for each player
   */
  public void setPayoffs(int[] outcome, double[] values) {
    Map<Integer, Integer> counts = createCounts(outcome);
    payoffs.get(counts).setPayoffs(createSymmetricPayoffs(outcome, counts, values));
  }

  /**
   * Sets the payoffs for all players
   *
   * @param counts counts of each action in the profile
   * @param values payoff values for each player
   */
  public void setPayoffs(Map<Integer, Integer> counts, double[] values) {
    payoffs.get(counts).setPayoffs(createSymmetricPayoffs(counts, values));
  }
}
