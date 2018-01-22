package subgame;

import games.EmpiricalMatrixGame;
import games.MatrixGame;
import games.MixedStrategy;
import parsers.GamutParser;
import solvers.QRESolver;

public class QREAgent{

  // empty constructor
  private QREAgent() {
  }

  public static void main(String[] args) {
      int  player = 0;
      String filename = "";
      double lambda = 2.4;
      if(args.length==4){
             for(int i = 0; i < args.length; i++){
                if(args[i].equals("-game")){
                    try{
                        filename = args[++i];
                    }catch(Exception e){
                        System.err.println("Error parsing for QRE." );
                    }
                }else if(args[i].equals("-player")){
                    try{
                        player = Integer.parseInt(args[++i]);
                        player-=1;
                    }catch(Exception e){
                        System.err.println("Error parsing for QRE." );
                    }
                }
            }
      }
      /*else{
          for(int i = 0; i < args.length; i++){
                if(args[i].equals("-game")){
                    try{
                        filename = args[++i];
                    }catch(Exception e){
                        System.err.println("Error parsing for QRE." );
                    }
                }else if(args[i].equals("-player")){
                    try{
                        player = Integer.parseInt(args[++i]);
                        player-=1;
                    }catch(Exception e){
                        System.err.println("Error parsing for QRE." );
                    }
                }
                else if(args[i].equals("-lambda")){
                    try{
                        lambda = Double.parseDouble(args[++i]);
                    }catch(Exception e){
                        System.err.println("Error parsing for QRE." );
                    }
                }
            }
      }*/
      System.out.println(runQRE(player,lambda, new EmpiricalMatrixGame(readGame(filename))));
  }
  /*public static MatrixGame readGame(){
      return GamutParser.readGamutGame(GAME_FILES_PATH +"2501"+GAMUT_GAME_EXTENSION);
  }*/
  public static MatrixGame readGame(String path){
      //debug
      //System.out.println("Path: "+path);
      return GamutParser.readGamutGame(path);
  }
  public static MixedStrategy runQRE (int player, double lambda, EmpiricalMatrixGame g){
      QRESolver qre = new QRESolver(lambda);
      qre.setDecisionMode(QRESolver.DecisionMode.BR);
      return qre.solveGame(g,player);
  }
}