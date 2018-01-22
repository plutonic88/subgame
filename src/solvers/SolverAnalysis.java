package solvers;

import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.nf;
import static subgame.EGAUtils.returnSB;
import static subgame.Parameters.AGGREGATE_ANALYSIS_DIR;
import static subgame.Parameters.EXTENSION;
import static subgame.Parameters.GAME_FILES_PATH;
import static subgame.Parameters.GAMUT_GAME_EXTENSION;
import static subgame.Parameters.RESULTS_PATH;
import static subgame.Parameters.STRATEGIES_EXTENSION;
import static subgame.Parameters.STRATEGY_DIR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import games.EmpiricalMatrixGame;
import games.Game;
import games.GameUtils;
import games.MatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import games.SymmetricEmpiricalMatrixGame;
import observers.GameObserver;
import output.SimpleOutput;
import parsers.GamutParser;
import support.ActionData;
import support.ActionDataInteger;
import support.ProfileInfo;

/**
 * Class that performs analysis of a set of algorithms
 * Includes facilities for applying solution algorithms to sample gamesmax and
 * generating/analyzing the "algorithm game matrix"
 */

public final class SolverAnalysis {

  private static final boolean VERBOSE_WRITE_ALL_STRATEGY_CHOICES = false;
//  public static final boolean VERBOSE_WRITE_ALL_SAMPLE_MS_GAMES = false;

  private static final int DEFAULT_SAMPLES_PER_GAME_INSTANCE = 1;
  private static final String FULL_GAME_NAME = "algorithm_game";
  private static final String FULL_GAME_NAME_SYMMETRIC = "algorithm_game_symmetric";
  private static final String VERBOSE_ANALYSIS_FILE_NAME = "verbose_analysis_symmetric";
  private static final String DESCRIPTION_FILE_NAME = "description.txt";
  private static final int PROFILES_TO_PRINT = 25;

  private String experimentName = null;
  private String subExperimentName = null;
  private int samplesPerGameInstance = DEFAULT_SAMPLES_PER_GAME_INSTANCE;
  private final ArrayList<GameSolver> algorithms = new ArrayList<GameSolver>();
  private final ArrayList<GameObserver> observers = new ArrayList<GameObserver>();
  private String gameClassName = null;
  private int numBenchmarks = 1;
  private int maxSamples = Integer.MAX_VALUE;

  private int nAlgs;
  private int nGameInstances;
  private int nSamples;
  private int nPlayers;
  private int[] nActions;
  private EmpiricalMatrixGame algGame;
  private OutcomeIterator itr;

  private final ArrayList<MixedStrategy[]> selectedStrategies = new ArrayList<MixedStrategy[]>();
  private final ArrayList<MixedStrategy> strategyList = new ArrayList<MixedStrategy>();
  private EmpiricalMatrixGame sampleGame = null;
  private OutcomeDistribution sampleDistribution = null;

  private StringBuilder[] BTDsamples = null;
  private double[] BTDsum = null;
  private double[][] BTDPairSum = null;
  private double[] BTDsumSquared = null;
  private double[][] BTDPairSumSquared = null;
  private double[] algParams = null;

  public SolverAnalysis() {
  }

  public void setAlgParams(double[] algParams) {
    this.algParams = algParams;
  }

  public int getMaxSamples() {
    return maxSamples;
  }

  public void setMaxSamples(int maxSamples) {
    this.maxSamples = maxSamples;
  }

  /**
   * Set the top-level experiment name
   */
  public void setExperimentName(String experimentName) {
    this.experimentName = experimentName;
  }

  /**
   * Set the 2nd-tier analysis name
   */
  public void setSubExperimentName(String subExperimentName) {
    this.subExperimentName = subExperimentName;
  }

  public void setNumBenchmarks(int numBenchmarks) {
    this.numBenchmarks = numBenchmarks;
  }

  /**
   * Set the number of samples to collect for each test game instance
   * This is useful when the solution methods have high variance in the chosen strategies
   */
  public void setSamplesPerGameInstance(int numSamples) {
    this.samplesPerGameInstance = numSamples;
  }

  /**
   * Add an algorithm for analysis
   */
  public void addAlgorithm(GameSolver algorithm, GameObserver observer) {
    algorithms.add(algorithm);
    observers.add(observer);
  }

  public int getNumAlgorithms() {
    return algorithms.size();
  }

  public List<String> getAlgorithmNames() {
    List<String> list = new ArrayList<String>();
    for (GameSolver solver : algorithms) {
      list.add(solver.getName());
    }
    return list;
  }

  /**
   * Set the name of the class of underlying game instances
   */
  public void setGameClassName(String name) {
    this.gameClassName = name;
  }

