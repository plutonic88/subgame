package observers;

import games.Game;

/**
 * Implements noiseless access to the game matrix.
 */

public class NoiselessObserver extends GameObserver {

  public NoiselessObserver() {
    super();
    deterministic = true;
  }

  public NoiselessObserver(boolean deterministic) {
    super(deterministic);
  }

  public NoiselessObserver(Game g) {
    super(g);
    deterministic = true;
  }

  public NoiselessObserver(Game g, boolean deterministic) {
    super(g, deterministic);
  }

  public String getDescription() {
    return "Noiseless observer\nObservation bound: " + bound + "\nDensity bound: " + densityBound + "\n";
  }

  /**
   * Returns payoffs for all players
   *
   * @param outcome the action choices for all players
   */
  protected double[] samplePayoffs(int[] outcome) {
    return game.getPayoffs(outcome);
  }
}
