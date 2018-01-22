package solvers;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.returnSB;

import java.util.Arrays;
import java.util.TreeSet;

import candidate.CompareProfileMaxDevBounds;
import games.EmpiricalMatrixGame;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import support.ActionData;
import support.ProfileInfo;
import util.GenericTensor;

/**
 * Utilities to the stability of pure-strategy profiles in games
 * <p/>
 * TODO: Add some error checking
 */

public final class StabilityAnalysis {

  private static final int DEFAULT_SAMPLE_THRESHOLD = 1;

  // The threshold for determining whether to consider profiles sampled
  // or unsampled. Unsampled do not count towards the analysis
  private int sampleThreshold = DEFAULT_SAMPLE_THRESHOLD;

  private final candidate.CompareProfileMaxDevBounds compDevBounds =
          new candidate.CompareProfileMaxDevBounds();
  private final GenericTensor<ProfileInfo> profiles = new GenericTensor<ProfileInfo>();
  private final TreeSet<ProfileInfo> profilesByDevBound = new TreeSet<ProfileInfo>(compDevBounds);

  private int nPlayers;
  private int[] nActs;
  private int nProfiles;
  private int nDeviations;
  private int totalDeviationsSampled;
  private final ActionData minDeviationBenefitsByAction;   // minimum benefit to deviating from a *profile* containing this action
  private final ActionData aveDeviationBenefitsByAction;   // average benefit to deviating from *profiles* containing this action

  /**
   * Create a new stability analysis object, analyzing the given game
   */
  public StabilityAnalysis(EmpiricalMatrixGame game, int sampleThreshold) {
    this.nPlayers = game.getNumPlayers();
    this.nActs = game.getNumActions();
    this.nProfiles = game.getNumProfiles();
    this.nDeviations = game.getNumPossibleDeviations();
    this.sampleThreshold = sampleThreshold;
    this.minDeviationBenefitsByAction = new ActionData(game.getNumActions(), Double.POSITIVE_INFINITY);
    this.aveDeviationBenefitsByAction = new ActionData(game.getNumActions());
    analyzeStability(game);
  }

  /**
   * Get the number of possible deviations; this is the same for all profiles
   */
  public int getNumDeviations() {
    return nDeviations;
  }

  /**
   * Gets the total number of deviations sampled over all profiles
   */
  public int getTotalDeviationsSampled() {
    return totalDeviationsSampled;
  }

  /**
   * Gets the average number of deviations sampled per explored profile
   */
  public double getAveDeviationsSampled() {
    return (double) totalDeviationsSampled / (double) profilesByDevBound.size();
  }

