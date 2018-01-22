package generate;

import java.util.ArrayList;
import java.util.List;

import games.FactorGame;
import games.Game;
import games.MatrixGame;
import util.UniformSampler;

/**
 * Class used to generate factored games, using Gamut to generate subgames
 */

public class FactorGameGenerator {

  // static class
  private FactorGameGenerator() {
  }

  /**
   * Generates a factored game representation, using a parameter file
   */
  public static FactorGame generateFactorGame(FactorGameParameters params) {
    List<Game> strategic = new ArrayList<Game>();
    List<List<Game>> isolated = new ArrayList<List<Game>>();

    // first, create all of the strategic games
    for (int i = 0; i < params.nStrategic; i++) {
      MatrixGame g;
      switch (params.typeStrategic.get(i)) {
        case RANDOM_DEFAULT:
          Integer[] tmpActsIntegers =
                  params.nActsStrategic.get(i).toArray(new Integer[params.nActsStrategic.get(i).size()]);
          int[] tmpActs = new int[tmpActsIntegers.length];
          for (int j = 0; j < tmpActsIntegers.length; j++) {
            tmpActs[j] = tmpActsIntegers[j];
          }
          g = RandomGenerator.generateRandomGame(params.nPlayers, tmpActs, new UniformSampler(0, 0.5d));
          break;
        default:
          throw new RuntimeException("Error generating factor game. Unknown game type: " + params.typeStrategic.get(i));
      }
      strategic.add(g);
    }

    // next, create all of the isolated dimensions
    for (int pl = 0; pl < params.nPlayers; pl++) {
      List<Game> tmp = new ArrayList<Game>();
      for (int i = 0; i < params.nIsolated.get(pl); i++) {
        MatrixGame g;
        switch (params.typeIsolated.get(pl).get(i)) {
          case RANDOM_DEFAULT:
            int[] tmpActs = new int[1];
            tmpActs[0] = params.nActsIsolated.get(pl).get(i);
            g = RandomGenerator.generateRandomGame(1, tmpActs, new UniformSampler(0, 0.5d));
            break;
          default:
            throw new RuntimeException(
                    "Error generating factor game. Unknown game type: " + params.typeIsolated.get(pl).get(i));
        }
        tmp.add(g);
      }
      isolated.add(tmp);
    }
    return new FactorGame(params.nPlayers, strategic, isolated);
  }
}


