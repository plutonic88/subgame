package solvers;

import static subgame.EGAUtils.rand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.EmpiricalMatrixGame;
import observers.GameObserver;

/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Oct 2, 2007
 * Time: 3:34:05 AM
 */
public class ExplorationUtil {

  private final List<List<Integer>> candidates = new ArrayList<List<Integer>>();
  private final List<List<Integer>> chosen = new ArrayList<List<Integer>>();

  // RANDOM_WITH_REPLACEMENT:  Uniform random sampling over all profiles
  // RANDOM_WITHOUT_REPLACEMENT: Uniform random over all profiles, without replacement
  // ALL_EVEN_PLUS_RANDOM: Sample all profiles evenly as many times as
  // ONCE_PER_ACTION:      Sample exactly one time per action (uniform random)
  // SUBGAME_RANDOM:       Uniform random selection of players/actions to include, until we hit sample bound
  // SUBGAME_ALL_RANDOM:   All actions for specified player included, opponent actions are selected at random
  // SUBGAME_ALL_EVEN:     All actions for specified player included,
  //                       opponent actions are random but evenly distributed across players
  // SUBGAME_ALL_GREEDY:   All actions for specified player included,
  //                       opponent actions are selected greedily (minimum number of profiles added)
  public enum SamplingMode {
    RANDOM_WITH_REPLACEMENT, RANDOM_WITHOUT_REPLACEMENT, ALL_EVEN_PLUS_RANDOM, ONCE_PER_ACTION,
    SUBGAME_RANDOM, SUBGAME_ALL_RANDOM, SUBGAME_ALL_EVEN, SUBGAME_ALL_GREEDY
  }

  public void exploreGame(SamplingMode samplingMode, EmpiricalMatrixGame eGame,
                          GameObserver gameObs, int player,
                          int samplesPerProfile) {

    // TODO: add some options for computing default payoffs differently (eg from ave payoff)
    eGame.setDefaultPayoff(gameObs.getDefaultPayoff());

    int profileBound = gameObs.numObsLeft() / samplesPerProfile;

    // collect samples from the game matrix according to the given procedure
    switch (samplingMode) {
      case RANDOM_WITH_REPLACEMENT:
        SolverUtils.sampleRandomlyWithReplacement(gameObs, eGame, gameObs.numObsLeft(), samplesPerProfile);
        break;
      case RANDOM_WITHOUT_REPLACEMENT:
        SolverUtils.sampleRandomlyWithoutReplacement(gameObs, eGame, gameObs.numObsLeft(), samplesPerProfile);
        break;
      case ALL_EVEN_PLUS_RANDOM:
        SolverUtils.sampleAllProfiles(gameObs, eGame, gameObs.numObsLeft() / gameObs.getNumProfiles());
        SolverUtils.sampleRandomlyWithoutReplacement(gameObs, eGame, gameObs.numObsLeft(), 1);
        break;
      case ONCE_PER_ACTION:
        sampleOnce(gameObs, eGame, player);
        break;
      case SUBGAME_RANDOM:
      case SUBGAME_ALL_RANDOM:
      case SUBGAME_ALL_EVEN:
      case SUBGAME_ALL_GREEDY:
        sampleSubgame(eGame, profileBound, gameObs, player, samplingMode, samplesPerProfile);
        break;
      default:
        throw new RuntimeException("Error exploring game. Invalid sampling mode: " + samplingMode);
    }
  }

  private void initializeLists(GameObserver go) {
    int numPlayers = go.getNumPlayers();
    int[] numActs = go.getNumActions();

    if (numPlayers != candidates.size() ||
        numPlayers != chosen.size()) {
      // set up data structures for selecting a subgame from the possible actions
      for (int pl = 0; pl < numPlayers; pl++) {
        List<Integer> tmp = new ArrayList<Integer>();
        for (int i = 1; i <= numActs[pl]; i++) {
          tmp.add(i);
        }
        candidates.add(tmp);
        chosen.add(new ArrayList<Integer>());
      }
    } else {
      for (int pl = 0; pl < numPlayers; pl++) {
        List<Integer> tmp = candidates.get(pl);
        tmp.clear();
        for (int i = 1; i <= numActs[pl]; i++) {
          tmp.add(i);
        }
        tmp = chosen.get(pl);
        tmp.clear();
      }
    }
  }

  private void sampleOnce(GameObserver go, EmpiricalMatrixGame eg, int player) {
    // sample (random) exactly one outcome for each action this player has
    int[] outcome = new int[go.getNumPlayers()];
    for (int i = 1; i <= go.getNumActions(player); i++) {
      outcome[player] = i;
      for (int pl = 0; pl < go.getNumPlayers(); pl++) {
        if (pl == player) continue;
        outcome[pl] = rand.nextInt(go.getNumActions(pl)) + 1;
      }
      eg.addSample(outcome, go.getSample(outcome));
      if (go.numObsLeft() == 0) break;
    }
  }

