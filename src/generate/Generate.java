package generate;

import static subgame.Parameters.GAME_FILES_PATH;
import static subgame.Parameters.GAMUT_GAME_EXTENSION;

import java.io.File;
import java.io.IOException;

import games.EmpiricalMatrixGame;
import games.MatrixGame;
import parsers.GamutParser;
import solvers.StabilityAnalysis;
import subgame.EGAUtils;

/**
 * User: ckiekint
 * Date: Aug 18, 2008
 * Time: 4:33:38 PM
 */
public class Generate {

  public static void screenForPSNE(String gameClassName) {
    int PSNEcnt = 1;
    int NOPSNEcnt = 1;

    File gameFileDir = new File(GAME_FILES_PATH + gameClassName);
    if (!gameFileDir.exists()) {
      throw new RuntimeException("Attempting to screen non-existent class of games: " + gameFileDir);
    }
    File destDir = new File(GAME_FILES_PATH + gameClassName + "_PSNE");
    if (!destDir.exists()) {
      destDir.mkdirs();
    }
    destDir = new File(GAME_FILES_PATH + gameClassName + "_NOPSNE");
    if (!destDir.exists()) {
      destDir.mkdirs();
    }

    for (String str : gameFileDir.list()) {
      // not a gamut game file
      if (!str.endsWith(GAMUT_GAME_EXTENSION)) {
        continue;
      }

      String gameFileName = GAME_FILES_PATH + gameClassName + "/" + str;
      System.out.println("Game file: " + gameFileName);

      // read in the game
      MatrixGame game = GamutParser.readGamutGame(gameFileName);
      EmpiricalMatrixGame eGame = new EmpiricalMatrixGame(game);

      // check if it has at least one PSNE
      StabilityAnalysis stability = new StabilityAnalysis(eGame, 1);
      try {
        if (stability.getMostStableProfile(1).maxBenefit <= 0) {
          EGAUtils.copy(new File(gameFileName),
                        new File(GAME_FILES_PATH + gameClassName + "_PSNE/" + PSNEcnt + GAMUT_GAME_EXTENSION));
          PSNEcnt++;
        } else {
//          EGAUtils.copy(new File(gameFileName),
//                        new File(GAME_FILES_PATH + gameClassName + "_NOPSNE/" + NOPSNEcnt + GAMUT_GAME_EXTENSION));
//          NOPSNEcnt++;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * Edit this to generate necessary games
   */
  public static void generateGames() {
//    int[] nActs = new int[] {25, 25};
//    for (int i = 0; i < 500; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_-1_25_25/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, -1d);
//    }
//
//    nActs = new int[] {100, 100};
//    for (int i = 0; i < 100; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_-1_100_100/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, -1d);
//    }
//    int[] nActs;
//
//    nActs = new int[] {10, 10};
//    for (int i = 1; i < 1000; i++) {
//      GamutGenerator.generateRandomGame(GAME_FILES_PATH + "random_10_10/" + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
//    }

//    for (int i = 0; i < 1000; i++) {
//      GamutGenerator.generateCongestionGame(GAME_FILES_PATH + "CongestionGame_31_31/" + i + GAMUT_GAME_EXTENSION,
//                                            2, 5, 0, 1);
//    }
//
//    for (int i = 0; i < 1000; i++) {
//      GamutGenerator.generateSupermodularGame(GAME_FILES_PATH + "Supermodular_10_10/" + i + GAMUT_GAME_EXTENSION,
//                                              2, 10, 0, 1);
//    }
//
//    for (int i = 0; i < 500; i++) {
//      GamutGenerator.generateSupermodularGame(GAME_FILES_PATH + "Supermodular_25_25/" + i + GAMUT_GAME_EXTENSION,
//                                              2, 25, 0, 1);
//    }
//
//    for (int i = 0; i < 100; i++) {
//      GamutGenerator.generateSupermodularGame(GAME_FILES_PATH + "Supermodular_100_100/" + i + GAMUT_GAME_EXTENSION,
//                                              2, 100, 0, 1);
//    }

//    for (int i = 0; i < 1000; i++) {
//      GamutGenerator.generateCompactGame(GAME_FILES_PATH + "Compact_10_10/" + i + GAMUT_GAME_EXTENSION,
//                                         2, 10, 0, 1);
//    }
//
//    for (int i = 0; i < 500; i++) {
//      GamutGenerator.generateCompactGame(GAME_FILES_PATH + "Compact_25_25/" + i + GAMUT_GAME_EXTENSION,
//                                         2, 25, 0, 1);
//    }
//
//    for (int i = 0; i < 100; i++) {
//      GamutGenerator.generateCompactGame(GAME_FILES_PATH + "Compact_100_100/" + i + GAMUT_GAME_EXTENSION,
//                                         2, 100, 0, 1);
//    }

//    for (int i = 0; i < 1000; i++) {
//      GamutGenerator.generateRandomLEGGame(GAME_FILES_PATH + "RandomLEG_10_10/" + i + GAMUT_GAME_EXTENSION,
//                                           2, 10, 0, 1);
//    }
//
//    for (int i = 0; i < 500; i++) {
//      GamutGenerator.generateRandomLEGGame(GAME_FILES_PATH + "RandomLEG_25_25/" + i + GAMUT_GAME_EXTENSION,
//                                           2, 25, 0, 1);
//    }
//
//    for (int i = 0; i < 100; i++) {
//      GamutGenerator.generateRandomLEGGame(GAME_FILES_PATH + "RandomLEG_100_100/" + i + GAMUT_GAME_EXTENSION,
//                                           2, 100, 0, 1);
//    }

//    nActs = new int[] {4, 4};
//    for (int i = 0; i < 2500; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_-1_4_4/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, -1d);
//    }
//    screenForPSNE("covariant_-1_4_4");
//
//    nActs = new int[] {4, 4};
//    for (int i = 0; i < 2500; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_-0.5_4_4/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, -0.5d);
//    }
//    screenForPSNE("covariant_-0.5_4_4");
//
//    nActs = new int[] {4, 4};
//    for (int i = 0; i < 2500; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_0_4_4/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, 0d);
//    }
//    screenForPSNE("covariant_0_4_4");
//
//    nActs = new int[] {4, 4};
//    for (int i = 0; i < 2500; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_0.5_4_4/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, 0.5d);
//    }
//    screenForPSNE("covariant_0.5_4_4");
//
//    nActs = new int[] {4, 4};
//    for (int i = 0; i < 2500; i++) {
//      GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_1.0_4_4/" + i + GAMUT_GAME_EXTENSION,
//                                           2, nActs, 0, 1, 1.0d);
//    }
//    screenForPSNE("covariant_1.0_4_4");

    int [] nActs = {20, 20};
      //int nActs = 4;
    //oscar added the if statement with a smaller game generation
    if(System.getProperty("os.name").toLowerCase().contains("win")){
        for (int i = 2500; i < 3000; i++) {
          //System.out.println(GAME_FILES_PATH + "random_4_4\\" + i + GAMUT_GAME_EXTENSION);
          GamutGenerator.generateRandomGame(GAME_FILES_PATH + "random_4_4\\" + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
          //GamutGenerator.generateRandomGame(i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
        }
        for(int i = 0;i<500;i++){
            GamutGenerator.generateZeroSumGame(GAME_FILES_PATH + "random_4_4\\" + i + GAMUT_GAME_EXTENSION, nActs);
            //GamutGenerator.generateZeroSumGame(i + GAMUT_GAME_EXTENSION, nActs);
        }


        //screenForPSNE("random_16_16");

        /*for (int i = 2500; i < 2600; i++) {
          GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_1.0_4_4\\" + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1, 1.0d);
        }

        for (int i = 2500; i < 2600; i++) {
          GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_-1_4_4\\" + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1, -1.0d);
        }*/
    }
    else{
         for (int i = 2500; i < 3000; i++) {
          //System.out.println(GAME_FILES_PATH + "random_4_4\\" + i + GAMUT_GAME_EXTENSION);
          //GamutGenerator.generateRandomGame(GAME_FILES_PATH + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
          GamutGenerator.generateRandomGame(i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
        }
        for(int i = 0;i<500;i++){
            //GamutGenerator.generateZeroSumGame(GAME_FILES_PATH + i + GAMUT_GAME_EXTENSION, nActs);
            GamutGenerator.generateZeroSumGame(i + GAMUT_GAME_EXTENSION, nActs);
        }
        /*for (int i = 2500; i < 10000; i++) {
          GamutGenerator.generateRandomGame(GAME_FILES_PATH + "random_4_4/" + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
        }
        //screenForPSNE("random_16_16");

        for (int i = 2500; i < 10000; i++) {
          GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_1.0_4_4/" + i + GAMUT_GAME_EXTENSION,
                                               2, nActs, 0, 1, 1.0d);
        }

        for (int i = 2500; i < 10000; i++) {
          GamutGenerator.generateCovariantGame(GAME_FILES_PATH + "covariant_-1_4_4/" + i + GAMUT_GAME_EXTENSION,
                                               2, nActs, 0, 1, -1.0d);
        }*/
    }


//
//    nActs = new int[] {8, 8};
//    for (int i = 1000; i < 2500; i++) {
//      GamutGenerator.generateRandomGame(GAME_FILES_PATH + "random_8_8/" + i + GAMUT_GAME_EXTENSION, 2, nActs, 0, 1);
//    }
//    screenForPSNE("random_8_8");
//
//    nActs = new int[] {3, 3, 3};
//    for (int i = 0; i < 2500; i++) {
//      GamutGenerator.generateRandomGame(GAME_FILES_PATH + "random_3_3_3/" + i + GAMUT_GAME_EXTENSION, 3, nActs, 0, 1);
//    }
//    screenForPSNE("random_3_3_3");

//    // default to "standard" exponential distribution
//    ExponentialSampler exponentialSampler = new ExponentialSampler();
//
//    nActs = new int[] {4, 4};
//    for (int i = 0; i < 2500; i++) {
//      RandomGenerator.generateRandomGame("exponential_4_4", i + GAMUT_GAME_EXTENSION,
//                                         2, nActs, exponentialSampler);
//    }
//    screenForPSNE("exponential_4_4");
//
//    nActs = new int[] {8, 8};
//    for (int i = 0; i < 2500; i++) {
//      RandomGenerator.generateRandomGame("exponential_8_8", i + GAMUT_GAME_EXTENSION,
//                                         2, nActs, exponentialSampler);
//    }
//    screenForPSNE("exponential_8_8");
//
//    nActs = new int[] {3, 3, 3};
//    for (int i = 0; i < 2500; i++) {
//      RandomGenerator.generateRandomGame("exponential_3_3_3", i + GAMUT_GAME_EXTENSION,
//                                         3, nActs, exponentialSampler);
//    }
//    screenForPSNE("exponential_3_3_3");

//    FactorGameParameters params;
//    FactorGame fg;
//    MatrixGame mg;
//
//    params = new FactorGameParameters(2,
//                                      0, 0, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 16, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 1000; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_0_0_1_16_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_0_0_1_16_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
//
//    params = new FactorGameParameters(2,
//                                      1, 16, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      0, 0, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 1000; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_16_0_0_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_16_0_0_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }

//    FactorGameParameters params;
//    FactorGame fg;
//    MatrixGame mg;
//
//    params = new FactorGameParameters(2,
//                                      1, 2, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 5, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 1000; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_2_1_5_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_2_1_5_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
//
//    params = new FactorGameParameters(2,
//                                      1, 5, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 2, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 1000; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_5_1_2_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_5_1_2_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
//
//    params = new FactorGameParameters(2,
//                                      1, 5, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 5, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 500; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_5_1_5_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_5_1_5_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
//
//    params = new FactorGameParameters(2,
//                                      1, 10, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 10, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 100; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_10_1_10_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_10_1_10_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
//
//    params = new FactorGameParameters(2,
//                                      1, 4, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 25, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 100; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_4_1_25_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_4_1_25_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
//
//    params = new FactorGameParameters(2,
//                                      1, 25, FactorGameParameters.GameType.RANDOM_DEFAULT,
//                                      1, 4, FactorGameParameters.GameType.RANDOM_DEFAULT);
//    for (int i = 0; i < 100; i++) {
//      fg = FactorGameGenerator.generateFactorGame(params);
//      fg.outputFactors(GAME_FILES_PATH + "fg_2_1_25_1_4_random/" + i + "_factors/", true);
//      mg = fg.getUnfactoredGameRepresentation(true);
//      SimpleOutput.writeGame(GAME_FILES_PATH + "fg_2_1_25_1_4_random/" + i + GAMUT_GAME_EXTENSION, mg);
//    }
  }

}
