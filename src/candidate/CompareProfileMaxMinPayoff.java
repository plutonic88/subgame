package candidate;

import java.io.Serializable;
import java.util.Comparator;

import support.ProfileInfo;

public class CompareProfileMaxMinPayoff implements Serializable, Comparator<ProfileInfo> {

  public int compare(ProfileInfo o1, ProfileInfo o2) {

    double min1 = Double.POSITIVE_INFINITY;
    double min2 = Double.POSITIVE_INFINITY;

    for (double payoff : o1.payoffs) {
      min1 = Math.max(min1, payoff);
    }

    for (double payoff : o2.payoffs) {
      min2 = Math.max(min2, payoff);
    }

    if (min1 < min2) {
      return 1;
    } else if (min1 > min2) {
      return -1;
    }

    // order by strategies (shouldn't be necessary in most cases)
    for (int i = 0; i < o1.outcome.length; i++) {
      if (o1.outcome[i] > o2.outcome[i]) {
        return 1;
      } else if (o1.outcome[i] < o2.outcome[i]) {
        return -1;
      }
    }

    // should never get here
    return 0;
  }
}