  /**
   * Sample subgames in using one of several methods
   */
  private void sampleSubgame(EmpiricalMatrixGame eGame, int profileBound,
                             GameObserver gameObs, int player,
                             SamplingMode samplingMode, int samplesPerProfile) {

    initializeLists(gameObs);

    // check to see if we can sample everything, if so, skip the selection process
    if (profileBound >= eGame.getNumProfiles()) {
      SolverUtils.sampleAllProfiles(gameObs, eGame, gameObs.numObsLeft() / gameObs.getNumProfiles());
      return;
    }

    // figure out which subgame to sample, depending on the mode
    switch (samplingMode) {
      case SUBGAME_RANDOM:
        selectRandomSubgame(chosen, candidates, profileBound);
        break;
      case SUBGAME_ALL_RANDOM:
        selectAllRandomSubgame(chosen, candidates, profileBound, player);
        break;
      case SUBGAME_ALL_EVEN:
        selectAllEvenSubgame(chosen, candidates, profileBound, player);
        break;
      case SUBGAME_ALL_GREEDY:
        selectAllGreedySubgame(chosen, candidates, profileBound, player);
        break;
      default:
        throw new RuntimeException("Error in epsilon nash solver. Invalid mode in subgame selection: " + samplingMode);
    }

    // collect the samples
    SolverUtils.sampleSubgame(gameObs, eGame, samplesPerProfile, chosen);
  }

  /**
   * Choose all actions randomly
   */
  private void selectRandomSubgame(List<List<Integer>> chosen, List<List<Integer>> candidates, int profileBound) {
    // no samples allowed at all
    if (profileBound < 1) return;

    // select one strategy randomly for each player as a base case
    int playersWithCandidates = selectBase(chosen, candidates);

    // select the rest randomly
    selectRandomly(chosen, candidates, profileBound, playersWithCandidates);
  }

  /**
   * Select all strategies for the given player first, and then randomly choose actions to include for other players
   */
  private void selectAllRandomSubgame(List<List<Integer>> chosen, List<List<Integer>> candidates,
                                      int profileBound, int player) {
    // no samples allowed at all
    if (profileBound < 1) return;

    // select one strategy randomly for each player as a base case
    int playersWithCandidates = selectBase(chosen, candidates);

    // add all of the strategies for the selected player
    selectAllForPlayer(chosen, candidates, profileBound, player);
    playersWithCandidates--;

    // select the rest randomly
    selectRandomly(chosen, candidates, profileBound, playersWithCandidates);
  }

  /**
   * Select all strategies for the given player first, and then select actions for the other
   * players randomly, but maintaining and even distribution across these players
   */
  private void selectAllEvenSubgame(List<List<Integer>> chosen, List<List<Integer>> candidates,
                                    int profileBound, int player) {
    // no samples allowed at all
    if (profileBound < 1) return;

    int nPlayers = candidates.size();

    // select one strategy randomly for each player as a base case
    int playersWithCandidates = selectBase(chosen, candidates);

    // add all of the strategies for the selected player
    selectAllForPlayer(chosen, candidates, profileBound, player);
    playersWithCandidates--;

    // select the rest of the strategies, maintaining balance between the
    // players (but selecting the specific actions randomly)
    List<Integer> eligiblePlayers = new ArrayList<Integer>();
    while (playersWithCandidates > 0) {

      // add all players to the eligible list
      eligiblePlayers.clear();
      for (int pl = 0; pl < nPlayers; pl++) {
        if (candidates.get(pl).size() > 0) {
          eligiblePlayers.add(pl);
        }
      }

      // randomize the list
      Collections.shuffle(eligiblePlayers);

      // select an action for each player
      for (int pl : eligiblePlayers) {
        List<Integer> tmp = candidates.get(pl);

        // select an action
        if (!tmp.isEmpty()) {
          List<Integer> tmpChosen = chosen.get(pl);
          int actIndex = rand.nextInt(tmp.size());
          int act = tmp.get(actIndex);
          tmpChosen.add(act);
          tmp.remove(actIndex);

          // if this causes us to exceed the bound, we cannot add any more actions for this player
          if (subGameSize(chosen) > profileBound) {
            tmpChosen.remove(tmpChosen.size() - 1);
            tmp.clear();
          }

          // if there are no more actions, decrement the counter
          if (tmp.isEmpty()) {
            playersWithCandidates--;
          }
        }
      }
    }
  }

