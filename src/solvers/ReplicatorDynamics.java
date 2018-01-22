/**
 * $Id$
 * $Date$
 * $Author$
 * $Revision$
 */
package solvers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import games.Game;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import support.ActionData;

public class ReplicatorDynamics {

  //TODO: Update this class to update only a single player at a time

  private static final double MIN_PROB = 0d;
  //private static final double DEFAULT_POPULATION_CHANGE_TOLERANCE = 0d;
  private static final double DEFAULT_EQUILIBRIUM_TOLERANCE = 0.0001d;
  private static final int DEFAULT_RESTART_ITERATIONS = 5;
  private static final int DEFAULT_FORCE_RESTART_ITERATIONS = 100;
  private static final double NO_RESTART_EPSILON = 0.08;

  // The termination tolerance on the L-infinity norm
  //private double populationTolerance;

  // The tolerance for finding an approximate equilibria
  private double equilibriumTolerance;

  // The maximum number of iterations to run.
  private int maxIteration;

  // The number of iterations to run before randomly restarting the population
  private int restartIterations;

  // The number of iterations to run before restarting, even if we are close to equilibrium
  private int forceRestartIterations;

  // cached for efficiency
  List<MixedStrategy> mostStableProfile = new ArrayList<MixedStrategy>();
  List<MixedStrategy> currentPopulation = new ArrayList<MixedStrategy>();
  List<MixedStrategy> nextPopulation = new ArrayList<MixedStrategy>();
  OutcomeDistribution currentOutcome;
  OutcomeIterator itr;
  double[] outcomePayoffs;
  ActionData pureStrategyPayoffs;
  List<Integer> restrictedPlayers = new ArrayList<Integer>();


  public ReplicatorDynamics() {
    this(DEFAULT_EQUILIBRIUM_TOLERANCE, Integer.MAX_VALUE,
         DEFAULT_RESTART_ITERATIONS, DEFAULT_FORCE_RESTART_ITERATIONS);
  }

  public ReplicatorDynamics(int maxIterations) {
    this(DEFAULT_EQUILIBRIUM_TOLERANCE, maxIterations,
         DEFAULT_RESTART_ITERATIONS, DEFAULT_FORCE_RESTART_ITERATIONS);
  }

  public void setMaxIteration(int maxIteration) {
    this.maxIteration = maxIteration;
  }

  /**
   * Create a new replicator dynamics search.
   *
   * @param maxIteration the maximum number of iterations to run.
   */
  public ReplicatorDynamics(double equilibriumTolerance, int maxIteration,
                            int restartIterations, int forceRestartIterations) {
    this.equilibriumTolerance = equilibriumTolerance;
    this.maxIteration = maxIteration;
    this.restartIterations = restartIterations;
    this.forceRestartIterations = forceRestartIterations;
  }

  /**
   * Run discrete-time replicator dynamics until the maximum number of steps is reached
   * or the termination criteria is achieved
   */
  public List<MixedStrategy> run(Game game) {
    int iteration = 0;
    boolean terminationCondition = false;
    int nPlayers = game.getNumPlayers();
    int[] nActs = game.getNumActions();
    double noResetThreshold = Double.POSITIVE_INFINITY;
    double noResetEpsilon = game.getPayoffRange() * NO_RESTART_EPSILON;
    double minPayoff = game.getExtremePayoffs()[1];
    double minEpsilon = Double.POSITIVE_INFINITY;

    // adjust the size of the populations
    if (nPlayers != mostStableProfile.size()) {
      initPopulations(nPlayers, nActs);
    } else {
      for (int pl = 0; pl < nPlayers; pl++) {
        if (nActs[pl] != mostStableProfile.get(pl).getNumActions()) {
          initPopulations(nPlayers, nActs);
          break;
        }
      }
    }

    // initialize the population; uniform random play to start
    for (int pl = 0; pl < nPlayers; pl++) {
      currentPopulation.get(pl).setUniform();
      mostStableProfile.get(pl).setUniform();
    }

    // run the update process until a termination condition is reached
    while (iteration++ < maxIteration && !terminationCondition) {
      terminationCondition = false;

      // initialize the joint distribution of play
      currentOutcome.setMixedStrategies(currentPopulation);

      // compute ave (weighted) score of the population
      computeOutcomePayoffs(game, currentOutcome);

      // compute the payoffs for each pure strategy in this context
      computePureStrategyPayoffs(game, currentOutcome);

      double epsilon = SolverUtils.computeStability(outcomePayoffs, pureStrategyPayoffs);

      if (epsilon < minEpsilon) {
        minEpsilon = epsilon;
        noResetThreshold = minEpsilon + noResetEpsilon;
        for (int pl = 0; pl < nPlayers; pl++) {
          mostStableProfile.get(pl).setProbs(currentPopulation.get(pl));
        }
      }

      if (epsilon < equilibriumTolerance) {
        terminationCondition = true;
      }

      //System.out.println("payoffs: " + pureStrategyPayoffs);

      // for each player, compute a new population
      for (int pl = 0; pl < nPlayers; pl++) {
        MixedStrategy newPopulation = nextPopulation.get(pl);
        MixedStrategy oldPopulation = currentPopulation.get(pl);
        for (int a = 1; a <= nActs[pl]; a++) {
          newPopulation.setProb(a, Math.max(MIN_PROB, oldPopulation.getProb(a)) *
                                   (pureStrategyPayoffs.get(pl, a) - minPayoff));

//          double delta = stepSize * oldPopulation.getProb(a) * (pureStrategyPayoffs.get(pl, a) - outcomePayoffs[pl]);
//          delta /= 1d + (stepSize * outcomePayoffs[pl]);
//          newPopulation.setProb(a, oldPopulation.getProb(a) + delta);
        }
        newPopulation.normalize();
        //System.out.println("New population " + pl + ": " + newPopulation);

        // sanity check to see that the new population is valid...
//        if (!newPopulation.isValid()) {
//          System.out.println("WARNING: Invalid new population in replicator dynamics: " + newPopulation);
//        }
      }

      // check to see if the populations are still changing (within the tolerance)
//      boolean populationChanging = false;
//      for (int pl = 0; pl < nPlayers; pl++) {
//        double norm = Linfinity(currentPopulation.get(pl).getProbs(), nextPopulation.get(pl).getProbs());
//        if (norm >= populationTolerance) {
//          populationChanging = true;
//          break;
//        }
//      }

      // random restart if population is not changing *or* we hit a particular number of iterations
      // if the current population looks promising, continue exploration for more iterations
      //if (!populationChanging || iteration % restartIterations == 0) {
      if (iteration % restartIterations == 0) {
        if (iteration % (forceRestartIterations) == 0 ||
            epsilon > noResetThreshold) {
          for (int pl = 0; pl < nPlayers; pl++) {
            nextPopulation.get(pl).setRandom();
          }
        }
      }

      // update the population structures
      for (int pl = 0; pl < nPlayers; pl++) {
        currentPopulation.get(pl).setProbs(nextPopulation.get(pl));
      }
    }

//    System.out.println("Min epsilon: " + minEpsilon);
//    System.out.println("Best population: ");
//    for (int pl = 0; pl < nPlayers; pl++) {
//      System.out.println("Pl " + pl + ": " + mostStableProfile.get(pl));
//    }

    return mostStableProfile;
  }

