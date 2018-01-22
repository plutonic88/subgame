package games;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the (symmetric) payoffs for a profile
 * These are stored as a map from actions to payoff
 */
public final class SymmetricPayoffs {

  // a default value for the payoffs,
  private static final double DEFAULT_VALUE = Double.NEGATIVE_INFINITY;

  private final Map<Integer, Double> payoffs = new HashMap<Integer, Double>();

  public SymmetricPayoffs() {
    // do nothing
  }

  public SymmetricPayoffs(SymmetricPayoffs p) {
    this.payoffs.putAll(p.payoffs);
  }

  public SymmetricPayoffs(Map<Integer, Double> actionPayoffs) {
    this.payoffs.putAll(actionPayoffs);
  }

  /**
   * Clears all of the payoffs
   */
  public void clear() {
    payoffs.clear();
  }

  /**
   * Sets all of the payoffs; CLEARS ANY PREVIOUS PAYOFF MAPPINGS
   */
  public void setPayoffs(Map<Integer, Double> actionPayoffs) {
    payoffs.clear();
    payoffs.putAll(actionPayoffs);
  }

  /**
   * Set the payoff for a particular action
   */
  public void setPayoff(int action, double actionPayoff) {
    payoffs.put(action, actionPayoff);
  }

  /**
   * Gives direct access to the underlying payoffs
   */
  public Map<Integer, Double> getPayoffs() {
    return payoffs;
  }

  /**
   * Get the payoff associated with this action
   */
  public double getPayoffs(int action) {
    if (payoffs.containsKey(action)) {
      return payoffs.get(action);
    } else {
      return DEFAULT_VALUE;
    }
  }

  public String toString() {
    return payoffs.toString();
  }
}
