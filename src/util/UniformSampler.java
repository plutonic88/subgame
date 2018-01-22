package util;

import static subgame.EGAUtils.rand;

/**
 * Generate Samples from a uniform distribution
 */

public final class UniformSampler implements DistributionSampler {

  final double min;
  final double max;
  final double range;

  public UniformSampler(double min, double max) {
    this.min = min;
    this.max = max;
    this.range = max - min;
  }

  // "standard" uniform distribution
  // E(X) = 1/2, Var(X) = 1/12
  public UniformSampler() {
    this.min = 0;
    this.max = 1;
    this.range = 1;
  }

  /**
   * Returns a uniform sample from the range given
   */
  public double getSampleDouble() {
    return (min + (rand.nextDouble() * range));
  }

  public String toString() {
    return "Uniform distribution. [" + min + "," + max + ")\n";
  }
}