  /**
   * Get the number of deviations sampled from this profile, relative to
   * the threshold
   */
  public int getNumDeviationsSampled(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null) {
      return -1;
    }
    return prof.numDeviationsSampled;
  }

  /**
   * Get the percentage of deviations sampled from this profile, relative to
   * the threshold
   */
  public double getPercentDeviationsSampled(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null || prof.numDeviationsSampled <= 0) {
      return Double.NEGATIVE_INFINITY;
    }
    return (double) prof.numDeviationsSampled / (double) prof.numPossibleDeviations;
  }

  /**
   * Get the number of beneficial deviations from this profile
   */
  public int getNumBeneficialDeviations(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null) {
      return 0;
    }
    return prof.numBeneficial;
  }

  /**
   * Get the percentage of beneficial deviations from this profile
   */
  public double getPercentBeneficialDeviations(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null || prof.numDeviationsSampled <= 0) {
      return Double.NEGATIVE_INFINITY;
    }
    return (double) prof.numBeneficial / (double) prof.numDeviationsSampled;
  }

  /**
   * Get the maximum benefit to deviating from this profile
   * Note the this may be NEGATIVE
   */
  public double getMaxDeviationBenefit(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null) {
      return Double.NEGATIVE_INFINITY;
    }
    return prof.maxBenefit;
  }

  /**
   * Get the average benefit to deviating from this profile
   */
  public double getAveDeviationBenefit(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null || prof.numDeviationsSampled <= 0) {
      return Double.NEGATIVE_INFINITY;
    }
    return prof.getAveBenefit();
  }

  /**
   * Ruturns the epsilon bound (max benefit to deviation, bounded below by 0)
   */
  public double getEpsilonBound(int[] outcome) {
    ProfileInfo prof = profiles.getValue(outcome);
    if (prof == null) {
      return 0d;
    }
    return prof.getEpsBound();
  }

  /**
   * Direct access to the full set of sorted profiles
   */
  public TreeSet<ProfileInfo> getAllProfilesSorted() {
    return profilesByDevBound;
  }

  /**
   * Returns the payoffs for each action when it is played by all players in the game
   * NOTE: This is only defined for SYMMETRIC GAMES
   *
   * @return the payoff vector for each strategy; null if there is an assymetric number of actions
   */
  public double[] getPureProfilePayoffs() {
    // check to make sure the game is not assymetric (at least in terms of number of actions)
    for (int pl = 1; pl < nPlayers; pl++) {
      if (nActs[pl] != nActs[pl - 1]) {
        return null;
      }
    }

    double[] purePayoffs = new double[nActs[0] + 1];
    int[] outcome = new int[nPlayers];
    for (int a = 1; a <= nActs[0]; a++) {
      Arrays.fill(outcome, a);
      ProfileInfo pi = profiles.getValue(outcome);
      if (pi != null) {
        purePayoffs[a] = pi.payoffs[0];
      } else {
        purePayoffs[a] = Double.NEGATIVE_INFINITY;
      }
    }
    return purePayoffs;
  }

  /**
   * Returns the max benefit to deviating for each action when it is played by all players in the game
   * NOTE: This is only defined for SYMMETRIC GAMES
   *
   * @return the max BTD vector for each strategy; null if there is an assymetric number of actions
   */
  public double[] getPureProfileBTD() {
    // check to make sure the game is not assymetric (at least in terms of number of actions)
    for (int pl = 1; pl < nPlayers; pl++) {
      if (nActs[pl] != nActs[pl - 1]) {
        return null;
      }
    }

    double[] pureBTD = new double[nActs[0] + 1];
    int[] outcome = new int[nPlayers];
    for (int a = 1; a <= nActs[0]; a++) {
      Arrays.fill(outcome, a);
      ProfileInfo pi = profiles.getValue(outcome);
      pureBTD[a] = pi.maxBenefit;
    }
    return pureBTD;
  }

  /**
   * @return The index of the pure strategy with the most stable pure symmetric profile
   */
  public int getMostStablePureProfile() {
    // check to make sure the game is not assymetric (at least in terms of number of actions)
    for (int pl = 1; pl < nPlayers; pl++) {
      if (nActs[pl] != nActs[pl - 1]) {
        return 0;
      }
    }

    double minBTD = Double.POSITIVE_INFINITY;
    int minStrategy = 0;

    int[] outcome = new int[nPlayers];
    for (int a = 1; a <= nActs[0]; a++) {
      Arrays.fill(outcome, a);
      ProfileInfo pi = profiles.getValue(outcome);

      if (pi.maxBenefit < minBTD) {
        minBTD = pi.maxBenefit;
        minStrategy = a;
      }
    }
    return minStrategy;
  }

  /**
   * @return the benefit to deviating for the *most stable profile* containing each pure strategy
   */
  public ActionData getMinDeviationBenefitsByAction() {
    return minDeviationBenefitsByAction;
  }

  /**
   * @return the average benefit to deviating from a profile containing each pure strategy;
   *         profiles are weighted by the number of occurences of each pure strategy
   */
  public ActionData getAveDeviationBenefitsByAction() {
    return aveDeviationBenefitsByAction;
  }

  /**
   * @param sampleBound the minimum number of deviations that must be explored; use 0 for unbounded
   * @return the profile with the minimum benefit to deviating (null if no profile meets the criteria)
   */
  public ProfileInfo getMostStableProfile(int sampleBound) {
    for (ProfileInfo pi : profilesByDevBound) {
      if (pi.numDeviationsSampled >= sampleBound) {
        return pi;
      }
    }
    return null;
  }

  /**
   * Computes a Boltzmann distribution over the outcomes using the maximum benefit to deviating
   * as an indication of the stability and thus likelihood of a given profile. As the temperature
   * approaches infinity, this approaches the uniform random distribution. As the temperature
   * approaches 0, only the most stable profiles have positive probabilty.
   *
   * @param temperature         The temperature parameter in the Boltzmann distribution
   * @param sampleBound         Absolute bound on the number of deviations that must be explored to consider a profile
   * @param useNegativeBenefits If true, the most stable profiles have negative benefit to deviating. If false
   *                            the benefit to deviating is bounded by 0 from below (all Nash equilbria are considered equivalent).
   * @return A Boltzmann distribution over outcomes.
   */
  public OutcomeDistribution getBoltzmannOutcome(double temperature, double sampleBound, boolean useNegativeBenefits) {
    OutcomeDistribution dist = new OutcomeDistribution(nActs, 0d);
    double total = 0d;

    //BigDecimal total = BigDecimal.ZERO;

    //System.out.println("Computing boltzman: " + temperature + " bound: " + sampleBound);

    // first, compute the nomalization term (summation over all values)
    for (ProfileInfo pi : profilesByDevBound) {
      //System.out.println("Profile: " + pi.numDeviationsSampled);
      if (pi.numDeviationsSampled < sampleBound || pi.numDeviationsSampled < 1) continue;
      //System.out.println("Valid profile: " + pi.maxBenefit);
      double btd = useNegativeBenefits ? pi.maxBenefit : pi.getEpsBound();

      total += Math.pow(Math.E, -btd / temperature);
      //double tmp = Math.pow(Math.E, -btd / temperature);
      //total = total.add(new BigDecimal(tmp));
      //System.out.println("Total: " + total);
    }

    //if (total.compareTo(BigDecimal.ZERO) == 0) {
    if (total == 0) {
      System.out.println("Warning: No valid profiles for Boltzmann distribution! Setting uniform...");
      dist.setCentroid();
      return dist;
    }

    // compute probability for each profile
    for (ProfileInfo pi : profilesByDevBound) {
      if (pi.numDeviationsSampled < sampleBound) continue;
      double btd = useNegativeBenefits ? pi.maxBenefit : pi.getEpsBound();
      double prob = Math.pow(Math.E, -btd / temperature);

      //System.out.println("BTD: " + btd + " Prob" + prob/total);
      //dist.setProb(pi.outcome, (new BigDecimal(prob)).divide(total, BigDecimal.ROUND_HALF_EVEN).doubleValue() );
      dist.setProb(pi.outcome, prob / total);
    }

    // check that the resulting distribution is valid
    if (!dist.isValid()) {
      System.out.println("Warning: Invalid Boltzmann distribution: " + dist.toString());
      System.exit(1);
    }

    return dist;
  }

  /**
   * @param sampleBound the minimum number of deviations that must be explored; use 0 for unbounded
   * @return the set of profiles that are most stable (minimum benefit to deviating); empty if none
   */
  public TreeSet<ProfileInfo> getMostStableProfiles(int sampleBound) {
    TreeSet<ProfileInfo> profileSet =
            new TreeSet<ProfileInfo>(new candidate.CompareProfileMaxDevBounds());
    boolean foundMin = false;
    double min = Double.NEGATIVE_INFINITY;

    for (ProfileInfo pi : profilesByDevBound) {
      if (pi.numDeviationsSampled >= sampleBound) {
        if (foundMin) {
          if (pi.maxBenefit == min) {
            profileSet.add(pi);
          } else {
            break;
          }
        } else {
          foundMin = true;
          min = pi.maxBenefit;
          profileSet.add(pi);
        }
      }
    }
    return profileSet;
  }

  /**
   * @param sampleBound the minimum number of deviations that must be explored; use 0 for unbounded
   * @return the set of profiles that have the minimum epsilon value (empty if none)
   */
  public TreeSet<ProfileInfo> getMinEpsilonProfiles(int sampleBound) {
    TreeSet<ProfileInfo> profileSet = new TreeSet<ProfileInfo>(new CompareProfileMaxDevBounds());
    boolean foundMin = false;
    double minEps = Double.NEGATIVE_INFINITY;

    for (ProfileInfo pi : profilesByDevBound) {
      if (pi.numDeviationsSampled >= sampleBound) {
        if (foundMin) {
          if (pi.getEpsBound() == minEps) {
            profileSet.add(pi);
          } else {
            break;
          }
        } else {
          foundMin = true;
          minEps = pi.getEpsBound();
          profileSet.add(pi);
        }
      }
    }
    return profileSet;
  }

  /**
   * @param bound       the maximum epsilon to include a profile in the set
   * @param sampleBound the minimum number of deviations that must be explored; use 0 for unbounded samples
   * @return a set of all profiles with deviation benefit below the given threshold (empty if none)
   */
  public TreeSet<ProfileInfo> getEpsilonBoundedProfileSet(int sampleBound, double bound) {
    TreeSet<ProfileInfo> profileSet =
            new TreeSet<ProfileInfo>(new candidate.CompareProfileMaxDevBounds());

    for (ProfileInfo pi : profilesByDevBound) {
      // done; epsilon too big
      if (pi.maxBenefit > bound) {
        return profileSet;
      }
      if (pi.numDeviationsSampled >= sampleBound) {
        profileSet.add(pi);
      }
    }
    return profileSet;
  }

  /**
   * @param sizeBound   the maximum number of profiles to include in the set
   * @param sampleBound is the minimum number of deviations that must be explored; use 0 for unbounded
   * @return the set of N profiles with the lowest benefits to deviating (empty if none)
   */
  public TreeSet<ProfileInfo> getSizeBoundedProfileSet(int sampleBound, int sizeBound) {
    TreeSet<ProfileInfo> profileSet =
            new TreeSet<ProfileInfo>(new candidate.CompareProfileMaxDevBounds());

    for (ProfileInfo pi : profilesByDevBound) {
      if (pi.numDeviationsSampled >= sampleBound) {
        profileSet.add(pi);
      }
      // done; we have enough profiles
      if (profileSet.size() >= sizeBound) {
        return profileSet;
      }
    }
    return profileSet;
  }

  /**
   * @param profileSet A set of profiles
   * @return a string representation of a set of profiles
   */
  public String toString(TreeSet<ProfileInfo> profileSet) {
    StringBuilder sb = getSB();
    for (ProfileInfo pi : profileSet) {
      sb.append(pi.toString()).append("\n");
    }
    return returnSB(sb);
  }

  /**
   * Compute stability for a particular game
   * Must be run before running the other access methods
   *
   * @param game the game to analyze
   */
  //TODO: optimize to take advantage of symmetry
  private void analyzeStability(EmpiricalMatrixGame game) {
    int[] devOutcome = new int[nPlayers];
    OutcomeIterator itr = game.iterator();

    // initialize the tensor for storing the profile information
    profiles.init(nActs);

    // loop over all of the possible outcomes of the game
    while (itr.hasNext()) {
      int[] outcome = itr.next();

      // check whether we have enough samples of this profile
      if (game.getNumSamples(outcome) < sampleThreshold) continue;

      double[] payoffs = game.getPayoffs(outcome);
      ProfileInfo profile = new ProfileInfo(outcome, payoffs, nDeviations);

      // copy current profile to the deviation profile
      System.arraycopy(outcome, 0, devOutcome, 0, nPlayers);

      // check each possible deviation for this game
      // TODO: update this to use a deviation iterator
      for (int pl = 0; pl < nPlayers; pl++) {
        for (int a = 1; a <= nActs[pl]; a++) {
          if (outcome[pl] == a) continue;
          devOutcome[pl] = a;

          // check the number of samples for the deviating profile
          if (game.getNumSamples(devOutcome) < sampleThreshold) continue;

          // payoffs for deviation
          double[] devPayoffs = game.getPayoffs(devOutcome);
          double diff = devPayoffs[pl] - payoffs[pl];

          // update all stats
          profile.numDeviationsSampled++;
          totalDeviationsSampled++;
          if (diff > 0) {
            profile.numBeneficial++;
          }
          profile.maxBenefit = Math.max(profile.maxBenefit, diff);
          profile.totBenefit += diff;
        }
        devOutcome[pl] = outcome[pl];
      }

      // update profile epsilon statistics for each strategy
      for (int pl = 0; pl < nPlayers; pl++) {
        double tmpMin = minDeviationBenefitsByAction.get(pl, profile.outcome[pl]);
        if (profile.maxBenefit < tmpMin) {
          minDeviationBenefitsByAction.set(pl, profile.outcome[pl], profile.maxBenefit);
        }
        double oldAve = aveDeviationBenefitsByAction.get(pl, profile.outcome[pl]);
        aveDeviationBenefitsByAction.set(pl, profile.outcome[pl], oldAve + profile.maxBenefit);
      }

      // store the profile statistics
      profiles.setValue(profile, outcome);
      profilesByDevBound.add(profile);
    }

    // average out the epsilons
    for (int pl = 0; pl < nPlayers; pl++) {
      double nSamples = (double) nProfiles / (double) nActs[pl];
      for (int a = 1; a <= nActs[pl]; a++) {
        double tot = aveDeviationBenefitsByAction.get(pl, a);
        aveDeviationBenefitsByAction.set(pl, a, tot / nSamples);
      }
    }
  }
}
