package support;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.util.ArrayList;
import java.util.List;

/**
 * Store information about each action in the game
 * I would like to make this generic, but that turns out to be very difficult due to the
 * problems with declaring generic arrays, using clone, etc.
 */
public final class ActionDataInteger {

  private final int nPlayers;
  private final int[] nActions;
  private final List<List<Integer>> values;

  public ActionDataInteger(int[] nActions) {
    this.nPlayers = nActions.length;
    this.nActions = nActions.clone();
    values = new ArrayList<List<Integer>>(nPlayers);
    for (int pl = 0; pl < nPlayers; pl++) {
      List<Integer> tmp = new ArrayList<Integer>(nActions[pl] + 1);
      for (int i = 0; i <= nActions[pl]; i++) {
        tmp.add(0);
      }
      values.add(tmp);
    }
  }

  public ActionDataInteger(int[] nActions, int initialValue) {
    this.nPlayers = nActions.length;
    this.nActions = nActions.clone();
    values = new ArrayList<List<Integer>>(nPlayers);
    for (int pl = 0; pl < nPlayers; pl++) {
      List<Integer> tmp = new ArrayList<Integer>(nActions[pl] + 1);
      for (int i = 0; i <= nActions[pl]; i++) {
        tmp.add(initialValue);
      }
      values.add(tmp);
    }
  }

  public int getNumPlayers() {
    return nPlayers;
  }

  public int[] getNumActions() {
    return nActions;
  }

  public List<Integer> get(int player) {
    return values.get(player);
  }

  public Integer get(int player, int action) {
    return values.get(player).get(action);
  }

  public void set(int player, int action, Integer value) {
    values.get(player).set(action, value);
  }

  public void increment(int player, int action) {
    List<Integer> tmpList = values.get(player);
    tmpList.set(action, tmpList.get(action) + 1);
  }

  public void setZeros() {
    for (int pl = 0; pl < nPlayers; pl++) {
      List<Integer> tmpList = values.get(pl);
      for (int i = 0; i <= nActions[pl]; i++) {
        tmpList.set(i, 0);
      }
    }
  }

  public String toString() {
    return toString(false);
  }

  public String toString(boolean symmetric) {
    StringBuilder sb = getSB();
    for (int pl = 0; pl < nPlayers; pl++) {
      List<Integer> tmpList = values.get(pl);
      if (!symmetric) {
        sb.append("Player ").append(pl).append(": ");
      }
      for (int i = 1; i <= nActions[pl]; i++) {
        sb.append("[").append(i).append("] ").append(tmpList.get(i)).append(" ");
      }
      sb.append("\n");
      if (symmetric) break;
    }
    return returnSB(sb);
  }
}
