package games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds empirical payoff information for a profile
 */
public final class SymmetricEmpiricalPayoffs {

  // a default value for the payoffs,
  private static final double DEFAULT_VALUE = Double.NEGATIVE_INFINITY;

  // summary statistics about the payoffs for this profile
  private int numSamples = 0;
  private boolean payoffsUpToDate = false;

  private final Map<Integer, PayoffStats> payoffs = new HashMap<Integer, PayoffStats>();

  // and explicit list of the samples
  private Map<Integer, List<Double>> samples = null;
  private boolean RECORD_SAMPLES = false;

  public SymmetricEmpiricalPayoffs() {
    // do nothing
  }

  public SymmetricEmpiricalPayoffs(SymmetricEmpiricalPayoffs ep) {
    this.numSamples = ep.numSamples;
    this.payoffsUpToDate = ep.payoffsUpToDate;
    this.payoffs.putAll(ep.payoffs);
    this.RECORD_SAMPLES = ep.RECORD_SAMPLES;
    if (this.RECORD_SAMPLES) {
      this.samples = new HashMap<Integer, List<Double>>();
      for (Map.Entry<Integer, List<Double>> entry : samples.entrySet()) {
        samples.put(Integer.valueOf(entry.getKey()), new ArrayList<Double>(entry.getValue()));
      }
    }
  }

  public void reset() {
    numSamples = 0;
    payoffsUpToDate = false;
    payoffs.clear();
    if (RECORD_SAMPLES) {
      samples.clear();
    }
  }

  // set the flag to record each individual sample and initialize the data structure
  public void setRecordSamples(boolean value) {
    RECORD_SAMPLES = value;
    if (RECORD_SAMPLES) {
      samples = new HashMap<Integer, List<Double>>();
    } else {
      samples = null;
    }
  }

  public void addSample(Map<Integer, Double> actionPayoffs) {
    for (Map.Entry<Integer, Double> entry : actionPayoffs.entrySet()) {
      int act = entry.getKey();
      double val = entry.getValue();

      if (numSamples == 0) {
        payoffs.put(act, new PayoffStats(val));
      } else {
        payoffs.get(act).addSample(val);
      }
      if (RECORD_SAMPLES) {
        samples.get(act).add(val);
      }
    }
    if (numSamples > 0) {
      payoffsUpToDate = false;
    }
    numSamples++;
  }

  public int getNumSamples() {
    return numSamples;
  }

  public double getMeanPayoff(int action) {
    if (payoffsUpToDate) return payoffs.get(action).mean;
    if (numSamples > 0) return payoffs.get(action).payoffSum / (double) numSamples;
    return DEFAULT_VALUE;
  }

  public Map<Integer, Double> getMeanPayoffs() {
    Map<Integer, Double> tmpPayoffs = new HashMap<Integer, Double>();
    for (Integer act : payoffs.keySet()) {
      PayoffStats stats = payoffs.get(act);
      if (!payoffsUpToDate && numSamples > 0) {
        stats.updateMean();
      }
      tmpPayoffs.put(act, stats.mean);
    }
    return tmpPayoffs;
  }

  public double getStdDev(int action) {
    if (numSamples > 0) return payoffs.get(action).computeStdDev();
    return Double.NEGATIVE_INFINITY;
  }


  public Map<Integer, Double> getStdDevs() {
    Map<Integer, Double> tmpSD = new HashMap<Integer, Double>();
    for (Integer act : payoffs.keySet()) {
      PayoffStats stats = payoffs.get(act);
      if (numSamples > 0) {
        tmpSD.put(act, stats.computeStdDev());
      } else {
        tmpSD.put(act, Double.NEGATIVE_INFINITY);
      }
    }
    return tmpSD;
  }


  public Map<Integer, List<Double>> getSamples() {
    return samples;
  }

  public String toString() {
    return getMeanPayoffs().toString();
  }

  private class PayoffStats {
    double payoffSum = 0;
    double payoffSumSquared = 0;
    double mean = DEFAULT_VALUE;

    PayoffStats(double value) {
      payoffSum = value;
      payoffSumSquared = Math.pow(value, 2);
      mean = value;
    }

    void addSample(double value) {
      payoffSum += value;
      payoffSumSquared += Math.pow(value, 2);
    }

    void updateMean() {
      mean = payoffSum / (double) numSamples;
    }

    double computeStdDev() {
      double stdDev = numSamples * payoffSumSquared;
      stdDev -= Math.pow(payoffSum, 2);
      stdDev /= numSamples * (numSamples - 1);
      stdDev = Math.sqrt(stdDev);
      return stdDev;
    }
  }
}
