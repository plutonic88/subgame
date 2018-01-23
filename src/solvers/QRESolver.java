package solvers;



import static subgame.EGAUtils.getSB;
import static subgame.EGAUtils.nf;
import static subgame.EGAUtils.returnSB;
import static subgame.Parameters.GAMBIT_LOGIT_PATH;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import agent.Agent;
import games.EmpiricalMatrixGame;
import games.GameUtils;
import games.MixedStrategy;
import games.OutcomeDistribution;
import observers.GameObserver;
import observers.NoiselessObserver;
import output.GambitOutput;
import subgame.Parameters;
import subgame.QRE;



/**
 * Created by IntelliJ IDEA.
 * User: ckiekint
 * Date: Jun 9, 2007
 * Time: 12:43:28 AM
 */
public class QRESolver implements GameSolver, GameOutcomePredictor {

  private ExplorationUtil explorationUtil = new ExplorationUtil();
  private ArrayList<QRE> qres = new ArrayList<QRE>();
  private double[] maxEntro = new double[2];
  private String gameName;

  // RAW:  play the QRE as given
  // BR:  play a best response to the the QRE distribution
  public enum DecisionMode {
    RAW, BR
  }

  //private static final String GAMBIT_QRE_BINARY_PATH = "/usr/local/bin/gambit-logit";
  //oscar changed this
    private static final String GAMBIT_QRE_BINARY_PATH = GAMBIT_LOGIT_PATH;
  private static final long GAMBIT_TIMEOUT = 2000;
  private static final double INITIAL_STEP_SIZE = 0.05;
  private static final double MAX_ACCELERATION = 1.2;


  // this is the parameter that controls how much noise the QRE calculation assumes
  // in the best response functions for all players
  private double lambda;

  // these are parameters that control how gambit traces the branch of the QRE correspondence
  // we tweak these if we run into problems computing the solution
  private double initialStepSize = INITIAL_STEP_SIZE;
  private double maxAcceleration = MAX_ACCELERATION;

  // selects the method for sampling the game matrix
  private ExplorationUtil.SamplingMode samplingMode = ExplorationUtil.SamplingMode.ALL_EVEN_PLUS_RANDOM;
  private int samplesPerProfile = 1;

  private DecisionMode decisionMode = DecisionMode.BR;

  private String name;

  // whether or not this solver is stochastic or not (use for fully-observable benchmark case)
  private boolean isStochastic = true;

  private int numPlayers = 0;
  private int[] numActs = null;
  private ArrayList<MixedStrategy> strategies = new ArrayList<MixedStrategy>();
  private OutcomeDistribution outcomeDistribution = null;
  private MixedStrategy strategy = null;
  private EmpiricalMatrixGame eGame = null;
  private ArrayList<String> cmd = new ArrayList<String>(10);

  //private ExecuteGambit executeGambit = new ExecuteGambit();

  public QRESolver() {
    this.lambda = 10000;
    this.name = "QRE " + decisionMode + " " + nf.format(lambda);
  }

  public QRESolver(double lambda) {
    this.lambda = lambda;
    this.name = "QRE " + decisionMode + " " + nf.format(lambda);
  }

  // returns the description
  public String getDescription() {
    StringBuilder sb = getSB();
    sb.append("Quantal Response Equilibrium Solver\n");
    sb.append("Plays according to the QRE calculated for the observed game and the given lambda parameter\n");
    sb.append("Sampling mode: ").append(samplingMode).append("\n");
    sb.append("Decision mode: ").append(decisionMode).append("\n");
    sb.append("Sample per profile: ").append(samplesPerProfile).append("\n");
    sb.append("Lambda: ").append(lambda).append("\n");
    return returnSB(sb);
  }

  public double getLambda() {
    return lambda;
  }

  public void setLambda(double lambda) {
    this.lambda = lambda;
    this.name = "QRE " + decisionMode + " " + nf.format(lambda);
  }

  public ExplorationUtil.SamplingMode getSamplingMode() {
    return samplingMode;
  }