  /**
   * Initialize all data for the start of a run
   */
  private void init() {
    nAlgs = algorithms.size();
    nGameInstances = 0;
    nSamples = 0;
    nPlayers = 0;
    nActions = null;
    algGame = null;
    sampleGame = null;
    sampleDistribution = null;

    // initialize structures for tracking BTD samples
    BTDsamples = new StringBuilder[nAlgs+1];
    BTDsum = new double[nAlgs+1];
    BTDsumSquared = new double[nAlgs+1];
    Arrays.fill(BTDsum, 0d);
    Arrays.fill(BTDsumSquared, 0d);
    for (int i = 1; i <= nAlgs; i++) {
      BTDsamples[i] = getSB();
    }

    BTDPairSum = new double[nAlgs+1][nAlgs+1];
    BTDPairSumSquared = new double[nAlgs+1][nAlgs+1];
  }

  /**
   * Perform initializations once we see the first game instance and have the dimensions
   */
  private void firstGame(MatrixGame game) {
    nPlayers = game.getNumPlayers();
    nActions = new int[nPlayers];
    Arrays.fill(nActions, nAlgs);
    algGame = new EmpiricalMatrixGame(nPlayers, nActions);
    itr = algGame.iterator();
    sampleGame = new EmpiricalMatrixGame(nPlayers, nActions);
    sampleDistribution = new OutcomeDistribution(game.getNumActions());
    selectedStrategies.clear();
    for (int alg = 0; alg < nAlgs; alg++) {
      selectedStrategies.add(new MixedStrategy[nPlayers]);
    }
  }

  /**
   * Create an empirical matrix specifying the expected payoffs to each player for using each
   * possible analysis algorithm, over the given classes of games
   * <p/>
   * NOTE: assumes that all games have the same number of players
   */
  public EmpiricalMatrixGame computeAlgorithmGame() {
    init();
    String slash = "/";
    if(System.getProperty("os.name").toLowerCase().contains("win")){
        slash = "\\";
    }
    // read in the game files for this class
    File gameFileDir = new File(GAME_FILES_PATH + gameClassName);
    if (!gameFileDir.exists()) {
      throw new RuntimeException("Attempting to analyze non-existent class of games: " + gameFileDir);
    }

    // create necessary directory structures for holding the results
    File tmpDir = new File(RESULTS_PATH + experimentName + slash + AGGREGATE_ANALYSIS_DIR);
    if (!tmpDir.exists()) tmpDir.mkdirs();

    tmpDir = new File(RESULTS_PATH + experimentName + slash + subExperimentName);
    if (!tmpDir.exists()) tmpDir.mkdirs();

//    if (VERBOSE_WRITE_ALL_SAMPLE_MS_GAMES) {
//      tmpDir = new File(RESULTS_PATH + experimentName + "/" + subExperimentName + "/" + SAMPLE_GAME_DIR);
//      if (!tmpDir.exists()) tmpDir.mkdirs();
//    }

    if (VERBOSE_WRITE_ALL_STRATEGY_CHOICES) {
      for (int alg = 1; alg <= nAlgs; alg++) {
        tmpDir = new File(RESULTS_PATH + experimentName + slash + subExperimentName + slash + STRATEGY_DIR + slash + alg);
        if (!tmpDir.exists()) tmpDir.mkdirs();
      }
    }

    System.out.println("Computing for game class: " + gameClassName);

    for (String str : gameFileDir.list()) {
      // not a gamut game file
      if (!str.endsWith(GAMUT_GAME_EXTENSION)) {
        continue;
      }

      // extract the game number
      String id = str.split("\\.")[0];
      String gameFileName = GAME_FILES_PATH + gameClassName + slash + str;
      System.out.println("Game file: " + gameFileName);

      // read in the game
      MatrixGame game = GamutParser.readGamutGame(gameFileName);

      // first game
      if (algGame == null) {
        firstGame(game);
      }
                  
      nGameInstances++;
      for (int sample = 0; sample < samplesPerGameInstance; sample++) {
        nSamples++;
        System.out.println("Sample: " + sample);

        // analyze this game for the current set of strategies
        computeAllStrategies(game, sample);
        System.out.println("Computed all strategies.");

        if (VERBOSE_WRITE_ALL_STRATEGY_CHOICES) {
          writeAllStrategies(id, sample);
          System.out.println("Wrote all strategies.");
        }

        computeAndAddSample(game, algGame);
        updateBTD();
        System.out.println("Computed and added sample MS game");

//        EmpiricalMatrixGame tmpGame = computePayoffMatrix(game);
//        System.out.println("Computed game payoff matrix.");
//
//        addSampleGame(algGame, tmpGame);
//        System.out.println("Added sample to main game.");
//
//        if (VERBOSE_WRITE_ALL_SAMPLE_MS_GAMES) {
//          SimpleOutput.writeGame(RESULTS_PATH + experimentName + "/" + subExperimentName +
//                                 "/" + SAMPLE_GAME_DIR + "/" + id + "_" + sample + GAMUT_GAME_EXTENSION,
//                                 tmpGame);
//        }

        if (nSamples >= maxSamples) break;
      }
      if (nSamples >= maxSamples) break;
    }

    if (nSamples <= 0) {
      System.err.println("Solver Analysis: no sample games to process!");
      return null;
    }

    // output a description of this experiment
    outputExperimentDescription();

    // output the final matrices and analysis of this meta-strategy game
    outputAnalysis(algGame);

    return algGame;
  }


