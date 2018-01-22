package solvers;

import java.util.Iterator;
import java.util.Set;

import subgame.EGAUtils;
import support.ProfileInfo;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Apr 5, 2008
 * Time: 2:28:17 AM
 */
public class SelectCandidateEquilibrium {

  // selects a random candidate from the set
  public static ProfileInfo selectRandom(Set<ProfileInfo> candidates) {
    if (candidates.size() <= 0) return null;

    int index = EGAUtils.rand.nextInt(candidates.size());
    Iterator itr = candidates.iterator();
    while (index > 0) {
      itr.next();
      index--;
    }
    return (ProfileInfo) itr.next();
  }

  // selects the candidate profile with the lowest benefit to deviating
  public static ProfileInfo selectMinEpsilonBound(Set<ProfileInfo> candidates) {
    if (candidates.size() <= 0) return null;

    ProfileInfo best = null;
    for (ProfileInfo pi : candidates) {
      if (best == null) {
        best = pi;
      } else if (pi.maxBenefit < best.maxBenefit) {
        best = pi;
      }
    }
    return best;
  }

  public static ProfileInfo selectMaxDeviationsTested(Set<ProfileInfo> candidates) {
    if (candidates.size() <= 0) return null;

    ProfileInfo best = null;
    for (ProfileInfo pi : candidates) {
      if (best == null) {
        best = pi;
      } else if (pi.numDeviationsSampled > best.numDeviationsSampled) {
        best = pi;
      }
    }
    return best;
  }

  public static ProfileInfo selectHighestAvePayoff(Set<ProfileInfo> candidates) {
    if (candidates.size() <= 0) return null;

    double bestAve = Double.NEGATIVE_INFINITY;
    ProfileInfo best = null;

    for (ProfileInfo pi : candidates) {
      double ave = 0;
      for (double payoff : pi.payoffs) ave += payoff;
      ave /= pi.payoffs.length;
      if (ave > bestAve) {
        bestAve = ave;
        best = pi;
      }
    }
    return best;
  }

  public static ProfileInfo selectHighestMinPayoff(Set<ProfileInfo> candidates) {
    if (candidates.size() <= 0) return null;

    double bestMin = Double.NEGATIVE_INFINITY;
    ProfileInfo best = null;

    for (ProfileInfo pi : candidates) {
      double min = Double.POSITIVE_INFINITY;
      for (double payoff : pi.payoffs) {
        min = payoff < min ? payoff : min;
      }
      if (min > bestMin) {
        bestMin = min;
        best = pi;
      }
    }
    return best;
  }

}
