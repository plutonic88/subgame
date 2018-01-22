package games;

import static subgame.Parameters.GAMUT_GAME_EXTENSION;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import output.SimpleOutput;

/**
 * Implementation of factored games.
 * Players actions have dimensions, which can be: strategic, isolated, or irrelevant
 */

public final class FactorGame extends Game {

  // stores the number of actions for each dimension of each player's action
  private final List<List<Integer>> actionDimensionSizes;

  // stores the payoff information for strategic dimensions
  private final List<Game> strategicDimensions;

  // stores the payoff information for each players' isolated dimensions
  private final List<List<Game>> isolatedDimensions;

  /**
   * Constructor for a new factor game.
   * <p/>
   * Note that this does not specify the number of actions
   * This is designed to allow the game to be constructed by adding factors
   * individually; the number of actions is derived from the factors
   */
  public FactorGame(int numPlayers) {
    // start with number of actions set to 0
    super(numPlayers, new int[numPlayers]);

    // initialize lists of strategic/isolated games
    actionDimensionSizes = new ArrayList<List<Integer>>();
    strategicDimensions = new ArrayList<Game>();
    isolatedDimensions = new ArrayList<List<Game>>();
    for (int pl = 0; pl < numPlayers; pl++) {
      actionDimensionSizes.add(new ArrayList<Integer>());
      isolatedDimensions.add(new ArrayList<Game>());
    }
  }

  /**
   * Constructor that takes the game factors as input directly
   * This is slightly more efficient, because it does not recompute the action sizes
   * each time a factor is added.
   */
  public FactorGame(int numPlayers, List<Game> strategic, List<List<Game>> isolated) {
    // start with number of actions set to 0
    super(numPlayers, new int[numPlayers]);

    strategicDimensions = strategic;
    isolatedDimensions = isolated;

    actionDimensionSizes = new ArrayList<List<Integer>>();
    for (int pl = 0; pl < numPlayers; pl++) {
      actionDimensionSizes.add(new ArrayList<Integer>());
    }
    computeActionSizes();
  }

  /**
   * Constructor that reads in the representation from a set of files
   */
//   public FactorGame(String path) {
//     // TODO: write this function
//   }

  /**
   * Add a new strategic dimension
   * NOTE: strategic dimensions *MUST* have exactly the same set of players as the full factor game
   */
  public void addStrategicDimension(Game newStrategicDimension) {
    // strategic dimensions *must* account for each player
    if (newStrategicDimension.getNumPlayers() != nPlayers) {
      System.err.println(
              "Error generating factor game: assigning a strategic dimension with the wrong number of players.");
      return;
    }

    // add the game to the list
    strategicDimensions.add(newStrategicDimension);

    // update the action sizes
    computeActionSizes();
  }

  /**
   * Add a new isolated dimension
   * Each of these is for a single player, but is still represented using the game interface
   */
  public void addIsolatedDimension(Game newIsolatedDimension, int player) {
    if (newIsolatedDimension.getNumPlayers() != nPlayers) {
      System.err.println("Error generating factor game: assigning an isolated dimension with numPlayer != 1.");
      return;
    }

    // add the isolated dimension to the list
    isolatedDimensions.get(player).add(newIsolatedDimension);

    // update the action sizes
    computeActionSizes();
  }

  /**
   * Compute the number of actions and action vectors based on the existing dimensions
   */
  private void computeActionSizes() {
    int nPlayers = getNumPlayers();

    // clear out all of the sizes so we can re-compute
    for (int pl = 0; pl < nPlayers; pl++) {
      actionDimensionSizes.get(pl).clear();
    }

    // insert the sizes for the strategic dimensions
    for (Game g : strategicDimensions) {
      for (int pl = 0; pl < nPlayers; pl++) {
        actionDimensionSizes.get(pl).add(g.getNumActions(pl));
      }
    }

    // insert the sizes of the isolated dimensions
    for (int pl = 0; pl < nPlayers; pl++) {
      for (Game g : isolatedDimensions.get(pl)) {
        actionDimensionSizes.get(pl).add(g.getNumActions(0));
      }
    }

    // compute the total size of the strategy space for each player
    for (int pl = 0; pl < nPlayers; pl++) {
      if (actionDimensionSizes.get(pl).size() == 0) {
        nActions[pl] = 0;
      } else {
        nActions[pl] = 1;
        for (int nActs : actionDimensionSizes.get(pl)) {
          nActions[pl] *= nActs;
        }
      }
    }

    // recompute the number of profiles and deviations
    updateGameSize();
  }