  /**
   * Output a file describing this experiment, including the game classes, candidate algorithms, and obvservers
   */
  public void outputExperimentDescription() {
    try {
      FileWriter out = new FileWriter(RESULTS_PATH + experimentName + "/" +
                                      subExperimentName + "/" +
                                      DESCRIPTION_FILE_NAME);
      out.write(getExperimentDescription());
      out.close();

    } catch (IOException e) {
      throw new RuntimeException("Error writing game description to file: " + e.getMessage());
    }
  }

  /**
   * Returns a string describing this experiment
   */
  public String getExperimentDescription() {
    StringBuilder sb = getSB();
    sb.append("Game class: ").append(gameClassName).append("\n");
    sb.append("Number of game instances: ").append(nGameInstances).append("\n");
    sb.append("Number of samples: ").append(nSamples).append("\n\n");

    for (int i = 0; i < algorithms.size(); i++) {
      sb.append("********************************************\n");
      sb.append("Candidate Algorithm ").append(i + 1).append("\n");
      sb.append("********************************************\n\n");
      sb.append(algorithms.get(i).getDescription()).append("\n");
      sb.append(observers.get(i).getDescription()).append("\n");
    }
    return returnSB(sb);
  }

  /**
   * Output the analysis of this trial
   */
  private void outputAnalysis(EmpiricalMatrixGame algGame) {

    // create a "symmetrified" version of the algorithm game by averaging across permutations
    SymmetricEmpiricalMatrixGame symmetricAlgGame = GameUtils.symmetrifyGame(algGame);
    EmpiricalMatrixGame symmetricAlgGameAsAsymmetric = new EmpiricalMatrixGame(symmetricAlgGame);

    // write out the main game
    SimpleOutput.writeGame(RESULTS_PATH + experimentName + "/" + subExperimentName + "/" +
                           FULL_GAME_NAME + GAMUT_GAME_EXTENSION, algGame);

    // write out the symmetrified version
    SimpleOutput.writeGame(RESULTS_PATH + experimentName + "/" + subExperimentName + "/" +
                           FULL_GAME_NAME_SYMMETRIC + GAMUT_GAME_EXTENSION, symmetricAlgGameAsAsymmetric);

    outputVerboseAnalysis(RESULTS_PATH + experimentName + "/" + subExperimentName + "/",
                          VERBOSE_ANALYSIS_FILE_NAME + "_" + experimentName + EXTENSION,
                          symmetricAlgGameAsAsymmetric,
                          PROFILES_TO_PRINT,
                          true, numBenchmarks);

    // this will append to the end of exising files for each sub-experiment
    outputSymmetricStrategyAnalysis(RESULTS_PATH + experimentName + "/",
                                    AGGREGATE_ANALYSIS_DIR,
                                    symmetricAlgGameAsAsymmetric,
                                    experimentName + " " + subExperimentName + " ",
                                    numBenchmarks);

    // outputs BTD sampling information for all strategies
    outputBTDAnalysis();
  }

  /**
   * Wrapper for writing symmetric game analysis strings to files, one for each strategy
   * TODO: another wrapper for writing all subExperiments?
   */
  public static void outputSymmetricStrategyAnalysis(String path, String analysisDir, EmpiricalMatrixGame algGame,
                                                     String prefix, int numBenchmarks) {
    try {
      List<String> outStrings = getSymmetricStrategyAnalysis(algGame, prefix, numBenchmarks);

      int cnt = 1;
      for (String outString : outStrings) {
        // append to existing files; typically we will be combining data from several instances here
        FileWriter out = new FileWriter(path + analysisDir + "/" + cnt + EXTENSION, true);
        out.write(outString);
        out.close();
        cnt++;
      }
    } catch (IOException e) {
      throw new RuntimeException("Error writing symmetric strategy analysis to file: " + e.getMessage());
    }
  }

