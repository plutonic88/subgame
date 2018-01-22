package observers;

import games.Game;
import util.DistributionSampler;

/**
 * Allows noisy access to the game matrix
 * Any samples have noise added, based on the given distribution
 */

public class NoisyObserver extends GameObserver {

  private DistributionSampler sampler = null;

   public NoisyObserver(DistributionSampler sampler) {
    super();
    this.sampler = sampler;
  }

  public NoisyObserver(boolean deterministic, DistributionSampler sampler) {
    super(deterministic);
    this.sampler = sampler;
  }

  public NoisyObserver(Game g, DistributionSampler sampler) {
    super(g);
    this.sampler = sampler;
  }

  public NoisyObserver(Game g, boolean deterministic, DistributionSampler sampler) {
    super(g, deterministic);
    this.sampler = sampler;
  }

  public DistributionSampler getSampler() {
    return sampler;
  }

  public void setSampler(DistributionSampler sampler) {
    this.sampler = sampler;
  }

  public String getDescription() {
    return "Noisy observer\nObservation bound: " + bound + "\nDensity bound: " + densityBound + "\n" + sampler.toString();
  }

  /**
   * Returns payoffs for all players
   *
   * @param outcome the action choices for all players
   */
  protected double[] samplePayoffs(int[] outcome) {
    double[] truePayoffs = game.getPayoffs(outcome);
    double[] samplePayoffs = new double[outcome.length];
    for (int pl = 0; pl < outcome.length; pl++) {
      samplePayoffs[pl] = truePayoffs[pl] + sampler.getSampleDouble();
    }
    return samplePayoffs;
  }
}
