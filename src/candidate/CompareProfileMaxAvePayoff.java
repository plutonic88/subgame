package candidate;

import java.io.Serializable;
import java.util.Comparator;

import support.ProfileInfo;

public class CompareProfileMaxAvePayoff implements Serializable, Comparator<ProfileInfo> {

  public int compare(ProfileInfo o1, ProfileInfo o2) {

    double ave1 = 0d;
    double ave2 = 0d;

    for (double payoff : o1.payoffs) {
      ave1 += payoff;
    }

    for (double payoff : o2.payoffs) {
      ave2 += payoff;
    }

    ave1 /= o1.payoffs.length;
    ave2 /= o1.payoffs.length;

    if (ave1 < ave2) {
      return 1;
    } else if (ave1 > ave2) {
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
