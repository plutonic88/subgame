package util;

/**
 * Abstract class representing a method for sampling from an arbitrary distribution
 */

public interface DistributionSampler {

  /**
   * Returns a sample from the specific distribution
   */
  public double getSampleDouble();

  /**
   * Returns a string describing the parameters of the distribution
   */
  public String toString();
}
