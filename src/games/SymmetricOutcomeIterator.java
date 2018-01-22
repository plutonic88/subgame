package games;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.util.Arrays;
import java.util.Iterator;

/**
 * An Iterator for looping through the symmetric outcomes in the game
 */

public final class SymmetricOutcomeIterator implements Iterator<int[]> {
  private final int nActions;
  private final int nPlayers;

  private int[] actions;
  private boolean firstOutcome;

  // Need to create a SymmetricGame interface at some point to implement these correctly
  public SymmetricOutcomeIterator(SymmetricGame g) {
    this.nPlayers = g.getNumPlayers();
    this.nActions = g.getNumSymmetricActions();
    init();
  }

//   public OutcomeIterator(GameObserver go) {
//     this.nPlayers = go.getNumPlayers();
//     this.nActions = go.getNumActions();
//     init();
//   }

  public SymmetricOutcomeIterator(int numPlayers, int numActions) {
    this.nPlayers = numPlayers;
    this.nActions = numActions;
    init();
  }

  private void init() {
    //nOutcomes = getNChooseR(nPlayers+nActions-1, nPlayers);
    actions = new int[nPlayers];
    Arrays.fill(actions, 1);
    firstOutcome = true;
  }

  public Iterator iterator() {
    return this;
  }

  public void reset() {
    firstOutcome = true;
    Arrays.fill(actions, 1);
  }

  public int[] getOutcome() {
    return actions;
  }

  public boolean hasNext() {
    for (int i = 0; i < nPlayers; i++) {
      if (actions[i] < nActions) {
        return true;
      }
    }
    return false;
  }

  /**
   * In a two by two matrix game, the outcomes are looped over
   * in the order top left, bottom left, top right, bottom right.
   * Can extend this idea of first player's actions being looped
   * through quickly and repeatedly, and last player's actions
   * being looped through slowly and only once to figure out
   * ordering for games of other sizes.
   */
  public int[] next() {
    // nothing left
    if (!hasNext()) return actions;

    if (firstOutcome) {
      firstOutcome = false;
      return actions;
    }

    // to generate the next profile, find the furthest right player than can
    // be incremented. increment the action and then fill this action to the right
    int tmpPlayer = nPlayers - 1;
    while (actions[tmpPlayer] == nActions && tmpPlayer > 0) {
      tmpPlayer--;
    }

    // increment the actions for the player found
    actions[tmpPlayer]++;
    tmpPlayer++;

    // fill to the right
    while (tmpPlayer < nPlayers) {
      actions[tmpPlayer] = actions[tmpPlayer - 1];
      tmpPlayer++;
    }

    return actions;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder sb = getSB();
    sb.append("[");
    for (int i = 0; i < nPlayers; i++) {
      sb.append(actions[i]).append((i < nPlayers - 1 ? "  " : "]"));
    }
    String tmp = sb.toString();
    returnSB(sb);
    return tmp;
  }
}

