package candidate;

import java.io.Serializable;
import java.util.Comparator;

import support.ProfileInfo;

// Todo: make this by max dev, not just epsilon bound
// secondary ordering based on ave. dev benefit
// tertiary by strategies

public class CompareProfileEpsilonBounds implements Serializable, Comparator<ProfileInfo> {

  public int compare(ProfileInfo o1, ProfileInfo o2) {

    // first, order by epsilon bound
    if (o1.getEpsBound() > o2.getEpsBound()) {
      return 1;
    } else if (o1.getEpsBound() < o2.getEpsBound()) {
      return -1;
    }

    // order by ave benefit
    if (o1.getAveBenefit() > o2.getAveBenefit()) {
      return 1;
    } else if (o1.getAveBenefit() < o2.getAveBenefit()) {
      return -1;
    }

    // order by strategies
    for (int i = 0; i < o1.outcome.length; i++) {
      if (o1.outcome[i] > o2.outcome[i]) {
        return 1;
      } else if (o1.outcome[i] < o2.outcome[i]) {
        return -1;
      }
    }

    // should never actually get there
    return 0;
  }
}
