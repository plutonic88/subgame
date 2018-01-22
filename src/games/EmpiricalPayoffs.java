package games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds empirical payoff information for a profile
 */
public class EmpiricalPayoffs {

  // a default value for the payoffs,
  private static final double DEFAULT_VALUE = Double.NEGATIVE_INFINITY;

  // summary statistics about the payoffs for this profile
  private int numSamples = 0;
  private double[] payoffSum;
  private double[] payoffSumSquared;
  private double[] payoffs;
  private boolean payoffsUpToDate = false;

  // and explicit list of the samples
  private List<double[]> samples = null;
  private boolean RECORD_SAMPLES = false;

  public EmpiricalPayoffs() {
    // do nothing
  }

  public EmpiricalPayoffs(int numPlayers) {
    init(numPlayers);
  }

  public EmpiricalPayoffs(EmpiricalPayoffs ep) {
    setEmpiricalPayoffs(ep);
  }

  public void setEmpiricalPayoffs(EmpiricalPayoffs ep) {
    this.numSamples = ep.numSamples;
    this.payoffSum = ep.payoffSum.clone();
    this.payoffSumSquared = ep.payoffSumSquared.clone();
    this.payoffs = ep.payoffs.clone();
    this.payoffsUpToDate = ep.payoffsUpToDate;
    this.RECORD_SAMPLES = ep.RECORD_SAMPLES;
    if (this.RECORD_SAMPLES) {
      this.samples = new ArrayList<double[]>(ep.samples.size());
      for (double[] tmp : ep.samples) {
        this.samples.add(tmp.clone());
      }
    }
  }

  public void init(int numPlayers) {
    payoffSum = new double[numPlayers];
    payoffSumSquared = new double[numPlayers];
    payoffs = new double[numPlayers];
    Arrays.fill(payoffs, DEFAULT_VALUE);
    payoffsUpToDate = true;
  }

  public void reset() {
    numSamples = 0;
    Arrays.fill(payoffSum, 0);
    Arrays.fill(payoffSumSquared, 0);
    Arrays.fill(payoffs, DEFAULT_VALUE);
    payoffsUpToDate = true;
    if (RECORD_SAMPLES) {
      samples.clear();
    }
  }

  // set the flag to record each individual sample and initialize the data structure
  public void setRecordSamples(boolean value) {
    RECORD_SAMPLES = value;
    if (RECORD_SAMPLES) {
      samples = new ArrayList<double[]>();
    } else {
      samples = null;
    }
  }

  public void addSample(double[] samplePayoffs) {
    payoffsUpToDate = false;
    numSamples++;
    for (int i = 0; i < samplePayoffs.length; i++) {
      payoffSum[i] += samplePayoffs[i];
      payoffSumSquared[i] += Math.pow(payoffs[i], 2);
    }

    if (RECORD_SAMPLES) {
      samples.add(samplePayoffs.clone());
    }
  }

  public int getNumSamples() {
    return numSamples;
  }

  public void setNumSamples(int n) {
    numSamples = n;
  }

  public double getMeanPayoff(int player) {
    if (payoffsUpToDate) return payoffs[player];
    if (numSamples > 0) return payoffSum[player] / (double) numSamples;
    return DEFAULT_VALUE;
  }

  public double[] getMeanPayoffs() {
    if (!payoffsUpToDate && numSamples > 0) {
      for (int i = 0; i < payoffs.length; i++) {
        payoffs[i] = payoffSum[i] / (double) numSamples;
      }
    }
    return payoffs.clone();
  }

  public double getStdDev(int player) {
    double stdDev = numSamples * payoffSumSquared[player];
    stdDev -= Math.pow(payoffSum[player], 2);
    stdDev /= numSamples * (numSamples - 1);
    stdDev = Math.sqrt(stdDev);
    return stdDev;
  }

  public double[] getStdDevs() {
    double[] tmp = new double[payoffSum.length];
    for (int i = 0; i < payoffSum.length; i++) {
      tmp[i] = getStdDev(i);
    }
    return tmp;
  }

  public List<double[]> getSamples() {
    return samples;
  }

  public String toString() {
    return Arrays.toString(getMeanPayoffs());
  }
}