  /**
   * Takes in a set of action vectors for all players and returns the
   * outcome as action indexes
   */
  public int[] actionVectorsToOutcome(List<int[]> actionVectors) {
    if (actionVectors.size() != getNumPlayers()) {
      System.err.println("Factor game: Error converting action vectors to outcome. Number of players does not match.");
      return null;
    }

    int[] outcome = new int[nPlayers];
    for (int pl = 0; pl < nPlayers; pl++) {
      outcome[pl] = actionVectorToIndex(actionVectors.get(pl), pl);
    }
    return outcome;
  }

  /**
   * Take a vector of actions for a particular player and convert it to an action index
   */
  public int actionVectorToIndex(int[] actionVector, int pl) {
    int index = 0;
    int mult = 1;
    int numDims = actionDimensionSizes.get(pl).size();

    if (numDims != actionVector.length) {
      System.err.println("Factor game: error converting action vector to index. Dimension sizes do not match.");
      return -1;
    }

    for (int i = 0; i < numDims; i++) {
      if (actionVector[i] < 1 || actionVector[i] > actionDimensionSizes.get(pl).get(i)) {
        System.err.println(
                "Factor game: error converting action vector to index. Action of of range: " + actionVector[i]);
        return -1;
      }

      index += (actionVector[i] - 1) * mult;
      mult *= actionDimensionSizes.get(pl).get(i);
    }
    index++;
    return index;
  }

  /**
   * Takes in an action index and converts it to a vector of actions for each dimension
   */
  public int[] indexToActionVector(int action, int player) {
    int numDims = actionDimensionSizes.get(player).size();
    int[] actionVector = new int[numDims];
    int mult = 1;
    action--;

    // todo: cache multipliers for performance
    for (int i = 0; i < numDims; i++) {
      mult *= actionDimensionSizes.get(player).get(i);
    }

    for (int i = numDims - 1; i >= 0; i--) {
      mult /= actionDimensionSizes.get(player).get(i);
      actionVector[i] = action / mult;
      action -= actionVector[i] * mult;
      actionVector[i]++;
    }
    return actionVector;
  }

  /**
   * Returns payoffs for all players
   *
   * @param outcome action choices for all players
   */
  public double[] getPayoffs(int[] outcome) {
    //convert the outcome to the action vector version and get the payoffs using this
    List<int[]> outcomeVectors = new ArrayList<int[]>();
    for (int pl = 0; pl < nPlayers; pl++) {
      outcomeVectors.add(indexToActionVector(outcome[pl], pl));
    }

    // get the payoffs and return
    return getPayoffs(outcomeVectors);
  }

  /**
   * Returns the payoffs, given the outcome in the form of action vectors
   */
  public double[] getPayoffs(List<int[]> outcomeVectors) {
    double[] payoffs = new double[nPlayers];
    int[] outcome = new int[nPlayers];

    // first, deal with all of the strategic dimensions
    for (int i = 0; i < strategicDimensions.size(); i++) {
      Game g = strategicDimensions.get(i);

      // set up the outcome
      for (int pl = 0; pl < nPlayers; pl++) {
        outcome[pl] = outcomeVectors.get(pl)[i];
      }

      // get the actual payoffs for the outcome
      for (int pl = 0; pl < nPlayers; pl++) {
        payoffs[pl] += g.getPayoff(outcome, pl);
      }
    }

    // next, go through the isolated dimensions for each player
    int numStrategicDimensions = strategicDimensions.size();
    outcome = new int[1];
    for (int pl = 0; pl < nPlayers; pl++) {
      List<Game> gameList = isolatedDimensions.get(pl);
      for (int i = 0; i < gameList.size(); i++) {
        Game g = gameList.get(i);
        outcome[0] = outcomeVectors.get(pl)[numStrategicDimensions + i];
        payoffs[pl] += g.getPayoff(outcome, 0);
      }
    }
    return payoffs;
  }

