package candidate;

import java.io.Serializable;
import java.util.Comparator;

import support.ProfileInfo;

public class CompareProfileDeviationsExplored implements Serializable, Comparator<ProfileInfo> {

  public int compare(ProfileInfo o1, ProfileInfo o2) {

    if (o1.numDeviationsSampled < o2.numDeviationsSampled) {
      return 1;
    } else if (o1.numDeviationsSampled > o2.numDeviationsSampled) {
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
