package Main;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import Log.Logger;
/*import ega.generate.Generate;*/
import games.DeviationIterator;
import generate.Generate;
import subgame.GameReductionBySubGame;
import subgame.Parameters;
import subgame.StrategyMap;
import subnet.SubNet;
import util.GamutModifier;



 
public class Main {
	private static int numberOfGames;
	private static ArrayList<String> agentPaths;
	private static ArrayList<String> agentNames;
	private static int[] actions;

	/*
	 * final variables:
	 * 
	 * 1. distance metric
	 * 2. taking the max distance or sum them and taking the max distance
	 * 3. delta: either average or max
	 */


	//public static boolean noisy = false;

	public static long START_TIME = 0;
	public static long END_TIME = 0; 
	//public static long clustertime = 0; 

	public static long[] clustertime = new long[2]; 


	private static  boolean RAND_POINTS_FROM_OBSERVATION = false; 
	
	private static  boolean RAND_ACTION_INIT_TO_CLUSTERS = true;






	private static  boolean DIST_METRIC_LINE = false; //used to create dir, if this is true, then other one is false, vice versa
	private static  boolean DIST_METRIC_EUCLIDEAN =  true;



	
	
	//used to create dir
	//make these false if euclidean
	private static   boolean MAX_DIST = false;   
	private static  final boolean SUM_DIST = false; 
	
	
	

	private static  boolean MAX_DELTA = false; //used to create dir
	private static  boolean AVRG_DELTA = true;
	

	public static  String experimentdir = null;//Referee.DIST_METRIC_LINE? "1": "0" + Referee.SUM_DIST ? "1":"0" + Referee.MAX_DELTA ? "1" : "0";


	public static double percremovedstrategy = 0; 



	
	
	
	
	public static void setRAND_POINTS_FROM_OBSERVATION(
			boolean rAND_POINTS_FROM_OBSERVATION) {
		RAND_POINTS_FROM_OBSERVATION = rAND_POINTS_FROM_OBSERVATION;
	}




	public static void setRAND_ACTION_INIT_TO_CLUSTERS(
			boolean rAND_ACTION_INIT_TO_CLUSTERS) {
		RAND_ACTION_INIT_TO_CLUSTERS = rAND_ACTION_INIT_TO_CLUSTERS;
	}



	public static int getBitValue(int var, int bitposition)
	{
		int x = var & (1<<bitposition);
		if(x>0)
			return 1;


		return 0; 
	}




	public static boolean isRandActionInitToClusters() {
		return RAND_ACTION_INIT_TO_CLUSTERS;
	}



	public static boolean isRandPointsFromObservation() {
		return RAND_POINTS_FROM_OBSERVATION;
	}



	public static boolean isDistMetricLine() {
		return DIST_METRIC_LINE;
	}




	public static boolean isDistMetricEuclidean() {
		return DIST_METRIC_EUCLIDEAN;
	}




	public static boolean isMaxDist() {
		return MAX_DIST;
	}




	public static boolean isSumDist() {
		return SUM_DIST;
	}




	public static boolean isMaxDelta() {
		return MAX_DELTA;
	}




	public static boolean isAvrgDelta() {
		return AVRG_DELTA;
	}












