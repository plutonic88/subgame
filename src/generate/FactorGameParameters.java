package generate;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that holds the paramters that defined a factored game (used for generation)
 */

public class FactorGameParameters {

  public enum GameType {
    RANDOM_DEFAULT
  }

  public int nPlayers;

  // information about strategic dimensions
  public int nStrategic;
  public List<List<Integer>> nActsStrategic;
  public List<GameType> typeStrategic;

  // information about isolated dimensions
  public List<Integer> nIsolated;
  public List<List<Integer>> nActsIsolated;
  public List<List<GameType>> typeIsolated;

  private FactorGameParameters() {
  }

  /**
   * Initialize as a (simple) variation that is symmetric across players
   * same number of actions for each strategic dimension
   * same number of actions for each isolated dimension
   * players have the same number of isolated dimensions
   * all strategic and isolated dimensions have the same type
   */
  public FactorGameParameters(int nPlayers,
                              int nStrategic, int nActsStrategic, GameType typeStrategic,
                              int nIsolated, int nActsIsolated, GameType typeIsolated) {
    this.nPlayers = nPlayers;

    // strategic dimensions
    this.nStrategic = nStrategic;
    this.nActsStrategic = new ArrayList<List<Integer>>();
    for (int i = 0; i < nStrategic; i++) {
      List<Integer> tmp = new ArrayList<Integer>();
      for (int pl = 0; pl < nPlayers; pl++) {
        tmp.add(nActsStrategic);
      }
      this.nActsStrategic.add(tmp);
    }
    this.typeStrategic = new ArrayList<GameType>();
    for (int i = 0; i < nStrategic; i++) {
      this.typeStrategic.add(typeStrategic);
    }

    // isolated dimensions
    this.nIsolated = new ArrayList<Integer>();
    for (int pl = 0; pl < nPlayers; pl++) {
      this.nIsolated.add(nIsolated);
    }
    this.nActsIsolated = new ArrayList<List<Integer>>();
    for (int pl = 0; pl < nPlayers; pl++) {
      ArrayList<Integer> tmp = new ArrayList<Integer>();
      for (int i = 0; i < nIsolated; i++) {
        tmp.add(nActsIsolated);
      }
      this.nActsIsolated.add(tmp);
    }
    this.typeIsolated = new ArrayList<List<GameType>>();
    for (int pl = 0; pl < nPlayers; pl++) {
      ArrayList<GameType> tmp = new ArrayList<GameType>();
      for (int i = 0; i < nIsolated; i++) {
        tmp.add(typeIsolated);
      }
      this.typeIsolated.add(tmp);
    }
  }
}
