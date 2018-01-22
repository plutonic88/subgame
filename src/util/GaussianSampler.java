package util;

import static subgame.EGAUtils.rng;

/**
 * Generate samples from a normal distribution
 */

public final class GaussianSampler implements DistributionSampler {

  private double mean;
  private double stdDev;

  public GaussianSampler(double mean, double stdDev) {
    this.mean = mean;
    this.stdDev = stdDev;
  }

  // "standard" normal distribution
  // E(X) = 1, Var(X) = 1
  public GaussianSampler() {
    this.mean = 1;
    this.stdDev = 1;
  }

  public double getMean() {
    return mean;
  }

  public void setMean(double mean) {
    this.mean = mean;
  }

  public double getStdDev() {
    return stdDev;
  }

  public void setStdDev(double stdDev) {
    this.stdDev = stdDev;
  }

  /**
   * Returns a sample from the normal distribution
   */
  public double getSampleDouble() {
    return (mean + (stdDev * rng.nextGaussian()));
  }

  public String toString() {
    return "Gaussian distribution. Mean: " + mean + " StdDev: " + stdDev + "\n";
  }
}