	public static void addAgents(){
		//numberOfGames = 500;
		numberOfGames = 100;
		agentPaths = new ArrayList<String>();
		agentNames = new ArrayList<String>();
		agentPaths.add(Parameters.GAME_FILES_PATH+ "QRE.jar");//lambda 2.4
		agentPaths.add(Parameters.GAME_FILES_PATH+ "QRE5.jar");//lambda 5
		agentPaths.add(Parameters.GAME_FILES_PATH+ "QRE100.jar");//MSNE
		agentPaths.add(Parameters.GAME_FILES_PATH+ "QRE0.jar");//uniform
		agentPaths.add(Parameters.GAME_FILES_PATH+ "BRQRE0.jar");//best response to uniform
		agentPaths.add(Parameters.GAME_FILES_PATH+ "ENE.jar");//MSNE
		agentNames.add("QRE.jar");
		agentNames.add("QRE5.jar");
		agentNames.add("QRE100.jar");
		agentNames.add("QRE0.jar");
		agentNames.add("BRQRE0.jar");
		agentNames.add("ENE.jar");
		actions = new int[2];
		actions[0]=20;
		actions[1]=20;
	}
	public static void generateGames(){
		Generate g = new Generate();
		g.generateGames();
	}
	public static void removeActions(){
		GamutModifier gm = new GamutModifier();
		HashMap<String,StrategyMap> strategyMaps = new HashMap<String,StrategyMap>();
		StrategyMap[] temp = new StrategyMap[2];
		//double[] fractions = {.1,.2,.3,.4,.5,.6,.7,.8,.9};
		double[] fractions = {.5, .6, .75, .9};
		for(int f = 0;f<fractions.length;f++){
			for(int i = 0;i<500;i++){
				gm.setGame(i+"");
				temp = gm.removeActions(fractions[f],1);
				strategyMaps.put("r" + fractions[f] + "-1-1-" + i, temp[0]);
				strategyMaps.put("r" + fractions[f] + "-2-1-" + i, temp[1]);
				temp = gm.removeActions(fractions[f],2);
				strategyMaps.put("r" + fractions[f] + "-1-2-" + i, temp[0]);
				strategyMaps.put("r" + fractions[f] + "-2-2-" + i, temp[1]);
			}
			for(int i = 2500;i<3000;i++){
				gm.setGame(i+"");
				temp = gm.removeActions(fractions[f],1);
				strategyMaps.put("r" + fractions[f] + "-1-1-" + i, temp[0]);
				strategyMaps.put("r" + fractions[f] + "-2-1-" + i, temp[1]);
				temp = gm.removeActions(fractions[f],2);
				strategyMaps.put("r" + fractions[f] + "-1-2-" + i, temp[0]);
				strategyMaps.put("r" + fractions[f] + "-2-2-" + i, temp[1]);
			}
		}
		try{
			PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"strategyMaps.csv","UTF-8");
			Collection<StrategyMap> strategies = strategyMaps.values();
			Iterator it = strategies.iterator();
			while (it.hasNext()) {pw.write(it.next().toString()+"\n");}
			pw.close();
		}catch (Exception e){e.printStackTrace();}
	}
	public static void bucket(){
		GamutModifier gm = new GamutModifier();
		for(int i = 0;i<500;i++){//zero sum
			gm.setGame(i+"");
			for(int b = 2;b<=10;b++)
				gm.bucket(b);
		}
		for(int i = 2500;i<3000;i++){//random
			gm.setGame(i+"");
			for(int b = 2;b<10;b++)
				gm.bucket(b);
		}
	}
	
	
	
	
	
	
	
	
	

	
	public static void writePayoffsInFile()
	{
		try{
            PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"payoff.txt","UTF-8");
            
            
            GamutModifier gm = new GamutModifier("0");
            
            
            for(int i =0; i< gm.returnGame().getNumActions(0); i++)
            {
            	for(int j=0; j<gm.returnGame().getNumActions(1); j++)
            	{
            		int[] outcome = {i+1, j+1};
            		pw.write("  "+ gm.returnGame().getPayoff(outcome, 0));
            	}
            	pw.write("\n");
            	
            }
            
            pw.close();
		}
            catch(Exception ex)
            {
            	
            	
            }
		
		
		
	}
	
	
	
	public static void testDeviationItr()
	{
		
		
		int[] outcome = {3,2};
		int[] actions = {4, 4};
		
		DeviationIterator itr = new DeviationIterator(outcome, actions);
		
		
		while(itr.hasNext())
		{
			int[] devoutcome = itr.next();
			System.out.print("\n "+ devoutcome[0] + " "+ devoutcome[1]);
			
		}
		
	}
	
	

	public static void kMeansClustering(int numberofclusters)
	{




		/*	for(int c=0;c<Math.pow(2, 4); c++)
		{

			Logger.log("\n ++++++++++  Integer val++++++++++++: "+ c, false);
			for(int j=3; j>=0; j--)
			{




				if(j==3)
				{

					int v = Referee.getBitValue(c, j);

					if(v==1)
					{
						Referee.setRAND_POINTS_FROM_OBSERVATION(true);
						Referee.setRAND_ACTION_INIT_TO_CLUSTERS(false);
					}
					else if(v==0)
					{
						Referee.setRAND_POINTS_FROM_OBSERVATION(false);
						Referee.setRAND_ACTION_INIT_TO_CLUSTERS(true);

					}
				}







				if(j==2)
				{
					int v = Referee.getBitValue(c, j);

					if(v==1)
					{
						Referee.setDIST_METRIC_LINE(true);
						Referee.setDIST_METRIC_EUCLIDEAN(false);
					}
					else if(v==0)
					{
						Referee.setDIST_METRIC_LINE(false);
						Referee.setDIST_METRIC_EUCLIDEAN(true);

						Referee.setMAX_DIST(false);
						Referee.setSUM_DIST(false);


					}

				}

				if(j==1)
				{
					if(!Referee.isDIST_METRIC_EUCLIDEAN())
					{
						int v = Referee.getBitValue(c, j);

						if(v==1)
						{
							Referee.setSUM_DIST(true);
							Referee.setMAX_DIST(false);

						}
						else if(v==0)
						{

							Referee.setSUM_DIST(false);
							Referee.setMAX_DIST(true);

						}

					}
				}


				if(j==0)
				{
					int v= Referee.getBitValue(c, j);

					if(v==1)
					{
						Referee.setMAX_DELTA(true);
						Referee.setAVRG_DELTA(false);
					}
					else if(v==0)
					{
						Referee.setMAX_DELTA(false);
						Referee.setAVRG_DELTA(true);
					}
				}





			}


			if(Referee.isDIST_METRIC_EUCLIDEAN())
			{
				Referee.setMAX_DIST(false);
				Referee.setSUM_DIST(false);
			}
		 */




		/*
		 * make the experiment directory
		 */


		

		/*if(Referee.isRandPointsFromObservation())
		{
			Referee.experimentdir = "1";
		}
		else 
		{
			Referee.experimentdir = "0";
		}*/


		if(Main.isDistMetricLine())
		{
			Main.experimentdir = "1";
		}
		else 
		{
			Main.experimentdir = "0";
		}

		if(Main.MAX_DIST)
		{
			Main.experimentdir += "1";
		}
		else 
		{
			Main.experimentdir += "0";
		}


		if(Main.MAX_DELTA)
		{
			Main.experimentdir += "1";
		}
		else 
		{
			Main.experimentdir += "0";
		}






		boolean isdircreated = false;
		
		File file = null;
		
		
		{
			 file = new File(Parameters.GAME_FILES_PATH+Main.experimentdir);
		}

		
		if (!file.exists()) {
			if (file.mkdir()) {
				isdircreated = true;
				System.out.println("Directory is created!");
			} else {
				
				System.out.println("Failed to create directory!");
			}
		}
		else
		{
			/*try {
				FileUtils.deleteDirectory(file);
				file.mkdir();
				System.out.println("Directory is created!");
				isdircreated = true;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
		}





		if(isdircreated)
		{

			Logger.log("\n Clustering with: ", false);
			Logger.log("\n Initilization: ", false);
			Logger.log("\n RAND_POINTS_FROM_OBSERVATION : "+ Main.isRandPointsFromObservation(), false);
			Logger.log("\n RAND_ACTION_INIT_TO_CLUSTERS() "+ Main.isRandActionInitToClusters(),false);
			Logger.log("\n Distance Metric METRIC_LINE() "+ Main.isDistMetricLine(), false );
			Logger.log("\n Distance Metric METRIC_EUCLIDEAN() "+ Main.isDistMetricEuclidean(), false );
			Logger.log("\n Dist SUM_DIST() "+ Main.isSumDist(), false);
			Logger.log("\n Dist MAX_DIST() "+ Main.isMaxDist(), false);
			Logger.log("\n Delta MAX_DELTA() "+ Main.isMaxDelta(), false);
			Logger.log("\n Delta AVRG_DELTA() "+ Main.isAvrgDelta(), false);




			//	for(int gametype=0; gametype<2; gametype++)
			//{

			for(int clusternumber=numberofclusters; clusternumber>=2; clusternumber=clusternumber/2)
			{

				double[] sumdelta = {0,0,0};
				double[] sumepsilon = {0,0,0};
				final int ITERATION = 100;


				//for testing
				//int clusternumber = 4;
				//int gametype =0;



				/*
				 * reset time before iterations. 
				 */

				Main.clustertime[0] = 0;
				Main.clustertime[1] = 0;
				Main.percremovedstrategy =0;


				for(int i=1; i<=ITERATION; i++)
				{


					double[][] res =  GamutModifier.clusteringAbstractionOldBothPlayer(clusternumber, i);
					
				
					
					



					//for testing
					//	double[][] res = GamutModifier.clusteringAbstraction(numberofclusters, i+1, 0);

					for(int j=0; j<3; j++)
					{
						sumdelta[j] = sumdelta[j] + res[j][0];
						sumepsilon[j] = sumepsilon[j] + res[j][1];
						Logger.log("\n Running Instance "+ i+ " player "+ j + " delta: "+ res[j][0]+ " epsilon: "+res[j][1], false);

					}



				}
				
				Main.percremovedstrategy = (Main.percremovedstrategy/ITERATION)*100;

				for(int j=0; j<3; j++)
				{
					sumdelta[j] = sumdelta[j]/ITERATION;
					sumepsilon[j] = sumepsilon[j]/ITERATION ;
					
					
					
					Logger.log("\n Player: "+j+ " final delta: "+ sumdelta[j]+  " Player: "+j+ " final epsilon: "+ sumepsilon[j], false);

				}



				try{
					PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+Main.experimentdir+"/"+"result"+".result"),true));

					for(int j=0; j<3; j++)
					{



						pw.append("\n "+sumdelta[j] + " "+ sumepsilon[j] + "  "+ Main.clustertime[0]+ " "+Main.percremovedstrategy);

						String x = " ";
						
						if(j==0)
						{
							x = "NE";
						}
						else if(j==1)
						{
							x = "SUbgame";
						}
						else if(j==1)
						{
							x = "Max Expected";
						}
						
						
						Logger.logit("\nFor clustering "+clusternumber+" and "+x+" profile, Final Average delta and epsilon "+sumdelta[j] + " "+ sumepsilon[j] + "  ");
					
					
					
					}
					pw.append("\n\n");
					pw.close();


				}
				catch(Exception e)
				{

				}




			} // for loop for cluster number


			//	}



			//	}






		} // if for directory creation
















	}
	

	public static void main (String[] args){
		//generateGames();
		//removeActions();
		//bucket();
		//  addAgents();
		//  loadRemovedStrategies();
		//cnag();
		//cnazs();
		//bsag();
		//bsazs();
		//  bdag();
		//  bdazs();
		//  rrdrg();
		//  rrdrzs();
		//topN();

		//GamutModifier.makeNoisyGame(0, .5, 256);
	//kMeansClustering(16); //two player only 0 and 1
		
	//	GamutModifier.testClusteringAbstraction(4);
	//	writePayoffsInFile();
	//	testDeviationItr();
		
		
		try 
		{
			//GameReductionBySubGame.testSubGameMethod();
			//GameReductionBySubGame.startGameSolving();
			
			
			//GameReductionBySubGame.deltaExperiment();
			//GameReductionBySubGame.LouvainVsKmean();
			//GameReductionBySubGame.testSubGameSolverV3();
			
			//GameReductionBySubGame.clusterDistributionExperiment();
			//ClusteringForSubgame.testSubgameClustering();
			//SecurityGameAbstraction.wildlifeAbstraction();
			//SecurityGameAbstraction.testing1();
			//SecurityGameAbstraction.testingMMR();
			//LouvainClusteringActions.testLouvainClustering();
			
			//GameReductionBySubGame.testCyberSubGame();
			//SubNet.doExp();
			//SubNet.transmissionExp();
			
			int naction = 0;//Integer.parseInt(args[0]);
			int ncluster = 0;//Integer.parseInt(args[1]);
			
			
			/*int naction = Integer.parseInt(args[0]);
			int ncluster = Integer.parseInt(args[1]);*/
			
			
			
			//GameReductionBySubGame.testSubGameSolverV3();
			GameReductionBySubGame.testSubGameVSOrigSolving();
			
			
			//SubNet.deltaExp(naction, ncluster);
			//SubNet.deltaExpV2(naction, ncluster);
			
			
			
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	//	RegretLearner.startSolvingGameByRegret();
	//RegretLearner.doExPeriments();
	//	RegretClustering.doRegretClustering();

	}





	/**
	 * @param numberofclusters how many cluster you want
	 * @param player the player number, usually 0 or 1 for a two player game. 
	 */

}
