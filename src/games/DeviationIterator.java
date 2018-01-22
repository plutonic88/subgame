package games;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.util.Iterator;

/**
 * An Iterator for looping through possible deviations from an outcome
 */

public final class DeviationIterator implements Iterator<int[]> {
  private final int[] nActs;
  private final int nPlayers;
  private final int[] outcome;
  private final int[] deviation;
  private int cnt;
  private int nDeviations;
  private int deviatingPlayer;

  public DeviationIterator(int[] outcome, int[] nActions) {
    this.nPlayers = outcome.length;
    this.nActs = nActions.clone();
    this.outcome = outcome.clone();

    nDeviations = nActs[0] - 1;
    for (int i = 1; i < nPlayers; i++) {
      nDeviations += nActs[i] - 1;
    }
    deviation = new int[nPlayers];
    reset();
  }

  public Iterator iterator() {
    return this;
  }

  public void reset() {
    deviatingPlayer = 0;
    cnt = 0;
    System.arraycopy(outcome, 0, deviation, 0, nPlayers);
  }

  private void incrementDeviation() {
    while (true) {
      // increment the action of the current deviating player
      deviation[deviatingPlayer]++;
      if (deviation[deviatingPlayer] > nActs[deviatingPlayer]) {
        deviation[deviatingPlayer] = 1;
      }

      // see if this is a valid deviation
      if (deviation[deviatingPlayer] != outcome[deviatingPlayer]) {
        break;
      }

      // if not, increment the deviating player and try again
      deviatingPlayer++;

      // if we have reached the final player, we are done
      if (deviatingPlayer >= nPlayers) {
        System.err.println("Error in deviation iterator: no iteration left. cnt: " + cnt);
        break;
      }
    }
    cnt++;
  }

  public int getNumDeviations() {
    return nDeviations;
  }

  public int[] getDeviation() {
    return deviation;
  }

  public int getDeviatingPlayer() {
    return deviatingPlayer;
  }

  public boolean hasNext() {
    return cnt < nDeviations;
  }

  public int[] next() {
    // nothing left
    if (!hasNext()) return deviation;

    incrementDeviation();
    return deviation;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder sb = getSB();
    sb.append("[");
    for (int i = 0; i < nPlayers; i++) {
      sb.append(deviation[i]).append((i < nPlayers - 1 ? "  " : "]"));
    }
    String tmp = sb.toString();
    returnSB(sb);
    return tmp;
  }
}

