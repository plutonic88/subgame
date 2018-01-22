package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import Log.Logger;
import Main.Main;
import games.EmpiricalMatrixGame;
import games.Game;
import games.GameUtils;
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
import subgame.Parameters;
import subgame.StrategyMap;
import subgame.StrategyMapping;



/**
 * Created by IntelliJ IDEA.
 * User: Oscar-XPS
 * Date: 9/27/13
 * Time: 2:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class GamutModifier {
	public Game getMg() {
		return mg;
	}
	public void setMg(Game mg) {
		this.mg = mg;
	}


	private Game mg;
	private String name;
	private RandomNumberGenerator gen = new RandomNumberGenerator();
	public GamutModifier(String gameName){
		mg = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gameName+Parameters.GAMUT_GAME_EXTENSION));
		name = gameName;
	}
	public GamutModifier(){}

	public void setGame(String gameName){
		name = gameName;
		mg = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+name+Parameters.GAMUT_GAME_EXTENSION));

	}

	public Game returnGame()
	{
		return mg;
	}

	public String getName()
	{
		return name;
	}
	public void bucket(int bucketSize)
	{
		try{
			int players = mg.getNumPlayers();
			int[] actions = {mg.getNumActions(0),mg.getNumActions(0)};
			MatrixGame g = new MatrixGame(players, actions);
			double[] extreme = mg.getExtremePayoffs();
			double a = extreme[0];//max
			double b = extreme[1];//min
			double difference = a-b;
			double[] bucket = new double[bucketSize];
			//double stepSize = difference/bucketSize;
			double stepSize = difference/bucketSize;
			bucket[0] = b+stepSize;
			for(int i = 0; i< bucketSize-1;i++)
				bucket[i+1] = bucket[i] + stepSize;
			for(int i=0; i<actions[0];i++){
				for(int j=0; j<actions[1];j++){
					int[] outcome = {i+1,j+1};
					double[] d = mg.getPayoffs(outcome);
					int[] abstractOutcome = {i+1,j+1};
					int q = 1;
					while (q<bucketSize ){
						if( d[0] >= bucket[q-1])
							q++;
						else
							break;
					}
					if(q>bucketSize){q--;}
					d[0] = q*1.0/bucketSize;
					int r = 1;
					while (r<bucketSize){
						if(d[1] >= bucket[r-1])
							r++;
						else
							break;
					}
					if(r>bucketSize){r--;}
					d[1]=r*1.0/bucketSize;
					g.setPayoff(abstractOutcome,0,d[0]);
					g.setPayoff(abstractOutcome,1,d[1]);
				}
			}
			PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"b-"+bucketSize+"-"+name+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
			SimpleOutput.writeGame(pw,g);
			pw.close();
		}catch(Exception e){e.printStackTrace();}
	}
	public StrategyMap[] removeActions(double fraction, int instance)
	{
		StrategyMap[] result = new StrategyMap[2];
		try{
			int players = mg.getNumPlayers();
			int actions = mg.getNumActions(0);

			int remove = (int)((double)actions * (fraction));
			HashSet<Integer> p1 = new HashSet<Integer>();
			HashSet<Integer> p2 = new HashSet<Integer>();
			while(p1.size()<remove)
				p1.add(new Integer(gen.nextInt(actions-1))+1);
			while(p2.size()<remove)
				p2.add(new Integer(gen.nextInt(actions-1))+1);

			int[] p1remove = convert(p1);
			Arrays.sort(p1remove);
			int[] p2remove = convert(p2);
			Arrays.sort(p2remove);
			int[] action = new int[players];
			action[0]= mg.getNumActions(0)-remove;
			action[1]= mg.getNumActions(1)-remove;
			MatrixGame g = new MatrixGame(players, action);

			double[][][] fullPayoff = new double[actions][actions][2];
			for(int i = 0; i<actions;i++)
				for(int j = 0; j<actions;j++){
					int[] outcome = {i+1,j+1};
					fullPayoff[i][j]=mg.getPayoffs(outcome);
				}
			//mark p1outcomes for removal (rows)
			for(int q = 0; q<p1remove.length;q++)
				fullPayoff[p1remove[q]-1]=null;
			//make new matrix that does not contain the removed rows
			double[][][] copy = new double[action[0]][actions][2];
			int removed = 0;
			for(int q = 0; q<fullPayoff.length;q++){
				if(fullPayoff[q]!=null)
					for(int i = 0;i<fullPayoff[q].length;i++){
						copy[q-removed][i][0]= fullPayoff[q][i][0];
						copy[q-removed][i][1]= fullPayoff[q][i][1];
					}
				else
					removed++;
			}
			//mark p2outcomes for removal (columns)
			for(int q = 0;q<p2remove.length;q++)
				copy[0][p2remove[q]-1]=null;
			//make new matrix based off of already smaller matrix but now with columns removed
			double[][][] fin = new double[action[0]][action[1]][2];
			removed = 0;
			for(int i =0; i<action[0];i++){
				removed = 0;
				for(int j = 0;j<copy[1].length;j++){
					if(copy[0][j]!=null)
					{
						fin[i][j-removed][0] = copy[i][j][0];
						fin[i][j-removed][1] = copy[i][j][1];
					}
					else
						removed++;
				}
			}
			//add final payoffs to game and then print
			int[] numActions = {fin.length,fin[0].length};
			MatrixGame game  = new MatrixGame(mg.getNumPlayers(),numActions);
			int[] outcome = new int[mg.getNumPlayers()];
			for(int i =0;i<fin.length;i++)
				for(int j = 0;j<fin[0].length;j++){
					outcome[0] = i+1;
					outcome[1] = j+1;
					game.setPayoffs(outcome,fin[i][j]);
				}
			//PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"r"+fraction+"-"+name+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
			PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"r"+fraction+"-"+instance+"-"+name+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
			SimpleOutput.writeGame(pw,game);
			pw.close();

			for(int i =0;i<p1remove.length;i++)
				p1remove[i] = p1remove[i]-1;
			for(int i=0;i<p2remove.length;i++)
				p2remove[i] = p2remove[i]-1;

			StrategyMap p1sm = new StrategyMap(p1remove,actions,"r"+fraction+"-1-"+instance+"-"+name);
			StrategyMap p2sm = new StrategyMap(p2remove,actions,"r"+fraction+"-2-"+instance+"-"+name);
			result[0] = p1sm;
			result[1] = p2sm;
		}
		catch(Exception e){e.printStackTrace();}
		return result;

	}
	public int[] convert(HashSet<Integer> s)
	{
		int[] array = new int[s.size()];
		Iterator<Integer> iter = s.iterator();
		int i = 0;
		while(iter.hasNext()){
			array[i++] = iter.next().intValue();
		}
		return array;
	}

	public void weakRemove(){
		mg = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"b-9-"+name+Parameters.GAMUT_GAME_EXTENSION));
		int actions = mg.getNumActions(0);
		try{
			double[][][] fullPayoff = new double[actions][actions][2];
			for(int i = 0; i<actions;i++)
				for(int j = 0; j<actions;j++){
					int[] outcome = {i+1,j+1};
					fullPayoff[i][j]=mg.getPayoffs(outcome);
				}
			ArrayList<Integer> p1remove = new ArrayList<Integer>();
			ArrayList<Integer> p2remove = new ArrayList<Integer>();
			boolean removedWeak = false;
			boolean merged = false;
			do
			{

			}while(removedWeak || merged);
			//remove
			//add final payoffs to game and then print
			/*int[] numActions = {fin.length,fin[0].length};
            MatrixGame game  = new MatrixGame(mg.getNumPlayers(),numActions);
            int[] outcome = new int[mg.getNumPlayers()];
            for(int i =0;i<fin.length;i++)
                for(int j = 0;j<fin[0].length;j++){
                    outcome[0] = i+1;
                    outcome[1] = j+1;
                    game.setPayoffs(outcome,fin[i][j]);
                }
            //PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"r"+fraction+"-"+name+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
            PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"w-"+name+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
            SimpleOutput.writeGame(pw,game);
            pw.close();*/
		}
		catch(Exception e){e.printStackTrace();}
	}
	public boolean weak(ArrayList<Integer> ignore, ArrayList<Integer> remove,int player){
		for(int i =0;i<mg.getNumActions(0);i++){
			for(int j = 0;j<mg.getNumActions(0);j++){
				if(GameUtils.weaklyDominates(mg,player,i,j,ignore)){
					remove.add(i);
					ignore.add(i);
					return true;
				}
			}
		}
		return false;
	}
	public boolean equiv(ArrayList<Integer> ignore, ArrayList<Integer> remove,int player){
		for(int i =0;i<mg.getNumActions(0);i++){
			for(int j = 0;j<mg.getNumActions(0);j++){
				if(GameUtils.weaklyDominates(mg,player,i,j,ignore)){
					remove.add(i);
					ignore.add(i);
					return true;
				}
			}
		}
		return false;
	}


	public void performNTopAbstraction(int n)
	{
		int numberofplayers = mg.getNumPlayers();  
		int numberofactions[] = new int[numberofplayers]; //to contain the number of actions for players.
		numberofactions = mg.getNumActions();

		int[] actions = new int[numberofplayers];
		double[] temppayoffs = new double[2]; //contain the payoff pairs 
		int[][][] sumofpayoffsperplayer = new int[numberofplayers+1][numberofactions[0]][2]; 

		int i,j,k; //all of them started from 1, k is for player 1, 2....n

		for( k =1; k <= numberofplayers; k++)
		{
			int tempsum = 0;
			for( i = 1; i<= numberofactions[0]; i++) //iterate over the actions played by player 1
			{
				for( j =1; j<=numberofactions[0]; j++) //iterate over the actions played by player 2
				{
					if(k ==1) //if the player is 1
					{

						//iterating from left to right
						actions[0] = i;
						actions[1] = j;
						temppayoffs = mg.getPayoffs(actions);
						tempsum += temppayoffs[k];
					}
					else if(k ==2) //if it is player 2
					{ 
						//for player 2 iterating from top to bottom
						actions[0] = j;
						actions[1] = i;
						temppayoffs = mg.getPayoffs(actions);
						tempsum += temppayoffs[k];
					}
				}
			}
			sumofpayoffsperplayer[k][i][0] = i;   //[playernumber][numberofaction][0 is to save the action, 1 is to save the sum]
			sumofpayoffsperplayer[k][i][1] = tempsum;

		}


		//for player number 1 sort the sums to get the actions which we want to keep.
		// we can get the actions of player 1 by sumofpayoffsperplayer[1][j][0] and change the j parameter
		// j can be upto the number of actions


		int p;
		boolean flag = true;   // set flag to true to begin first pass
		int temp;   //holding variable

		while ( flag )
		{
			flag= false;    //set flag to false awaiting a possible swap
			for( p = 1;  p <= sumofpayoffsperplayer[1].length -1;  p++ )
			{
				if ( sumofpayoffsperplayer[1][p][1] < sumofpayoffsperplayer[1][p+1][1] )   // change to > for ascending sort
				{
					temp = sumofpayoffsperplayer[1][p][1];                //swap the sums
					sumofpayoffsperplayer[1][p][1] = sumofpayoffsperplayer[1][p+1][1];
					sumofpayoffsperplayer[1][p+1][1] = temp;


					temp = sumofpayoffsperplayer[1][p][0];                //swap the actions
					sumofpayoffsperplayer[1][p][0] = sumofpayoffsperplayer[1][p+1][0];
					sumofpayoffsperplayer[1][p+1][0] = temp;

					flag = true;              //shows a swap occurred  
				} 
			} 

			if(flag == false)
			{
				break;
			}
		} 



		//for player number 2 sort the sums to get the actions which we want to keep.
		// we can get the actions of player 2 by sumofpayoffsperplayer[2][j][0], and change the j parameter
		//// j can be upto the number of actions

		while ( flag )
		{
			flag= false;    //set flag to false awaiting a possible swap
			for( p = 1;  p <= sumofpayoffsperplayer[2].length -1;  p++ )
			{
				if ( sumofpayoffsperplayer[2][p][1] < sumofpayoffsperplayer[2][p+1][1] )   // change to > for ascending sort
				{
					temp = sumofpayoffsperplayer[2][p][1];                //swap elements
					sumofpayoffsperplayer[2][p][1] = sumofpayoffsperplayer[2][p+1][1];
					sumofpayoffsperplayer[2][p+1][1] = temp;


					temp = sumofpayoffsperplayer[2][p][0];                //swap elements
					sumofpayoffsperplayer[2][p][0] = sumofpayoffsperplayer[2][p+1][0];
					sumofpayoffsperplayer[2][p+1][0] = temp;

					flag = true;              //shows a swap occurred  
				} 
			} 

			if(flag == false)
			{
				break;
			}
		} 


		int[] newnumberofactions = {numberofactions[0]/n, numberofactions[0]/n};

		//   int[][] actionsfortwoplayers = new int[newnumberofactions[0]][2]; //holds the action pairs which we need
		// int[][] payoffsfortwoplayers = new int[newnumberofactions[0]][2]; //holds the payoff pairs which we need.
		MatrixGame game  = new MatrixGame(mg.getNumPlayers(), newnumberofactions);
		int[] outcome = new int[2]; 
		int[] newactions = new int[2];

		for(i = 1; i <= newnumberofactions[0]; i++)
		{
			for(j =1; j <= newnumberofactions[0]; j++)
			{
				outcome[0] = i;
				outcome[1] = j;

				//get the actions from array.
				newactions[0] = sumofpayoffsperplayer[1][i][0];
				newactions[1] = sumofpayoffsperplayer[2][j][0];

				//	 use the actions to get the payoffs from original game and set them in the new game
				game.setPayoffs(outcome, mg.getPayoffs(newactions));

			}



		}

		try{

			PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"n"+name+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
			SimpleOutput.writeGame(pw,game);
			pw.close();
		}
		catch(Exception ex)
		{

		}


	}



	/*public int getFurthestClusterIndex(double[][] clusterpoints, int numberofclusters, int numberofactions)
	{
		double maxdiff=0;
		int maxdiffindex =0;
		for(int i=0; i<numberofclusters-1; i++)
		{
			for(int j=i+1; j<numberofclusters; j++)
			{
				double tmpdiff =0;
				for(int k=0; k<numberofactions; k++)
				{
					tmpdiff= tmpdiff+ (clusterpoints[i][k]-clusterpoints[j][k]);
				}
				if(maxdiff<tmpdiff)
				{
					maxdiff = tmpdiff;
					maxdiffindex = 
				}
			}
		}




		return maxdiffindex;

	}
	 */


	public static boolean isDifferent(double[][] points, int action, int clusternumber, MatrixGame gm, int player)
	{
		for(int i=0; i<gm.getNumActions(player); i++)
		{
			int outcome[] = new int[2];
			if(player==0)
			{
				outcome[0] = action;
				outcome[0] =i;
			}
			else
			{
				outcome[0] = i;
				outcome[0] =action;
			}

			if(gm.getPayoff(outcome, player)!=points[clusternumber][i])
				return true;
		}

		return false;

	}



	public  void calculateClusterMean(List<double[]>[] clusters, double[][] clusterpoints, int numberofactions, int numberofclusters, int player)

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



				while(true)

				{



					// if cluster is empty, assign random points from the strategy

					randomaction = randInt(1,numberofactions );

					//check if the payoffs are same as centroid of another cluster





					//need to test

					if(checkIfActionIsOkToBeACentroid(mg, clusterpoints, randomaction, i, player))

					{

						break;

					}





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

					for(double[] x: clusters[i])

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




	public  void calculateClusterMean2(List<double[]>[] clusters, double[][] clusterpoints, int numberofactions, int numberofclusters, int player)
	{
		/*
		 * now recalculate the cluster mean
		 */


		int opponent = 1^player;

		double average1 = 0;
		double average2 = 0;
		for(int i = 0; i< numberofclusters; i++)
		{


			int clustersize = clusters[i].size();

			if(clustersize==0)
			{

				System.out.println("\n\nEmpty cluster: "+ i + " ");
				Logger.log("\n cluster: "+ i + " is empty, points before reassignment:\n", false);

				for(int k=0; k< (2*mg.getNumActions(opponent) ); k++)
				{
					Logger.log(" "+clusterpoints[i][k], false);
				}

				int randomaction;

				while(true)
				{

					// if cluster is empty, assign random points from the strategy
					randomaction = randInt(1,numberofactions );
					//check if the payoffs are same as centroid of another cluster



					if(checkIfActionIsOkToBeACentroid2(mg, clusterpoints, randomaction, i, player))
					{
						break;
					}


				}

				Logger.log("\nAction "+ randomaction+"'s payoffs are assigned to cluster "+ i, false);




				int clusterpointsindex =0; 
				for(int j =0; j< mg.getNumActions(opponent); j++)
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



					if((clusterpoints[i].length-1) >= clusterpointsindex)
					{

						double payoff = mg.getPayoff(outcome, player);
						clusterpoints[i][clusterpointsindex] = payoff;

					}


					if((clusterpoints[i].length-1) >= (clusterpointsindex+1))
					{

						double payoff2 = mg.getPayoff(outcome, opponent);
						clusterpoints[i][clusterpointsindex+1] = payoff2;
					}


					// now increase the clusterpoint index by 2

					clusterpointsindex = clusterpointsindex + 2;


				}


				Logger.log("\n cluster: "+ i + " is empty, points after reassignment:\n", false);
				for(int k=0; k< (2*mg.getNumActions(opponent)); k++)
				{
					Logger.log(" "+clusterpoints[i][k], false);
				}


			}
			else if(clustersize>0)
			{


				/*
				 * new way to calculate the mean
				 */

				int clusterpointindex = 0; 


				for(int j = 0; j< mg.getNumActions(opponent); j++)
				{
					average1 = 0; 
					average2 = 0; 

					for(double[] x: clusters[i])
					{
						int action = (int)x[0];

						int[] outcome = {0, 0};

						if(player == 0)
						{
							outcome[0] = action;
							outcome[1] = j+1;
						}
						else if(player == 1)
						{
							outcome[0] = j+1;
							outcome[1] = action;

						}

						average1 = average1 + mg.getPayoff(outcome, player);
						average2 = average2 + mg.getPayoff(outcome, (1^player));

					}


				//	System.out.println("clusterpoints[i] :"+ clusterpoints[i].length);

				//	System.out.println("clusterpointindex :"+ clusterpointindex);


					if((clusterpoints[i].length-1) >= clusterpointindex)
					{
						clusterpoints[i][clusterpointindex] = average1/clustersize;

					}

					if((clusterpoints[i].length-1) >= (clusterpointindex+1))
					{
						clusterpoints[i][clusterpointindex+1] = average2/clustersize;

					}




					clusterpointindex += 2;

				}

			}

		}

	}





	public void assignToCluster(List<double[]>[] clusters, int actiontoassign, int assignedcluster, int numberofactions, int player )

	{



		int opponent = 1^player;





		int oppnumaction = mg.getNumActions(opponent);



		double[] tupleincluster = new double[oppnumaction+1]; // +1 for the action



		tupleincluster[0] = actiontoassign; //the action in the first index











		/*

		 * now assign the payoffs

		 */



		int[] tmpoutcome = new int[2];







		for(int p = 0; p< oppnumaction; p++)

		{



			if(player == 0){



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





		/*

		 * TODOimplement with iterator

		 */











		clusters[assignedcluster].add(tupleincluster); 



	}





	public void assignToCluster2(List<double[]>[] clusters, int actiontoassign, int assignedcluster, int numberofactions, int player )
	{

		int opponent = 1^player;


		int oppnumaction = mg.getNumActions(opponent);

		double[] tupleincluster = new double[oppnumaction+1]; // +1 for the action

		tupleincluster[0] = actiontoassign; //the action in the first index





		/*
		 * now assign the payoffs
		 */

		int[] tmpoutcome = new int[2];



		for(int p = 0; p< oppnumaction; p++)
		{

			if(player == 0){

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


		/*
		 * TODOimplement with iterator
		 */





		clusters[assignedcluster].add(tupleincluster); 

	}






	public List<double[]>[] clusterActions2(int numberofclusters, int player )
	{


		int opponent = 1^player;



		//final int   RUNNING_FOR = 20;
		int[] numberofactions = mg.getNumActions();
		double[] extreampayoffs = mg.getExtremePayoffs();
		List<double[]>[] clusters = new List[numberofclusters]; // create an array of list. Each list will contain arrays of double
		//List<double[]>[] oldclusters = new List[numberofclusters];
		double[][] clusterpoints = new double[numberofclusters][2*numberofactions[opponent]];  // cluster points for each cluster
		double[][] oldclusterpoints = new double[numberofclusters][2*numberofactions[opponent]];  
		double[][] distancefromcluster = new double[numberofclusters][2*numberofactions[opponent]];
		int runningInstance = 0;
		//double payoffsforopponent[][] = new double[numberofclusters][numberofactions];


		// sumofdifferences, maxdifference, squaredrootdifference need to be fixed when calculated. 
		double[] sumofdifferences = new double[numberofclusters];  //store the sum of differences for clusters 
		double[] maxdifference = new double[numberofclusters]; 
		double[] squaredrootdifference = new double[numberofclusters];


		ArrayList<Integer> alreadyassignedactions = new ArrayList<Integer>(); // for random partition method
		boolean flagforrandompartition = false;

		for(int i=0; i< numberofclusters; i++)
		{

			clusters[i] = new ArrayList<double[]>(); 
		}




		//assign random values to cluster points

		/*
		if(Referee.isRandPoints()) // assign random points
		{

			Random rand = new Random();
			for(int i =0; i<numberofclusters; i++ )
			{
				for(int j =0; j<numberofactions; j++)
				{





					double x = (double)rand.nextInt(  (int)(extreampayoffs[0] + extreampayoffs[0]) ) - extreampayoffs[0];

					if( x< extreampayoffs[1]) // less than the minimum payoff
					{
						x = x+ extreampayoffs[0];
					}

					if(x> extreampayoffs[0]) // greater than maximum payoff
					{
						x = x - extreampayoffs[0];
					}

					clusterpoints[i][j] = x;

					//-extreampayoffs[1] + (int) (Math.random() * ((extreampayoffs[0] - (-extreampayoffs[1])) + 1));//rand.nextInt((int)maxclusternumber[0]) +1;



				}
			}


		}*/



		if(Main.isRandPointsFromObservation()) // implemented only for two players
		{
			Logger.log("\n entered RandPointsFromObservation() ", false);

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

				//	for(int j=0; j<numberofactions; j++) 
				//	{


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

						if(player==0)
						{
							clusterpoints[i][index++] = mg.getPayoff(outcomegame, player);
							clusterpoints[i][index++] = mg.getPayoff(outcomegame, 1);

						}
						else if(player==1)
						{
							clusterpoints[i][index++] = mg.getPayoff(outcomegame, player);
							clusterpoints[i][index++] = mg.getPayoff(outcomegame, 0);

						}

					}

				}


			}
		}



		/*
		 * assigns random actions to each clusters
		 */
		if(Main.isRandActionInitToClusters())
		{

			Logger.log("\n entered RandActionInitToClusters() ", false);

			/*
			 * pick an action and randomly assign it to a cluster.  
			 * then calculate the mean
			 */


			//	ArrayList<ArrayList<Integer>> clusteractions = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> unassignedactions = new ArrayList<Integer>();



			// first assign all the actions to unassigned list
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



						// only need to keep the payoffs of trhe player for whom clustering is done. So no
						//need to adjust
						assignToCluster2(clusters, unassignedactions.get(chosenactionindex), i, numberofactions[player], player);
						//	Logger.log("\n random paritiion<<<>>>>> Action "+ z+ " of player "+ player+" is assigned to cluster "+i, false);
						unassignedactions.remove(chosenactionindex);
					}
					else if(unassignedactions.size() ==1)
					{
						assignToCluster2(clusters, unassignedactions.get(0), i, numberofactions[player], player);
						int x = unassignedactions.get(0);
						//	Logger.log("\n random paritiion<<<>>>>> Action "+x + " of player "+ player+" is assigned to cluster "+i, false);
						unassignedactions.remove(0); //remove the last element
						break;
					}



				}



			}  // end of for loop

			//check if there are any actions remained unassigned
			if(unassignedactions.size() != 0)
			{


				for(Integer x: unassignedactions)
				{

					int a = numberofclusters-1;
					assignToCluster(clusters, x, numberofclusters-1, numberofactions[player], player); // assign all the remainning actions to the last cluster.
					Logger.log("\n random paritiion<<<>>>>> Action "+ x+ " of player "+ player+" is assigned to cluster "+a, false);


				}

			}



			// print the clusters..

			Logger.log("\n\nInitialization to clusters for player "+ player + ":\n", false);

			for(int i=0; i<clusters.length; i++)
			{
				Logger.log("Cluster "+ i, false);

				for(double[] x: clusters[i])
				{
					Logger.log(x[0]+", ", false);
				}
				Logger.log("\n", false);
			}



			calculateClusterMean2(clusters, clusterpoints, numberofactions[player], numberofclusters, player);





		}




		while(true){
			System.out.println("\nIteration: "+ runningInstance);

			runningInstance++;

			if(runningInstance>=100)
			{
				return clusters;
			}



			//copy the cluster points to old cluster points.


			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j< (2*numberofactions[opponent] ); j++)
				{
					oldclusterpoints[i][j] = clusterpoints[i][j];

				}

			}







			//	System.out.print("\n\nIteration: "+ runningInstance + " ");
			Logger.log("\n\nK-mean Iteration: "+ runningInstance  +" plauer "+player+"  cluster points\n", false);

			for(int i =0; i< numberofclusters; i++){

				//	System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< (2*numberofactions[opponent] ) ; j++){

					//	System.out.print(" "+oldclusterpoints[i][j]); 
					Logger.log(" "+oldclusterpoints[i][j], false);

				}
				//	System.out.print("\n");
				Logger.log("\n", false);
			}






			// now clear/create e cluster object the new cluster for a new iteration. 

			for(int i=0; i< numberofclusters; i++)
			{

				clusters[i]= new ArrayList<double[]>(); //.clear();

			}



			/*
			 * Now iterate over all the possible action touples for player 1. 
			 * calclate the difference from cluster points
			 * assign to the cluster with the minimum difference.
			 *  
			 */


		//	System.out.println("NUmber of actions: "+ mg.getNumActions(0) + " "+ mg.getNumActions(1));

			for(int i = 0; i < numberofactions[player]; i++)
			{

				//	int opponent =1;
				/*	if(player==1)
				{
					opponent=0;
				}*/


				//int clusterpointindex = 0; 
				int tmpplayer = player;
				int outcomeindex = 1;
				boolean f1 = false;
				boolean f2 = false;
				
				
				int limit = (( 2*numberofactions[opponent] ) - 1);
				
				
				

				for(int j = 0; j < limit ; j++)
				{
					
				//	System.out.println("outcome index "+ outcomeindex);


					int outcome[] = new int [2];

					if(player ==0){

						outcome[0] = i+1;
						outcome[1] =  outcomeindex; // ouj+1;
					}
					else if(player == 1)
					{
						outcome[0] =  outcomeindex;//j+1;
						outcome[1] = i+1;
					}

				//	System.out.println("player "+player+" outcome "+ outcome[0] + " "+ outcome[1] + " j : "+ j + " limit "+ limit);

					double tmppayoff = mg.getPayoff(outcome, tmpplayer); //get the payoff for player 1 or player 2
					Logger.logit(outcomeindex +" ");

					if(tmpplayer==0)
					{
						f1 = true;
					}
					else if(tmpplayer==1)
					{
						f2 = true;
					}

					if(f1 == true && f2 == true)
					{
						outcomeindex++;
						f1 = false;
						f2 = false; 
					}

					tmpplayer = 1^tmpplayer;

					for(int k =0; k<numberofclusters; k++)
					{
						/*
						 * calculate the differences of payoffs for each cluster points 
						 */


						if(Main.isDistMetricLine())
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

						if(Main.isDistMetricEuclidean())
						{

							//	Logger.log("\n entered DistMetricEuclidean() ", false);
							distancefromcluster[k][j] = (clusterpoints[k][j]  - (tmppayoff));
							distancefromcluster[k][j] = distancefromcluster[k][j] * distancefromcluster[k][j];
						}




					} // end of innner for loop

				}

				double min = Double.POSITIVE_INFINITY;
				int minindex = 0;


				if(Main.isSumDist())
				{

					Logger.log("\n entered SumDist() ", false);

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
						Logger.log("\n Action "+ a+"'s sum distance from cluster "+l+" is : "+sumofdifferences[l] , false);

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


				if(Main.isMaxDist())
				{

					Logger.log("\n entered MaxDist() ", false);

					/*
					 * calculate the max difference instead of summing them
					 */

					for(int l =0; l< numberofclusters; l++)
					{

						double maxdiff =Double.NEGATIVE_INFINITY;
						for(int m =0; m< distancefromcluster[l].length; m++)
						{

							if(maxdiff<distancefromcluster[l][m])
							{
								maxdiff = distancefromcluster[l][m];
							}


						}

						maxdifference[l] = maxdiff;
						int a = i+1;
						Logger.log("\n Action "+ a+"'s max distance from cluster "+l+" is : "+maxdifference[l] , false);

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


				if(Main.isDistMetricEuclidean())
				{

					Logger.log("\n entered DistMetricEuclidean() ", false);
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
						Logger.log("\n Action "+ a+"'s euclidean distance from cluster "+ l+ " : "+squaredrootdifference[l], false);
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
				Logger.log("\nAction "+ a+" is assigned to cluster :"+minindex , false);


				assignToCluster2(clusters, i+1, minindex, numberofactions[player], player);





			}  // end of outer for loop




			/*System.out.print("\n\nIteration: "+ runningInstance + " ");
			Logger.log("\n\nIteration: "+ runningInstance  +" plauer "+player+"  cluster points\n", false);

			for(int i =0; i< numberofclusters; i++){

				System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< numberofactions; j++){

					System.out.print(" "+oldclusterpoints[i][j]); 
					Logger.log(" "+oldclusterpoints[i][j], false);

				}
				System.out.print("\n");
				Logger.log("\n", false);
			}
			 */

			Logger.log("\nActions in clusters for the mean\n", false);

			for(int i=0; i< numberofclusters; i++)
			{
				//	System.out.print("Cluster: " + i + " "+ "Actions: ");
				Logger.log("Cluster: " + i + " "+ "Actions: ", false);
				for(double[] x : clusters[i]){
					//	System.out.print(x[0] + " ");
					Logger.log(x[0] + " ", false);


				}

				//if(runningInstance != RUNNING_FOR)
				//clusters[i].clear();
				//	System.out.print("\n");
				Logger.log("\n", false);
			}







			/*
			 * now recalculate the cluster points
			 */

			calculateClusterMean2(clusters, clusterpoints, numberofactions[player], numberofclusters, player);






			//System.out.print("Hello");

			//	System.out.print("\n\nIteration: "+ runningInstance + " ");
			Logger.log("\n\nK-mean Iteration: "+ runningInstance  +" player "+player+" new cluster points(mean)\n", false);

			for(int i =0; i< numberofclusters; i++){

				//	System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< (2*numberofactions[opponent] ); j++){

					//		System.out.print(" "+clusterpoints[i][j]); 
					Logger.log(" "+clusterpoints[i][j], false);

				}
				//	System.out.print("\n");
				Logger.log("\n", false);
			}




			boolean checkforstop = true;

			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j<clusterpoints[i].length; j++)
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

			if(checkforstop == true)
			{
				break;
			}






			//	System.out.print("\n");
			//	System.out.print("\n");
			//	System.out.print("\n");
			Logger.log("\n\n", false);





		}    // end of while loop 	



		/*	
		 // here calculate the payoffs for the opponent
		double average = 0;
		int outcome[] = new int[2];
		int clustersize = 0;

		for(int i =0; i< numberofclusters; i++)
		{
			for(int j=0; j< numberofactions; j++){

				average = 0;
				clustersize = clusters[i].size();

				for(double[] x: clusters[i]){

					outcome[0] = (int)x[0];
					outcome[1] = j;

					average += mg.getPayoff(outcome, Math.abs(player-1));  // get payoff for opponent


				}


				payoffsforopponent[i][j] = average/clustersize;
			}
		} //end of for loop
		 */

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
			for(double[] x: clusters[i])
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


		return clusters;





	}







	public List<double[]>[] clusterActions(int numberofclusters, int player )
	{


		int opponent = 1^player;



		//final int   RUNNING_FOR = 20;
		int[] numberofactions = mg.getNumActions();
		double[] extreampayoffs = mg.getExtremePayoffs();
		List<double[]>[] clusters = new List[numberofclusters]; // create an array of list. Each list will contain arrays of double
		//List<double[]>[] oldclusters = new List[numberofclusters];
		double[][] clusterpoints = new double[numberofclusters][numberofactions[opponent]];  // cluster points for each cluster
		double[][] oldclusterpoints = new double[numberofclusters][numberofactions[opponent]];  
		double[][] distancefromcluster = new double[numberofclusters][numberofactions[opponent]];
		int runningInstance = 0;
		//double payoffsforopponent[][] = new double[numberofclusters][numberofactions];
		double[] sumofdifferences = new double[numberofclusters];  //store the sum of differences for clusters
		double[] maxdifference = new double[numberofclusters]; 
		double[] squaredrootdifference = new double[numberofclusters];
		ArrayList<Integer> alreadyassignedactions = new ArrayList<Integer>(); // for random partition method
		boolean flagforrandompartition = false;

		for(int i=0; i< numberofclusters; i++){

			clusters[i] = new ArrayList<double[]>(); 
		}




		//assign random values to cluster points

		/*
		if(Referee.isRandPoints()) // assign random points
		{

			Random rand = new Random();
			for(int i =0; i<numberofclusters; i++ )
			{
				for(int j =0; j<numberofactions; j++)
				{





					double x = (double)rand.nextInt(  (int)(extreampayoffs[0] + extreampayoffs[0]) ) - extreampayoffs[0];

					if( x< extreampayoffs[1]) // less than the minimum payoff
					{
						x = x+ extreampayoffs[0];
					}

					if(x> extreampayoffs[0]) // greater than maximum payoff
					{
						x = x - extreampayoffs[0];
					}

					clusterpoints[i][j] = x;

					//-extreampayoffs[1] + (int) (Math.random() * ((extreampayoffs[0] - (-extreampayoffs[1])) + 1));//rand.nextInt((int)maxclusternumber[0]) +1;



				}
			}


		}*/



		if(Main.isRandPointsFromObservation()) // implemented only for two players
		{
			Logger.log("\n entered RandPointsFromObservation() ", false);

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

				//	for(int j=0; j<numberofactions; j++) 
				//	{


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





				//}
			}
		}



		/*
		 * assigns random actions to each clusters
		 */
		if(Main.isRandActionInitToClusters())
		{

			Logger.log("\n entered RandActionInitToClusters() ", false);

			/*
			 * pick an action and randomly assign it to a cluster.  
			 * then calculate the mean
			 */


			//	ArrayList<ArrayList<Integer>> clusteractions = new ArrayList<ArrayList<Integer>>();
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

						assignToCluster(clusters, unassignedactions.get(chosenactionindex), i, numberofactions[player], player);
						//	Logger.log("\n random paritiion<<<>>>>> Action "+ z+ " of player "+ player+" is assigned to cluster "+i, false);
						unassignedactions.remove(chosenactionindex);
					}
					else if(unassignedactions.size() ==1)
					{
						assignToCluster(clusters, unassignedactions.get(0), i, numberofactions[player], player);
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
					assignToCluster(clusters, x, numberofclusters-1, numberofactions[player], player); // assign all the remainning actions to the last cluster.
					//	Logger.log("\n random paritiion<<<>>>>> Action "+ x+ " of player "+ player+" is assigned to cluster "+a, false);


				}

			}



			// print the clusters..

			Logger.log("\n\nInitialization to clusters for player "+ player + ":\n", false);

			for(int i=0; i<clusters.length; i++)
			{
				Logger.log("Cluster "+ i, false);

				for(double[] x: clusters[i])
				{
					Logger.log(x[0]+", ", false);
				}
				Logger.log("\n", false);
			}



			calculateClusterMean(clusters, clusterpoints, numberofactions[player], numberofclusters, player);





		}




		while(true){
			//System.out.println("\nIteration: "+ runningInstance);

			runningInstance++;

			if(runningInstance>=100)
			{
				return clusters;
			}



			//copy the cluster points to old cluster points.


			for(int i=0; i< numberofclusters; i++)
			{

				for(int j=0; j<numberofactions[opponent]; j++)
				{
					oldclusterpoints[i][j] = clusterpoints[i][j];

				}

			}







			//	System.out.print("\n\nIteration: "+ runningInstance + " ");
			Logger.log("\n\nK-mean Iteration: "+ runningInstance  +" plauer "+player+"  cluster points\n", false);

			for(int i =0; i< numberofclusters; i++){

				//	System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< numberofactions[opponent]; j++){

					//	System.out.print(" "+oldclusterpoints[i][j]); 
					Logger.log(" "+oldclusterpoints[i][j], false);

				}
				//	System.out.print("\n");
				Logger.log("\n", false);
			}






			// now clear/create e cluster object the new cluster for a new iteration. 

			for(int i=0; i< numberofclusters; i++)
			{

				clusters[i]= new ArrayList<double[]>(); //.clear();

			}



			/*
			 * Now iterate over all the possible action touples for player 1. 
			 * calclate the difference from cluster points
			 * assign to the cluster with the minimum difference.
			 *  
			 */

			for(int i = 0; i < numberofactions[player]; i++)
			{

				//	int opponent =1;
				/*	if(player==1)
				{
					opponent=0;
				}*/

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


						if(Main.isDistMetricLine())
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

						if(Main.isDistMetricEuclidean())
						{

							//	Logger.log("\n entered DistMetricEuclidean() ", false);
							distancefromcluster[k][j] = (clusterpoints[k][j]  - (tmppayoff));
							distancefromcluster[k][j] = distancefromcluster[k][j] * distancefromcluster[k][j];
						}




					}

				}

				double min = Double.POSITIVE_INFINITY;
				int minindex = 0;


				if(Main.isSumDist())
				{

					Logger.log("\n entered SumDist() ", false);

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
						Logger.log("\n Action "+ a+"'s sum distance from cluster "+l+" is : "+sumofdifferences[l] , false);

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


				if(Main.isMaxDist())
				{

					Logger.log("\n entered MaxDist() ", false);

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
						Logger.log("\n Action "+ a+"'s max distance from cluster "+l+" is : "+maxdifference[l] , false);

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


				if(Main.isDistMetricEuclidean())
				{

					Logger.log("\n entered DistMetricEuclidean() ", false);
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
						Logger.log("\n Action "+ a+"'s euclidean distance from cluster "+ l+ " : "+squaredrootdifference[l], false);
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
				Logger.log("\nAction "+ a+" is assigned to cluster :"+minindex , false);


				assignToCluster(clusters, i+1, minindex, numberofactions[player], player);





			}  // end of outer for loop




			/*System.out.print("\n\nIteration: "+ runningInstance + " ");
			Logger.log("\n\nIteration: "+ runningInstance  +" plauer "+player+"  cluster points\n", false);

			for(int i =0; i< numberofclusters; i++){

				System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< numberofactions; j++){

					System.out.print(" "+oldclusterpoints[i][j]); 
					Logger.log(" "+oldclusterpoints[i][j], false);

				}
				System.out.print("\n");
				Logger.log("\n", false);
			}
			 */

			Logger.log("\nActions in clusters for the mean\n", false);

			for(int i=0; i< numberofclusters; i++)
			{
				//	System.out.print("Cluster: " + i + " "+ "Actions: ");
				Logger.log("Cluster: " + i + " "+ "Actions: ", false);
				for(double[] x : clusters[i]){
					//	System.out.print(x[0] + " ");
					Logger.log(x[0] + " ", false);


				}

				//if(runningInstance != RUNNING_FOR)
				//clusters[i].clear();
				//	System.out.print("\n");
				Logger.log("\n", false);
			}







			/*
			 * now recalculate the cluster points
			 */

			calculateClusterMean(clusters, clusterpoints, numberofactions[player], numberofclusters, player);






			//System.out.print("Hello");

			//	System.out.print("\n\nIteration: "+ runningInstance + " ");
			Logger.log("\n\nK-mean Iteration: "+ runningInstance  +" player "+player+" new cluster points(mean)\n", false);

			for(int i =0; i< numberofclusters; i++)
			{

				//	System.out.print("Cluster: "+ i + " ");
				Logger.log("Cluster: "+ i + " ", false);
				for(int j =0; j< numberofactions[opponent]; j++)
				{

					//		System.out.print(" "+clusterpoints[i][j]); 
					Logger.log(" "+clusterpoints[i][j], false);

				}
				//	System.out.print("\n");
				Logger.log("\n", false);
			}




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

			if(checkforstop == true)
			{
				break;
			}






			//	System.out.print("\n");
			//	System.out.print("\n");
			//	System.out.print("\n");
			Logger.log("\n\n", false);





		}    // end of while loop 	



		/*	
		 // here calculate the payoffs for the opponent
		double average = 0;
		int outcome[] = new int[2];
		int clustersize = 0;

		for(int i =0; i< numberofclusters; i++)
		{
			for(int j=0; j< numberofactions; j++){

				average = 0;
				clustersize = clusters[i].size();

				for(double[] x: clusters[i]){

					outcome[0] = (int)x[0];
					outcome[1] = j;

					average += mg.getPayoff(outcome, Math.abs(player-1));  // get payoff for opponent


				}


				payoffsforopponent[i][j] = average/clustersize;
			}
		} //end of for loop
		 */

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
			for(double[] x: clusters[i])
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


		return clusters;





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



	public static boolean checkIfActionIsOkToBeACentroid(Game mg, double[][] clusterpoints, int actiontocheck, int emptycluster, int player)

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





	public static boolean checkIfActionIsOkToBeACentroid2(Game mg, double[][] clusterpoints, int actiontocheck, int emptycluster, int player)
	{

		boolean flag = false;
		int[] outcome = new int[2];
		int opponent = 1^player;



		for(int i=0; i<clusterpoints.length; i++)
		{
			if(i!=emptycluster)
			{
				flag = false;
				int tmpplayer = player;

				boolean f1 = false;
				boolean f2 = false;
				int outcomeindex = 0;

				for(int j=0; j< 2*(mg.getNumActions(opponent) ); j++)
				{

					if(player==0)
					{
						outcome[0] = actiontocheck;
						outcome[1] = outcomeindex+1;
					}
					else if(player==1)
					{
						outcome[0] = outcomeindex+1;
						outcome[1] = actiontocheck;
					}

					if(clusterpoints[i][j] != mg.getPayoff(outcome, tmpplayer))
					{
						flag = true;
						break;
					}

					if(tmpplayer==0)
					{
						f1= true; 
					}
					else if(tmpplayer==1)
					{
						f2 = true;
					}

					if(f1==true && f2==true)
					{
						outcomeindex++;
						f1= false;
						f2= false;
					}

					tmpplayer = 1^ tmpplayer;

				}  // end of inner for loop

				if(flag== false)
				{
					return false;
				}
			}
		}

		return true;


	}





	public static void makeNoisyGame(double mean, double stdDev, int numberofaction)
	{
		GaussianSampler gs = new GaussianSampler(mean, stdDev);

		/*
		 * make the directory for noisy games
		 */

		File file = new File(Parameters.GAME_FILES_PATH+"noisy");
		if (!file.exists()) {
			if (file.mkdir()) {

				System.out.println("Directory is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}


		for(int gamenum=1; gamenum<=100; gamenum++)
		{
			String gameName = "logicalgame"+Integer.toString(gamenum);

			MatrixGame matgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gameName+Parameters.GAMUT_GAME_EXTENSION));


			if(matgame.getNumActions(0)>=numberofaction && matgame.getNumActions(1)>=numberofaction)
			{


				int[] numaction = { numberofaction, numberofaction};

				MatrixGame noisygame = new MatrixGame(matgame.getNumPlayers(), numaction);

				OutcomeIterator itr = matgame.iterator();

				while(itr.hasNext())
				{
					int[] outcome = itr.next();

					if(outcome[0] <=numberofaction && outcome[1] <= numberofaction)
					{

						for(int player=0; player<2; player++)
						{
							double payoff = matgame.getPayoff(outcome, player);
							payoff= payoff+ gs.getSampleDouble();
							noisygame.setPayoff(outcome, player, payoff);


						}
					}


				}



				/*
				 * normalize the payoffs
				 */

				for(int i=0; i<noisygame.getNumPlayers(); i++)
				{
					double[] p = noisygame.getExtremePayoffs(i);



					OutcomeIterator it = noisygame.iterator();

					while(it.hasNext())
					{
						int[] outcome = it.next();

						double payoff = noisygame.getPayoff(outcome, i);

						payoff += Math.abs(p[1]);

						noisygame.setPayoff(outcome, i, payoff);


					}



				}





				/*
				 * now write the game
				 */



				String noisygamename = Parameters.GAME_FILES_PATH+"noisy/"+gamenum+Parameters.GAMUT_GAME_EXTENSION;

				//String absgmname = "k"+numberofclusters+"-"+game;

				try{

					PrintWriter pw = new PrintWriter(noisygamename,"UTF-8");
					SimpleOutput.writeGame(pw,noisygame);
					pw.close();
				}
				catch(Exception ex){
					System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
				}

			}



		} // end of for loop





	}





	public static void makeNoisyGame(double mean, double stdDev)
	{
		GaussianSampler gs = new GaussianSampler(mean, stdDev);

		/*
		 * make the directory for noisy games
		 */

		File file = new File(Parameters.GAME_FILES_PATH+"noisy");
		if (!file.exists()) {
			if (file.mkdir()) {

				System.out.println("Directory is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}


		for(int gamenum=0; gamenum<=10; gamenum++)
		{
			String gameName = Integer.toString(gamenum);

			MatrixGame matgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gameName+Parameters.GAMUT_GAME_EXTENSION));

			MatrixGame noisygame = new MatrixGame(matgame.getNumPlayers(), matgame.getNumActions());

			OutcomeIterator itr = matgame.iterator();

			while(itr.hasNext())
			{
				int[] outcome = itr.next();

				for(int player=0; player<2; player++)
				{
					double payoff = matgame.getPayoff(outcome, player);
					payoff= payoff+ gs.getSampleDouble();
					noisygame.setPayoff(outcome, player, payoff);


				}


			}



			/*
			 * normalize the payoffs
			 */

			for(int i=0; i<noisygame.getNumPlayers(); i++)
			{
				double[] p = noisygame.getExtremePayoffs(i);



				OutcomeIterator it = matgame.iterator();

				while(it.hasNext())
				{
					int[] outcome = it.next();

					double payoff = noisygame.getPayoff(outcome, i);

					payoff += Math.abs(p[1]);

					noisygame.setPayoff(outcome, i, payoff);


				}



			}





			/*
			 * now write the game
			 */



			String noisygamename = Parameters.GAME_FILES_PATH+"noisy/"+gamenum+Parameters.GAMUT_GAME_EXTENSION;

			//String absgmname = "k"+numberofclusters+"-"+game;

			try{

				PrintWriter pw = new PrintWriter(noisygamename,"UTF-8");
				SimpleOutput.writeGame(pw,noisygame);
				pw.close();
			}
			catch(Exception ex){
				System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
			}





		}





	}


	/*
	 * this method has to be called after setting the strategymapping
	 */
	public static double[] buildOriginalStrategyFromAbstractStrategy(StrategyMapping strategymap, MixedStrategy abstractstrategy, int originalnumberofactions, int abstractgamenumberofactions, int player)
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


		return originalstrategy;

	}


	public static double calcPercRemovedStrateg(int[] actionsafteried, int[] actionbeforeied )
	{

		double percremoved = Double.NEGATIVE_INFINITY;

		for(int i=0; i<2; i++)
		{

			if(percremoved < ((actionbeforeied[i]-actionsafteried[i])/actionbeforeied[i]))
			{
				percremoved = ((actionbeforeied[i]-actionsafteried[i])/actionbeforeied[i]);
			}


		}

		percremoved= percremoved/2;

		return percremoved;
	}


	/*public static void testClusteringAbstraction(int numberofcluster)
	{
		GamutModifier gm = new GamutModifier("game8");


		List<double[]>[] clusterforplayer1 = new List[numberofcluster];
		List<double[]>[] clusterforplayer2 = new List[numberofcluster];

		clusterforplayer1 = gm.clusterActions(numberofcluster, 0);
		clusterforplayer2 = gm.clusterActions(numberofcluster, 1);



		int[] numberofclustersforeachplayer = {numberofcluster, numberofcluster};
		StrategyMapping strategymap = new StrategyMapping(gm.returnGame().getNumPlayers(), gm.returnGame().getNumActions(), numberofclustersforeachplayer , gm.returnGame(), gm.getName());


		strategymap.mapActions(clusterforplayer1, 0);
		strategymap.mapActions(clusterforplayer2, 1);

		//int[] x = {0,1,2,0,1,2,1,0,2,2,3,3,0,2,1};

		//strategymap.mapActions(x, 1);

		Logger.log("\nend strategy mapping%%%%%%%%%%%%%%", false);

		Logger.log("\nStaring building abstract game%%%%%%%%%%%%%%", false);

		//	String abstractedgamename = strategymap.buildAbstractedGame();
		String abstractedgamename = strategymap.makeAbstractGame();




		QRESolver qre = new QRESolver(100);

		GamutModifier absgm = new GamutModifier(abstractedgamename);






		EmpiricalMatrixGame emg = new EmpiricalMatrixGame(absgm.returnGame());
		qre.setDecisionMode(QRESolver.DecisionMode.RAW);
		//	System.out.println(qre.solveGame(emg, 0));
		//	System.out.println(qre.solveGame(emg, 1));


		//	MixedStrategy abstractgamemixedstrategy1 = qre.solveGame(emg, 0);
		//	MixedStrategy abstractgamemixedstrategy2 = qre.solveGame(emg, 1);





		
		 * USe the CalcEpsilonBuondedEquilibrium class to calculate most robust strategy
		 * 1. call the constructor
		 * 
		 


		CalcEpsilonBuondedEquilibrium solverBuondedEquilibrium = new CalcEpsilonBuondedEquilibrium(strategymap, absgm.returnGame());
		solverBuondedEquilibrium.calcMaxEpsilon();



		MixedStrategy abstractgamemixedstrategy1 =  solverBuondedEquilibrium.getEpsilonBoundedEq(0);
		MixedStrategy abstractgamemixedstrategy2 = solverBuondedEquilibrium.getEpsilonBoundedEq(1);






		String strategy1 = abstractgamemixedstrategy1+"";
		String strategy2 = abstractgamemixedstrategy2+"";


		Logger.log("\nPlayer 0 equilibrium strategy "+strategy1, false);
		Logger.log("\nPlayer 1 equilibrium strategy "+strategy2, false);


		List<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(abstractgamemixedstrategy1);
		list.add(abstractgamemixedstrategy2);


		MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(g, distro);


		//	System.out.println("Expected Payoff player 0 : "+ expectedpayoff[0]+ "Expected Payoff player 1 : "+ expectedpayoff[1] );

		Logger.log("\n Expected Payoff abstract game player 0 : "+ expectedpayoff[0]+ "Expected Payoff abstract game player 1 : "+ expectedpayoff[1], false);







		
		 * calculate original game expected payoffs for players
		 

		double[] originalactionprobsplayer1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgamemixedstrategy1, gm.returnGame().getNumActions(0), numberofcluster, 0);

		double[] originalactionprobsplayer2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgamemixedstrategy2, gm.returnGame().getNumActions(1), numberofcluster, 1); 


		double[] orgprbpl1 = new double[originalactionprobsplayer1.length+1];

		orgprbpl1[0] =0;
		int index =1;
		for(double x: originalactionprobsplayer1)
		{
			orgprbpl1[index++] = x;
		}


		double[] orgprbpl2 = new double[originalactionprobsplayer2.length+1];

		orgprbpl2[0] =0;
		int index2 =1;
		for(double x: originalactionprobsplayer2)
		{
			orgprbpl2[index2++] = x;
		}








		MixedStrategy originalmixedstrategyplayer1 = new MixedStrategy(orgprbpl1);
		MixedStrategy originalmixedstrategyplayer2 = new MixedStrategy(orgprbpl2);

		String str1 = originalmixedstrategyplayer1+ " ";
		String str2 = originalmixedstrategyplayer2+ " ";



		System.out.println( "strategy player 1: "+abstractgamemixedstrategy1);
		System.out.println("strategy player 2: "+abstractgamemixedstrategy2);








		Logger.log("\n player 0 abstract strategy: "+abstractgamemixedstrategy1, false);
		Logger.log("\n player 1 abstract strategy: "+abstractgamemixedstrategy2, false);

		Logger.log("\n Player 0 original strategy "+ str1, false);
		Logger.log("\n Player 1 original strategy "+ str2, false);

		double normalizedplayer1 = originalmixedstrategyplayer1.checkIfNormalized();
		double normalizedplayer2 = originalmixedstrategyplayer2.checkIfNormalized();

		String normalized = "\n Player 0 original mixed strategy's normalized value "+normalizedplayer1;

		normalized += "\n Player 1 original mixed strategy's normalized value "+normalizedplayer2;
		Logger.log(normalized, false);






		List<MixedStrategy> originallist = new ArrayList<MixedStrategy>();
		originallist.add(originalmixedstrategyplayer1);
		originallist.add(originalmixedstrategyplayer2);


		//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
		OutcomeDistribution originaldistro = new OutcomeDistribution(originallist);
		double[]  originalexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), originaldistro);

		double epsilonz = SolverUtils.computeOutcomeStability(gm.returnGame(), originaldistro);
		Logger.log("\n Expected Payoff original game player 0 : "+ originalexpectedpayoff[0]+ "Expected Payoff original game player 1 : "+ originalexpectedpayoff[1], false);


		Logger.log("\n Epsilon in original game "+ epsilonz, false);










	}*/


	public static Game getGameWithUpperBound(Game game, StrategyMapping strategymap)
	{
		MatrixGame gamewithupperbound  = new MatrixGame(game.getNumPlayers(), game.getNumActions());

		for(int i=0; i<gamewithupperbound.getNumPlayers(); i++)
		{
			OutcomeIterator itr = gamewithupperbound.iterator();

			while(itr.hasNext())
			{
				int[] outcome = itr.next();
				double payoff = strategymap.calcMaxPayoffOriginalGame(outcome, i);

				Logger.logit("\n player "+ i+", for outcome ("+outcome[0]+", "+outcome[1]+") max payoff is set to "+ payoff);
				gamewithupperbound.setPayoff(outcome, i, payoff);

			}
		}

		return gamewithupperbound;


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




	public static double[][] clusteringAbstractionOld(int numberofclusters, int gamenumber)
	{

		/*
		 * for random restart we need to save the clusterings... and deltas...
		 */
		HashMap<Integer,List<double[]>[]> clustersplayer1 = new HashMap<Integer, List<double[]>[]>();
		HashMap<Integer,List<double[]>[]> clustersplayer2 = new HashMap<Integer, List<double[]>[]>();
		HashMap <Integer, Double> deltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> deltasplayer2 = new HashMap<Integer, Double>();

		HashMap <Integer, Double> maxdeltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer2 = new HashMap<Integer, Double>();

		double[][] result = new double[3][2];
		final  int RANDOM_RESTART_ITERATION = 6; // 
		int timecounter =0;
		//	String gametyp="";

		/*if(gametype==0)
		{
			gametyp="randomgame";
		}
		else if(gametype==1)
		{
			gametyp= "logicalgame";
		}*/

		String game = Integer.toString(gamenumber);




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

		Main.percremovedstrategy += GamutModifier.calcPercRemovedStrateg(numberofactionafterIED, actionsbeforeied);


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

			//numberofclusters = min;

			//return result; 

			// delta is zero. solve the game and calculate epsilon 



			//	 first make the game file



			String absgamename = Parameters.GAME_FILES_PATH+Main.experimentdir+"/"+"k"+numberofclusters+"-"+game+Parameters.GAMUT_GAME_EXTENSION;
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

			GamutModifier absgm = new GamutModifier(Main.experimentdir+"/"+absgmname);

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
			timecounter++;

			Logger.log("After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters, false);
			Logger.logit("\n After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters);

			GamutModifier gm = new GamutModifier(game); 
			gm.setMg(gamewodominatedstrategies);



			//test
			//GamutModifier gm = new GamutModifier("gamene");


			for(int i=0;i<2;i++)
			{
				double[] val = gm.returnGame().getExtremePayoffs(i);

				Logger.log("\n player "+i+" extreme payoffs "+ val[0]+" "+val[1], false);

			}

			List<double[]>[] clusterforplayer1 = new List[numberofclusters];
			List<double[]>[] clusterforplayer2 = new List[numberofclusters];

			for(int randomitr =0; randomitr<RANDOM_RESTART_ITERATION; randomitr++)
			{	


				if(randomitr<3)
				{
					Main.setRAND_ACTION_INIT_TO_CLUSTERS(true);
					Main.setRAND_POINTS_FROM_OBSERVATION(false);

				}
				else
				{
					Main.setRAND_ACTION_INIT_TO_CLUSTERS(false);
					Main.setRAND_POINTS_FROM_OBSERVATION(true);

				}

				clusterforplayer1 = new List[numberofclusters];
				clusterforplayer2 = new List[numberofclusters];


				Main.START_TIME = System.currentTimeMillis();


				//test
				clusterforplayer1 = gm.clusterActions(numberofclusters, 0);


				//clusterforplayer1 = gm.clusterActions(numberofclusters, 0);

				Main.END_TIME = System.currentTimeMillis();

				Main.clustertime[0] +=  Main.END_TIME - Main.START_TIME;



				Main.START_TIME = System.currentTimeMillis();

				//test
				clusterforplayer2 = gm.clusterActions(numberofclusters, 1);



				//clusterforplayer2 = gm.clusterActions(numberofclusters, 1);

				Main.END_TIME = System.currentTimeMillis();

				Main.clustertime[1] +=  Main.END_TIME - Main.START_TIME;



				/*
				 * calculate the delta, for random restarts
				 */


				double[] delta1 = GamutModifier.calculateDelta(gm.returnGame(), clusterforplayer1, 0);
				double[] delta2 = GamutModifier.calculateDelta(gm.returnGame(), clusterforplayer2, 1);



				Logger.log("\n\n Player 0 clusters after CAA\n", false);


				for(int i=0; i<clusterforplayer1.length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(double[] x: clusterforplayer1[i])
					{
						Logger.log(x[0]+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);

				for(double x: delta1)
				{
					String y = x+ " ";
					Logger.log(y, false);
				}

				//Logger.log("\n", true);

				Logger.log("\n\nPlayer 1 clusters after CAA\n ", false);


				for(int i=0; i<clusterforplayer2.length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(double[] x: clusterforplayer2[i])
					{
						Logger.log(x[0]+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);

				for(double x: delta2)
				{
					String y = x+ " ";
					Logger.log(y, false);
				}


				/*
				 * do the code for max delta
				 * 
				 */

				if( Main.isMaxDelta())
				{
					double max1delta =Double.NEGATIVE_INFINITY; 

					for(int i=0; i< delta1.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta1[i]);
						if(max1delta<delta1[i])
							max1delta = delta1[i];
					}


					double max2delta =Double.NEGATIVE_INFINITY; 

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);
						if(max2delta<delta2[i])
							max2delta = delta2[i];
					}

					deltasplayer1.put(randomitr, max1delta);
					deltasplayer2.put(randomitr, max2delta);

					Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ max1delta, false);
					Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ max2delta, false);


				}

				if(Main.isAvrgDelta())
				{
					double avg1delta =0; 


					/*
					 * calculate average delta
					 */

					for(int i=0; i< delta1.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta1[i]);
						//if(max1delta<delta1[i])
						avg1delta = avg1delta+ delta1[i];
					}

					avg1delta = avg1delta/delta1.length;



					//System.out.println("Delta: "+max1 + " "+ delta2);



					/*	System.out.print("\n\n");
				for(int i=0; i<gm.returnGame().getNumActions(0); i++)
				{
					for(int j=0; j< gm.returnGame().getNumActions(1); j++)
					{
						int[] outcome = {i+1,j+1};
						System.out.print(" "+ gm.returnGame().getPayoff(outcome, 1));
					}
					System.out.print("\n");
				}*/

					double avg2delta =0; 

					//System.out.println("Deltas: "+ deltas);




					/*
					 * average delta
					 */

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);

						avg2delta = avg2delta + delta2[i];
					}

					avg2delta = avg2delta/delta2.length;


					deltasplayer1.put(randomitr, avg1delta);
					deltasplayer2.put(randomitr, avg2delta);

					Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ avg1delta, false);
					Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ avg2delta, false);



					/*
					 * also need to calculate the max delta to show in the graph
					 */

					double maxdelta1 = Double.NEGATIVE_INFINITY;

					for(int i=0;i<delta1.length; i++)
					{
						if(maxdelta1 < delta1[i])
						{
							maxdelta1 = delta1[i];
						}
					}


					double maxdelta2 = Double.NEGATIVE_INFINITY;

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);
						if(maxdelta2 < delta2[i])
						{
							maxdelta2 = delta2[i];
						}
					}


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


				clustersplayer1.put(randomitr, clusterforplayer1);
				clustersplayer2.put(randomitr, clusterforplayer2);



			} // end of random iteration loop


			/*
			 * calculate the average time
			 */

			for(int p =0; p<gm.returnGame().getNumPlayers(); p++)
			{
				Main.clustertime[p] = Main.clustertime[p]/timecounter;
			}




			/*
			 * now find the best delta. minimum one. 
			 */


			Logger.log("\n Selecting minimum delta", false);
			double[] mindeltas = new double[gm.returnGame().getNumPlayers()]; // will contain the minimum delta for 2 players
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


					clusterforplayer1 = clustersplayer1.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's delta and clustering is used for player "+ i, false );


					if(Main.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer1.get(minindex);
						Logger.log("\n Player 0 max delta for the selected cluster : "+maxdeltas[i] , false);
					}	


					Logger.log("\n\n Final clustering for Player 0\n", false);


					for(int k=0; k<clusterforplayer1.length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(double[] x: clusterforplayer1[k])
						{
							Logger.log(x[0]+", ", false);
						}
						Logger.log("\n", false);
					}




				}
				else if(i==1) // player 2
				{

					clusterforplayer2 = clustersplayer2.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's, delta and clustering is used for player "+ i, false );

					if(Main.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer2.get(minindex);
						Logger.log("\n Player 1 max delta for the selected cluster : "+maxdeltas[i] , false);
					}


					Logger.log("\n\n Final clustering for Player 1\n", false);


					for(int k=0; k<clusterforplayer2.length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(double[] x: clusterforplayer2[k])
						{
							Logger.log(x[0]+", ", false);
						}
						Logger.log("\n", false);
					}




				}



			}





			Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);



			Logger.log("\n clustering done################",false);

			int[] numberofclustersforeachplayer = new int[gm.returnGame().getNumPlayers()];
			for(int i =0; i< gm.returnGame().getNumPlayers(); i++)
			{
				numberofclustersforeachplayer[i] = numberofclusters;
			}


			/* For the strategy map
			 * 1. give the constructor appropriate variables.
			 * 2. pass the cluster mapping to the strategy map or pass the array, which contain the cluster number for each actions, for each player

			 */	
			//		System.out.println("Staring strategy mapping%%%%%%%%%%%%%%");

			StrategyMapping strategymap = new StrategyMapping(gm.returnGame().getNumPlayers(), gm.returnGame().getNumActions(), numberofclustersforeachplayer, gm.returnGame(), gm.getName());


			//strategymap.mapActions(clusterforplayer1, 0);
			//strategymap.mapActions(clusterforplayer2, 1);

			//int[] x = {0,1,2,0,1,2,1,0,2,2,3,3,0,2,1};

			//strategymap.mapActions(x, 1);

			Logger.log("\nend strategy mapping%%%%%%%%%%%%%%", false);

			Logger.log("\nStaring building abstract game%%%%%%%%%%%%%%", false);

			//	String abstractedgamename = strategymap.buildAbstractedGame();
			String abstractedgamename = "";strategymap.makeAbstractGame();




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


			double[] originalqreprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalqreprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);



			double[] orgqreprbpl1 = new double[originalqreprofile1.length+1];

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
			}


			MixedStrategy origqreprofile1 = new MixedStrategy(orgqreprbpl1);
			MixedStrategy origqreprofile2 = new MixedStrategy(orgqreprbpl2);


			List<MixedStrategy> originalqrelist = new ArrayList<MixedStrategy>();
			originalqrelist.add(origqreprofile1);
			originalqrelist.add(origqreprofile2);



			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origqredistro = new OutcomeDistribution(originalqrelist);
			double[]  originalqrepayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origqredistro);

			double qreepsilon = SolverUtils.computeOutcomeStability(gm.returnGame(), origqredistro);
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


			Game gmwithmaxexpectedpayoff = GamutModifier.getGameWithMaxExpectedPayoff(absgm.returnGame(), strategymap);


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


			double[] originalmaxexpectedprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalmaxexpectedprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);



			double[] orgmaxexpectedprbpl1 = new double[originalmaxexpectedprofile1.length+1];

			orgmaxexpectedprbpl1[0] =0;
			int index2 =1;
			for(double x: originalmaxexpectedprofile1)
			{
				orgmaxexpectedprbpl1[index2++] = x;
			}


			double[] orgmaxexpectedprbpl2 = new double[originalmaxexpectedprofile2.length+1];

			orgmaxexpectedprbpl2[0] =0;
			int index22 =1;
			for(double x: originalmaxexpectedprofile2)
			{
				orgmaxexpectedprbpl2[index22++] = x;
			}








			MixedStrategy orginalmaxexpectedprofile1 = new MixedStrategy(orgmaxexpectedprbpl1);
			MixedStrategy orginalmaxexpectedprofile2 = new MixedStrategy(orgmaxexpectedprbpl2);



			/*
			 * build original game strategy for subgame
			 */


			/*	MixedStrategy[] orginalmaxexpectedprofile = strategymap.getStrategySubgameSols(abstractmaxexpectedprofile);


			System.out.println("orginalmaxexpectedprofile[0].checkIfNormalized() "+ orginalmaxexpectedprofile[0].checkIfNormalized());
			System.out.println("orginalmaxexpectedprofile[1].checkIfNormalized() "+ orginalmaxexpectedprofile[1].checkIfNormalized());

			 */



			List<MixedStrategy> originalmaxexpectedlist = new ArrayList<MixedStrategy>();
			originalmaxexpectedlist.add(orginalmaxexpectedprofile1);
			originalmaxexpectedlist.add(orginalmaxexpectedprofile2);

			//	originalmaxexpectedlist.add(orginalmaxexpectedprofile[0]);
			//originalmaxexpectedlist.add(orginalmaxexpectedprofile[1]);


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origmaxexpecteddistro = new OutcomeDistribution(originalmaxexpectedlist);
			double[]  originalmaxexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origmaxexpecteddistro);

			double epsilonmaxexpected = SolverUtils.computeOutcomeStability(gm.returnGame(), origmaxexpecteddistro);
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





			double[] originalactionprobsplayer1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);

			double[] originalactionprobsplayer2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1); 


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
			double[]  originalexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), originaldistro);

			double epsilonz = SolverUtils.computeOutcomeStability(gm.returnGame(), originaldistro);
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


			if(Main.isAvrgDelta())
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


	public static double[][] clusteringAbstractionOldBothPlayer(int numberofclusters, int gamenumber)
	{

		/*
		 * for random restart we need to save the clusterings... and deltas...
		 */
		HashMap<Integer,List<double[]>[]> clustersplayer1 = new HashMap<Integer, List<double[]>[]>();
		HashMap<Integer,List<double[]>[]> clustersplayer2 = new HashMap<Integer, List<double[]>[]>();
		HashMap <Integer, Double> deltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> deltasplayer2 = new HashMap<Integer, Double>();

		HashMap <Integer, Double> maxdeltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer2 = new HashMap<Integer, Double>();

		double[][] result = new double[3][2];
		final  int RANDOM_RESTART_ITERATION = 6; // 
		int timecounter =0;
		//	String gametyp="";

		/*if(gametype==0)
		{
			gametyp="randomgame";
		}
		else if(gametype==1)
		{
			gametyp= "logicalgame";
		}*/

		String game =Integer.toString(gamenumber);




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

		Main.percremovedstrategy += GamutModifier.calcPercRemovedStrateg(numberofactionafterIED, actionsbeforeied);


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

			//numberofclusters = min;

			//return result; 

			// delta is zero. solve the game and calculate epsilon 



			//	 first make the game file



			String absgamename = Parameters.GAME_FILES_PATH+Main.experimentdir+"/"+"k"+numberofclusters+"-"+game+Parameters.GAMUT_GAME_EXTENSION;
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

			GamutModifier absgm = new GamutModifier(Main.experimentdir+"/"+absgmname);

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
			timecounter++;

			Logger.log("After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters, false);
			Logger.logit("\n After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters);

			GamutModifier gm = new GamutModifier(game); 
			gm.setMg(gamewodominatedstrategies);



			//test
			//GamutModifier gm = new GamutModifier("gamene");


			for(int i=0;i<2;i++)
			{
				double[] val = gm.returnGame().getExtremePayoffs(i);

				Logger.log("\n player "+i+" extreme payoffs "+ val[0]+" "+val[1], false);

			}

			List<double[]>[] clusterforplayer1 = new List[numberofclusters];
			List<double[]>[] clusterforplayer2 = new List[numberofclusters];

			for(int randomitr =0; randomitr<RANDOM_RESTART_ITERATION; randomitr++)
			{	


				if(randomitr<3)
				{
					Main.setRAND_ACTION_INIT_TO_CLUSTERS(true);
					Main.setRAND_POINTS_FROM_OBSERVATION(false);

				}
				else
				{
					Main.setRAND_ACTION_INIT_TO_CLUSTERS(false);
					Main.setRAND_POINTS_FROM_OBSERVATION(true);

				}

				clusterforplayer1 = new List[numberofclusters];
				clusterforplayer2 = new List[numberofclusters];


				Main.START_TIME = System.currentTimeMillis();


				//test
				clusterforplayer1 = gm.clusterActions2(numberofclusters, 0);


				//clusterforplayer1 = gm.clusterActions(numberofclusters, 0);

				Main.END_TIME = System.currentTimeMillis();

				Main.clustertime[0] +=  Main.END_TIME - Main.START_TIME;



				Main.START_TIME = System.currentTimeMillis();

				//test
				clusterforplayer2 = gm.clusterActions2(numberofclusters, 1);



				//clusterforplayer2 = gm.clusterActions(numberofclusters, 1);

				Main.END_TIME = System.currentTimeMillis();

				Main.clustertime[1] +=  Main.END_TIME - Main.START_TIME;



				/*
				 * calculate the delta, for random restarts
				 */


				double[] delta1 = GamutModifier.calculateDelta2(gm.returnGame(), clusterforplayer1, 0);
				double[] delta2 = GamutModifier.calculateDelta2(gm.returnGame(), clusterforplayer2, 1);



				Logger.log("\n\n Player 0 clusters after CAA\n", false);


				for(int i=0; i<clusterforplayer1.length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(double[] x: clusterforplayer1[i])
					{
						Logger.log(x[0]+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);

				for(double x: delta1)
				{
					String y = x+ " ";
					Logger.log(y, false);
				}

				//Logger.log("\n", true);

				Logger.log("\n\nPlayer 1 clusters after CAA\n ", false);


				for(int i=0; i<clusterforplayer2.length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(double[] x: clusterforplayer2[i])
					{
						Logger.log(x[0]+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);

				for(double x: delta2)
				{
					String y = x+ " ";
					Logger.log(y, false);
				}


				/*
				 * do the code for max delta
				 * 
				 */

				if( Main.isMaxDelta())
				{
					double max1delta =Double.NEGATIVE_INFINITY; 

					for(int i=0; i< delta1.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta1[i]);
						if(max1delta<delta1[i])
							max1delta = delta1[i];
					}


					double max2delta =Double.NEGATIVE_INFINITY; 

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);
						if(max2delta<delta2[i])
							max2delta = delta2[i];
					}

					deltasplayer1.put(randomitr, max1delta);
					deltasplayer2.put(randomitr, max2delta);

					Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ max1delta, false);
					Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ max2delta, false);


				}

				if(Main.isAvrgDelta())
				{
					double avg1delta =0; 


					/*
					 * calculate average delta
					 */

					for(int i=0; i< delta1.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta1[i]);
						//if(max1delta<delta1[i])
						avg1delta = avg1delta+ delta1[i];
					}

					avg1delta = avg1delta/delta1.length;



					//System.out.println("Delta: "+max1 + " "+ delta2);



					/*	System.out.print("\n\n");
				for(int i=0; i<gm.returnGame().getNumActions(0); i++)
				{
					for(int j=0; j< gm.returnGame().getNumActions(1); j++)
					{
						int[] outcome = {i+1,j+1};
						System.out.print(" "+ gm.returnGame().getPayoff(outcome, 1));
					}
					System.out.print("\n");
				}*/

					double avg2delta =0; 

					//System.out.println("Deltas: "+ deltas);




					/*
					 * average delta
					 */

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);

						avg2delta = avg2delta + delta2[i];
					}

					avg2delta = avg2delta/delta2.length;


					deltasplayer1.put(randomitr, avg1delta);
					deltasplayer2.put(randomitr, avg2delta);

					Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ avg1delta, false);
					Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ avg2delta, false);



					/*
					 * also need to calculate the max delta to show in the graph
					 */

					double maxdelta1 = Double.NEGATIVE_INFINITY;

					for(int i=0;i<delta1.length; i++)
					{
						if(maxdelta1 < delta1[i])
						{
							maxdelta1 = delta1[i];
						}
					}


					double maxdelta2 = Double.NEGATIVE_INFINITY;

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);
						if(maxdelta2 < delta2[i])
						{
							maxdelta2 = delta2[i];
						}
					}


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


				clustersplayer1.put(randomitr, clusterforplayer1);
				clustersplayer2.put(randomitr, clusterforplayer2);



			} // end of random iteration loop


			/*
			 * calculate the average time
			 */

			for(int p =0; p<gm.returnGame().getNumPlayers(); p++)
			{
				Main.clustertime[p] = Main.clustertime[p]/timecounter;
			}




			/*
			 * now find the best delta. minimum one. 
			 */


			Logger.log("\n Selecting minimum delta", false);
			double[] mindeltas = new double[gm.returnGame().getNumPlayers()]; // will contain the minimum delta for 2 players
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


					clusterforplayer1 = clustersplayer1.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's delta and clustering is used for player "+ i, false );


					if(Main.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer1.get(minindex);
						Logger.log("\n Player 0 max delta for the selected cluster : "+maxdeltas[i] , false);
					}	


					Logger.log("\n\n Final clustering for Player 0\n", false);


					for(int k=0; k<clusterforplayer1.length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(double[] x: clusterforplayer1[k])
						{
							Logger.log(x[0]+", ", false);
						}
						Logger.log("\n", false);
					}




				}
				else if(i==1) // player 2
				{

					clusterforplayer2 = clustersplayer2.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's, delta and clustering is used for player "+ i, false );

					if(Main.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer2.get(minindex);
						Logger.log("\n Player 1 max delta for the selected cluster : "+maxdeltas[i] , false);
					}


					Logger.log("\n\n Final clustering for Player 1\n", false);


					for(int k=0; k<clusterforplayer2.length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(double[] x: clusterforplayer2[k])
						{
							Logger.log(x[0]+", ", false);
						}
						Logger.log("\n", false);
					}




				}



			}





			Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);



			Logger.log("\n clustering done################",false);

			int[] numberofclustersforeachplayer = new int[gm.returnGame().getNumPlayers()];
			for(int i =0; i< gm.returnGame().getNumPlayers(); i++)
			{
				numberofclustersforeachplayer[i] = numberofclusters;
			}


			/* For the strategy map
			 * 1. give the constructor appropriate variables.
			 * 2. pass the cluster mapping to the strategy map or pass the array, which contain the cluster number for each actions, for each player

			 */	
			//		System.out.println("Staring strategy mapping%%%%%%%%%%%%%%");

			StrategyMapping strategymap = new StrategyMapping(gm.returnGame().getNumPlayers(), gm.returnGame().getNumActions(), numberofclustersforeachplayer, gm.returnGame(), gm.getName());


		//	strategymap.mapActions(clusterforplayer1, 0);
		//	strategymap.mapActions(clusterforplayer2, 1);

			//int[] x = {0,1,2,0,1,2,1,0,2,2,3,3,0,2,1};

			//strategymap.mapActions(x, 1);

			Logger.log("\nend strategy mapping%%%%%%%%%%%%%%", false);

			Logger.log("\nStaring building abstract game%%%%%%%%%%%%%%", false);

			//	String abstractedgamename = strategymap.buildAbstractedGame();
			String abstractedgamename = "";strategymap.makeAbstractGame();




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


			double[] originalqreprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalqreprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);



			double[] orgqreprbpl1 = new double[originalqreprofile1.length+1];

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
			}


			MixedStrategy origqreprofile1 = new MixedStrategy(orgqreprbpl1);
			MixedStrategy origqreprofile2 = new MixedStrategy(orgqreprbpl2);


			List<MixedStrategy> originalqrelist = new ArrayList<MixedStrategy>();
			originalqrelist.add(origqreprofile1);
			originalqrelist.add(origqreprofile2);



			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origqredistro = new OutcomeDistribution(originalqrelist);
			double[]  originalqrepayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origqredistro);

			double qreepsilon = SolverUtils.computeOutcomeStability(gm.returnGame(), origqredistro);
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


			Game gmwithmaxexpectedpayoff = GamutModifier.getGameWithMaxExpectedPayoff(absgm.returnGame(), strategymap);


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


			double[] originalmaxexpectedprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalmaxexpectedprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);



			double[] orgmaxexpectedprbpl1 = new double[originalmaxexpectedprofile1.length+1];

			orgmaxexpectedprbpl1[0] =0;
			int index2 =1;
			for(double x: originalmaxexpectedprofile1)
			{
				orgmaxexpectedprbpl1[index2++] = x;
			}


			double[] orgmaxexpectedprbpl2 = new double[originalmaxexpectedprofile2.length+1];

			orgmaxexpectedprbpl2[0] =0;
			int index22 =1;
			for(double x: originalmaxexpectedprofile2)
			{
				orgmaxexpectedprbpl2[index22++] = x;
			}








			MixedStrategy orginalmaxexpectedprofile1 = new MixedStrategy(orgmaxexpectedprbpl1);
			MixedStrategy orginalmaxexpectedprofile2 = new MixedStrategy(orgmaxexpectedprbpl2);



			/*
			 * build original game strategy for subgame
			 */


			/*	MixedStrategy[] orginalmaxexpectedprofile = strategymap.getStrategySubgameSols(abstractmaxexpectedprofile);


			System.out.println("orginalmaxexpectedprofile[0].checkIfNormalized() "+ orginalmaxexpectedprofile[0].checkIfNormalized());
			System.out.println("orginalmaxexpectedprofile[1].checkIfNormalized() "+ orginalmaxexpectedprofile[1].checkIfNormalized());

			 */



			List<MixedStrategy> originalmaxexpectedlist = new ArrayList<MixedStrategy>();
			originalmaxexpectedlist.add(orginalmaxexpectedprofile1);
			originalmaxexpectedlist.add(orginalmaxexpectedprofile2);

			//	originalmaxexpectedlist.add(orginalmaxexpectedprofile[0]);
			//originalmaxexpectedlist.add(orginalmaxexpectedprofile[1]);


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origmaxexpecteddistro = new OutcomeDistribution(originalmaxexpectedlist);
			double[]  originalmaxexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origmaxexpecteddistro);

			double epsilonmaxexpected = SolverUtils.computeOutcomeStability(gm.returnGame(), origmaxexpecteddistro);
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





			double[] originalactionprobsplayer1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);

			double[] originalactionprobsplayer2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1); 


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
			double[]  originalexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), originaldistro);

			double epsilonz = SolverUtils.computeOutcomeStability(gm.returnGame(), originaldistro);
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


			if(Main.isAvrgDelta())
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











	public static double[][] clusteringAbstraction(int numberofclusters, int gamenumber)
	{

		/*
		 * for random restart we need to save the clusterings... and deltas...
		 */
		HashMap<Integer,List<double[]>[]> clustersplayer1 = new HashMap<Integer, List<double[]>[]>();
		HashMap<Integer,List<double[]>[]> clustersplayer2 = new HashMap<Integer, List<double[]>[]>();
		HashMap <Integer, Double> deltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> deltasplayer2 = new HashMap<Integer, Double>();

		HashMap <Integer, Double> maxdeltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer2 = new HashMap<Integer, Double>();

		double[][] result = new double[3][2];
		final  int RANDOM_RESTART_ITERATION = 6; // 
		int timecounter =0;
		//	String gametyp="";

		/*if(gametype==0)
		{
			gametyp="randomgame";
		}
		else if(gametype==1)
		{
			gametyp= "logicalgame";
		}*/

		String game = Integer.toString(gamenumber);




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

		Main.percremovedstrategy += GamutModifier.calcPercRemovedStrateg(numberofactionafterIED, actionsbeforeied);


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

			//numberofclusters = min;

			//return result; 

			// delta is zero. solve the game and calculate epsilon 



			//	 first make the game file



			String absgamename = Parameters.GAME_FILES_PATH+Main.experimentdir+"/"+"k"+numberofclusters+"-"+game+Parameters.GAMUT_GAME_EXTENSION;
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

			GamutModifier absgm = new GamutModifier(Main.experimentdir+"/"+absgmname);

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
			timecounter++;

			Logger.log("After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters, false);
			Logger.logit("\n After IED number of action in game "+game+ "is greater than number of clusters "+ numberofclusters);

			GamutModifier gm = new GamutModifier(game); 
			gm.setMg(gamewodominatedstrategies);



			//test
			//GamutModifier gm = new GamutModifier("gamene");


			for(int i=0;i<2;i++)
			{
				double[] val = gm.returnGame().getExtremePayoffs(i);

				Logger.log("\n player "+i+" extreme payoffs "+ val[0]+" "+val[1], false);

			}

			List<double[]>[] clusterforplayer1 = new List[numberofclusters];
			List<double[]>[] clusterforplayer2 = new List[numberofclusters];

			for(int randomitr =0; randomitr<RANDOM_RESTART_ITERATION; randomitr++)
			{	


				if(randomitr<3)
				{
					Main.setRAND_ACTION_INIT_TO_CLUSTERS(true);
					Main.setRAND_POINTS_FROM_OBSERVATION(false);

				}
				else
				{
					Main.setRAND_ACTION_INIT_TO_CLUSTERS(false);
					Main.setRAND_POINTS_FROM_OBSERVATION(true);

				}

				clusterforplayer1 = new List[numberofclusters];
				clusterforplayer2 = new List[numberofclusters];


				Main.START_TIME = System.currentTimeMillis();


				//test
				clusterforplayer1 = gm.clusterActions(numberofclusters, 0);


				//clusterforplayer1 = gm.clusterActions(numberofclusters, 0);

				Main.END_TIME = System.currentTimeMillis();

				Main.clustertime[0] +=  Main.END_TIME - Main.START_TIME;



				Main.START_TIME = System.currentTimeMillis();

				//test
				clusterforplayer2 = gm.clusterActions(numberofclusters, 1);



				//clusterforplayer2 = gm.clusterActions(numberofclusters, 1);

				Main.END_TIME = System.currentTimeMillis();

				Main.clustertime[1] +=  Main.END_TIME - Main.START_TIME;



				/*
				 * calculate the delta, for random restarts
				 */


				double[] delta1 = GamutModifier.calculateDelta(gm.returnGame(), clusterforplayer1, 0);
				double[] delta2 = GamutModifier.calculateDelta(gm.returnGame(), clusterforplayer2, 1);



				Logger.log("\n\n Player 0 clusters after CAA\n", false);


				for(int i=0; i<clusterforplayer1.length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(double[] x: clusterforplayer1[i])
					{
						Logger.log(x[0]+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);

				for(double x: delta1)
				{
					String y = x+ " ";
					Logger.log(y, false);
				}

				//Logger.log("\n", true);

				Logger.log("\n\nPlayer 1 clusters after CAA\n ", false);


				for(int i=0; i<clusterforplayer2.length; i++)
				{
					Logger.log("Cluster "+ i, false);

					for(double[] x: clusterforplayer2[i])
					{
						Logger.log(x[0]+", ", false);
					}
					Logger.log("\n", false);
				}

				Logger.log("Delta: ", false);

				for(double x: delta2)
				{
					String y = x+ " ";
					Logger.log(y, false);
				}


				/*
				 * do the code for max delta
				 * 
				 */

				if( Main.isMaxDelta())
				{
					double max1delta =Double.NEGATIVE_INFINITY; 

					for(int i=0; i< delta1.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta1[i]);
						if(max1delta<delta1[i])
							max1delta = delta1[i];
					}


					double max2delta =Double.NEGATIVE_INFINITY; 

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);
						if(max2delta<delta2[i])
							max2delta = delta2[i];
					}

					deltasplayer1.put(randomitr, max1delta);
					deltasplayer2.put(randomitr, max2delta);

					Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ max1delta, false);
					Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ max2delta, false);


				}

				if(Main.isAvrgDelta())
				{
					double avg1delta =0; 


					/*
					 * calculate average delta
					 */

					for(int i=0; i< delta1.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta1[i]);
						//if(max1delta<delta1[i])
						avg1delta = avg1delta+ delta1[i];
					}

					avg1delta = avg1delta/delta1.length;



					//System.out.println("Delta: "+max1 + " "+ delta2);



					/*	System.out.print("\n\n");
				for(int i=0; i<gm.returnGame().getNumActions(0); i++)
				{
					for(int j=0; j< gm.returnGame().getNumActions(1); j++)
					{
						int[] outcome = {i+1,j+1};
						System.out.print(" "+ gm.returnGame().getPayoff(outcome, 1));
					}
					System.out.print("\n");
				}*/

					double avg2delta =0; 

					//System.out.println("Deltas: "+ deltas);




					/*
					 * average delta
					 */

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);

						avg2delta = avg2delta + delta2[i];
					}

					avg2delta = avg2delta/delta2.length;


					deltasplayer1.put(randomitr, avg1delta);
					deltasplayer2.put(randomitr, avg2delta);

					Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ avg1delta, false);
					Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ avg2delta, false);



					/*
					 * also need to calculate the max delta to show in the graph
					 */

					double maxdelta1 = Double.NEGATIVE_INFINITY;

					for(int i=0;i<delta1.length; i++)
					{
						if(maxdelta1 < delta1[i])
						{
							maxdelta1 = delta1[i];
						}
					}


					double maxdelta2 = Double.NEGATIVE_INFINITY;

					for(int i=0; i< delta2.length; i++)
					{
						//System.out.println("Deltas["+i+"]: "+ delta2[i]);
						if(maxdelta2 < delta2[i])
						{
							maxdelta2 = delta2[i];
						}
					}


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


				clustersplayer1.put(randomitr, clusterforplayer1);
				clustersplayer2.put(randomitr, clusterforplayer2);



			} // end of random iteration loop


			/*
			 * calculate the average time
			 */

			for(int p =0; p<gm.returnGame().getNumPlayers(); p++)
			{
				Main.clustertime[p] = Main.clustertime[p]/timecounter;
			}




			/*
			 * now find the best delta. minimum one. 
			 */


			Logger.log("\n Selecting minimum delta", false);
			double[] mindeltas = new double[gm.returnGame().getNumPlayers()]; // will contain the minimum delta for 2 players
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


					clusterforplayer1 = clustersplayer1.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's delta and clustering is used for player "+ i, false );


					if(Main.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer1.get(minindex);
						Logger.log("\n Player 0 max delta for the selected cluster : "+maxdeltas[i] , false);
					}	


					Logger.log("\n\n Final clustering for Player 0\n", false);


					for(int k=0; k<clusterforplayer1.length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(double[] x: clusterforplayer1[k])
						{
							Logger.log(x[0]+", ", false);
						}
						Logger.log("\n", false);
					}




				}
				else if(i==1) // player 2
				{

					clusterforplayer2 = clustersplayer2.get(minindex);
					Logger.log("\n"+minindex+"th random iteration's, delta and clustering is used for player "+ i, false );

					if(Main.isAvrgDelta())
					{
						maxdeltas[i] = maxdeltasplayer2.get(minindex);
						Logger.log("\n Player 1 max delta for the selected cluster : "+maxdeltas[i] , false);
					}


					Logger.log("\n\n Final clustering for Player 1\n", false);


					for(int k=0; k<clusterforplayer2.length; k++)
					{
						Logger.log("Cluster "+ k, false);

						for(double[] x: clusterforplayer2[k])
						{
							Logger.log(x[0]+", ", false);
						}
						Logger.log("\n", false);
					}




				}



			}





			Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);



			Logger.log("\n clustering done################",false);

			int[] numberofclustersforeachplayer = new int[gm.returnGame().getNumPlayers()];
			for(int i =0; i< gm.returnGame().getNumPlayers(); i++)
			{
				numberofclustersforeachplayer[i] = numberofclusters;
			}


			/* For the strategy map
			 * 1. give the constructor appropriate variables.
			 * 2. pass the cluster mapping to the strategy map or pass the array, which contain the cluster number for each actions, for each player

			 */	
			//		System.out.println("Staring strategy mapping%%%%%%%%%%%%%%");

			StrategyMapping strategymap = new StrategyMapping(gm.returnGame().getNumPlayers(), gm.returnGame().getNumActions(), numberofclustersforeachplayer, gm.returnGame(), gm.getName());


		//	strategymap.mapActions(clusterforplayer1, 0);
		//	strategymap.mapActions(clusterforplayer2, 1);

			//int[] x = {0,1,2,0,1,2,1,0,2,2,3,3,0,2,1};

			//strategymap.mapActions(x, 1);

			Logger.log("\nend strategy mapping%%%%%%%%%%%%%%", false);

			Logger.log("\nStaring building abstract game%%%%%%%%%%%%%%", false);

			//	String abstractedgamename = strategymap.buildAbstractedGame();
			String abstractedgamename = "";strategymap.makeAbstractGame();


			/*

			QRESolver qre = new QRESolver(100);

			GamutModifier absgm = new GamutModifier(abstractedgamename);
			 */



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
			/*
			EmpiricalMatrixGame emg = new EmpiricalMatrixGame(absgm.returnGame());
			qre.setDecisionMode(QRESolver.DecisionMode.RAW);
			//	System.out.println(qre.solveGame(emg, 0));
			//	System.out.println(qre.solveGame(emg, 1));


			MixedStrategy abstractqreprofile1 = qre.solveGame(emg, 0);
			MixedStrategy abstractqreprofile2 = qre.solveGame(emg, 1);


			double[] originalqreprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalqreprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractqreprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);



			double[] orgqreprbpl1 = new double[originalqreprofile1.length+1];

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
			}


			MixedStrategy origqreprofile1 = new MixedStrategy(orgqreprbpl1);
			MixedStrategy origqreprofile2 = new MixedStrategy(orgqreprbpl2);


			List<MixedStrategy> originalqrelist = new ArrayList<MixedStrategy>();
			originalqrelist.add(origqreprofile1);
			originalqrelist.add(origqreprofile2);



			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origqredistro = new OutcomeDistribution(originalqrelist);
			double[]  originalqrepayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origqredistro);

			double qreepsilon = SolverUtils.computeOutcomeStability(gm.returnGame(), origqredistro);
			Logger.logit("\n Expected Payoff for qre profile player 0 : "+ originalqrepayoff[0]+ "Expected Payoff original game player 1 : "+ originalqrepayoff[1]);

			Logger.logit("\n Final EPsilon for qre profile "+ qreepsilon);

			 */










			////////////////////////////////





			/*
			 * SUbgame NE
			 * 1. Build the abstract game. 
			 * 2. Use the method for subgame NE
			 */


			Game abstractsubgame = strategymap.recAbstract(numberofclusters);


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


			//	Game gmwithmaxexpectedpayoff = GamutModifier.getGameWithMaxExpectedPayoff(absgm.returnGame(), strategymap);


			/*
			 * for subgame build the abstracted game by recAbstract(). then build a game with maxexpectedpayoff
			 */






			Game gmmaxexpectedforsbgame = GamutModifier.getGameWithMaxExpectedPayoff(abstractsubgame, strategymap);






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

			/*
			MixedStrategy abstractmaxexpectedprofile1 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithmaxexpectedpayoff).get(0);
			MixedStrategy abstractmaxexpectedprofile2 =  MinEpsilonBoundSolver.getMinEpsilonBoundProfile(absgm.returnGame(), gmwithmaxexpectedpayoff).get(1);;
			 */


			/*
			 * use the abstractsubgame for subgame mixedstrategy
			 */

			MixedStrategy[] abstractmaxexpectedprofile =  new MixedStrategy[2];



			abstractmaxexpectedprofile[0] = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractsubgame, gmmaxexpectedforsbgame).get(0);
			abstractmaxexpectedprofile[1] = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractsubgame, gmmaxexpectedforsbgame).get(1);



			System.out.println("abstractmaxexpectedprofile[0].checkIfNormalized() "+ abstractmaxexpectedprofile[0].checkIfNormalized());
			System.out.println("abstractmaxexpectedprofile[1].checkIfNormalized() "+ abstractmaxexpectedprofile[1].checkIfNormalized());



			/*
			 * build the original strategies for average abstract game
			 */

			/*
			double[] originalmaxexpectedprofile1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);
			double[] originalmaxexpectedprofile2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractmaxexpectedprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1);



			double[] orgmaxexpectedprbpl1 = new double[originalmaxexpectedprofile1.length+1];

			orgmaxexpectedprbpl1[0] =0;
			int index2 =1;
			for(double x: originalmaxexpectedprofile1)
			{
				orgmaxexpectedprbpl1[index2++] = x;
			}


			double[] orgmaxexpectedprbpl2 = new double[originalmaxexpectedprofile2.length+1];

			orgmaxexpectedprbpl2[0] =0;
			int index22 =1;
			for(double x: originalmaxexpectedprofile2)
			{
				orgmaxexpectedprbpl2[index22++] = x;
			}








			MixedStrategy orginalmaxexpectedprofile1 = new MixedStrategy(orgmaxexpectedprbpl1);
			MixedStrategy orginalmaxexpectedprofile2 = new MixedStrategy(orgmaxexpectedprbpl2);*/



			/*
			 * build original game strategy for subgame
			 */


			MixedStrategy[] orginalmaxexpectedprofile = strategymap.getStrategySubgameSols(abstractmaxexpectedprofile);


			System.out.println("orginalmaxexpectedprofile[0].checkIfNormalized() "+ orginalmaxexpectedprofile[0].checkIfNormalized());
			System.out.println("orginalmaxexpectedprofile[1].checkIfNormalized() "+ orginalmaxexpectedprofile[1].checkIfNormalized());





			List<MixedStrategy> originalmaxexpectedlist = new ArrayList<MixedStrategy>();
			//originalmaxexpectedlist.add(orginalmaxexpectedprofile1);
			//originalmaxexpectedlist.add(orginalmaxexpectedprofile2);

			originalmaxexpectedlist.add(orginalmaxexpectedprofile[0]);
			originalmaxexpectedlist.add(orginalmaxexpectedprofile[1]);


			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution origmaxexpecteddistro = new OutcomeDistribution(originalmaxexpectedlist);
			double[]  originalmaxexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), origmaxexpecteddistro);

			double epsilonmaxexpected = SolverUtils.computeOutcomeStability(gm.returnGame(), origmaxexpecteddistro);
			Logger.logit("\n Expected Payoff original game player 0 : "+ originalmaxexpectedpayoff[0]+ "Expected Payoff original game player 1 : "+ originalmaxexpectedpayoff[1]);

			Logger.logit("\n Final Epsilon for Max expected payoff "+ epsilonmaxexpected);




			////////////////////


			Logger.logit("\n Deviaitons for NE ");

			/*

			MixedStrategy abstractgameneprofile1 =  MinEpsilonBoundSolver.getPSNE(absgm.returnGame()).get(0);
			MixedStrategy abstractgameneprofile2 = MinEpsilonBoundSolver.getPSNE(absgm.returnGame()).get(1);
			 */

			/*
			 * use the abssubgame for subgaame psne
			 */

			MixedStrategy[] abstractgameneprofile =  new MixedStrategy[2];

			abstractgameneprofile[0] = MinEpsilonBoundSolver.getPSNE(abstractsubgame).get(0);
			abstractgameneprofile[1] = MinEpsilonBoundSolver.getPSNE(abstractsubgame).get(1);

			System.out.println(" abstractgameneprofile[0].checkIfNormalized() "+ abstractgameneprofile[0].checkIfNormalized());
			System.out.println(" abstractgameneprofile[1].checkIfNormalized() "+ abstractgameneprofile[1].checkIfNormalized());









			/*
			 * calculate original game expected payoffs for NE profile 
			 */


			/*


			double[] originalactionprobsplayer1 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile1, gm.returnGame().getNumActions(0), numberofclusters, 0);

			double[] originalactionprobsplayer2 = buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgameneprofile2, gm.returnGame().getNumActions(1), numberofclusters, 1); 


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

			String str1 = originalmixedstrategyplayer1+ " ";
			String str2 = originalmixedstrategyplayer2+ " ";



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


			MixedStrategy[] originalmixedstrategyplayer = strategymap.getStrategySubgameSols(abstractgameneprofile);


			System.out.println("originalmixedstrategyplayer[0].checkIfNormalized() "+ originalmixedstrategyplayer[0].checkIfNormalized());
			System.out.println("originalmixedstrategyplayer[1].checkIfNormalized() "+ originalmixedstrategyplayer[1].checkIfNormalized());





			List<MixedStrategy> originallist = new ArrayList<MixedStrategy>();
			//	originallist.add(originalmixedstrategyplayer1);
			//	originallist.add(originalmixedstrategyplayer2);



			// for subgame
			originallist.add(originalmixedstrategyplayer[0]);
			originallist.add(originalmixedstrategyplayer[1]);






			//MatrixGame g = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+ abstractedgamename+Parameters.GAMUT_GAME_EXTENSION));
			OutcomeDistribution originaldistro = new OutcomeDistribution(originallist);
			double[]  originalexpectedpayoff = SolverUtils.computeOutcomePayoffs(gm.returnGame(), originaldistro);

			double epsilonz = SolverUtils.computeOutcomeStability(gm.returnGame(), originaldistro);
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


			if(Main.isAvrgDelta())
			{


				result[0][0] = maxdeltas[0]; // max deltas
				result[0][1] = epsilonz; //NE epsilon

				result[1][0] = maxdeltas[1];
				result[1][1] = sbgmepsilon;  // subgame qre



				result[2][0] = maxdeltas[1];
				result[2][1] = epsilonmaxexpected;	

			}
			else
			{
				result[0][0] = mindeltas[0]; // min over max delta
				result[0][1] = epsilonz;   //NE epsilon

				result[1][0] = mindeltas[1];
				result[1][1] = sbgmepsilon;  //from subgame solver

				result[2][0] = mindeltas[0];
				result[2][1] = epsilonmaxexpected;	



			}














		} //end of if else







		return result;
















		/*System.out.println( "strategy player 1: "+abstractgamemixedstrategy1);
		System.out.println("strategy player 2: "+abstractgamemixedstrategy2);*/

		//	int[] outcome = new int[2];
		//double[] expectedpayoff = {0,0}; 
		//int payoffcount =0;

		/*	ArrayList<double[]> abstractgameequilibriumoutcomesplayer1 = new ArrayList<double[]>();
		ArrayList<double[]> abstractgameequilibriumoutcomesplayer2 = new ArrayList<double[]>();





		for(int i=0; i<absgm.returnGame().getNumPlayers(); i++)
		{
			payoffcount =0;

			OutcomeIterator	iteratorabstractgame = absgm.returnGame().iterator();


			while(iteratorabstractgame.hasNext())
			{

				outcome = iteratorabstractgame.next();
				if( (abstractgamemixedstrategy1.getProb(outcome[0]) > 0) &&  (abstractgamemixedstrategy2.getProb(outcome[1]) > 0))
				{
					//System.out.println("Equilibrium outcome: "+ outcome[0] + ", "+ outcome[1]);

					if(i==0) //player 0
					{
						//	check if the action is already in list
						if(abstractgameequilibriumoutcomesplayer1.size()>0)
						{
							//check if the action is already in list
							boolean flag = false;

							for(double[] y: abstractgameequilibriumoutcomesplayer1)
							{
								if(y[0]==outcome[0])
								{
									flag=true;
									break;
								}
							}
							if(flag==false)
							{
								double[] actionwithprobability = new double[2];
								actionwithprobability[0] = outcome[0];
								actionwithprobability[1] = abstractgamemixedstrategy1.getProb(outcome[0]) ;

								abstractgameequilibriumoutcomesplayer1.add(actionwithprobability);
							}

						}
						else if(abstractgameequilibriumoutcomesplayer1.size()==0)
						{
							double[] actionwithprobability = new double[2];
							actionwithprobability[0] = outcome[0];
							actionwithprobability[1] = abstractgamemixedstrategy1.getProb(outcome[0]) ;

							abstractgameequilibriumoutcomesplayer1.add(actionwithprobability);
						}
					}
					else if(i==1)
					{
						//						check if the action is already in list
						if(abstractgameequilibriumoutcomesplayer2.size()>0)
						{
							//check if the action is already in list
							boolean flag = false;

							for(double[] y: abstractgameequilibriumoutcomesplayer2)
							{
								if(y[0]==outcome[1])
								{
									flag=true;
									break;
								}
							}
							if(flag==false)
							{
								double[] actionwithprobability = new double[2];
								actionwithprobability[0] = outcome[1];
								actionwithprobability[1] = abstractgamemixedstrategy2.getProb(outcome[1]) ;

								abstractgameequilibriumoutcomesplayer2.add(actionwithprobability);
							}

						}
						else if(abstractgameequilibriumoutcomesplayer2.size()==0)
						{
							double[] actionwithprobability = new double[2];
							actionwithprobability[0] = outcome[1];
							actionwithprobability[1] = abstractgamemixedstrategy2.getProb(outcome[1]) ;

							abstractgameequilibriumoutcomesplayer2.add(actionwithprobability);
						}
					}


					//	payoffcount++;
					//System.out.println("Equilibrium outcome: "+ outcome[0] + ", "+ outcome[1]);
					//expectedpayoff[i]= expectedpayoff[i] + absgm.returnGame().getPayoff(outcome, i);
				}
			}

			//if(payoffcount>0)
			{
				//		expectedpayoff[i]= expectedpayoff[i]/payoffcount;
			}
		}

		//	System.out.println("Expected Payoff player 0 : "+ expectedpayoff[0]+ "Expected Payoff player 1 : "+ expectedpayoff[1] );

		 */




		/*
		 * compute epsilon using OutcomeDistribution class
		 */



		///////////////////////




		////////////////////////





		//	double epsilon1 =	GamutModifier.calculateEpsilon(strategymap, abstractgameequilibriumoutcomesplayer1,abstractgameequilibriumoutcomesplayer2, gm.returnGame(), originalexpectedpayoff, 0);
		//	double epsilon2 =	GamutModifier.calculateEpsilon(strategymap, abstractgameequilibriumoutcomesplayer1,abstractgameequilibriumoutcomesplayer2, gm.returnGame(), originalexpectedpayoff, 1);

		//	Logger.log("\n +ve Epsilon player 0: "+ epsilon1+ " \n +ve Epsilon player 1: "+ epsilon2, false);

		//	Logger.log("\n  Epsilon from Util : "+ epsilonz, false);

		//	System.out.println("Epsilon1: "+ epsilon1+ " Epsilon2: "+ epsilon2);

		//List<Double> x = strategymap.getOriginalActionsFromAbstractedAction(1, 0);

		//	System.out.print(x);

		//System.out.println("Staring epsilon%%%%%%%%%%%%%%");












	}






	public static double calculateEpsilon(StrategyMapping strategymap, ArrayList<double[]> abstractgameequilibriumplayer1, ArrayList<double[]> abstractgameequilibriumplayer2, MatrixGame gm, double[] originalgameexpectedpayoff, int player)
	{
		//	System.out.println("player0 equilibriums");
		for(double[] x: abstractgameequilibriumplayer1)
		{
			System.out.println(x[0]+" "+x[1]);
			System.out.println(strategymap.getOriginalActionsFromAbstractedAction((int)x[0], 0));
		}

		//	System.out.println("player1 equilibriums");

		for(double[] x: abstractgameequilibriumplayer2)
		{
			System.out.println(x[0]+" "+x[1]);
			System.out.println(strategymap.getOriginalActionsFromAbstractedAction((int)x[0], 1));
		}


		ArrayList<double[]> epsilons = new ArrayList<double[]>();


		if(player==0) //player 0
		{

			for(int i=0; i< gm.getNumActions(player); i++) //number of actions for original game
			{
				//if(GamutModifier.isInEquilibrium(strategymap,abstractgameequilibriumplayer1, i+1, player)== false)
				{
					double payoffsum =0;
					int payoffcounter =0;
					double normalizevalue =0;
					double normalizedvalabstract =0;
					for(double[] y: abstractgameequilibriumplayer2)  //player 1
					{

						//y is not original game equilibrium, we need the reverse mapping function 
						List<Double> originalactions = strategymap.getOriginalActionsFromAbstractedAction((int)y[0], 1); //player 1

						Logger.log("\n (abstract)prob value by my epsilon calc: "+ y[1], false);
						normalizedvalabstract+=y[1];
						for(double originalaction: originalactions)
						{


							int ac = (int)Math.floor(originalaction);
							int csize = strategymap.getClusterSize(ac, 1);

							double prob =y[1] / csize ;
							Logger.log("\n cluster size for original action "+ac +" my epsilon calc: "+ csize, false);

							normalizevalue+=prob;
							int[] outcome = {i+1, (int)originalaction};
							payoffsum = payoffsum + (gm.getPayoff(outcome, player) * prob);
							payoffcounter++;
						}




					}

					//payoffsum = payoffsum/payoffcounter;
					int a = i+1;
					Logger.log("\n normalize value by my epsilon calc: "+ normalizevalue, false);
					Logger.log("\n normalize value(abstract) by my epsilon calc: "+ normalizedvalabstract, false);
					double epsilon = payoffsum-originalgameexpectedpayoff[player];
					Logger.log("\nAction "+ a+"'s expected payoff "+payoffsum+" ,"+" epsilon for player "+ player+" : "+ epsilon, false);
					//	if(originalgameexpectedpayoff[player]<payoffsum)
					{
						double[] tmpepsilon = {i+1, payoffsum-originalgameexpectedpayoff[player]};
						epsilons.add(tmpepsilon);
					}
				}
			}


		}

		else if(player==1)
		{


			for(int i=0; i< gm.getNumActions(player); i++)
			{
				//	if(GamutModifier.isInEquilibrium(strategymap, abstractgameequilibriumplayer2, i+1, player)== false)
				{
					double payoffsum =0;
					int payoffcounter =0;
					for(double[] y: abstractgameequilibriumplayer1) //player 0
					{

						List<Double> originalactions = strategymap.getOriginalActionsFromAbstractedAction((int)y[0], 0); //player 0


						for(double originalaction: originalactions)
						{


							int ac = (int)Math.floor(originalaction);
							double prob = y[1]/strategymap.getClusterSize(ac, 0);
							int[] outcome = {(int)originalaction, i+1};
							payoffsum = payoffsum + (gm.getPayoff(outcome, player)*prob);
							payoffcounter++;
						}



					}

					//	payoffsum = payoffsum/payoffcounter;
					int a = i+1;
					double epsilon = payoffsum-originalgameexpectedpayoff[player];
					Logger.log("\nAction "+ a+"'s expected payoff "+payoffsum+" ,"+" epsilon for player "+ player+" : "+ epsilon, false);
					//if(originalgameexpectedpayoff[player]<payoffsum)
					{
						double[] tmpepsilon = {i+1, epsilon};
						epsilons.add(tmpepsilon);
					}
				}
			}

		}



		double max = Double.NEGATIVE_INFINITY;
		int action = 0;
		//System.out.println("Player :" + player+ " action : epsilons");
		for(double[] x: epsilons)
		{
			//System.out.println(x[0]+ " : "+ x[1]);
			if(x[1]>max)
			{
				max=x[1];
				action = (int)x[0];
			}
		}

		if(max>0 && action>0)
		{

			//	System.out.println("Player : " + player+ " epsilon : "+ max+ " For action: "+ action);
		}
		else 
		{
			max =0;
			//System.out.println("No epsilon");
		}

		return max;




	}

	public static boolean isInEquilibrium(StrategyMapping strategymap, ArrayList<double[]> abstractequilibriumprofile, int originalaction, int player)
	{
		//	boolean flag = false;



		for(double[] x: abstractequilibriumprofile)
		{
			List<Double> y = strategymap.getOriginalActionsFromAbstractedAction((int)x[0], player);
			for(Double z: y)
			{
				if(z==originalaction)
				{
					//flag = true;
					return true;
				}
			}
		}

		return false;
	}



	public static double[] calculateDelta(Game game, List<double[]>[] cluster, int player)
	{

		//	System.out.println("Staring epsilon calcl**************");

		int[] numactions = game.getNumActions();

		double[] deltas = new double[cluster.length]; // there are deltas for each cluster. 
		int opponent =0;
		if(player==0)
			opponent =1;





		/*
		 * for each cluster take the actions and calcualte the delta
		 */
		for(int i=0; i< cluster.length; i++) // can be improved, i<cluster.length-1
		{

			double maxdiffplayer =0;

			for(double[] x : cluster[i]  ) // x[0] is the action
			{
				for(double[] y: cluster[i]) // can be improved , cluster[i+1]
				{
					if(cluster[i].indexOf(x)!=cluster[i].indexOf(y))// dont want to calculate difference between same actions
					{

						// now iterate over payoffs for action x[0] and y[0]

						for(int z =1; z<= numactions[opponent]; z++)
						{
							int[] outcome1 = new int[2];
							int[] outcome2 = new int[2];


							if(player==0)
							{
								outcome1[0] = (int)x[0];
								outcome1[1] =  z;
								outcome2[0] = (int)y[0];
								outcome2[1] =  z;

							}
							else if(player==1)
							{
								outcome1[1] = (int)x[0];
								outcome1[0] =  z;
								outcome2[1] = (int)y[0];
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
				}// inner cluster loop


			} // outer cluster loop

			deltas[i] = maxdiffplayer;
		}





		return deltas;
	}



	public static double[] calculateDelta2(Game game, List<double[]>[] cluster, int player)
	{

		//	System.out.println("Staring epsilon calcl**************");

		int[] numactions = game.getNumActions();

		double[] deltas = new double[cluster.length]; // there are deltas for each cluster. 
		int opponent =0;
		if(player==0)
			opponent =1;





		/*
		 * for each cluster take the actions and calcualte the delta
		 */
		for(int i=0; i< cluster.length; i++) // can be improved, i<cluster.length-1
		{

			double maxdiffplayer =0;

			for(double[] x : cluster[i]  ) // x[0] is the action
			{
				for(double[] y: cluster[i]) // can be improved , cluster[i+1]
				{
					if(cluster[i].indexOf(x)!=cluster[i].indexOf(y))// dont want to calculate difference between same actions
					{

						// now iterate over payoffs for action x[0] and y[0]

						for(int z =1; z<= numactions[opponent]; z++)
						{
							int[] outcome1 = new int[2];
							int[] outcome2 = new int[2];


							if(player==0)
							{
								outcome1[0] = (int)x[0];
								outcome1[1] =  z;
								outcome2[0] = (int)y[0];
								outcome2[1] =  z;

							}
							else if(player==1)
							{
								outcome1[1] = (int)x[0];
								outcome1[0] =  z;
								outcome2[1] = (int)y[0];
								outcome2[0] =  z;
							}

							double payoff1= game.getPayoff(outcome1, player);
							double payoff2 = game.getPayoff(outcome2, player);

							double payoff3 = game.getPayoff(outcome1, opponent);
							double payoff4 = game.getPayoff(outcome2, opponent);


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


							double diff2 = 0; 

							if((payoff3<0 && payoff4 <0) || (payoff3>=0 && payoff4>=0))
							{
								diff2 = Math.abs(Math.abs(payoff4) - Math.abs(payoff3));

							}
							else if(payoff3<0 && payoff4>= 0)
							{
								diff2 = Math.abs(Math.abs(payoff3) + payoff4);
							}
							else if(payoff3>=0 && payoff4< 0)
							{
								diff2 = Math.abs(Math.abs(payoff4) + payoff3);
							}

							double tmpmax = 0;
							if(diff2>diff)
							{
								tmpmax = diff2;
							}
							else 
							{
								tmpmax = diff;
							}





							if(tmpmax>maxdiffplayer)
								maxdiffplayer= tmpmax;




						}


					}
				}// inner cluster loop


			} // outer cluster loop

			deltas[i] = maxdiffplayer;
		}





		return deltas;
	}




}
