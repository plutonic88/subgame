package subgame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import Log.Logger;
import Main.Main;
import games.EmpiricalMatrixGame;
import games.Game;
import games.MatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import output.SimpleOutput;
import parsers.GamutParser;
import solvers.IEDSMatrixGames;
import solvers.MinEpsilonBoundSolver;
import solvers.QRESolver;
import solvers.SolverUtils;
import util.GamutModifier;

public class KmeanClustering {

	private static  boolean RAND_POINTS_FROM_OBSERVATION = true; 
	private static  boolean RAND_ACTION_INIT_TO_CLUSTERS = false;
	private static  boolean DIST_METRIC_LINE = true; //used to create dir, if this is true, then other one is false, vice versa
	private static  boolean DIST_METRIC_EUCLIDEAN =  false;
	private static  boolean MAX_DIST = true;   //make it false if euclidean
	private static  final boolean SUM_DIST = false; //make it false if euclidean
	private static  boolean MAX_DELTA = true; 
	private static  boolean AVRG_DELTA = false;


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




	public static void doExPeriemnt(int numberofclusters)
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


			Main.percremovedstrategy =0;


			for(int i=1; i<=ITERATION; i++)
			{



				MatrixGame mg = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+i+Parameters.GAMUT_GAME_EXTENSION));

				double[][] res =  doKmean(clusternumber, mg, Integer.toString(i));







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


				if(j<2)
					Main.clustertime[j] = Main.clustertime[j]/ ITERATION;

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


	}



	public static List<Integer>[] clusterTargets(int numberofclusters, int[][] gamedata)
	{
		int numberoftargets = gamedata.length;
		//int opponent = 1^player;
		//final int   RUNNING_FOR = 20;
		//int[] numberofactions = mg.getNumActions();
		//double[] extreampayoffs = mg.getExtremePayoffs();
		int INDEX_LIMIT = 4;
		List<Integer[]>[] clusters = new List[numberofclusters]; // create an array of list. Each list will contain arrays of double
		double[][] clusterpoints = new double[numberofclusters][INDEX_LIMIT];  // cluster points for each cluster
		double[][] oldclusterpoints = new double[numberofclusters][INDEX_LIMIT];  
		double[][] distancefromcluster = new double[numberofclusters][INDEX_LIMIT];
		int runningInstance = 0;
		//double[] sumofdifferences = new double[numberofclusters];  //store the sum of differences for clusters
		//double[] maxdifference = new double[numberofclusters]; 
		double[] squaredrootdifference = new double[numberofclusters];
		ArrayList<Integer> alreadyassignedactions = new ArrayList<Integer>(); // for random partition method
		//boolean flagforrandompartition = false;
		for(int i=0; i< numberofclusters; i++)
		{
			clusters[i] = new ArrayList<Integer[]>(); 
		}
		if(KmeanClustering.isRandPointsFromObservation()) // implemented only for two players
		{
			ArrayList<Integer> unassignedpoints = new ArrayList<Integer>();
			int targetindex = 0;
			for(int i=0; i<numberoftargets; i++)
			{
				unassignedpoints.add(i);
			}
			for(int i=0; i< numberofclusters; i++)
			{
				targetindex = randInt(0, unassignedpoints.size()-1);
				int tmptarget = unassignedpoints.get(targetindex);
				unassignedpoints.remove(targetindex);
				int index = 0;
				while(index<INDEX_LIMIT)
				{
					clusterpoints[i][index] = gamedata[tmptarget][index];
					index++;
				}
			}
			//System.out.println("Initial cluster points...");
			for(int i =0; i< numberofclusters; i++)
			{
				//System.out.print("Cluster: "+ i + " ");
				for(int j =0; j< INDEX_LIMIT; j++)
				{
					//System.out.print(" "+clusterpoints[i][j]); 
				}
				//System.out.print("\n");
				
			}
		}
		while(true)
		{
			//System.out.println("\nIteration: "+ runningInstance);
			
			if(runningInstance>=100)
			{
				List<Integer>[] finalcluster = new List[numberofclusters];
				for(int i=0; i< numberofclusters; i++){

					finalcluster[i] = new ArrayList<Integer>(); 
				}
				for(int i=0; i<numberofclusters; i++)
				{
					for(Integer[] x: clusters[i])
					{
						finalcluster[i].add(x[0].intValue());
					}
				}
				return finalcluster;
			}
			//copy the cluster points to old cluster points.
			for(int i=0; i< numberofclusters; i++)
			{
				for(int j=0; j<INDEX_LIMIT; j++)
				{
					oldclusterpoints[i][j] = clusterpoints[i][j];
				}
			}
			// now clear/create e cluster object the new cluster for a new iteration. 
			for(int i=0; i< numberofclusters; i++)
			{
				clusters[i]= new ArrayList<Integer[]>(); //.clear();
			}
			/*
			 * Now iterate over all the possible action touples for player 1. 
			 * calclate the difference from cluster points
			 * assign to the cluster with the minimum difference.
			 *  
			 */
			for(int target = 0; target < numberoftargets; target++)
			{
				for(int rewardindex = 0; rewardindex < INDEX_LIMIT; rewardindex++)
				{

					double tmppayoff = gamedata[target][rewardindex]; //mg.getPayoff(outcome, player); //get the payoff for player 1 or player 2
					for(int clusterindex =0; clusterindex<numberofclusters; clusterindex++)
					{
						/*
						 * calculate the differences of payoffs for each cluster points 
						 * calculate euclidean distance: first take the squares of difference....
						 */
						if(KmeanClustering.isDistMetricEuclidean())
						{
							//	Logger.log("\n entered DistMetricEuclidean() ", false);
							distancefromcluster[clusterindex][rewardindex] = (clusterpoints[clusterindex][rewardindex]  - (tmppayoff));
							distancefromcluster[clusterindex][rewardindex] = distancefromcluster[clusterindex][rewardindex] * distancefromcluster[clusterindex][rewardindex];
						}
					}
				}
				double min = Double.POSITIVE_INFINITY;
				int minindex = 0;
				if(KmeanClustering.isDistMetricEuclidean())
				{
					/*
					 * find the squared root distances
					 */
					for(int l =0; l< numberofclusters; l++)
					{
						squaredrootdifference[l] = 0;
						for(int m =0; m< distancefromcluster[l].length; m++)
						{
							squaredrootdifference[l] += distancefromcluster[l][m];
						}
						squaredrootdifference[l] = Math.sqrt(squaredrootdifference[l]);
						//int a = target+1;
						//System.out.println("\n Target "+ target+"'s euclidean distance from cluster "+ l+ " : "+squaredrootdifference[l]);
					}
					// find the minimum squared root distance
					for(int n =0; n< squaredrootdifference.length; n++)
					{
						if(min > squaredrootdifference[n])
						{
							min = squaredrootdifference[n];
							minindex = n;
						}
					}

				}
				/*
				 * 
				 * assign the action i+1 to minindex cluster
				 */
				//System.out.println("\nIteration: "+ runningInstance + " \n Assigning cluster points ");
				//System.out.println("target "+target +" is assigned to cluster "+ minindex);
				assignToCluster(clusters, target, minindex, gamedata, INDEX_LIMIT);

			}  // end of outer for loop
			/*
			 * now recalculate the cluster points
			 */
			
			//System.out.println("Clustered Targets : " );
			for(int i=0; i< clusters.length; i++)
			{
				//System.out.print("Cluster " + i + " : ");
				for(Integer[] target: clusters[i])
				{
					//System.out.print(target[0]);
					if(clusters[i].indexOf(target) < (clusters[i].size()-1) )
					{
						//System.out.print(",");
					}
				}
				//System.out.print("\n");
			}
			
			
			
			

			calculateClusterMean(clusters, clusterpoints, numberofclusters, gamedata, INDEX_LIMIT);
			//System.out.println("\n\nIteration: "+ runningInstance + " ");
			//System.out.println("\n\nK-mean Iteration: "+ runningInstance  +" new cluster points(mean)\n");
			for(int i =0; i< numberofclusters; i++)
			{

				//System.out.print("Cluster: "+ i + " ");
				//Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< INDEX_LIMIT; j++)
				{

					//System.out.print(" "+clusterpoints[i][j]); 
					//Logger.log(" "+clusterpoints[i][j], false);

				}
				//System.out.print("\n");
				//Logger.log("\n", false);
			}
			//System.out.println("Checking or stop ");
			boolean checkforstop = true;
			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j<INDEX_LIMIT; j++)
				{
					if(clusterpoints[i][j] != oldclusterpoints[i][j])
					{
						checkforstop = false;
						break;
					}
				}
				if(checkforstop==false)
				{
					break;
				}
			}

			//System.out.println("\nIteration: "+ runningInstance + "Checking exit condition ");
			if(checkforstop == true && (!isClusterIsEmpty(clusters)))
			{
				//System.out.println("\n Exiting..." );
				break;
			}
			//Logger.log("\n\n", false);
			runningInstance++;



		}//end of outer while loop
		List<Integer>[] finalcluster = new List[numberofclusters];
		for(int i=0; i< numberofclusters; i++){

			finalcluster[i] = new ArrayList<Integer>(); 
		}
		for(int i=0; i<numberofclusters; i++)
		{
			for(Integer[] x: clusters[i])
			{
				finalcluster[i].add(x[0].intValue());
			}
		}

		return finalcluster;


	}
	
	
	public static List<Double>[] clusterUsers(int numberofclusters, double[][] examples)
	{
		int numberofexamples = examples.length;
		//int opponent = 1^player;
		//final int   RUNNING_FOR = 20;
		//int[] numberofactions = mg.getNumActions();
		//double[] extreampayoffs = mg.getExtremePayoffs();
		int INDEX_LIMIT = examples[0].length;
		List<Double[]>[] clusters = new List[numberofclusters]; // create an array of list. Each list will contain arrays of double
		double[][] clusterpoints = new double[numberofclusters][INDEX_LIMIT];  // cluster points for each cluster
		double[][] oldclusterpoints = new double[numberofclusters][INDEX_LIMIT];  
		double[][] distancefromcluster = new double[numberofclusters][INDEX_LIMIT];
		int runningInstance = 0;
		//double[] sumofdifferences = new double[numberofclusters];  //store the sum of differences for clusters
		//double[] maxdifference = new double[numberofclusters]; 
		double[] squaredrootdifference = new double[numberofclusters];
		ArrayList<Integer> alreadyassignedactions = new ArrayList<Integer>(); // for random partition method
		//boolean flagforrandompartition = false;
		for(int i=0; i< numberofclusters; i++)
		{
			clusters[i] = new ArrayList<Double[]>(); 
		}
		if(KmeanClustering.isRandPointsFromObservation()) // implemented only for two players
		{
			ArrayList<Integer> unassignedpoints = new ArrayList<Integer>();
			int targetindex = 0;
			for(int i=0; i<numberofexamples; i++)
			{
				unassignedpoints.add(i);
			}
			for(int i=0; i< numberofclusters; i++)
			{
				targetindex = randInt(0, unassignedpoints.size()-1);
				int tmptarget = unassignedpoints.get(targetindex);
				unassignedpoints.remove(targetindex);
				int index = 0;
				while(index<INDEX_LIMIT)
				{
					clusterpoints[i][index] = examples[tmptarget][index];
					index++;
				}
			}
			//System.out.println("Initial cluster points...");
			for(int i =0; i< numberofclusters; i++)
			{
				//System.out.print("Cluster: "+ i + " ");
				for(int j =0; j< INDEX_LIMIT; j++)
				{
					//System.out.print(" "+clusterpoints[i][j]); 
				}
				//System.out.print("\n");
				
			}
		}
		while(true)
		{
			//System.out.println("\nIteration: "+ runningInstance);
			
			if(runningInstance>=100)
			{
				List<Double>[] finalcluster = new List[numberofclusters];
				for(int i=0; i< numberofclusters; i++){

					finalcluster[i] = new ArrayList<Double>(); 
				}
				for(int i=0; i<numberofclusters; i++)
				{
					for(Double[] x: clusters[i])
					{
						finalcluster[i].add(x[0]);
					}
				}
				return finalcluster;
			}
			//copy the cluster points to old cluster points.
			for(int i=0; i< numberofclusters; i++)
			{
				for(int j=0; j<INDEX_LIMIT; j++)
				{
					oldclusterpoints[i][j] = clusterpoints[i][j];
				}
			}
			// now clear/create e cluster object the new cluster for a new iteration. 
			for(int i=0; i< numberofclusters; i++)
			{
				clusters[i]= new ArrayList<Double[]>(); //.clear();
			}
			/*
			 * Now iterate over all the possible action touples for player 1. 
			 * calclate the difference from cluster points
			 * assign to the cluster with the minimum difference.
			 *  
			 */
			for(int target = 0; target < numberofexamples; target++)
			{
				for(int rewardindex = 0; rewardindex < INDEX_LIMIT; rewardindex++)
				{

					double tmppayoff = examples[target][rewardindex]; //mg.getPayoff(outcome, player); //get the payoff for player 1 or player 2
					for(int clusterindex =0; clusterindex<numberofclusters; clusterindex++)
					{
						/*
						 * calculate the differences of payoffs for each cluster points 
						 * calculate euclidean distance: first take the squares of difference....
						 */
						if(KmeanClustering.isDistMetricEuclidean())
						{
							//	Logger.log("\n entered DistMetricEuclidean() ", false);
							distancefromcluster[clusterindex][rewardindex] = (clusterpoints[clusterindex][rewardindex]  - (tmppayoff));
							distancefromcluster[clusterindex][rewardindex] = distancefromcluster[clusterindex][rewardindex] * distancefromcluster[clusterindex][rewardindex];
						}
					}
				}
				double min = Double.POSITIVE_INFINITY;
				int minindex = 0;
				if(KmeanClustering.isDistMetricEuclidean())
				{
					/*
					 * find the squared root distances
					 */
					for(int l =0; l< numberofclusters; l++)
					{
						squaredrootdifference[l] = 0;
						for(int m =0; m< distancefromcluster[l].length; m++)
						{
							squaredrootdifference[l] += distancefromcluster[l][m];
						}
						squaredrootdifference[l] = Math.sqrt(squaredrootdifference[l]);
						//int a = target+1;
						//System.out.println("\n Target "+ target+"'s euclidean distance from cluster "+ l+ " : "+squaredrootdifference[l]);
					}
					// find the minimum squared root distance
					for(int n =0; n< squaredrootdifference.length; n++)
					{
						if(min > squaredrootdifference[n])
						{
							min = squaredrootdifference[n];
							minindex = n;
						}
					}

				}
				/*
				 * 
				 * assign the action i+1 to minindex cluster
				 */
				//System.out.println("\nIteration: "+ runningInstance + " \n Assigning cluster points ");
				//System.out.println("target "+target +" is assigned to cluster "+ minindex);
				assignToCluster(clusters, target, minindex, examples, INDEX_LIMIT);

			}  // end of outer for loop
			/*
			 * now recalculate the cluster points
			 */
			
			//System.out.println("Clustered Targets : " );
			for(int i=0; i< clusters.length; i++)
			{
				//System.out.print("Cluster " + i + " : ");
				for(Double[] target: clusters[i])
				{
					//System.out.print(target[0]);
					if(clusters[i].indexOf(target) < (clusters[i].size()-1) )
					{
						//System.out.print(",");
					}
				}
				//System.out.print("\n");
			}
			
			
			
			

			calculateClusterMeanD(clusters, clusterpoints, numberofclusters, examples, INDEX_LIMIT);
			//System.out.println("\n\nIteration: "+ runningInstance + " ");
			//System.out.println("\n\nK-mean Iteration: "+ runningInstance  +" new cluster points(mean)\n");
			for(int i =0; i< numberofclusters; i++)
			{

				//System.out.print("Cluster: "+ i + " ");
				//Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< INDEX_LIMIT; j++)
				{

					//System.out.print(" "+clusterpoints[i][j]); 
					//Logger.log(" "+clusterpoints[i][j], false);

				}
				//System.out.print("\n");
				//Logger.log("\n", false);
			}
			//System.out.println("Checking or stop ");
			boolean checkforstop = true;
			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j<INDEX_LIMIT; j++)
				{
					if(clusterpoints[i][j] != oldclusterpoints[i][j])
					{
						checkforstop = false;
						break;
					}
				}
				if(checkforstop==false)
				{
					break;
				}
			}

			//System.out.println("\nIteration: "+ runningInstance + "Checking exit condition ");
			if(checkforstop == true && (!isClusterIsEmptyD(clusters)))
			{
				//System.out.println("\n Exiting..." );
				break;
			}
			//Logger.log("\n\n", false);
			runningInstance++;



		}//end of outer while loop
		List<Double>[] finalcluster = new List[numberofclusters];
		for(int i=0; i< numberofclusters; i++){

			finalcluster[i] = new ArrayList<Double>(); 
		}
		for(int i=0; i<numberofclusters; i++)
		{
			for(Double[] x: clusters[i])
			{
				finalcluster[i].add(x[0]);
			}
		}

		return finalcluster;


	}

	private static boolean isClusterIsEmpty(List<Integer[]>[] clusters) {
		for(int i=0; i<clusters.length; i++)
		{
			if(clusters[i].size()==0)
				return true;
		}

		return false;
	}
	
	private static boolean isClusterIsEmptyD(List<Double[]>[] clusters) {
		for(int i=0; i<clusters.length; i++)
		{
			if(clusters[i].size()==0)
				return true;
		}

		return false;
	}

	/**
	 * calculates cluster mean for security games
	 * @param clusters
	 * @param clusterpoints
	 * @param numberofclusters
	 * @param gamedata
	 */
	private static void calculateClusterMean(List<Integer[]>[] clusters,
			double[][] clusterpoints, int numberofclusters, int[][] gamedata, int INDEX_LIMIT) {

		/*

		 * now recalculate the cluster mean

		 */
		//int opponent = 1^player;
		double average = 0;
		for(int clusterindex = 0; clusterindex< numberofclusters; clusterindex++)
		{
			int clustersize = clusters[clusterindex].size();
			if(clustersize==0)
			{
				System.out.println("\n\nEmpty cluster: "+ clusterindex + " ");
				int randomtarget;
				while(true)
				{
					// if cluster is empty, assign random points from the strategy
					randomtarget = randInt(0,gamedata.length-1 );
					//check if the payoffs are same as centroid of another cluster
					System.out.println(".....");
					break;

				}
				Logger.log("\nAction "+ randomtarget+"'s payoffs are assigned to cluster "+ clusterindex, false);
				for(int j =0; j<INDEX_LIMIT; j++)
				{
					double reward = gamedata[randomtarget][j];  //mg.getPayoff(outcome, player);
					clusterpoints[clusterindex][j] = reward;
				}
				Logger.log("\n cluster: "+ clusterindex + " is empty, points after reassignment:\n", false);


			}
			else if(clustersize>0)
			{
				for(int rewardindex = 0; rewardindex< INDEX_LIMIT; rewardindex++)
				{
					average = 0;   // corrected, average should be reset after calculating for every action
					for(Integer[] x: clusters[clusterindex])
					{
						average += x[rewardindex+1]; 
					}
					if(clustersize != 0)
					{
						clusterpoints[clusterindex][rewardindex] = average/clustersize; 
					}
				}
			}

		}

	}
	
	
	/**
	 * calculates cluster mean for security games
	 * @param clusters
	 * @param clusterpoints
	 * @param numberofclusters
	 * @param gamedata
	 */
	private static void calculateClusterMeanD(List<Double[]>[] clusters,
			double[][] clusterpoints, int numberofclusters, double[][] gamedata, int INDEX_LIMIT) {

		/*

		 * now recalculate the cluster mean

		 */
		//int opponent = 1^player;
		double average = 0;
		for(int clusterindex = 0; clusterindex< numberofclusters; clusterindex++)
		{
			int clustersize = clusters[clusterindex].size();
			if(clustersize==0)
			{
				System.out.println("\n\nEmpty cluster: "+ clusterindex + " ");
				int randomtarget;
				while(true)
				{
					// if cluster is empty, assign random points from the strategy
					randomtarget = randInt(0,gamedata.length-1 );
					//check if the payoffs are same as centroid of another cluster
					System.out.println(".....");
					break;

				}
				Logger.log("\nAction "+ randomtarget+"'s payoffs are assigned to cluster "+ clusterindex, false);
				for(int j =0; j<INDEX_LIMIT; j++)
				{
					double reward = gamedata[randomtarget][j];  //mg.getPayoff(outcome, player);
					clusterpoints[clusterindex][j] = reward;
				}
				Logger.log("\n cluster: "+ clusterindex + " is empty, points after reassignment:\n", false);


			}
			else if(clustersize>0)
			{
				for(int rewardindex = 0; rewardindex< INDEX_LIMIT; rewardindex++)
				{
					average = 0;   // corrected, average should be reset after calculating for every action
					for(Double[] x: clusters[clusterindex])
					{
						average += x[rewardindex+1]; 
					}
					if(clustersize != 0)
					{
						clusterpoints[clusterindex][rewardindex] = average/clustersize; 
					}
				}
			}

		}

	}

	/**
	 * assigns targets to a cluster
	 * @param clusters
	 * @param target
	 * @param minindex
	 * @param targettoassign
	 * @param gamedata
	 */
	private static void assignToCluster(List<Integer[]>[] clusters, int target,
			int assignedcluster, int[][] gamedata, int INDEX_LIMIT) {

		//int opponent = 1^player;
		//int oppnumaction = mg.getNumActions(opponent);
		Integer[] tupleincluster = new Integer[INDEX_LIMIT+1]; // +1 for the target
		tupleincluster[0] = target; //the target in the first index
		/*
		 * now assign the rewards
		 */
		for(int p = 0; p<INDEX_LIMIT; p++)
		{
			tupleincluster[p+1] = gamedata[target][p]; //mg.getPayoff(tmpoutcome, player); 
		}
		clusters[assignedcluster].add(tupleincluster); 

	}
	
	
	private static void assignToCluster(List<Double[]>[] clusters, int target,
			int assignedcluster, double[][] gamedata, int INDEX_LIMIT) {

		//int opponent = 1^player;
		//int oppnumaction = mg.getNumActions(opponent);
		Double[] tupleincluster = new Double[INDEX_LIMIT+1]; // +1 for the target
		tupleincluster[0] = (double)target; //the target in the first index
		/*
		 * now assign the rewards
		 */
		for(int p = 0; p<INDEX_LIMIT; p++)
		{
			tupleincluster[p+1] = gamedata[target][p]; //mg.getPayoff(tmpoutcome, player); 
		}
		clusters[assignedcluster].add(tupleincluster); 

	}

	public static List<Integer>[] clusterActions(int numberofclusters, int player, MatrixGame mg )
	{

		int opponent = 1^player;
		//final int   RUNNING_FOR = 20;
		int[] numberofactions = mg.getNumActions();
		double[] extreampayoffs = mg.getExtremePayoffs();
		List<Double[]>[] clusters = new List[numberofclusters]; // create an array of list. Each list will contain arrays of double
		double[][] clusterpoints = new double[numberofclusters][numberofactions[opponent]];  // cluster points for each cluster
		double[][] oldclusterpoints = new double[numberofclusters][numberofactions[opponent]];  
		double[][] distancefromcluster = new double[numberofclusters][numberofactions[opponent]];
		int runningInstance = 0;
		double[] sumofdifferences = new double[numberofclusters];  //store the sum of differences for clusters
		double[] maxdifference = new double[numberofclusters]; 
		double[] squaredrootdifference = new double[numberofclusters];
		ArrayList<Integer> alreadyassignedactions = new ArrayList<Integer>(); // for random partition method
		boolean flagforrandompartition = false;
		for(int i=0; i< numberofclusters; i++)
		{

			clusters[i] = new ArrayList<Double[]>(); 
		}
		if(KmeanClustering.isRandPointsFromObservation()) // implemented only for two players
		{
			//Logger.log("\n entered RandPointsFromObservation() ", false);
			ArrayList<Integer> points = new ArrayList<Integer>();
			int actionforclusterpoint =0;
			for(int i=0; i< numberofclusters; i++)
			{

				while(true)
				{
					if(points.size()==0)
					{
						actionforclusterpoint = randInt(1, numberofactions[player]); // choose an action randomly
						points.add(actionforclusterpoint);
						break;
					}
					else
					{
						actionforclusterpoint = randInt(1, numberofactions[player]); // choose an action randomly
						if(!points.contains(actionforclusterpoint))
						{
							points.add(actionforclusterpoint);
							break;
						}
					}
				}

				/*
				 * create an iterator to generate the outcomes,
				 *  then match the randomly generated chosen action in the outcome.
				 *   if match, then get the payoff, and assign it to the cluster point
				 */

				OutcomeIterator iterator = mg.iterator();
				int[] outcomegame = new int[mg.getNumPlayers()];
				int index =0;
				while(iterator.hasNext())
				{
					outcomegame = iterator.next();
					if(outcomegame[player] == actionforclusterpoint)
					{
						clusterpoints[i][index++] = mg.getPayoff(outcomegame, player);
					}

				}

			}
		}
		/*
		 * assigns random actions to each clusters
		 */
		if(KmeanClustering.isRandActionInitToClusters())
		{

			//	Logger.log("\n entered RandActionInitToClusters() ", false);
			/*
			 * pick an action and randomly assign it to a cluster.  
			 * then calculate the mean
			 */
			ArrayList<Integer> unassignedactions = new ArrayList<Integer>();

			for(int i=0;i<numberofactions[player];i++)
			{
				unassignedactions.add(i+1);
			}
			// iterate through all the actions and assign to a cluster randomly
			for(int i=0; i<numberofclusters; i++)
			{

				for(int j=0; j<(numberofactions[player]/numberofclusters); j++) //slot, it ensures that every cluster has equal number of actions for random partition
				{

					if(unassignedactions.size()>1)
					{
						int chosenactionindex = randInt(1, unassignedactions.size()) -1;
						int z = unassignedactions.get(chosenactionindex);
						assignToCluster(clusters, unassignedactions.get(chosenactionindex), i, numberofactions[player], player, mg);
						//	Logger.log("\n random paritiion<<<>>>>> Action "+ z+ " of player "+ player+" is assigned to cluster "+i, false);
						unassignedactions.remove(chosenactionindex);
					}
					else if(unassignedactions.size() ==1)
					{
						assignToCluster(clusters, unassignedactions.get(0), i, numberofactions[player], player, mg);
						int x = unassignedactions.get(0);
						//	Logger.log("\n random paritiion<<<>>>>> Action "+x + " of player "+ player+" is assigned to cluster "+i, false);
						unassignedactions.remove(0); //remove the last element
						break;
					}

				}
				/*
				 * minindex has the index for cluster
				 * make a tuple like (action, payoffs1, payoff2,...)
				 */

			}  // end of for loop

			//check if there are any actions remained unassigned
			if(unassignedactions.size() != 0)
			{
				for(Integer x: unassignedactions)
				{
					int a = numberofclusters-1;
					assignToCluster(clusters, x, numberofclusters-1, numberofactions[player], player, mg); // assign all the remainning actions to the last cluster.
					//	Logger.log("\n random paritiion<<<>>>>> Action "+ x+ " of player "+ player+" is assigned to cluster "+a, false);

				}

			}
			// print the clusters..
			//Logger.log("\n\nInitialization to clusters for player "+ player + ":\n", false);
			/*for(int i=0; i<clusters.length; i++)
			{
				Logger.log("Cluster "+ i, false);

				for(Double[] x: clusters[i])
				{
					Logger.log(x[0]+", ", false);
				}
				Logger.log("\n", false);
			}*/
			calculateClusterMean(clusters, clusterpoints, numberofactions[player], numberofclusters, player, mg);

		}




		while(true)
		{
			System.out.println("\nIteration: "+ runningInstance);

			runningInstance++;

			if(runningInstance>=100)
			{

				List<Integer>[] finalcluster = new List[numberofclusters];
				for(int i=0; i< numberofclusters; i++){

					finalcluster[i] = new ArrayList<Integer>(); 
				}
				for(int i=0; i<numberofclusters; i++)
				{
					for(Double[] x: clusters[i])
					{
						finalcluster[i].add(x[0].intValue());
					}
				}

				return finalcluster;
			}



			//copy the cluster points to old cluster points.

			//System.out.println("\nIteration: "+ runningInstance + " Copying clusterpoints");
			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j<numberofactions[opponent]; j++)
				{
					oldclusterpoints[i][j] = clusterpoints[i][j];

				}

			}

			//	System.out.print("\n\nIteration: "+ runningInstance + " ");
			/*Logger.log("\n\nK-mean Iteration: "+ runningInstance  +" plauer "+player+"  cluster points\n", false);
			for(int i =0; i< numberofclusters; i++)
			{

				//	System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< numberofactions[opponent]; j++){

					//	System.out.print(" "+oldclusterpoints[i][j]); 
					Logger.log(" "+oldclusterpoints[i][j], false);

				}
				//	System.out.print("\n");
				Logger.log("\n", false);
			}*/
			// now clear/create e cluster object the new cluster for a new iteration. 
			for(int i=0; i< numberofclusters; i++)
			{
				clusters[i]= new ArrayList<Double[]>(); //.clear();
			}
			/*
			 * Now iterate over all the possible action touples for player 1. 
			 * calclate the difference from cluster points
			 * assign to the cluster with the minimum difference.
			 *  
			 */
			for(int i = 0; i < numberofactions[player]; i++)
			{
				for(int j = 0; j < numberofactions[opponent]; j++)
				{
					int outcome[] = new int [2];
					if(player ==0){

						outcome[0] = i+1;
						outcome[1] = j+1;
					}
					else if(player == 1)
					{
						outcome[0] = j+1;
						outcome[1] = i+1;
					}
					double tmppayoff = mg.getPayoff(outcome, player); //get the payoff for player 1 or player 2
					for(int k =0; k<numberofclusters; k++)
					{
						/*
						 * calculate the differences of payoffs for each cluster points 
						 */
						if(KmeanClustering.isDistMetricLine())
						{
							//	Logger.log("\n entered DistMetricLine() ", false);

							if((tmppayoff < 0 && clusterpoints[k][j] < 0) ||  (tmppayoff >=0  && clusterpoints[k][j] >= 0))

							{
								distancefromcluster[k][j] = Math.abs((Math.abs(tmppayoff) - Math.abs(clusterpoints[k][j])));
							}
							else if((tmppayoff >= 0 && clusterpoints[k][j] < 0))
							{
								distancefromcluster[k][j] = (tmppayoff + Math.abs(clusterpoints[k][j]));
							}
							else if((tmppayoff < 0 && clusterpoints[k][j] >= 0))
							{
								distancefromcluster[k][j] = (clusterpoints[k][j]  + Math.abs(tmppayoff));
							}
						}
						/*
						 * calculate euclidean distance: first take the squares of difference....
						 */
						if(KmeanClustering.isDistMetricEuclidean())
						{
							//	Logger.log("\n entered DistMetricEuclidean() ", false);
							distancefromcluster[k][j] = (clusterpoints[k][j]  - (tmppayoff));
							distancefromcluster[k][j] = distancefromcluster[k][j] * distancefromcluster[k][j];
						}

					}

				}

				double min = Double.POSITIVE_INFINITY;
				int minindex = 0;
				if(KmeanClustering.isSumDist())
				{

					//
					//Logger.log("\n entered SumDist() ", false);
					/*
					 * Here you have all the differences for action i 
					 * calculate the sum of the differences
					 * Then find the minimum sum
					 * 
					 */
					for(int l =0; l< numberofclusters; l++)
					{
						sumofdifferences[l] = 0;
						for(int m =0; m< distancefromcluster[l].length; m++)
						{
							sumofdifferences[l] += distancefromcluster[l][m];
						}
						int a = i+1;
						//Logger.log("\n Action "+ a+"'s sum distance from cluster "+l+" is : "+sumofdifferences[l] , false);

					}
					for(int n =0; n< sumofdifferences.length; n++)
					{
						if(min > sumofdifferences[n])
						{
							min = sumofdifferences[n];
							minindex = n;
						}
					}

				}
				if(KmeanClustering.isMaxDist())
				{

					//Logger.log("\n entered MaxDist() ", false);

					/*
					 * calculate the max difference instead of summing them
					 */
					for(int l =0; l< numberofclusters; l++)
					{

						double maxdiff =Double.NEGATIVE_INFINITY;
						for(int m =0; m< numberofactions[opponent]; m++)
						{
							if(maxdiff<distancefromcluster[l][m])
							{
								maxdiff = distancefromcluster[l][m];
							}

						}

						maxdifference[l] = maxdiff;
						int a = i+1;
						//Logger.log("\n Action "+ a+"'s max distance from cluster "+l+" is : "+maxdifference[l] , false);

					}
					/*
					 * find the minimum difference among the maximum differences
					 */
					for(int n =0; n< maxdifference.length; n++)
					{
						if(min > maxdifference[n])
						{
							min = maxdifference[n];
							minindex = n;
						}
					}
				}
				if(KmeanClustering.isDistMetricEuclidean())
				{
					//System.out.println("\nIteration: "+ runningInstance + "entered DistMetricEuclidean() ");

					//Logger.log("\n entered DistMetricEuclidean() ", false);
					/*
					 * find the squared root distances
					 */

					for(int l =0; l< numberofclusters; l++)
					{
						squaredrootdifference[l] = 0;
						for(int m =0; m< distancefromcluster[l].length; m++)
						{
							squaredrootdifference[l] += distancefromcluster[l][m];

						}
						squaredrootdifference[l] = Math.sqrt(squaredrootdifference[l]);
						int a = i+1;
						//Logger.log("\n Action "+ a+"'s euclidean distance from cluster "+ l+ " : "+squaredrootdifference[l], false);
					}
					// find the minimum squared root distance
					for(int n =0; n< squaredrootdifference.length; n++)
					{
						if(min > squaredrootdifference[n])
						{
							min = squaredrootdifference[n];
							minindex = n;
						}
					}

				}
				/*
				 * 
				 * assign the action i+1 to minindex cluster
				 */
				int a = i+1;
				//Logger.log("\nAction "+ a+" is assigned to cluster :"+minindex , false);
				//System.out.println("\nIteration: "+ runningInstance + "Assigning cluster points ");
				assignToCluster(clusters, i+1, minindex, numberofactions[player], player, mg);

			}  // end of outer for loop

			/*Logger.log("\nActions in clusters for the mean\n", false);
			for(int i=0; i< numberofclusters; i++)
			{
				//	System.out.print("Cluster: " + i + " "+ "Actions: ");
				Logger.log("Cluster: " + i + " "+ "Actions: ", false);
				for(Double[] x : clusters[i]){
					//	System.out.print(x[0] + " ");
					Logger.log(x[0] + " ", false);

				}
				//if(runningInstance != RUNNING_FOR)
				//clusters[i].clear();
				//	System.out.print("\n");
				Logger.log("\n", false);
			}*/
			/*
			 * now recalculate the cluster points
			 */
			//	System.out.println("\nIteration: "+ runningInstance + "Calculating mean ");
			calculateClusterMean(clusters, clusterpoints, numberofactions[player], numberofclusters, player, mg);
			//	System.out.print("\n\nIteration: "+ runningInstance + " ");
			//Logger.log("\n\nK-mean Iteration: "+ runningInstance  +" player "+player+" new cluster points(mean)\n", false);
			for(int i =0; i< numberofclusters; i++)
			{

				//	System.out.print("Cluster: "+ i + " ");
				//Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< numberofactions[opponent]; j++)
				{

					//		System.out.print(" "+clusterpoints[i][j]); 
					Logger.log(" "+clusterpoints[i][j], false);

				}
				//	System.out.print("\n");
				Logger.log("\n", false);
			}
			//System.out.println("\nIteration: "+ runningInstance + "Checking or stop ");
			boolean checkforstop = true;
			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j<numberofactions[opponent]; j++)
				{
					if(clusterpoints[i][j] != oldclusterpoints[i][j])
					{
						checkforstop = false;
						break;
					}
				}
				if(checkforstop==false)
				{
					break;
				}
			}

			//System.out.println("\nIteration: "+ runningInstance + "Checking exit condition ");
			if(checkforstop == true && noClusterIsEmpty(clusters))
			{
				//System.out.println("\nIteration: "+ runningInstance + "Ext ");
				break;
			}
			Logger.log("\n\n", false);

		}    // end of while loop 	

		PrintWriter pw = null;
		String actions = "K"+numberofclusters+"-"+player+"-"+0+","+numberofactions[player]+",";
		try {
			pw = new PrintWriter("abstractions"+player+".txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i =0; i< numberofclusters; i++)
		{
			int tmpcounter = 0; //keep track of how many actions we need. dont add -/hyphen for the last action
			for(Double[] x: clusters[i])
			{
				String tmp = String.valueOf(x[0]);
				actions = actions+ tmp;
				tmpcounter++;
				if(tmpcounter < clusters[i].size())
				{
					actions = actions+"-";
				}
			}
			if( (i+1) != numberofclusters) //add only when it's not the last action of the last cluster
			{
				actions = actions+",";
			}
		}
		pw.write(actions);
		pw.close();

		List<Integer>[] finalcluster = new List[numberofclusters];
		for(int i=0; i< numberofclusters; i++){

			finalcluster[i] = new ArrayList<Integer>(); 
		}
		for(int i=0; i<numberofclusters; i++)
		{
			for(Double[] x: clusters[i])
			{
				finalcluster[i].add(x[0].intValue());
			}
		}

		return finalcluster;


	}


	/**
	 * 
	 * @param clusters contains the cluster actions
	 * @return true if no cluster is empty
	 */
	private static boolean noClusterIsEmpty(List<Double[]>[] clusters) 
	{

		for(int i=0; i<clusters.length; i++)
		{
			if(clusters[i].size()==0)
				return false;
		}

		return true;
	}




	public static int randInt(int min, int max) {

		// Usually this should be a field rather than a method variable so
		// that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}




	public static void assignToCluster(List<Double[]>[] clusters, int actiontoassign,
			int assignedcluster, int numberofactions, int player, MatrixGame mg )
	{

		int opponent = 1^player;
		int oppnumaction = mg.getNumActions(opponent);
		Double[] tupleincluster = new Double[oppnumaction+1]; // +1 for the action
		tupleincluster[0] = (double)actiontoassign; //the action in the first index
		/*

		 * now assign the payoffs

		 */
		int[] tmpoutcome = new int[2];
		for(int p = 0; p< oppnumaction; p++)
		{
			if(player == 0)
			{
				tmpoutcome[0] = actiontoassign;
				tmpoutcome[1] = p+1;

			}
			else if(player == 1)
			{
				tmpoutcome[0] = p+1;
				tmpoutcome[1] = actiontoassign;

			}
			tupleincluster[p+1] = mg.getPayoff(tmpoutcome, player); 

		}
		clusters[assignedcluster].add(tupleincluster); 
	}



	public static  void calculateClusterMean(List<Double[]>[] clusters, 
			double[][] clusterpoints, int numberofactions, 
			int numberofclusters, int player, MatrixGame mg)
	{
		/*

		 * now recalculate the cluster mean

		 */
		int opponent = 1^player;
		double average = 0;
		for(int i = 0; i< numberofclusters; i++)
		{
			int clustersize = clusters[i].size();
			if(clustersize==0)
			{
				System.out.println("\n\nEmpty cluster: "+ i + " ");
				Logger.log("\n cluster: "+ i + " is empty, points before reassignment:\n", false);
				for(int k=0; k<mg.getNumActions(opponent); k++)
				{
					Logger.log(" "+clusterpoints[i][k], false);

				}
				int randomaction;
				//	ArrayList<Integer> alreadychecked = new ArrayList<Integer>();
				while(true)
				{



					// if cluster is empty, assign random points from the strategy
					randomaction = randInt(1,numberofactions );
					//check if the payoffs are same as centroid of another cluster

					System.out.println(".....");
					//need to test
					//if(checkIfActionIsOkToBeACentroid(mg, clusterpoints, randomaction, i, player))
					{
						break;
					}



					/*// if cluster is empty, assign random points from the strategy
					randomaction = randInt(1,numberofactions );
					System.out.println(".....");
					if((alreadychecked.size()==0) || (alreadychecked.size()>0 && !alreadychecked.contains(randomaction)))
					{
						alreadychecked.add(randomaction);
						if(checkIfActionIsOkToBeACentroid(mg, clusterpoints, randomaction, i, player))
						{

							break;

						}
					}
					//check if the payoffs are same as centroid of another cluster
					 */

				}
				Logger.log("\nAction "+ randomaction+"'s payoffs are assigned to cluster "+ i, false);
				for(int j =0; j<mg.getNumActions(opponent); j++)
				{

					int[] outcome={0,0};
					if(player==0)
					{

						outcome[0]= randomaction;
						outcome[1]= j+1;

					}
					else if(player==1)
					{

						outcome[0]= j+1;
						outcome[1]= randomaction;

					}
					double payoff = mg.getPayoff(outcome, player);
					clusterpoints[i][j] = payoff;

				}
				Logger.log("\n cluster: "+ i + " is empty, points after reassignment:\n", false);
				for(int k=0; k<mg.getNumActions(opponent); k++)
				{

					Logger.log(" "+clusterpoints[i][k], false);

				}

			}

			else if(clustersize>0)
			{
				for(int j = 0; j< mg.getNumActions(opponent); j++)
				{

					average = 0;   // corrected, average should be reset after calculating for every action
					for(Double[] x: clusters[i])
					{
						average += x[j+1]; 

					}
					if(clustersize != 0)
					{
						clusterpoints[i][j] = average/clustersize; 

					}

				}

			}

		}

	}
	
	
	



	public static boolean checkIfActionIsOkToBeACentroid(Game mg, double[][] clusterpoints, 
			int actiontocheck, int emptycluster, int player)

	{
		boolean flag = false;
		int[] outcome = new int[2];
		int opponent = 1^player;

		for(int i=0; i<clusterpoints.length; i++)
		{

			if(i!=emptycluster)
			{
				flag = false;
				for(int j=0; j<mg.getNumActions(opponent); j++)
				{
					if(player==0)
					{
						outcome[0] = actiontocheck;
						outcome[1] = j+1;
					}
					else if(player==1)
					{
						outcome[0] = j+1;
						outcome[1] = actiontocheck;
					}
					if(clusterpoints[i][j] != mg.getPayoff(outcome, player))
					{

						flag = true;
						break;
					}
				}
				if(flag== false)
				{
					return false;

				}

			}

		}
		return true;

	}

	/**
	 * 
	 * @param game
	 * @param cluster cluster for both players
	 * @param player
	 * @param max max or average delta
	 * @return
	 */
	public static double calculateDelta(Game game, List<Integer>[][] cluster, int player, boolean max)
	{

		//	System.out.println("Staring epsilon calcl**************");
		int[] numactions = game.getNumActions();

		double[] deltas = new double[cluster[player].length]; // there are deltas for each cluster. 
		int opponent =0;
		if(player==0)
			opponent =1;
		/*
		 * for each cluster take the actions and calcualte the delta
		 */
		for(int i=0; i< cluster[player].length; i++) // can be improved, i<cluster.length-1
		{

			double maxdiffplayer =0;

			for(Integer x : cluster[player][i]  ) // x[0] is the action
			{
				for(Integer y: cluster[player][i]) // can be improved , cluster[i+1]
				{
					if(cluster[player][i].indexOf(x)!=cluster[player][i].indexOf(y))// dont want to calculate difference between same actions
					{

						// now iterate over payoffs for action x[0] and y[0]

						for(int z =1; z<= numactions[opponent]; z++)
						{
							if(!cluster[opponent][i].contains(z)) // if  cluster i does not have z
							{
								int[] outcome1 = new int[2];
								int[] outcome2 = new int[2];
								if(player==0)
								{
									outcome1[0] = x;
									outcome1[1] =  z;
									outcome2[0] = y;
									outcome2[1] =  z;

								}
								else if(player==1)
								{
									outcome1[1] = x;
									outcome1[0] =  z;
									outcome2[1] = y;
									outcome2[0] =  z;
								}

								double payoff1= game.getPayoff(outcome1, player);
								double payoff2 = game.getPayoff(outcome2, player);
								double diff =0;
								if((payoff1<0 && payoff2< 0) || (payoff1>=0 && payoff2>=0))
								{
									diff = Math.abs(Math.abs(payoff2) - Math.abs(payoff1));

								}
								else if(payoff1<0 && payoff2>= 0)
								{
									diff = Math.abs(Math.abs(payoff1) + payoff2);
								}
								else if(payoff1>=0 && payoff2< 0)
								{
									diff = Math.abs(Math.abs(payoff2) + payoff1);
								}


								if(diff>maxdiffplayer)
									maxdiffplayer= diff;

							}


						}


					}
				}// inner cluster loop


			} // outer cluster loop

			deltas[i] = maxdiffplayer;
		}



		if(max==true)
		{
			//int worstcluster =-1;
			double maximum = Double.NEGATIVE_INFINITY;

			for(int i=0; i< deltas.length; i++)
			{
				if(deltas[i]>maximum)
				{
					//worstcluster = i;
					maximum = deltas[i];
				}
			}
			//double[] deltawithcluster = {maximum, worstcluster};
			if(maximum>100.00)
			{
				System.out.println();
			}

			return maximum;

		}
		else
		{

			/*int worstcluster =-1;
			double maximum = Double.NEGATIVE_INFINITY;

			for(int i=0; i< deltas.length; i++)
			{
				if(deltas[i]>maximum)
				{
					worstcluster = i;
					maximum = deltas[i];
				}
			}

			 */
			double sum = 0.0;

			for(int i=0; i< deltas.length; i++)
			{
				sum+=deltas[i];
			}

			sum= sum/deltas.length;

			//double[] deltawithcluster = {sum, worstcluster};
			return sum;

			//return deltawithcluster;


		}

	}





	public static double[][] doKmean(int numberofclusters, MatrixGame mg, String gamename)
	{

		/*
		 * for random restart we need to save the clusterings... and deltas...
		 */
		HashMap<Integer,List<Integer>[]> clustersplayer1 = new HashMap<Integer, List<Integer>[]>();
		HashMap<Integer,List<Integer>[]> clustersplayer2 = new HashMap<Integer, List<Integer>[]>();
		HashMap <Integer, Double> deltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> deltasplayer2 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer2 = new HashMap<Integer, Double>();
		double[][] result = new double[3][2];
		final  int RANDOM_RESTART_ITERATION = 6; // 
		String game = gamename;
		GamutModifier gmforIED = new GamutModifier(game);
		//test
		//	GamutModifier gmforIED = new GamutModifier("game");
		/*
		 * print the original game in logfile
		 */
		Logger.logit("\n\n Original Game\n\n");
		/*	String logfile = Parameters.GAME_FILES_PATH+"logfile"+".log";
       // String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		 try{

			    PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));
	            SimpleOutput.writeGame(pw,gmforIED.returnGame());
	            pw.close();
	        }
	        catch(Exception ex){
	            System.out.println("GAmutMOdifier class :something went terribly wrong during log file creation ");
	        }
		 */
		////////////////////
		int[] actionsbeforeied = gmforIED.returnGame().getNumActions();

		//test
		//	MatrixGame gamewodominatedstrategies = gmforIED.returnGame();//IEDSMatrixGames.IEDS(gmforIED.returnGame()); 
		Game gamewodominatedstrategies = IEDSMatrixGames.IEDS(gmforIED.returnGame()); 
		int[] numberofactionafterIED = gamewodominatedstrategies.getNumActions();
		//KmeanClustering.percremovedstrategy += GamutModifier.calcPercRemovedStrateg(numberofactionafterIED, actionsbeforeied);
		if(numberofactionafterIED[0] ==1 && numberofactionafterIED[1]==1)
		{
			Logger.log("Equilibrium reached after IED", false);
			return result;
		}
		else if(numberofactionafterIED[0]<=numberofclusters || numberofactionafterIED[1]<=numberofclusters)
		{
			Logger.log("After IED number of action in game "+game+ "is less than number of clusters "+ numberofclusters, false);

			Logger.logit("\n After IED number of action in game "+game+ "is less than number of clusters "+ numberofclusters);

			int min = 999999;

			for(int i=0; i< numberofactionafterIED.length; i++)
			{
				if(numberofactionafterIED[i]<min)
				{
					min = numberofactionafterIED[i];
				}
			}
			//	 first make the game file
			String absgamename = Parameters.GAME_FILES_PATH+"k"+numberofclusters+"-"+game+Parameters.GAMUT_GAME_EXTENSION;
			String absgmname = "k"+numberofclusters+"-"+game;

			try{

				PrintWriter pw = new PrintWriter(absgamename,"UTF-8");
				SimpleOutput.writeGame(pw,gamewodominatedstrategies);
				pw.close();
			}
			catch(Exception ex){
				System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
			}

			// * now solve the game
			QRESolver qre = new QRESolver(100);

			GamutModifier absgm = new GamutModifier(Parameters.GAME_FILES_PATH+absgmname);

			EmpiricalMatrixGame emg = new EmpiricalMatrixGame(absgm.returnGame());
			qre.setDecisionMode(QRESolver.DecisionMode.RAW);
			//	System.out.println(qre.solveGame(emg, 0));
			//	System.out.println(qre.solveGame(emg, 1));
			MixedStrategy abstractgamemixedstrategy1 = qre.solveGame(emg, 0);
			MixedStrategy abstractgamemixedstrategy2 = qre.solveGame(emg, 1);




			//	 * now calculate epsilon




			List<MixedStrategy> strategylist = new ArrayList<MixedStrategy>();
			strategylist.add(abstractgamemixedstrategy1);
			strategylist.add(abstractgamemixedstrategy2);


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution originaldistro = new OutcomeDistribution(strategylist);
			double[]  originalexpectedpayoff = SolverUtils.computeOutcomePayoffs(absgm.returnGame(), originaldistro);

			double epsilonz = SolverUtils.computeOutcomeStability(absgm.returnGame(), originaldistro);
			Logger.log("\n Expected Payoff original game player 0 : "+ originalexpectedpayoff[0]+ "Expected Payoff original game player 1 : "+ originalexpectedpayoff[1], false);


			result[0][0] = 0; // max deltas
			result[0][1] = epsilonz;

			result[1][0] = 0;
			result[1][1] = epsilonz;

			result[2][0] = 0;
			result[2][1] = epsilonz;	

			//	return result;




		}
		else if(numberofactionafterIED[0]>numberofclusters && numberofactionafterIED[1]>numberofclusters)
		{

			Logger.log("After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters, false);
			Logger.logit("\n After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters);

			for(int i=0;i<2;i++)
			{
				double[] val = mg.getExtremePayoffs(i);

				Logger.log("\n player "+i+" extreme payoffs "+ val[0]+" "+val[1], false);

			}


			List<Integer>[][] clusterforplayers = new List[mg.getNumPlayers()][numberofclusters];


			for(int randomitr =0; randomitr<RANDOM_RESTART_ITERATION; randomitr++)
			{	


				if(randomitr<3)
				{
					KmeanClustering.setRAND_ACTION_INIT_TO_CLUSTERS(true);
					KmeanClustering.setRAND_POINTS_FROM_OBSERVATION(false);

				}
				else
				{
					KmeanClustering.setRAND_ACTION_INIT_TO_CLUSTERS(false);
					KmeanClustering.setRAND_POINTS_FROM_OBSERVATION(true);

				}


				clusterforplayers[0] = clusterActions(numberofclusters, 0, mg);
				clusterforplayers[1] = clusterActions(numberofclusters, 1, mg);



				//clusterforplayer2 = gm.clusterActions(numberofclusters, 1);



				/*
				 * calculate the delta, for random restarts
				 */



				/*
				Logger.log("\n\n Player 0 clusters after CAA\n", false);


				for(int i=0; i<clusterforplayers[0].length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(Integer x: clusterforplayers[0][i])
					{
						Logger.log(x+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);


				Logger.log("\n\nPlayer 1 clusters after CAA\n ", false);


				for(int i=0; i<clusterforplayers[1].length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(Integer x: clusterforplayers[1][i])
					{
						Logger.log(x+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);



				 * do the code for max delta
				 * 

				 */
				if( KmeanClustering.isMaxDelta())
				{

					double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, true);
					double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, true);




					deltasplayer1.put(randomitr, delta1);
					deltasplayer2.put(randomitr, delta2);

					Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ delta1, false);
					Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ delta2, false);


				}

				if(KmeanClustering.isAvrgDelta())
				{


					double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, false);
					double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, false);





					deltasplayer1.put(randomitr, delta1);
					deltasplayer2.put(randomitr, delta2);

					Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ delta1, false);
					Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ delta2, false);



					/*
					 * also need to calculate the max delta to show in the graph
					 */


					double maxdelta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, true);
					double maxdelta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, true);





					/*
					 * now put the max deltas in a hashmap
					 */
					maxdeltasplayer1.put(randomitr, maxdelta1);
					maxdeltasplayer2.put(randomitr, maxdelta2);

					Logger.log("\nplayer 0 MaxDelta random iteration "+randomitr+" : "+ maxdelta1, false);
					Logger.log("\nplayer 1 MaxDelta random iteration "+randomitr+" : "+ maxdelta2, false);


				}




				/*System.out.print("\n\n");
			for(int i=0; i<gm.returnGame().getNumActions(0); i++)
			{
				for(int j=0; j< gm.returnGame().getNumActions(1); j++)
				{
					int[] outcome = {i+1,j+1};
					System.out.print(" "+ gm.returnGame().getPayoff(outcome, 0));
				}
				System.out.print("\n");
			}*/



				/*
				 * save the clusterings and deltas
				 */


				clustersplayer1.put(randomitr, clusterforplayers[0]);
				clustersplayer2.put(randomitr, clusterforplayers[1]);



			} // end of random iteration loop





			/*
			 * now find the best delta. minimum one. 
			 */


			Logger.log("\n Selecting minimum delta", false);
			double[] mindeltas = new double[mg.getNumPlayers()]; // will contain the minimum delta for 2 players
			double min = Double.POSITIVE_INFINITY;
			int minindex = 0;
			double[] maxdeltas = new double[mindeltas.length];


			for(int i =0; i< mindeltas.length; i++) // implemented for 2 players 
			{
				min = Double.POSITIVE_INFINITY;
				minindex = 0;

				for(int j=0; j<RANDOM_RESTART_ITERATION; j++) //there are RANDOM_RESTART_ITERATION # of deltas
				{

					if(i==0)  //player 1
					{
						if(min>deltasplayer1.get(j))
						{
							min = deltasplayer1.get(j);
							minindex = j;
						}
					}
					else if(i==1) // player 2
					{
						if(min>deltasplayer2.get(j))
						{
							min = deltasplayer2.get(j);
							minindex = j;
						}
					}

				}

				mindeltas[i] = min;


				if(i==0) // player 1
				{


					clusterforplayers[0] = clustersplayer1.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's delta and clustering is used for player "+ i, false );


					if(KmeanClustering.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer1.get(minindex);
						Logger.log("\n Player 0 max delta for the selected cluster : "+maxdeltas[i] , false);
					}	


					Logger.log("\n\n Final clustering for Player 0\n", false);


					for(int k=0; k<clusterforplayers[0].length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(Integer x: clusterforplayers[0][k])
						{
							Logger.log(x+", ", false);
						}
						Logger.log("\n", false);
					}




				}
				else if(i==1) // player 2
				{

					clusterforplayers[1] = clustersplayer2.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's, delta and clustering is used for player "+ i, false );

					if(KmeanClustering.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer2.get(minindex);
						Logger.log("\n Player 1 max delta for the selected cluster : "+maxdeltas[i] , false);
					}


					Logger.log("\n\n Final clustering for Player 1\n", false);


					for(int k=0; k<clusterforplayers[1].length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(Integer x: clusterforplayers[1][k])
						{
							Logger.log(x+", ", false);
						}
						Logger.log("\n", false);
					}



				}



			}





			Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);



			Logger.log("\n clustering done################",false);

			int[] numberofclustersforeachplayer = new int[mg.getNumPlayers()];
			for(int i =0; i< mg.getNumPlayers(); i++)
			{
				numberofclustersforeachplayer[i] = numberofclusters;
			}


			/* For the strategy map
			 * 1. give the constructor appropriate variables.
			 * 2. pass the cluster mapping to the strategy map or pass the array, which contain the cluster number for each actions, for each player

			 */	
			//		System.out.println("Staring strategy mapping%%%%%%%%%%%%%%");

			StrategyMapping strategymap = new StrategyMapping(mg.getNumPlayers(), mg.getNumActions(), numberofclustersforeachplayer,mg, gamename);


			strategymap.mapActions(clusterforplayers[0], 0);
			strategymap.mapActions(clusterforplayers[1], 1);

			//int[] x = {0,1,2,0,1,2,1,0,2,2,3,3,0,2,1};

			//strategymap.mapActions(x, 1);

			Logger.log("\nend strategy mapping%%%%%%%%%%%%%%", false);

			Logger.log("\nStaring building abstract game%%%%%%%%%%%%%%", false);

			//	String abstractedgamename = strategymap.buildAbstractedGame();
			String abstractedgamename = "";//strategymap.makeAbstractGame();




			QRESolver qre = new QRESolver(100);

			GamutModifier absgm = new GamutModifier(abstractedgamename);




			/*
			 * 
			 * print the abstract game in logfile
			 */


			/*	Logger.logit("\n\nAbstract Game\n\n");


			 try{

				    PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));
		            SimpleOutput.writeGame(pw,absgm.returnGame());
		            pw.close();
		        }
		        catch(Exception ex){
		            System.out.println("GAmutMOdifier class :something went terribly wrong during log file creation ");
		        }
			 */



			/*
			 * use the qre solver 
			 */

			EmpiricalMatrixGame emg = new EmpiricalMatrixGame(absgm.returnGame());
			qre.setDecisionMode(QRESolver.DecisionMode.RAW);
			//	System.out.println(qre.solveGame(emg, 0));
			//	System.out.println(qre.solveGame(emg, 1));


			MixedStrategy abstractqreprofile1 = qre.solveGame(emg, 0);
			MixedStrategy abstractqreprofile2 = qre.solveGame(emg, 1);


			//	double[] originalqreprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile1, mg.getNumActions(0), numberofclusters, 0);
			//	double[] originalqreprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile2, mg.getNumActions(1), numberofclusters, 1);



			/*double[] orgqreprbpl1 = new double[originalqreprofile1.length+1];

			orgqreprbpl1[0] =0;
			int index0 =1;
			for(double x: originalqreprofile1)
			{
				orgqreprbpl1[index0++] = x;
			}


			double[] orgqreprbpl2 = new double[originalqreprofile2.length+1];

			orgqreprbpl2[0] =0;
			int index01 =1;
			for(double x: originalqreprofile2)
			{
				orgqreprbpl2[index01++] = x;
			}*/


			/*MixedStrategy origqreprofile1 = new MixedStrategy(orgqreprbpl1);
			MixedStrategy origqreprofile2 = new MixedStrategy(orgqreprbpl2);
			 */

			List<MixedStrategy> originalqrelist = new ArrayList<MixedStrategy>();
			/*originalqrelist.add(origqreprofile1);
			originalqrelist.add(origqreprofile2);
			 */


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origqredistro = new OutcomeDistribution(originalqrelist);
			double[]  originalqrepayoff = SolverUtils.computeOutcomePayoffs(mg, origqredistro);

			double qreepsilon = SolverUtils.computeOutcomeStability(mg, origqredistro);
			Logger.logit("\n Expected Payoff for qre profile player 0 : "+ originalqrepayoff[0]+ "Expected Payoff original game player 1 : "+ originalqrepayoff[1]);

			Logger.logit("\n Final EPsilon for qre profile "+ qreepsilon);












			////////////////////////////////





			/*
			 * SUbgame NE
			 * 1. Build the abstract game. 
			 * 2. Use the method for subgame NE
			 */


			/*	Game abstractsubgame = strategymap.recAbstract(numberofclusters);


			// some method needs to be used to find the eqlbrm strategy in abstract game

			QRESolver qresubgame = new QRESolver(100);



			EmpiricalMatrixGame emsubgame = new EmpiricalMatrixGame(abstractsubgame);
			qresubgame.setDecisionMode(QRESolver.DecisionMode.RAW);


			MixedStrategy subgameprofile1 = qresubgame.solveGame(emsubgame, 0);
			MixedStrategy subgameprofile2 = qresubgame.solveGame(emsubgame, 1);





			MixedStrategy[] sol = {subgameprofile1, subgameprofile2};

			System.out.println("subgameprofile1.checkIfNormalized() "+ subgameprofile1.checkIfNormalized());
			System.out.println("subgameprofile2.checkIfNormalized() "+ subgameprofile2.checkIfNormalized());



			MixedStrategy[] origsbgmprofile = strategymap.getStrategySubgameSols(sol); 


			System.out.println("origsbgmprofile[0].checkIfNormalized() "+ origsbgmprofile[0].checkIfNormalized());
			System.out.println("origsbgmprofile[1].checkIfNormalized() "+ origsbgmprofile[1].checkIfNormalized());




			List<MixedStrategy> originalsbgmlist = new ArrayList<MixedStrategy>();
			originalsbgmlist.add(origsbgmprofile[0]);
			originalsbgmlist.add(origsbgmprofile[1]);



			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origsbgmdistro = new OutcomeDistribution(originalsbgmlist);
			double[]  originalsbgmpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origsbgmdistro);

			double sbgmepsilon = SolverUtils.computeOutcomeStability(gm.returnGame(), origsbgmdistro);
			Logger.logit("\n Expected Payoff for subgm profile player 0 : "+ originalsbgmpayoff[0]+ "Expected Payoff original game player 1 : "+ originalsbgmpayoff[1]);

			Logger.logit("\n Final EPsilon for sbgame profile "+ sbgmepsilon);


			 */







			//////////////////////







			/*
			 * USe the CalcEpsilonBuondedEquilibrium class to calculate most robust strategy
			 * 1. call the constructor
			 * 
			 */

			/*
			CalcEpsilonBuondedEquilibrium solverBuondedEquilibrium = new CalcEpsilonBuondedEquilibrium(strategymap, absgm.returnGame());
			solverBuondedEquilibrium.calcMaxEpsilon();
			MixedStrategy abstractgamemixedstrategy1 =  solverBuondedEquilibrium.getEpsilonBoundedEq(0);
			MixedStrategy abstractgamemixedstrategy2 = solverBuondedEquilibrium.getEpsilonBoundedEq(1);

			 */











			/*	

			Game gmwithmaxexpectedpayoff = GamutModifier.getGameWithMaxExpectedPayoff(absgm.returnGame(), strategymap);


			MixedStrategy abstractgamemixedstrategy1 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithmaxexpectedpayoff).get(0);
			MixedStrategy abstractgamemixedstrategy2 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithmaxexpectedpayoff).get(1);;


			 */



			/*

			String strategy1 = abstractgameneprofile1+"";
			String strategy2 = abstractgameneprofile2+"";


			Logger.log("\nPlayer 0 equilibrium strategy "+strategy1, false);
			Logger.log("\nPlayer 1 equilibrium strategy "+strategy2, false);


			List<MixedStrategy> list = new ArrayList<MixedStrategy>();
			list.add(abstractgameneprofile1);
			list.add(abstractgameneprofile2);


			MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution distro = new OutcomeDistribution(list);
			double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(g, distro);


			//	System.out.println("Expected Payoff player 0 : "+ expectedpayoff[0]+ "Expected Payoff player 1 : "+ expectedpayoff[1] );

			Logger.log("\n Expected Payoff abstract game player 0 : "+ expectedpayoff[0]+ "Expected Payoff abstract game player 1 : "+ expectedpayoff[1], false);

			 */




			/*
			 * calculate epsilon bounded original profile
			 */



			//	Game gmwithupperbound = GamutModifier.getGameWithUpperBound(absgm.returnGame(), strategymap);





			/*
			 * print the max epsilon bounded game
			 */



			/*Logger.logit("\n\nAbstract EPsilon bounded Game\n\n");


			 try{

				    PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));
		            SimpleOutput.writeGame(pw,gmwithupperbound);
		            pw.close();
		        }
		        catch(Exception ex){
		            System.out.println("GAmutMOdifier class :something went terribly wrong during log file creation ");
		        }



			 Logger.logit("\n Deviaitons for Upper bounded profile ");

			 */

			/*	MixedStrategy minepsilonprofile1 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithupperbound).get(0);
			MixedStrategy minepsilonprofile2 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithupperbound).get(1);;




			double[] originalepsilonboundedprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, minepsilonprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalepsilonboundedprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, minepsilonprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);

			double[] orgepsilonboundedprbpl1 = new double[originalepsilonboundedprofile1.length+1];

			orgepsilonboundedprbpl1[0] =0;
			int index0 =1;
			for(double x: originalepsilonboundedprofile1)
			{
				orgepsilonboundedprbpl1[index0++] = x;
			}


			double[] orgepsilonboundedprbpl2 = new double[originalepsilonboundedprofile2.length+1];

			orgepsilonboundedprbpl2[0] =0;
			int index20 =1;
			for(double x: originalepsilonboundedprofile2)
			{
				orgepsilonboundedprbpl2[index20++] = x;
			}


			MixedStrategy originalepsilonboundprofile1 = new MixedStrategy(orgepsilonboundedprbpl1);
			MixedStrategy originalepsilonboundprofile2 = new MixedStrategy(orgepsilonboundedprbpl2);


			List<MixedStrategy> originalepsilonboundedlist = new ArrayList<MixedStrategy>();
			originalepsilonboundedlist.add(originalepsilonboundprofile1);
			originalepsilonboundedlist.add(originalepsilonboundprofile2);


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origepsilonboundeddistro = new OutcomeDistribution(originalepsilonboundedlist);
			double[]  originalepsilonboundexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origepsilonboundeddistro);

			double epsilonforbounded = SolverUtils.computeOutcomeStability(gm.returnGame(), origepsilonboundeddistro);
			Logger.logit("\n Expected Payoff for upper Bounded profile player 0 : "+ originalepsilonboundexpectedpayoff[0]+ "Expected Payoff original game player 1 : "+ originalepsilonboundexpectedpayoff[1]);

			Logger.logit("\n Final EPsilon for Bounded profile "+ epsilonforbounded);


			 */



			/////////////////////






			/*
			 * calculate max expected profile
			 */




			/*
			 * for average abstract game, use the abstratc game returned by makeabstractgame()
			 */


			Game gmwithmaxexpectedpayoff = KmeanClustering.getGameWithMaxExpectedPayoff(absgm.returnGame(), strategymap);


			/*
			 * for subgame build the abstracted game by recAbstract(). then build a game with maxexpectedpayoff
			 */






			//Game gmmaxexpectedforsbgame = GamutModifier.getGameWithMaxExpectedPayoff(absgm.returnGame(), strategymap);






			/*Logger.logit("\n\nAbstract Max Expected payoff bounded Game\n\n");


			 try{

				    PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));
		            SimpleOutput.writeGame(pw,gmwithmaxexpectedpayoff);
		            pw.close();
		        }
		        catch(Exception ex){
		            System.out.println("GAmutMOdifier class :something went terribly wrong during log file creation ");
		        }



			 Logger.logit("\n Deviaitons for Max abstracted payoff ");
			 */


			MixedStrategy abstractmaxexpectedprofile1 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithmaxexpectedpayoff).get(0);
			MixedStrategy abstractmaxexpectedprofile2 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithmaxexpectedpayoff).get(1);;



			/*
			 * use the abstractsubgame for subgame mixedstrategy
			 */
			/*
			MixedStrategy[] abstractmaxexpectedprofile =  new MixedStrategy[2];



			abstractmaxexpectedprofile[0] = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractsubgame, gmmaxexpectedforsbgame).get(0);
			abstractmaxexpectedprofile[1] = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractsubgame, gmmaxexpectedforsbgame).get(1);



			System.out.println("abstractmaxexpectedprofile[0].checkIfNormalized() "+ abstractmaxexpectedprofile[0].checkIfNormalized());
			System.out.println("abstractmaxexpectedprofile[1].checkIfNormalized() "+ abstractmaxexpectedprofile[1].checkIfNormalized());

			 */

			/*
			 * build the original strategies for average abstract game
			 */


			//	double[] originalmaxexpectedprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile1, mg.getNumActions(0), numberofclusters, 0);
			//	double[] originalmaxexpectedprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile2, mg.getNumActions(1), numberofclusters, 1);


			/*
		//	double[] orgmaxexpectedprbpl1 = new double[originalmaxexpectedprofile1.length+1];

			orgmaxexpectedprbpl1[0] =0;
			int index2 =1;
			for(double x: originalmaxexpectedprofile1)
			{
				orgmaxexpectedprbpl1[index2++] = x;
			}
			 */
			/*
			double[] orgmaxexpectedprbpl2 = new double[originalmaxexpectedprofile2.length+1];

			orgmaxexpectedprbpl2[0] =0;
			int index22 =1;
			for(double x: originalmaxexpectedprofile2)
			{
				orgmaxexpectedprbpl2[index22++] = x;
			}

			 */





			/*
			MixedStrategy orginalmaxexpectedprofile1 = new MixedStrategy(orgmaxexpectedprbpl1);
			MixedStrategy orginalmaxexpectedprofile2 = new MixedStrategy(orgmaxexpectedprbpl2);



			 *//*
			 * build original game strategy for subgame
			 */


			/*	MixedStrategy[] orginalmaxexpectedprofile = strategymap.getStrategySubgameSols(abstractmaxexpectedprofile);


			System.out.println("orginalmaxexpectedprofile[0].checkIfNormalized() "+ orginalmaxexpectedprofile[0].checkIfNormalized());
			System.out.println("orginalmaxexpectedprofile[1].checkIfNormalized() "+ orginalmaxexpectedprofile[1].checkIfNormalized());

			 */



			List<MixedStrategy> originalmaxexpectedlist = new ArrayList<MixedStrategy>();
			//	originalmaxexpectedlist.add(orginalmaxexpectedprofile1);
			//	originalmaxexpectedlist.add(orginalmaxexpectedprofile2);

			//	originalmaxexpectedlist.add(orginalmaxexpectedprofile[0]);
			//originalmaxexpectedlist.add(orginalmaxexpectedprofile[1]);


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origmaxexpecteddistro = new OutcomeDistribution(originalmaxexpectedlist);
			double[]  originalmaxexpectedpayoff = SolverUtils.computeOutcomePayoffs(mg, origmaxexpecteddistro);

			double epsilonmaxexpected = SolverUtils.computeOutcomeStability(mg, origmaxexpecteddistro);
			Logger.logit("\n Expected Payoff original game player 0 : "+ originalmaxexpectedpayoff[0]+ "Expected Payoff original game player 1 : "+ originalmaxexpectedpayoff[1]);

			Logger.logit("\n Final Epsilon for Max expected payoff "+ epsilonmaxexpected);




			////////////////////


			Logger.logit("\n Deviaitons for NE ");



			MixedStrategy abstractgameneprofile1 =  MinEpsilonBoundSolver.getPSNE(absgm.returnGame()).get(0);
			MixedStrategy abstractgameneprofile2 = MinEpsilonBoundSolver.getPSNE(absgm.returnGame()).get(1);


			/*
			 * use the abssubgame for subgaame psne
			 */

			/*	MixedStrategy[] abstractgameneprofile =  new MixedStrategy[2];

			abstractgameneprofile[0] = MinEpsilonBoundSolver.getPSNE(absgm.returnGame()).get(0);
			abstractgameneprofile[1] = MinEpsilonBoundSolver.getPSNE(absgm.returnGame()).get(1);

			System.out.println(" abstractgameneprofile[0].checkIfNormalized() "+ abstractgameneprofile[0].checkIfNormalized());
			System.out.println(" abstractgameneprofile[1].checkIfNormalized() "+ abstractgameneprofile[1].checkIfNormalized());

			 */







			/*
			 * calculate original game expected payoffs for NE profile 
			 */





			double[] originalactionprobsplayer1 = {0};//buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile1, mg.getNumActions(0), numberofclusters, 0);

			double[] originalactionprobsplayer2 = {0};//buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile2, mg.getNumActions(1), numberofclusters, 1); 


			double[] orgprbpl1 = new double[originalactionprobsplayer1.length+1];

			orgprbpl1[0] =0;
			int index =1;
			for(double x: originalactionprobsplayer1)
			{
				orgprbpl1[index++] = x;
			}


			double[] orgprbpl2 = new double[originalactionprobsplayer2.length+1];

			orgprbpl2[0] =0;
			int index23 =1;
			for(double x: originalactionprobsplayer2)
			{
				orgprbpl2[index23++] = x;
			}








			MixedStrategy originalmixedstrategyplayer1 = new MixedStrategy(orgprbpl1);
			MixedStrategy originalmixedstrategyplayer2 = new MixedStrategy(orgprbpl2);

			//	String str1 = originalmixedstrategyplayer1+ " ";
			//	String str2 = originalmixedstrategyplayer2+ " ";

			/*

			System.out.println( "strategy player 1: "+abstractgameneprofile1);
			System.out.println("strategy player 2: "+abstractgameneprofile2);



			for(int i=0;i<gm.returnGame().getNumPlayers(); i++)
			{
				for(int j=0; j<numberofclusters; j++)
				{
					Logger.log("\n Player "+ i+ " cluster "+ j + " size: "+  strategymap.getClusterSize1(j, i), false);
				}
			}




			Logger.log("\n player 0 abstract strategy: "+abstractgameneprofile1, false);
			Logger.log("\n player 1 abstract strategy: "+abstractgameneprofile2, false);

			Logger.log("\n Player 0 original strategy "+ str1, false);
			Logger.log("\n Player 1 original strategy "+ str2, false);

			double normalizedplayer1 = originalmixedstrategyplayer1.checkIfNormalized();
			double normalizedplayer2 = originalmixedstrategyplayer2.checkIfNormalized();

			String normalized = "\n Player 0 original mixed strategy's normalized value "+normalizedplayer1;

			normalized += "\n Player 1 original mixed strategy's normalized value "+normalizedplayer2;
			Logger.log(normalized, false);


			 */


			/*
			 * mixed strategy for subgame
			 */


			/*	MixedStrategy[] originalmixedstrategyplayer = strategymap.getStrategySubgameSols(abstractgameneprofile);


			System.out.println("originalmixedstrategyplayer[0].checkIfNormalized() "+ originalmixedstrategyplayer[0].checkIfNormalized());
			System.out.println("originalmixedstrategyplayer[1].checkIfNormalized() "+ originalmixedstrategyplayer[1].checkIfNormalized());

			 */



			List<MixedStrategy> originallist = new ArrayList<MixedStrategy>();
			originallist.add(originalmixedstrategyplayer1);
			originallist.add(originalmixedstrategyplayer2);



			// for subgame
			//	originallist.add(originalmixedstrategyplayer[0]);
			//	originallist.add(originalmixedstrategyplayer[1]);






			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution originaldistro = new OutcomeDistribution(originallist);
			double[]  originalexpectedpayoff = SolverUtils.computeOutcomePayoffs(mg, originaldistro);

			double epsilonz = SolverUtils.computeOutcomeStability(mg, originaldistro);
			Logger.logit("\n Expected Payoff for NE original game player 0 : "+ originalexpectedpayoff[0]+ "Expected Payoff original game player 1 : "+ originalexpectedpayoff[1]);
			Logger.logit("\n Final Epsilon for NE "+ epsilonz);


			try{
				PrintWriter pw = new PrintWriter("K"+numberofclusters+"-Logicalgame" ,"UTF-8");
				pw.write("Deltaplayer1: "+ mindeltas[0]+ " Deltaplayer2: "+ mindeltas[1]);
				pw.close();
			}
			catch(Exception ex){
				System.out.println("error in file  ");
			}



			//	System.out.println("\n\nDelta: "+mindeltas[0] + " "+ mindeltas[1]);


			if(KmeanClustering.isAvrgDelta())
			{


				result[0][0] = maxdeltas[0]; // max deltas
				result[0][1] = epsilonz; //NE epsilon

				result[1][0] = maxdeltas[1];
				result[1][1] = qreepsilon;  //  qre



				result[2][0] = maxdeltas[1];
				result[2][1] = epsilonmaxexpected;	

			}
			else
			{
				result[0][0] = mindeltas[0]; // min over max delta
				result[0][1] = epsilonz;   //NE epsilon

				result[1][0] = mindeltas[1];
				result[1][1] = qreepsilon;  //from qre solver

				result[2][0] = mindeltas[0];
				result[2][1] = epsilonmaxexpected;	



			}

		} //end of if else

		return result;





	}



	public static Game getGameWithMaxExpectedPayoff(Game game, StrategyMapping strategymap)
	{
		MatrixGame gamewithmaxexpectedpayoff  = new MatrixGame(game.getNumPlayers(), game.getNumActions());
		OutcomeIterator itr = gamewithmaxexpectedpayoff.iterator();
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			for(int i=0; i<gamewithmaxexpectedpayoff.getNumPlayers(); i++)
			{
				double payoff = strategymap.calcMaxExpectedPayoffOriginalGame(outcome, i);
				gamewithmaxexpectedpayoff.setPayoff(outcome, i, payoff);
			}

		}
		return gamewithmaxexpectedpayoff;

	}







	/**
	 * Finds the best clustering based on minimum delta and updates mindeltas[]
	 * @param deltasplayer deltas for RANDOM_RESTART_ITERATION
	 * @param clustersplayer cluster for the player
	 * @param player player
	 * @param mindeltas update minimum delta for player
	 * @return return the best cluster with minimum delta
	 */
	public static List<Integer>[] getBestCluster(
			HashMap<Integer, Double> deltasplayer,
			HashMap<Integer, Double> maxdeltasplayer,
			HashMap<Integer, List<Integer>[]> clustersplayer, 
			int player, double[] mindeltas, double[] maxdeltas, boolean ismaxdelta) 
			{

		double min = Double.POSITIVE_INFINITY;
		int minindex = 0;
		for(int j=0; j<clustersplayer.size(); j++) //there are RANDOM_RESTART_ITERATION # of deltas
		{
			if(min>deltasplayer.get(j))
			{
				min = deltasplayer.get(j);
				minindex = j;
			}
		}
		mindeltas[player] = min;
		if(!ismaxdelta)
		{
			maxdeltas[player] = maxdeltasplayer.get(minindex);
		}
		return clustersplayer.get(minindex);
			}



	/**
	 * 
	 * @param strategymap mapping of strategies from abstracted game to original game
	 * @param abstractstrategy abstract strategy
	 * @param originalnumberofactions number of actions in the original game
	 * @param abstractgamenumberofactions number of actions in the abstracted game
	 * @param player
	 * @return
	 */
	public static MixedStrategy buildOriginalStrategyFromAbstractStrategy(StrategyMapping strategymap, 
			MixedStrategy abstractstrategy, 
			int originalnumberofactions, int abstractgamenumberofactions, int player)
	{
		double[] originalstrategy = new double[originalnumberofactions];


		for(int i=0; i<abstractgamenumberofactions; i++)
		{
			if(abstractstrategy.getProb(i+1) > 0)
			{
				List<Double> originalactions = strategymap.getOriginalActionsFromAbstractedAction(i+1, player);
				for(Double x: originalactions)
				{
					int y = (int)Math.floor(x);
					//int sizeofcluster = strategymap.getClusterSize(y, player);

					int actionindex = (int)(x-1);
					originalstrategy[actionindex] = abstractstrategy.getProb(i+1)/originalactions.size();
				}
			}
		}



		double[] orgprbpl = new double[originalstrategy.length+1];

		orgprbpl[0] =0;
		int index0 =1;
		for(double x: originalstrategy)
		{
			orgprbpl[index0++] = x;
		}

		MixedStrategy origprofile = new MixedStrategy(orgprbpl);



		return origprofile;

	}





}
