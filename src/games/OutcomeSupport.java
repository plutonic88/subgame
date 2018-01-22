package games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates the notion of the "support" of an outcome
 * Set of actions for each player that are played with positive probability
 */

public final class OutcomeSupport {

  private final int nPlayers;
  private final int[] nActions;

  private final List<int[]> actionsWithSupport = new ArrayList<int[]>();

  // new support object with empty lists
  public OutcomeSupport(int[] nActions) {
    this.nPlayers = nActions.length;
    this.nActions = nActions.clone();
    for (int pl = 0; pl < nPlayers; pl++) {
      actionsWithSupport.add(new int[nActions[pl] + 1]);
    }
  }

  // new support object with empty lists
  public OutcomeSupport(Game g) {
    this.nPlayers = g.getNumPlayers();
    this.nActions = g.getNumActions().clone();
    for (int pl = 0; pl < nPlayers; pl++) {
      actionsWithSupport.add(new int[nActions[pl] + 1]);
    }
  }

  public OutcomeSupport(OutcomeDistribution od) {
    this.nPlayers = od.getNumPlayers();
    this.nActions = od.getNumActions().clone();
    for (int pl = 0; pl < nPlayers; pl++) {
      actionsWithSupport.add(new int[nActions[pl] + 1]);
    }
    setSupport(od);
  }

  // loop through all outcomes and record the strategies played for all outcomes with positive probability
  public void setSupport(OutcomeDistribution od) {
    OutcomeIterator itr = new OutcomeIterator(nPlayers, nActions);
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      if (od.getProb(outcome) > 0) {
        for (int pl = 0; pl < nPlayers; pl++) {
          actionsWithSupport.get(pl)[outcome[pl]] = 1;
        }
      }
    }
  }

  public void resetSupport() {
    for (int pl = 0; pl < nPlayers; pl++) {
      Arrays.fill(actionsWithSupport.get(pl), 0);
    }
  }

  public boolean inSupport(int player, int action) {
    return actionsWithSupport.get(player)[action] > 0;
  }

  public void setSupport(int player, int action, boolean value) {
    actionsWithSupport.get(player)[action] = value ? 1 : 0;
  }

}