  public void setSamplingMode(ExplorationUtil.SamplingMode samplingMode) {
    this.samplingMode = samplingMode;
  }

  public int getSamplesPerProfile() {
    return samplesPerProfile;
  }

  public void setSamplesPerProfile(int samplesPerProfile) {
    this.samplesPerProfile = samplesPerProfile;
  }

  public DecisionMode getDecisionMode() {
    return decisionMode;
  }

  public void setDecisionMode(DecisionMode decisionMode) {
    this.decisionMode = decisionMode;
    this.name = "QRE " + decisionMode + " " + nf.format(lambda);
  }

  public void setStochastic(boolean isStochastic) {
    this.isStochastic = isStochastic;
  }

  public boolean isStochastic() {
    return isStochastic;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }



  public OutcomeDistribution predictOutcome(EmpiricalMatrixGame emg) {
    initialize(emg, 0);
    runGambit();
    return new OutcomeDistribution(strategies);
  }

  public OutcomeDistribution predictOutcome(GameObserver gameObs) {
    initialize(gameObs, 0);
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, 0, samplesPerProfile);
    runGambit();
    return new OutcomeDistribution(strategies);
  }

  /**
   * @param emg    Observed empirical game
   * @param player The player to select an action for.
   * @return the mixed strategy to play
   */
  public MixedStrategy solveGame(EmpiricalMatrixGame emg, int player) {
    initialize(emg, player);
    computeStrategy(player);
    return strategy;
  }

  /**
   * @param gameObs The game observation module.
   * @param player  The player to select an action for.
   * @return the mixed strategy to play
   */
  public MixedStrategy solveGame(GameObserver gameObs, int player) {
    initialize(gameObs, player);
    explorationUtil.exploreGame(samplingMode, eGame, gameObs, player, samplesPerProfile);
    computeStrategy(player);
    return strategy;
  }

  private void computeStrategy(int player) {
      //System.out.println("Starting Game "+gameName+" player"+player);
      runGambit();
      //System.out.println("Completed Game "+gameName+" player"+player);
    switch (decisionMode) {
      case RAW:
        strategy.setProbs(strategies.get(player).getProbs());
        break;
      case BR:
        outcomeDistribution.setMixedStrategies(strategies);
        double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, outcomeDistribution, false);
        strategy.setBestResponse(stratPayoffs);
        break;
      default:
        new RuntimeException("Error in QRE solver: Unknown decision mode: " + decisionMode);
    }

    if (!strategy.isValid()) {
      System.out.println("Detected invalid strategy generated by QRE solver: " + strategy);
    }
    //System.out.println("Final QRE strategy " + decisionMode + " Player: "+ player+ " (" + lambda + "):" + strategy);
  }


  public MixedStrategy getStrategyEntro(double percent, int player, Agent.DecisionMode dm){
      if(qres.size()==0){
          return getInvalid(player);
      }
      int i = 0;
      //invert percent
      maxEntro[player] = GameUtils.computeEntropy(qres.get(0).getStrategy(player));
      //System.out.println("maximum entro " + maxEntro[player]);
      double target = percent*maxEntro[player];
      for(int k = 0; k < qres.size();k++){
          //MixedStrategy s = qres.get(k).getStrategy(player);
          //System.out.println("Strategy "+ s.toString()+" entro "+GameUtils.computeEntropy(s));
          //if(distance(qres.get(k).getEntropy(player),percent*maxEntro[player]) < distance(qres.get(i).getEntropy(player),percent*maxEntro[player]))
          if(distance(GameUtils.computeEntropy(qres.get(k).getStrategy(player)),target)
                  < distance(GameUtils.computeEntropy(qres.get(i).getStrategy(player)),target))
              i = k;
      }
      strategy = qres.get(i).getStrategy(player);
      if(dm == Agent.DecisionMode.RAW){
              //strategy.setProbs(strategies.get(player).getProbs());
                //return qres.get(i).getStrategy(player);
                return clone(strategy);
      }
      else{
              //outcomeDistribution.setMixedStrategies(strategies);
              outcomeDistribution.setMixedStrategies(Arrays.asList(qres.get(i).getStrategies()));
              double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, outcomeDistribution, false);
          //System.out.println(Arrays.toString(stratPayoffs));
              strategy.setBestResponse(stratPayoffs);
          //System.out.println(strategy.toString());
              return clone(strategy);
      }
  }
  public MixedStrategy clone(MixedStrategy strat){
      return new MixedStrategy(strat.getProbs());
  }
  public MixedStrategy getStrategy(double lam, int player, Agent.DecisionMode dm){
      if(qres.size()==0)
          //return new MixedStrategy(numActs[player]);
          return getInvalid(player);
      /*boolean found = false;
      int i = -1;
      do{
          i++;
          if(qres.get(i).getLambda() >= lam){
              found = true;
          }
      }while(!found && i != qres.size());
      if(i > 0 && distance(qres.get(i-1).getLambda(),lam) < distance(qres.get(i).getLambda(),lam))
          i--;*/
      int i = 0;
      for(int k = 0; k < qres.size();k++){
          if(distance(qres.get(k).getLambda(),lam) < distance(qres.get(i).getLambda(),lam))
              i = k;
      }
       strategy = qres.get(i).getStrategy(player);
      MixedStrategy ms = qres.get(i).getStrategy(player);
      if(dm == Agent.DecisionMode.RAW){
              //strategy.setProbs(strategies.get(player).getProbs());
                //return qres.get(i).getStrategy(player);
          return clone(ms);
      }
      else{
              //outcomeDistribution.setMixedStrategies(strategies);

            /*outcomeDistribution.setMixedStrategies(Arrays.asList(qres.get(i).getStrategies()));
              double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, outcomeDistribution, false);
              strategy.setBestResponse(stratPayoffs);
              return strategy;
              */
          outcomeDistribution.setMixedStrategies(Arrays.asList(qres.get(i).getStrategies()));
          double[] stratPayoffs = SolverUtils.computePureStrategyPayoffs(eGame, player, outcomeDistribution, false);
          ms.setBestResponse(stratPayoffs);
          return clone(ms);
      }


  }

    private double distance(double a, double b){
        return Math.abs(a - b);
    }



  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   *
   * @param gameObs obervation of the game
   * @param player  the player
   */
  private void initialize(GameObserver gameObs, int player) {
    // check to see if we are set up for a game of this dimension
    if (gameObs.getNumPlayers() == numPlayers &&
        Arrays.equals(gameObs.getNumActions(), numActs)) {
      reset(player);
      return;
    }

    numPlayers = gameObs.getNumPlayers();
    numActs = gameObs.getNumActions().clone();
    for (int pl = 0; pl < numPlayers; pl++) {
      strategies.add(new MixedStrategy(numActs[pl], 0d));
    }
    strategy = new MixedStrategy(numActs[player], 0d);
    eGame = new EmpiricalMatrixGame(numPlayers, numActs);
    outcomeDistribution = new OutcomeDistribution(numActs);
  }

  /**
   * Initialize/create data structures for analyzing this game, if necessary
   * If the game is the same size as the last one, just reset the structures and re-use them
   *
   * @param emg    observed empirical game
   * @param player the player
   */
  private void initialize(EmpiricalMatrixGame emg, int player) {
    numPlayers = emg.getNumPlayers();
    numActs = emg.getNumActions().clone();
    strategies.clear();
    for (int pl = 0; pl < numPlayers; pl++) {
      strategies.add(new MixedStrategy(numActs[pl], 0d));
    }
    strategy = new MixedStrategy(numActs[player], 0d);
    eGame = new EmpiricalMatrixGame(emg);
    outcomeDistribution = new OutcomeDistribution(numActs);
  }

  /**
   * Reset all private data structures to analyze a new game
   *
   * @param player the player
   */
  private void reset(int player) {
    eGame.clear();
    strategy = new MixedStrategy(numActs[player], 0d);
  }

  /**
   * This can be used to compute a QRE for a given game purely for analsyis, without the MS stuff
   * DO NOT use this when also using this as a meta-strategy
   *
   * @param game game to compute a QRE for
   * @return a string representing the QRE
   */
  public String getQRE(EmpiricalMatrixGame game) {
    GameObserver go = new NoiselessObserver(game);
    go.setDensityBound(1d);
    solveGame(go, 0);
    StringBuilder sb = getSB();
    for (int pl = 0; pl < go.getNumPlayers(); pl++) {
      sb.append("Player ").append(pl).append(": ");
      sb.append(strategies.get(pl)).append("\n");
    }
    return returnSB(sb);
  }

  private void runGambit() {
    boolean foundSolution = false;
    int iterationCnt = 0;

    initialStepSize = INITIAL_STEP_SIZE;
    maxAcceleration = MAX_ACCELERATION;

    while (!foundSolution) {
        qres = new ArrayList<QRE>();
      // TODO: Add noise here instead?
        //System.out.println("game number"+gameName+"itration number "+iterationCnt);
      if (iterationCnt > 3) {

        System.err.println("MORE THAN 3 iterations. Defaulting to uniform...");
        for (MixedStrategy tmpStrat : strategies) {
          tmpStrat.setUniform();
        }
        strategy.setUniform();
        break;
      }

      // run gambit in a separate thread so we can kill it if it hangs
      //System.out.println("Run gambit thread " + lambda);
      ExecuteGambit executeGambit = new ExecuteGambit();
      Thread gambitThread = new Thread(executeGambit);
      //gambitThread.setPriority(Thread.MIN_PRIORITY);
      gambitThread.start();
      try {
        if (iterationCnt >= 1) {
          gambitThread.join(2 * GAMBIT_TIMEOUT);
        } else {
          gambitThread.join(GAMBIT_TIMEOUT);
        }
        gambitThread.interrupt();
        gambitThread.join();
      } catch (InterruptedException ie) {
        System.err.println("Unexpected interrupt exception while running Gambit!");
      }
        //System.out.println("gambit thread ended "+gameName);

      //foundSolution = parseOutput(executeGambit.getInputStream());
      foundSolution = executeGambit.getSolutionFound();

      //debug
      //System.out.println("Found Solution after uniform: " + foundSolution);
      if (!foundSolution) {
        // if we haven't found the solution yet, tweak some parameters to slow down the trace
        initialStepSize = 0.02;
        maxAcceleration = 1 + ((maxAcceleration - 1) / 4);
        iterationCnt++;
        if (iterationCnt > 1) {
          initialStepSize = 0.01;
        }
      }
      //debug
      //else{//solution found
          /*double[] p1Prob =strategies.get(0).getProbs();//player 1
          double[] p2Prob =strategies.get(0).getProbs();//player 1
          System.out.print("Player 1 [ ");
          for(int i = 0; i < p1Prob.length;i++)
              System.out.print(p1Prob[i]+" ");
          System.out.println("]");
          System.out.print("Player 2[ ");
          for(int i = 0; i < p2Prob.length;i++)
              System.out.print(p2Prob[i]+" ");
          System.out.println("]");*/
      //}
    }
      //solution has been found
      /*double[] p1Prob =strategies.get(0).getProbs();//player 1
          double[] p2Prob =strategies.get(0).getProbs();//player 1
          System.out.print("Player 1 [ ");
          for(int i = 0; i < p1Prob.length;i++)
              System.out.print(p1Prob[i]+" ");
          System.out.println("]");
          System.out.print("Player 2[ ");
          for(int i = 0; i < p2Prob.length;i++)
              System.out.print(p2Prob[i]+" ");
          System.out.println("]");*/
  }

    
  // TODO: This can occassionally block when processing a hung gambit process... fix this... (how?)
  private boolean parseOutput(BufferedReader is) {
    if (is == null) return false;
    Scanner s = new Scanner(is);
    //char ls = System.getProperty("line.separator");
    if(System.getProperty("os.name").toLowerCase().contains("win"))
        //s.useDelimiter("[,\\"+ls+"]");
        s.useDelimiter("[,|\\n]");
    else
        s.useDelimiter("[,\\s]");

    // skip past any lines that are not the equilibrium we are looking for
    boolean found = false;
    double[] temp;

    while(s.hasNext()){
      String str = "";
      String next = "";
      double d = 0;
      next = s.next();
      str = str + " " +gameName+" "+ next;
      //System.out.print(next);
      try{
          d = Double.parseDouble(next);
      }
      catch(Exception e){
          while(s.hasNext())
              str = str + " ";
          System.out.println("Invalid line in QRE output: "+str);
          //break;
          return false;
      }
      /*if (!s.hasNextDouble()) {
      //if (!scanLine.hasNextDouble()) {
        //line = s.nextLine();
        //System.out.println("Invalid line in QRE output: " + s.nextLine());
          System.out.println("Invalid line in QRE output: "+s.nextLine());
          break;
      }
      else {*/
        //double lam = s.nextDouble();
        double lam = d;
        //double tmp = scanLine.nextDouble();
        if (lam > lambda - 0.01d && lam < lambda + 0.01d) {
          found = true;
          //break;
        }
        double tmp;
        MixedStrategy[] qStrat = new MixedStrategy[numPlayers];
        for (int pl = 0; pl < numPlayers; pl++) {
            //MixedStrategy tmpStrat = strategies.get(pl);
            MixedStrategy tmpStrat = new MixedStrategy(numActs[pl]);
            for (int a = 1; a <= numActs[pl]; a++) {
                /*if (!s.hasNextDouble()) {
                    throw new RuntimeException("Did not find expected player/act when parsing QRE output: " + pl + " " + a);
                }*/

                if(!s.hasNext()){
                    System.out.println("Invalid line in QRE output: "+str);
                    tmpStrat.setProb(a, 0d);
                }
                else{
                    next = s.next().trim();
                    //System.out.print(" "+next);
                    str = str + " " + next;
                    try{
                        d = Double.parseDouble(next);
                    }
                    catch(Exception e){
                        System.out.println("Invalid line in QRE output: "+str);
                        d = -100.0;
                    }
                    //tmp = s.nextDouble();
                    if (d > 0.0000001d) {
                        tmpStrat.setProb(a, d);
                    } else {
                        tmpStrat.setProb(a, 0d);
                    }
                }
            //System.out.println("lambda: "+ lam + " Temp strategy: " + tmpStrat);
        }
        qStrat[pl] = tmpStrat;
        //System.out.println(str);
      //QRE test =    new QRE(lam,strategies.toArray(new MixedStrategy[0]));
      //qres.add(new QRE(lam,strategies.toArray(new MixedStrategy[0])));

    }
    qres.add(new QRE(lam,qStrat));
    temp = qres.get(qres.size()-1).getEntropies();
    for(int i = 0; i < temp.length; i++)
        if(maxEntro[i] < temp[i])
            maxEntro[i] = temp[i];
        //System.out.println();
    }
    if(qres.size() > 0){
        MixedStrategy ms;
        MixedStrategy[] q = qres.get(qres.size()-1).getStrategies();
        for (int pl = 0; pl < numPlayers; pl++) {
            ms = strategies.get(pl);
            for (int a = 1; a <= numActs[pl]; a++) {
                ms.setProb(a,q[pl].getProb(a));
            }
        }

    }
    //return true;
    /*for(int p = 0; p < numPlayers;p++){
        maxEntro[p]=-1;
        System.out.println("max entro "+ maxEntro[p]);
    }
    for (QRE qre : qres) {
          for(int p = 0; p < numPlayers; p++){
            MixedStrategy ms = qre.getStrategy(p);
              double ent = GameUtils.computeEntropy(ms);
              if(ent>maxEntro[p])
                  maxEntro[p] = ent;
          }

    }
    for(int p = 0; p < numPlayers;p++){
        System.out.println("max entro "+ maxEntro[p]);
    }*/
    return found;
  }

  class ExecuteGambit implements Runnable {

    boolean solutionFound = false;

    public boolean getSolutionFound() {
      return solutionFound;
    }

    Process proc = null;

//    InputStream is = null;
//
//    public InputStream getInputStream() {
//      return is;
//    }

    public void run() {
      //is = null;

      solutionFound = false;

      // set up the external gambit command call
      cmd.clear();
      cmd.add(GAMBIT_QRE_BINARY_PATH);
      cmd.add("-q");
      cmd.add("-d");
      cmd.add("4");
      cmd.add("-s");
      cmd.add(String.valueOf(initialStepSize));
      cmd.add("-a");
      cmd.add(String.valueOf(maxAcceleration));
    // cmd.add("-S");
      cmd.add("-l");
      cmd.add(String.valueOf(lambda));
      try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            proc = pb.start();
            OutputStream os = proc.getOutputStream();

            // here we feed the gambit game representation to the solver on stdin
            String gambitGame = GambitOutput.gameToGambitString(eGame);
            os.write(gambitGame.getBytes());
            os.close();
            System.out.println("\nQRE Parseoutput entering ");
            InputStream is = proc.getInputStream();
            solutionFound = parseOutput(new BufferedReader(new InputStreamReader(is)));
            System.out.println("\nQRE Parseoutput exiting, now waiting ");
            proc.waitFor();

            is.close();
            proc.getOutputStream().close();
            proc.getErrorStream().close();
            proc.destroy();
            // WRITE OUT FILE
            /*GambitOutput.writeGame(Parameters.GAME_FILES_PATH+gameName+"_test.gambit", eGame);
            System.out.println("QRE "+gameName);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            File f = new File(Parameters.GAME_FILES_PATH+gameName+"_test.gambit");
            pb = pb.redirectInput(f);
            proc = pb.start();
            //OutputStream os = proc.getOutputStream();
            // here we feed the gambit game representation to the solver on stdin
            //String gambitGame = GambitOutput.gameToGambitString(eGame);
            //os.write(gambitGame.getBytes());
            //os.close();
            InputStream is = proc.getInputStream();
            solutionFound = parseOutput(new BufferedReader(new InputStreamReader(is)));
            proc.waitFor();
            System.out.print("done waiting "+gameName);
            is.close();
            proc.getOutputStream().close();
            proc.getErrorStream().close();
            proc.destroy();*/

      } catch (InterruptedException ie) {
        if (!Thread.interrupted()) {
          System.out.println("Interrupted exception but no interrupt flag.");
        }

        String gambitGame = GambitOutput.gameToGambitString(eGame);
        //System.out.println("Game: " + gambitGame);
        //GambitOutput.writeGame("/home/ckiekint/IdeaProjects/EmpiricalGameAnalysis/QREFail.gambit", eGame);
        GambitOutput.writeGame(Parameters.RESULTS_PATH+"QREFail.gambit", eGame);
        InputStream is = proc.getInputStream();
        Scanner s = new Scanner(is);
        while(s.hasNextLine()) {
          System.out.println(s.nextLine());
        }
        s = new Scanner(proc.getErrorStream());
        while(s.hasNextLine()) {
          System.out.println(s.nextLine());
        }
        System.out.println("Detected hung gambit process; destroying.");
 //       solutionFound = parseOutput(proc.getInputStream());
 //       System.out.println("Solution Found: "+solutionFound);
        proc.destroy();
      } catch (Exception e) {
          System.out.println(e.getStackTrace());
        throw new RuntimeException("Exception while running gambit: " + e.getMessage());
      }
        //System.out.println("done executing game "+gameName);
      //proc.destroy();
    }
  }

    public ArrayList<QRE> getQRES(){
        return qres;
    }

    public MixedStrategy getDefault(int player){
        return new MixedStrategy(numActs[player]);
    }
    public void setGameName(String str){
        this.gameName = str;
    }
    public MixedStrategy getInvalid(int player){
    MixedStrategy s = new MixedStrategy(numActs[player]);
      for(int i = 1; i <= numActs[player];i++)
          s.setProb(i,-1.0);
      return s;
    }
}