  private void initPopulations(int nPlayers, int[] nActs) {
    mostStableProfile.clear();
    currentPopulation.clear();
    nextPopulation.clear();
    for (int pl = 0; pl < nPlayers; pl++) {
      mostStableProfile.add(new MixedStrategy(nActs[pl]));
      currentPopulation.add(new MixedStrategy(nActs[pl]));
      nextPopulation.add(new MixedStrategy(nActs[pl]));
    }
    currentOutcome = new OutcomeDistribution(nActs);
    outcomePayoffs = new double[nPlayers];
    itr = new OutcomeIterator(nPlayers, nActs);
    pureStrategyPayoffs = new ActionData(nActs);
  }

  private double Linfinity(double[] a, double[] b) {
    double norm = 0;

    for (int i = 0; i < a.length; i++) {
      norm = Math.max(Math.abs(a[i] - b[i]), norm);
    }

    return norm;
  }

  // optimized version of SolverUtils version of this function
  public void computeOutcomePayoffs(Game g, OutcomeDistribution outcomeDistribution) {
    int nPlayers = g.getNumPlayers();
    Arrays.fill(outcomePayoffs, 0d);

    // loop through outcomes to compute the expected payoffs
    itr.reset();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      double prob = outcomeDistribution.getProb(outcome);
      if (prob > 0) {
        // add in this component of the payoffs
        double[] tmpPayoffs = g.getPayoffs(outcome);
        for (int pl = 0; pl < nPlayers; pl++) {
          outcomePayoffs[pl] += prob * tmpPayoffs[pl];
        }
      }
    }
  }

  // optimized version of same function from SolverUtils
  public void computePureStrategyPayoffs(Game g, OutcomeDistribution outcomeDistribution) {
    int nPlayers = g.getNumPlayers();
    int[] nActs = g.getNumActions();
    int[] outcome = new int[nPlayers];
    pureStrategyPayoffs.setZeros();

    // compute the payoff for each player's pure strategies
    for (int pl = 0; pl < nPlayers; pl++) {
      restrictedPlayers.clear();
      restrictedPlayers.add(pl);
      OutcomeDistribution conditional = outcomeDistribution.getConditionalDistribution(restrictedPlayers);

      // loop over all of the restricted outcomes, averaging the outcomes
      OutcomeIterator itr = conditional.iterator();
      while (itr.hasNext()) {
        int[] conditionalOutcome = itr.next();

        double prob = conditional.getProb(conditionalOutcome);
        if (prob <= 0) continue;

        // create the non-marginal outcomes to get the payoffs
        for (int pl2 = 0; pl2 < nPlayers; pl2++) {
          if (pl2 < pl) outcome[pl2] = conditionalOutcome[pl2];
          else if (pl2 > pl) outcome[pl2] = conditionalOutcome[pl2 - 1];
        }

        // average in the payoffs for each possible action
        for (int a = 1; a <= nActs[pl]; a++) {
          outcome[pl] = a;
          double[] payoffs = g.getPayoffs(outcome);
          double oldValue = pureStrategyPayoffs.get(pl, a);
          pureStrategyPayoffs.set(pl, a, oldValue + (prob * payoffs[pl]));
        }
      }
    }
  }

}