  public void outputBTDAnalysis() {
    for (int i = 1; i <= nAlgs; i++) {
      try {
        FileWriter out = new FileWriter(RESULTS_PATH + experimentName + "/" + subExperimentName + "/" +
                                        "BTDSamples_" + i + ".txt");
        out.write(BTDsamples[i].toString());
        out.write("\n");
        out.flush();
        out.close();

        out = new FileWriter(RESULTS_PATH + experimentName + "/" + AGGREGATE_ANALYSIS_DIR + "/" +
                             "BTDAverages_" + i + ".txt", true);
        out.write(subExperimentName + ", " + getAve(BTDsum[i], nSamples) + ", " +
                  getConfidenceInterval(BTDsum[i], BTDsumSquared[i], nSamples) + "\n");
        out.flush();
        out.close();

        out = new FileWriter(RESULTS_PATH + experimentName + "/" + AGGREGATE_ANALYSIS_DIR + "/" +
                             "BTDPairedAverages_" + i + ".txt", true);
        out.write(subExperimentName);
        for(int a = 1; a <= nAlgs; a++) {
          out.write(", " + getAve(BTDPairSum[i][a],nSamples) +
                    ", " + getConfidenceInterval(BTDPairSum[i][a], BTDPairSumSquared[i][a], nSamples));
        }
        out.write("\n");
        out.flush();
        out.close();

        out = new FileWriter(RESULTS_PATH + experimentName + "/" + AGGREGATE_ANALYSIS_DIR + "/" +
                                          "BTDPairedMaxes_" + i + ".txt", true);
        int a = getMaxPairedBTDIndex(i);
        out.write(subExperimentName + ", " + getAve(BTDPairSum[i][a],nSamples) +
                  ", " + getConfidenceInterval(BTDPairSum[i][a], BTDPairSumSquared[i][a], nSamples) + "\n");
        out.flush();
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      FileWriter out = new FileWriter(RESULTS_PATH + experimentName + "/" + AGGREGATE_ANALYSIS_DIR + "/" +
                                      "BTDMins.txt", true);
      if (algParams == null || nAlgs != algParams.length) {
        out.write(subExperimentName + ", " + getMinBTDIndex() + "\n");
      } else {
        out.write(subExperimentName + ", " + algParams[getMinBTDIndex()] + "\n");
      }
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

//    try {
//      FileWriter out = new FileWriter(RESULTS_PATH + experimentName + "/" + AGGREGATE_ANALYSIS_DIR + "/" +
//                                      "BTDParam_settings.txt", true);
//      if (algParams == null || nAlgs != algParams.length) {
//        out.write(getMinBTDIndex() + ", ");
//      } else {
//        out.write(algParams[getMinBTDIndex()] + ", ");
//      }
//      out.flush();
//      out.close();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }


  /**
   * Output stats in a form we can use for generating plots
   * A separate string is returned for each strategy, so these can be sent to separate files
   * Assumes a symmetric game (TODO: update this?)
   *
   * @param algGame       the meta-strategy game
   * @param prefix        typically "experimentName obsevations ", but leave room for other options
   * @param numBenchmarks the number of "benchmark" strategies; these must always be the last strategies
   *                      benchmarks strategies are not used in computing the context for the candidates,
   *                      but are still measured in the context of the candidates
   * @return string representing analysis
   */
  public static List<String> getSymmetricStrategyAnalysis(EmpiricalMatrixGame algGame, String prefix,
                                                          int numBenchmarks) {

    int nPlayers = algGame.getNumPlayers();
    int[] nActs = algGame.getNumActions();
    StringBuilder sb = getSB();
    List<String> analysis = new ArrayList<String>();

    // create a restricted version of the game without the benchmark strategies for analysis
    int[] nRestrictedActs = algGame.getNumActions().clone();
    List<List<Integer>> restrictedActionSets;
    EmpiricalMatrixGame restrictedGame = algGame;
    if (numBenchmarks > 0) {
      restrictedActionSets = new ArrayList<List<Integer>>(nPlayers);
      for (int pl = 0; pl < nPlayers; pl++) {
        nRestrictedActs[pl] -= numBenchmarks;
        List<Integer> tmpList = new ArrayList<Integer>(nRestrictedActs[pl]);
        for (int a = 1; a <= nRestrictedActs[pl]; a++) {
          tmpList.add(a);
        }
        restrictedActionSets.add(tmpList);
      }
      restrictedGame = new EmpiricalMatrixGame(algGame, restrictedActionSets, nRestrictedActs);
    }

    DominanceAnalysis dominance = new DominanceAnalysis(algGame, 0);
    StabilityAnalysis stability = new StabilityAnalysis(restrictedGame, 0);
    OutcomeDistribution od = new OutcomeDistribution(nActs);

    // create set representations for various stability restrictions
    Set<ProfileInfo> mostStableSet = stability.getMostStableProfiles(0);
    Set<int[]> mostStableSetOutcomes = new HashSet<int[]>(mostStableSet.size());
    for (ProfileInfo pi : mostStableSet) {
      mostStableSetOutcomes.add(pi.outcome);
    }

    Set<ProfileInfo> nashSet = stability.getMinEpsilonProfiles(0);
    Set<int[]> nashSetOutcomes = new HashSet<int[]>(nashSet.size());
    for (ProfileInfo pi : nashSet) {
      nashSetOutcomes.add(pi.outcome);
    }

    Set<ProfileInfo> top10Percent =
            stability.getSizeBoundedProfileSet(0, (int) Math.ceil(algGame.getNumProfiles() * 0.1d));
    Set<int[]> top10PercentOutcomes = new HashSet<int[]>(top10Percent.size());
    for (ProfileInfo pi : top10Percent) {
      top10PercentOutcomes.add(pi.outcome);
    }

    Set<ProfileInfo> top20Percent =
            stability.getSizeBoundedProfileSet(0, (int) Math.ceil(algGame.getNumProfiles() * 0.2d));
    Set<int[]> top20PercentOutcomes = new HashSet<int[]>(top20Percent.size());
    for (ProfileInfo pi : top20Percent) {
      top20PercentOutcomes.add(pi.outcome);
    }

    // compute statistics
    List<Integer> dominatedCounts = dominance.dominatedCounts(0, true);

    // create a distribution that is uniform over all outcomes containing only non-benchmark strategies
    od.setAll(0d);
    OutcomeIterator itr = restrictedGame.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      od.setProb(outcome, 1d);
    }
    od.normalize();
    ActionData averagePayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);
    od.setOutcomeSet(mostStableSetOutcomes);
    ActionData mostStableSetPayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);
    od.setOutcomeSet(nashSetOutcomes);
    ActionData nashSetPayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);
    od.setOutcomeSet(top10PercentOutcomes);
    ActionData top10PercentSetPayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);
    od.setOutcomeSet(top20PercentOutcomes);
    ActionData top20PercentSetPayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);

    ActionData minProfileDev = stability.getMinDeviationBenefitsByAction();
    ActionData aveProfileDev = stability.getAveDeviationBenefitsByAction();

    double[] pureProfilePayoffs = stability.getPureProfilePayoffs();
    double[] pureProfileBTD = stability.getPureProfileBTD();

    ActionDataInteger nashPayoffRanks = SolverUtils.computeRankings(nashSetPayoffs);
    ActionDataInteger avePayoffRanks = SolverUtils.computeRankings(averagePayoffs);
    ActionDataInteger minProfileRanks = SolverUtils.computeRankings(minProfileDev);

    for (int a = 1; a <= nActs[0]; a++) {
      sb.setLength(0);
      sb.append(prefix);
      sb.append(0).append(" ");
      sb.append(a).append(" ");
      sb.append(dominatedCounts.get(a)).append(" ");
      sb.append(nf.format(mostStableSetPayoffs.get(0, a))).append(" ");
      sb.append(nf.format(nashSetPayoffs.get(0, a))).append(" ");
      sb.append(nf.format(nashPayoffRanks.get(0, a))).append(" ");
      sb.append(nf.format(top10PercentSetPayoffs.get(0, a))).append(" ");
      sb.append(nf.format(top20PercentSetPayoffs.get(0, a))).append(" ");
      sb.append(nf.format(averagePayoffs.get(0, a))).append(" ");
      sb.append(nf.format(avePayoffRanks.get(0, a))).append(" ");
      if (a <= nRestrictedActs[0]) {
        sb.append(nf.format(minProfileDev.get(0, a))).append(" ");
        sb.append(nf.format(minProfileRanks.get(0, a))).append(" ");
        sb.append(nf.format(aveProfileDev.get(0, a))).append(" ");
        sb.append(nf.format(pureProfilePayoffs[a])).append(" ");
        sb.append(nf.format(pureProfileBTD[a])).append(" ");
      } else {
        sb.append(0).append(" ");
        sb.append(0).append(" ");
        sb.append(0).append(" ");
        int[] outcome = new int[nPlayers];
        Arrays.fill(outcome, a);
        sb.append(nf.format(algGame.getPayoffs(outcome)[0])).append(" ");
        sb.append(0).append(" ");
      }
      sb.append("\n");
      analysis.add(sb.toString());
    }

    returnSB(sb);
    return analysis;
  }

  /**
   * Wrapper for writing the verbose analysis string to a given file
   */
  public static void outputVerboseAnalysis(String path, String fileName, EmpiricalMatrixGame algGame,
                                           int profilesToPrint, boolean symmetric, int numBenchmarks) {

    // try to load the game from the default location if it is not given
    if (algGame == null) {
      MatrixGame mg = GamutParser.readGamutGame(path + FULL_GAME_NAME_SYMMETRIC + GAMUT_GAME_EXTENSION);
      if (mg == null) return;
      algGame = new EmpiricalMatrixGame(mg);
    }

    try {
      FileWriter out = new FileWriter(path + fileName);
      String outString = getVerboseAnalysis(algGame, profilesToPrint, symmetric, numBenchmarks);
      out.write(outString);
      out.close();

    } catch (IOException e) {
      throw new RuntimeException("Error writing verbose analysis to file: " + e.getMessage());
    }
  }

  /**
   * Returns a string of the analysis for this game
   * <p/>
   * Note: for now, benchmark strategies *must* be the final strategies so that the strategies labels map correctly
   *
   * @param profilesToPrint the number of stable profiles to print out, ordered by deviation benefit
   * @param symmetric       if the given game is symmetric, only print stats for a single player's actions
   * @param numBenchmarks   the number of "benchmark" strategies; these must always be the last strategies
   *                        benchmarks strategies are not used in computing the context for the candidates,
   *                        but are still measured in the context of the candidates
   */
  public static String getVerboseAnalysis(EmpiricalMatrixGame algGame, int profilesToPrint, boolean symmetric,
                                          int numBenchmarks) {

    int nPlayers = algGame.getNumPlayers();
    int[] nActs = algGame.getNumActions();
    StringBuilder sb = getSB();

    // create a restricted version of the game without the benchmark strategies for analysis
    int[] nRestrictedActs = algGame.getNumActions().clone();
    List<List<Integer>> restrictedActionSets;
    EmpiricalMatrixGame restrictedGame = algGame;
    if (numBenchmarks > 0) {
      restrictedActionSets = new ArrayList<List<Integer>>(nPlayers);
      for (int pl = 0; pl < nPlayers; pl++) {
        nRestrictedActs[pl] -= numBenchmarks;
        List<Integer> tmpList = new ArrayList<Integer>(nRestrictedActs[pl]);
        for (int a = 1; a <= nRestrictedActs[pl]; a++) {
          tmpList.add(a);
        }
        restrictedActionSets.add(tmpList);
      }
      restrictedGame = new EmpiricalMatrixGame(algGame, restrictedActionSets, nRestrictedActs);
    }

    DominanceAnalysis dominance = new DominanceAnalysis(algGame, 0);
    StabilityAnalysis stability = new StabilityAnalysis(restrictedGame, 0);
    OutcomeDistribution od = new OutcomeDistribution(nActs);

    Set<ProfileInfo> mostStableSet = stability.getMostStableProfiles(0);
    Set<int[]> mostStableSetOutcomes = new HashSet<int[]>(mostStableSet.size());
    for (ProfileInfo pi : mostStableSet) {
      mostStableSetOutcomes.add(pi.outcome);
    }

    Set<ProfileInfo> nashSet = stability.getMinEpsilonProfiles(0);
    Set<int[]> nashSetOutcomes = new HashSet<int[]>(nashSet.size());
    for (ProfileInfo pi : nashSet) {
      nashSetOutcomes.add(pi.outcome);
    }

    Set<ProfileInfo> top10Percent =
            stability.getSizeBoundedProfileSet(0, (int) Math.ceil(algGame.getNumProfiles() * 0.1d));
    Set<int[]> top10PercentOutcomes = new HashSet<int[]>(top10Percent.size());
    for (ProfileInfo pi : top10Percent) {
      top10PercentOutcomes.add(pi.outcome);
    }

    Set<ProfileInfo> top20Percent =
            stability.getSizeBoundedProfileSet(0, (int) Math.ceil(algGame.getNumProfiles() * 0.2d));
    Set<int[]> top20PercentOutcomes = new HashSet<int[]>(top20Percent.size());
    for (ProfileInfo pi : top20Percent) {
      top20PercentOutcomes.add(pi.outcome);
    }

    sb.append(dominance.outputBooleanDominance(true, symmetric));
    sb.append(dominance.outputPartialRankings(symmetric));
    sb.append(dominance.outputMaxDeviationBenefits(symmetric));
    sb.append(dominance.outputAverageDeviationBenefits(symmetric));

    sb.append("Minimum profile deviation benefit\n");
    sb.append("--------------------------------------------\n");
    ActionData minStability = stability.getMinDeviationBenefitsByAction();
    sb.append(minStability.toString(symmetric));
    sb.append("\n");

    sb.append("Minimum profile deviation benefit ranking\n");
    sb.append("--------------------------------------------\n");
    ActionDataInteger minStabilityRank = SolverUtils.computeRankings(minStability);
    sb.append(minStabilityRank.toString(symmetric));
    sb.append("\n");

    // create a distribution that is uniform over all outcomes containing only non-benchmark strategies
    od.setAll(0d);
    OutcomeIterator itr = restrictedGame.iterator();
    while (itr.hasNext()) {
      int[] outcome = itr.next();
      od.setProb(outcome, 1d);
    }

    od.normalize();
    sb.append("Average profile deviation benefit\n");
    sb.append("--------------------------------------------\n");
    sb.append(stability.getAveDeviationBenefitsByAction().toString(symmetric));
    sb.append("\n");

    od.setOutcomeSet(mostStableSetOutcomes);
    sb.append("Payoffs in context of most stable profile set (").append(mostStableSetOutcomes.size()).append(")\n");
    sb.append("--------------------------------------------------------------------------\n");
    sb.append(SolverUtils.computePureStrategyPayoffs(algGame, od).toString(symmetric));
    sb.append("\n");

    od.setOutcomeSet(nashSetOutcomes);
    sb.append("Payoffs in context of the NE profile set (").append(nashSetOutcomes.size()).append(")\n");
    sb.append("--------------------------------------------------------------------------\n");
    ActionData nashPayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);
    sb.append(nashPayoffs.toString(symmetric));
    sb.append("\n");

    sb.append("Payoff rank in context of the NE profile set (").append(nashSetOutcomes.size()).append(")\n");
    sb.append("--------------------------------------------------------------------------\n");
    ActionDataInteger nashPayoffRank = SolverUtils.computeRankings(nashPayoffs);
    sb.append(nashPayoffRank.toString(symmetric));
    sb.append("\n");

    od.setOutcomeSet(top10PercentOutcomes);
    sb.append("Payoffs in context of the top 10% of stable profiles (").append(top10Percent.size()).append(")\n");
    sb.append("--------------------------------------------------------------------------\n");
    sb.append(SolverUtils.computePureStrategyPayoffs(algGame, od).toString(symmetric));
    sb.append("\n");

    od.setOutcomeSet(top20PercentOutcomes);
    sb.append("Payoffs in context of the top 20% of stable profiles (").append(top20Percent.size()).append(")\n");
    sb.append("--------------------------------------------------------------------------\n");
    sb.append(SolverUtils.computePureStrategyPayoffs(algGame, od).toString(symmetric));
    sb.append("\n");

    od.setCentroid();
    sb.append("Payoffs in uniform context\n");
    sb.append("--------------------------------------------\n");
    ActionData uniformPayoffs = SolverUtils.computePureStrategyPayoffs(algGame, od);
    sb.append(uniformPayoffs.toString(symmetric));
    sb.append("\n");

    sb.append("Payoff ranks in uniform context\n");
    sb.append("--------------------------------------------\n");
    ActionDataInteger uniformPayoffRanks = SolverUtils.computeRankings(uniformPayoffs);
    sb.append(uniformPayoffRanks.toString(symmetric));
    sb.append("\n");

    double[] pureProfilePayoffs = stability.getPureProfilePayoffs();
    sb.append("Pure Profile Payoffs\n");
    sb.append("-------------------------\n");
    for (int a = 1; a < pureProfilePayoffs.length; a++) {
      sb.append("[").append(a).append("] ").append(nf.format(pureProfilePayoffs[a])).append(" ");
    }
    sb.append("\n\n");

    double[] pureProfileBTD = stability.getPureProfileBTD();
    sb.append("Pure Profile BTD\n");
    sb.append("-------------------------\n");
    for (int a = 1; a < pureProfileBTD.length; a++) {
      sb.append("[").append(a).append("] ").append(nf.format(pureProfileBTD[a])).append(" ");
    }
    sb.append("\n\n");

    sb.append("Sorted list of top ").append(profilesToPrint).append(" most stable profiles\n");
    sb.append("---------------------------------------------------------------------------\n");
    sb.append(stability.toString(stability.getSizeBoundedProfileSet(0, profilesToPrint)));

    return returnSB(sb);
  }

  /**
   * Write all of the strategies from the selectedStrategies object out to files
   */
  public void writeAllStrategies(String gameId, int sampleNum) {
    String path = RESULTS_PATH + experimentName + "/" + subExperimentName + "/" + STRATEGY_DIR + "/";
    String name = "/" + gameId + "_" + sampleNum + STRATEGIES_EXTENSION;

    try {
      for (int i = 1; i <= nAlgs; i++) {
        FileWriter out = new FileWriter(path + i + name);
        MixedStrategy[] strats = selectedStrategies.get(i - 1);
        for (int pl = 0; pl < strats.length; pl++) {
          MixedStrategy strat = strats[pl];
          out.write("Player " + pl + " : " + strat + "\n");
        }
        out.close();
      }
    } catch (IOException e) {
      throw new RuntimeException("Error writing strategies to file: " + e.getMessage());
    }
  }

  /**
   * For a given game instance, compute the strategies chosen for each player by each algorithm
   */
  public void computeAllStrategies(Game g, int sample) {
    // clear out the list of strategies
//    if (sample == 0) {
//      selectedStrategies.clear();
//    }

    for (int i = 0; i < nAlgs; i++) {
      GameSolver alg = algorithms.get(i);
      if (sample == 0 || alg.isStochastic()) {
        GameObserver go = observers.get(i);
        go.setGame(g);

        MixedStrategy[] strats = selectedStrategies.get(i);
        for (int pl = 0; pl < nPlayers; pl++) {
          go.reset();
          strats[pl] = alg.solveGame(go, pl);
        }
      }
    }
  }

  /**
   * Compute the payoff maxtrix for the current set of selected strategies and
   * add it into the meta-strategy game as an additional sample
   * <p/>
   * Note: we do not use SolverUtils.computeOutcomePayoffs here because we can
   * re-use the objects created by this function, reducing a bottleneck
   * <p/>
   * TODO: possible optimization: detect identical mixed strategies
   * TODO: possible optimization: only sample one outcome for each symmetric profile
   *
   * @param g    the game instance the the selected strategies refer to
   * @param base the meta-strategy game
   */
  public void computeAndAddSample(Game g, EmpiricalMatrixGame base) {
    itr.reset();
    int gNumPlayers = g.getNumPlayers();
    OutcomeIterator gItr = g.iterator();
    double[] tmpPayoffs = new double[gNumPlayers];

    sampleGame.clear();

    while (itr.hasNext()) {
      int[] outcome = itr.next();

      // set up the mixed strategies corresponding to this outcome
      strategyList.clear();
      for (int pl = 0; pl < nPlayers; pl++) {
        strategyList.add(selectedStrategies.get(outcome[pl] - 1)[pl]);
      }
      sampleDistribution.setMixedStrategies(strategyList);

      // loop through outcomes to compute the expected payoffs
      gItr.reset();
      Arrays.fill(tmpPayoffs, 0d);
      while (gItr.hasNext()) {
        int[] gOutcome = gItr.next();
        double prob = sampleDistribution.getProb(gOutcome);
        if (prob > 0) {
          // add in this component of the payoffs
          double[] gOutcomePayoffs = g.getPayoffs(gOutcome);
          for (int pl = 0; pl < gNumPlayers; pl++) {
            tmpPayoffs[pl] += prob * gOutcomePayoffs[pl];
          }
        }
      }

      // add the computed payoffs into the meta-strategy game
      base.addSample(outcome, tmpPayoffs);

      // add the computed payoffs to the game representing just this sample
      sampleGame.addSample(outcome, tmpPayoffs);
    }                                                                  
  }

  // track the pure profile BTD information for each sample
  public void updateBTD() {

    // create a "symmetrified" version of the algorithm game by averaging across permutations
    SymmetricEmpiricalMatrixGame symmetricAlgGame = GameUtils.symmetrifyGame(algGame);
    int[] profile = new int[symmetricAlgGame.getNumPlayers()];

    for (int a = 1; a <= nAlgs; a++) {
      Arrays.fill(profile, a);
      double[] originalPayoffs = symmetricAlgGame.getPayoffs(profile);

      double bestDevValue = Double.NEGATIVE_INFINITY;
      for (int dev = 1; dev <= nAlgs; dev++) {
        if (dev == a) continue;
        profile[0] = dev;
        double devPayoff = symmetricAlgGame.getPayoff(profile, 0);
        double diff = devPayoff - originalPayoffs[0];

        BTDPairSum[a][dev] += diff;
        BTDPairSumSquared[a][dev] += diff * diff;

        if (diff > bestDevValue) {
          bestDevValue = diff;
        }
      }

      BTDsum[a] += bestDevValue;
      BTDsumSquared[a] += bestDevValue * bestDevValue;
      BTDsamples[a].append(bestDevValue).append("\n");
    }
  }


  public double getAve(double total, int nSamples) {
    return total / nSamples;
  }

  public double getConfidenceInterval(double total, double totalSquared, int nSamples) {
    double tmp = totalSquared;
    tmp -= (1/(double)nSamples) * Math.pow(total,2);
    tmp *= (1/((double)nSamples-1));
    return 1.96d * Math.sqrt(tmp) / (Math.sqrt((double)nSamples));
  }

  public int getMinBTDIndex() {
    double min = getAve(BTDsum[1], nSamples);
    int minIndex = 1;

    for (int i = 2; i <= nAlgs; i++) {
      double tmp = getAve(BTDsum[i], nSamples);
      if (tmp < min) {
        min = tmp;
        minIndex = i;
      }
    }
    return minIndex;
  }

  public int getMaxPairedBTDIndex(int alg) {
    double max = Double.NEGATIVE_INFINITY;
    int maxIndex = -1;

    for (int i = 1; i <= nAlgs; i++) {
      if (i == alg) continue;
      double tmp = getAve(BTDPairSum[alg][i], nSamples);
      if (tmp > max) {
        max = tmp;
        maxIndex = i;
      }
    }
    return maxIndex;
  }
}
