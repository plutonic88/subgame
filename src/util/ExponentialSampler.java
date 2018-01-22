package util;

import static subgame.EGAUtils.rng;

/**
 * Generate a sample from the exponential distribution
 */

public final class ExponentialSampler implements DistributionSampler {

  final double beta;    // controls mean, variance
  final double lambda;  // shift

  public ExponentialSampler(double beta, double lambda) {
    this.beta = beta;
    this.lambda = lambda;
  }

  public ExponentialSampler(double beta) {
    this.beta = beta;
    this.lambda = 0;
  }

  // "standard" exponential distribution
  // E(X) = 1, Var(X) = 1
  public ExponentialSampler() {
    this.beta = 1;
    this.lambda = 0;
  }

  /**
   * Returns a uniform sample from the range given
   */
  public double getSampleDouble() {
    return rng.nextExp(beta, lambda);
  }

  public String toString() {
    return "Exponential distribution. beta: " + beta + " lambda: " + lambda + "\n";
  }
}