  /**
   * Returns the number of strategic dimensions; this is the same
   * for all players
   */
  public int getNumStrategicDimensions() {
    return strategicDimensions.size();
  }

  /**
   * Returns the number of isolated dimensions for the given player
   */
  public int getNumIsolatedDimensions(int player) {
    return isolatedDimensions.get(player).size();
  }

  /**
   * Returns the total number of dimensions for a given player
   */
  public int getNumTotalDimensions(int player) {
    return strategicDimensions.size() + isolatedDimensions.get(player).size();
  }

  /**
   * Returns the number of action in each dimension for the given player
   */
  public List<Integer> getDimensionSizes(int player) {
    return actionDimensionSizes.get(player);
  }

  /**
   * Translates the factored game representation into a standard matrix game
   * This is useful to prevent information from leaking to solution algorithms
   * NOTE: to prevent leaking, the order of the actions must be randomized
   */
  public MatrixGame getUnfactoredGameRepresentation(boolean randomizeActionOrdering) {
    List<List<Integer>> actionMappings = null;
    int[] mappedOutcome = new int[nPlayers];
    MatrixGame g = new MatrixGame(nPlayers, nActions);
    OutcomeIterator itr = this.iterator();

    if (randomizeActionOrdering) {
      // set up a mapping to randomize the ordering of the actions
      actionMappings = new ArrayList<List<Integer>>(nPlayers);
      for (int pl = 0; pl < nPlayers; pl++) {
        List<Integer> mapping = new ArrayList<Integer>(nActions[pl]);
        for (int i = 1; i <= nActions[pl]; i++) {
          mapping.add(i);
        }
        // randomize the ordering
        Collections.shuffle(mapping);
        actionMappings.add(mapping);
      }
    }

    // set the payoffs for the new game, iterating over all outcomes
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      double[] payoffs = getPayoffs(outcome);

      if (randomizeActionOrdering) {
        // find the mapped outcome
        for (int pl = 0; pl < nPlayers; pl++) {
          mappedOutcome[pl] = actionMappings.get(pl).get(outcome[pl] - 1);
        }
        g.setPayoffs(mappedOutcome, payoffs);
      } else {
        // use the same action indices
        g.setPayoffs(outcome, payoffs);
      }
    }
    return g;
  }

  /**
   * This function outputs all of the constituent games to the given directory
   */
  public void outputFactors(String path, boolean includeFullGame) {

    File tmpFile = new File(path);
    if (!tmpFile.exists()) {
      tmpFile.mkdirs();
    } else if (!tmpFile.isDirectory()) {
      System.out.println("Tried to write output factors to a non-directory: " + path);
      return;
    }

    // strategic dimensions
    for (int i = 0; i < strategicDimensions.size(); i++) {
      SimpleOutput.writeGame(path + "strategic_" + i + GAMUT_GAME_EXTENSION, strategicDimensions.get(i));
    }

    // isolated dimensions
    for (int pl = 0; pl < isolatedDimensions.size(); pl++) {
      for (int i = 0; i < isolatedDimensions.get(pl).size(); i++) {
        SimpleOutput
                .writeGame(path + "isolated_" + pl + "_" + i + GAMUT_GAME_EXTENSION, isolatedDimensions.get(pl).get(i));
      }
    }

    // full game
    if (includeFullGame) {
      SimpleOutput.writeGame(path + "full_game" + GAMUT_GAME_EXTENSION, this);
    }
  }
}

