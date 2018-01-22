package support;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.nf;
import static subgame.EGAUtils.returnSB;

public final class ProfileInfo {

  // the outcome represented
  public final int[] outcome;

  // the payoffs for this outcome
  public final double[] payoffs;

  // maximum number of possible deviations
  public final int numPossibleDeviations;

  // the number of deviations sampled
  public int numDeviationsSampled;

  // number of deviations that were beneficial
  public int numBeneficial;

  // maximum benefit to deviating
  public double maxBenefit;

  // total benefit for deviating (used to compute averages)
  public double totBenefit;

  public ProfileInfo(int[] outcome, double[] payoffs, int numPossibleDeviations) {
    this.outcome = outcome.clone();
    this.payoffs = payoffs.clone();
    this.numPossibleDeviations = numPossibleDeviations;
    this.numDeviationsSampled = 0;
    this.numBeneficial = 0;
    this.maxBenefit = Double.NEGATIVE_INFINITY;
    this.totBenefit = 0;
  }

  public double getEpsBound() {
    return Math.max(maxBenefit, 0d);
  }

  public double getAveBenefit() {
    if (numDeviationsSampled <= 0) {
      return Double.NEGATIVE_INFINITY;
    }
    return totBenefit / (double) numDeviationsSampled;
  }

  public double getFractionSampled() {
    if (numPossibleDeviations <= 0) {
      return Double.NEGATIVE_INFINITY;
    }
    return (double) numDeviationsSampled / (double) numPossibleDeviations;
  }

  public String toString() {
    int numPlayers = outcome.length;
    StringBuilder sb = getSB();

    sb.append("PROFILE: [");
    for (int i = 0; i < numPlayers - 1; i++) {
      sb.append(outcome[i]).append(",");
    }
    sb.append(outcome[numPlayers - 1]).append("]");

    sb.append("  Payoffs: [");
    for (int i = 0; i < numPlayers - 1; i++) {
      sb.append(nf.format(payoffs[i])).append(",");
    }
    sb.append(nf.format(payoffs[numPlayers - 1])).append("]");

//     sb.append("  Epsilon Bound: " + nf.format(getEpsBound()));
    sb.append("  Max Benefit: ").append(nf.format(maxBenefit));
    if (numDeviationsSampled > 0) {
      sb.append("  Ave Benefit: ").append(nf.format(totBenefit / (double) numDeviationsSampled));
    } else {
      sb.append("  Ave Benefit: NaN");
    }
    sb.append("  Num Beneficial: ").append(numBeneficial);
    //sb.append("  Sampled: " + numDeviationsSampled);
    if (numPossibleDeviations > 0) {
      sb.append("  Fraction Sampled: ")
              .append(nf.format((double) numDeviationsSampled / (double) numPossibleDeviations));
    } else {
      sb.append("  Fraction Sampled: NaN");
    }
    return returnSB(sb);
  }
}
