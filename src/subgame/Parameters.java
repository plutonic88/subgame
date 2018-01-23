package subgame;

/**
 * Utility class for constants/parameters
 */
public class Parameters {

  // base path to storage location of generated game instances
  //public static final String GAME_FILES_PATH = "/home/ckiekint/IdeaProjects/EmpiricalGameAnalysis/game_files/";
  //public static final String GAME_FILES_PATH = "C:\\Users\\Oscar-XPS\\Documents\\game\\game_files\\";
  public static final String GAME_FILES_PATH = "/Users/anjonsunny/eclipse-workspace/subgame/result/";
    //public static final String GAME_FILES_PATH = "";

  // base path to the directory where results should be stored
  //public static final String RESULTS_PATH = "/home/ckiekint/IdeaProjects/EmpiricalGameAnalysis/results/";
  //public static final String RESULTS_PATH = "C:\\Users\\Oscar-XPS\\Documents\\game\\results\\";
  public static final String RESULTS_PATH = "/Users/anjonsunny/eclipse-workspace/subgame/result/";

  // path to the gamut jar file
  //public static final String GAMUT_PATH = "/home/ckiekint/projects/gamut/gamut.jar";
  //public static final String GAMUT_PATH = "C:\\Program Files\\Gamut\\gamut.jar";
  public static final String GAMUT_PATH = "/Users/anjonsunny/eclipse-workspace/subgame/lib/gamut.jar";

  //path to gambit
  //public static final String GAMBIT_LOGIT_PATH = "C:\\Program Files (x86)\\gambit\\gambit-logit";
  public static final String GAMBIT_LOGIT_PATH = "/Users/anjonsunny/research/gambit/gambit-logit";

  // file extension for gamut games
  public static final String GAMUT_GAME_EXTENSION = ".gamut";

  // file extension for strategies chosen by an algorithm
  public static final String STRATEGIES_EXTENSION = ".strats";

  // default file extension
  public static final String EXTENSION = ".txt";

  // directory names used for storing results
  public static final String AGGREGATE_ANALYSIS_DIR = "aggregate_analysis";
  public static final String SAMPLE_GAME_DIR = "samples";
  public static final String STRATEGY_DIR = "strategies";


  private Parameters() {
  }
}