  /**
   * Select all strategies for the given player first, and then select actions for the other
   * players greedily, maximizing the number of strategies added per profile
   */
  private void selectAllGreedySubgame(List<List<Integer>> chosen, List<List<Integer>> candidates,
                                      int profileBound, int player) {
    // no samples allowed at all
    if (profileBound < 1) return;

    int nPlayers = candidates.size();

    // select one strategy randomly for each player as a base case
    int playersWithCandidates = selectBase(chosen, candidates);

    // add all of the strategies for the selected player
    selectAllForPlayer(chosen, candidates, profileBound, player);
    playersWithCandidates--;

    // select the rest of the strategies, maintaining balance between the
    // players (but selecting the specific actions randomly)
    List<Integer> eligiblePlayers = new ArrayList<Integer>();
    while (playersWithCandidates > 0) {

      // add players with min
      eligiblePlayers.clear();
      int currentSize = subGameSize(chosen);
      int minDiff = Integer.MAX_VALUE;
      for (int pl = 0; pl < nPlayers; pl++) {
        if (candidates.get(pl).size() > 0) {
          List<Integer> tmpChosen = chosen.get(pl);
          tmpChosen.add(-1);
          int diff = subGameSize(chosen) - currentSize;
          tmpChosen.remove(tmpChosen.size() - 1);
          if (diff < minDiff) {
            eligiblePlayers.clear();
            eligiblePlayers.add(pl);
          } else if (diff == minDiff) {
            eligiblePlayers.add(pl);
          }
        }
      }

      // randomize the list
      Collections.shuffle(eligiblePlayers);
      int pl = eligiblePlayers.get(0);
      List<Integer> tmp = candidates.get(pl);

      // select an action
      if (!tmp.isEmpty()) {
        List<Integer> tmpChosen = chosen.get(pl);
        int actIndex = rand.nextInt(tmp.size());
        int act = tmp.get(actIndex);
        tmpChosen.add(act);
        tmp.remove(actIndex);

        // if this causes us to exceed the bound, we cannot add any more actions for this player
        if (subGameSize(chosen) > profileBound) {
          tmpChosen.remove(tmpChosen.size() - 1);
          tmp.clear();
        }

        // if there are no more actions, decrement the counter
        if (tmp.isEmpty()) {
          playersWithCandidates--;
        }
      }
    }
  }

  /**
   * Adds all strategies for the given player
   * Terminates if the sample bound is violated
   */
  private void selectAllForPlayer(List<List<Integer>> chosen, List<List<Integer>> candidates,
                                  int profileBound, int player) {
    // add ALL of the strategies for the selected player
    List<Integer> tmp = candidates.get(player);
    List<Integer> tmpChosen = chosen.get(player);
    while (!tmp.isEmpty()) {
      int actIndex = rand.nextInt(tmp.size());
      int act = tmp.get(actIndex);
      tmpChosen.add(act);
      tmp.remove(actIndex);

      // if this causes us to exceed the bound, we cannot add any more actions for this player
      if (subGameSize(chosen) > profileBound) {
        tmpChosen.remove(tmpChosen.size() - 1);
        tmp.clear();
      }
    }
  }

  /**
   * Add additional actions, selecting randomly
   * Terminates when there are no actions that can be selected that do not violate the bound
   */
  private void selectRandomly(List<List<Integer>> chosen, List<List<Integer>> candidates,
                              int profileBound, int playersWithCandidates) {
    int nPlayers = chosen.size();
    int numCandidates = 0;
    for (int pl = 0; pl < nPlayers; pl++) {
      numCandidates += candidates.get(pl).size();
    }

    // now, select all of the others randomly
    while (playersWithCandidates > 0) {
      int selected = rand.nextInt(numCandidates);
      List<Integer> tmpChosen = null;
      List<Integer> tmpCand = null;

      for (int pl = 0; pl < nPlayers; pl++) {
        int tmp = candidates.get(pl).size();
        if (selected >= tmp) {
          selected -= tmp;
        } else {
          tmpCand = candidates.get(pl);
          tmpChosen = chosen.get(pl);
          break;
        }
      }

      if (tmpChosen == null || tmpCand == null) {
        throw new RuntimeException("Error selecting randomly in EpsilonNashSolver, no candidate/chosen list.");
      }

      int act = tmpCand.get(selected);
      tmpChosen.add(act);
      if (subGameSize(chosen) > profileBound) {
        tmpChosen.remove(tmpChosen.size() - 1);
        numCandidates -= tmpCand.size();
        tmpCand.clear();
      } else {
        tmpCand.remove(selected);
        numCandidates--;
      }

      if (tmpCand.isEmpty()) {
        playersWithCandidates--;
      }
    }
  }

  /**
   * Choose a single, randomly-selected action for each player
   */
  private int selectBase(List<List<Integer>> chosen, List<List<Integer>> candidates) {
    int nPlayers = chosen.size();
    int playersWithCandidates = 0;

    // select one strategy for each player randomly
    for (int pl = 0; pl < nPlayers; pl++) {
      List<Integer> tmp = candidates.get(pl);
      int actIndex = rand.nextInt(tmp.size());
      int act = tmp.get(actIndex);
      chosen.get(pl).add(act);
      tmp.remove(actIndex);
      if (tmp.size() > 0) {
        playersWithCandidates++;
      }
    }
    return playersWithCandidates;
  }

  /**
   * Compute the number of profiles in a subgame
   */
  private static int subGameSize(List<List<Integer>> subGame) {
    int cnt = 1;
    for (List<Integer> actList : subGame) {
      cnt *= actList.size();
    }
    return cnt;
  }

}
