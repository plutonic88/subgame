package candidate;

import java.io.Serializable;
import java.util.Comparator;

import support.ProfileInfo;

// Primary ordering by maximum deviation benefit (lower bound for incomplete game definitions)
// Secondary ordering by average deviation benefit
// Tertiary ordering by strategy identifiers

public class CompareProfileMaxDevBounds implements Serializable, Comparator<ProfileInfo> {

  public int compare(ProfileInfo o1, ProfileInfo o2) {

    // order by max benefit
    if (o1.maxBenefit > o2.maxBenefit) {
      return 1;
    } else if (o1.maxBenefit < o2.maxBenefit) {
      return -1;
    }

    // order by ave benefit
    if (o1.getAveBenefit() > o2.getAveBenefit()) {
      return 1;
    } else if (o1.getAveBenefit() < o2.getAveBenefit()) {
      return -1;
    }

    // secondary ordering based on strategies
    for (int i = 0; i < o1.outcome.length; i++) {
      if (o1.outcome[i] > o2.outcome[i]) {
        return 1;
      } else if (o1.outcome[i] < o2.outcome[i]) {
        return 1;
      }
    }

    // this should never actually happen
    return 0;
  }
}
