package subgame;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import Log.Logger;
import games.EmpiricalMatrixGame;
import games.Game;
import games.MatrixGame;
import games.MatrixSubgame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import output.SimpleOutput;
import solvers.QRESolver;
import util.GamutModifier;




/**
 * @author anjonsunny
 *
 */
public class StrategyMapping {

	private int numberofplayers;
	private int[] numberofactions;
	private int[] numberofclusters; // this can be an array to incorporate different number of cluster for different players
	private List<Integer>[][] clusterforplayerswithstrategy;// = new List[numberofplayers][numberofclusters]; //yeah, right
	private List<Integer>[] clusterfortargets; // for wildlife 
	public List<Integer>[] getClusterfortargets() {
		return clusterfortargets;
	}




	




	private int[][] securitygamedata;
	private Game originalgame;
	private String gamename;
	private Map<int[], MixedStrategy[]> subgameSols = new HashMap<int[], MixedStrategy[]>(); // strategy profiles for subgames given by the clustering



	/**
	 * 
	 * @param numberofplayers give the number of players
	 * @param numberofactions[] number of actions for each player
	 * @param numberofclusters[] number of clusters for each player 
	 * @param originalgame the original game
	 * @param gamename name of the game
	 */
	public StrategyMapping(int numberofplayers, int[] numberofactions, int[] numberofclusters, Game originalgame, String gamename)
	{
		this.numberofplayers = numberofplayers;

		this.numberofactions = new int[this.numberofplayers];

		for(int i = 0; i< numberofactions.length; i++)
		{
			this.numberofactions[i] = numberofactions[i];
		}

		this.numberofclusters = new int[this.numberofplayers];

		for(int i=0; i<numberofclusters.length; i++ )
		{
			this.numberofclusters[i] = numberofclusters[i];
		}



		clusterforplayerswithstrategy = new List[numberofplayers][];

		for(int i=0; i< clusterforplayerswithstrategy.length; i++)
		{
			clusterforplayerswithstrategy[i] = new List[numberofclusters[i]];
		}


		for(int i=0; i< this.numberofplayers; i++){

			for(int j =0; j< this.numberofclusters[i]; j++)
			{
				clusterforplayerswithstrategy[i][j] = new ArrayList<Integer>(); 
			}
		}

		this.originalgame = originalgame;
		this.gamename = gamename;

	}




	/**
	 * constructor for wildlife security games
	 * @param clusterfortargets
	 * @param numberofcluster
	 * @param securitygamedata
	 */
	public StrategyMapping(List<Integer>[] clusterfortargets, int numberofcluster, 
			int[][] securitygamedata) 
	{
		super();
		this.clusterfortargets = new List[numberofcluster];
		for(int i=0; i< this.clusterfortargets.length; i++)
		{
			this.clusterfortargets[i] = new ArrayList<Integer>();
		}
		for(int i=0; i< numberofcluster; i++)
		{
			for(Integer x: clusterfortargets[i])
			{
				this.clusterfortargets[i].add(x);
			}
		}
		this.securitygamedata = new int[securitygamedata.length][4];
		for(int i=0; i<this.securitygamedata.length; i++)
		{
			for(int j=0; j<4; j++)
				this.securitygamedata[i][j] = securitygamedata[i][j];
		}
	}
	
	public void printSecurityGameMapping()
	{
		for(int i=0; i< this.clusterfortargets.length; i++)
		{
			System.out.print("Cluster " + i + " : ");
			for(Integer target: this.clusterfortargets[i])
			{
				System.out.print(target);
				if(this.clusterfortargets[i].indexOf(target) < (this.clusterfortargets[i].size()-1) )
				{
					System.out.print(",");
				}
			}
			System.out.print("\n");
		}
	}


	public int[][][] makeAbstractSecurityGame()
	{
		int[][][] securitygame = new int[this.clusterfortargets.length][4][2];
		int dminr = Integer.MAX_VALUE;
		int dmaxr = Integer.MIN_VALUE;
		
		int dminp = Integer.MAX_VALUE;
		int dmaxp = Integer.MIN_VALUE;
		
		int aminr = Integer.MAX_VALUE;
		int amaxr = Integer.MIN_VALUE;
		
		int aminp = Integer.MAX_VALUE;
		int amaxp = Integer.MIN_VALUE;
		for(int clusterindex = 0; clusterindex<this.clusterfortargets.length; clusterindex++)
		{
			dminr = Integer.MAX_VALUE;
			dmaxr = Integer.MIN_VALUE;

			dminp = Integer.MAX_VALUE;
			dmaxp = Integer.MIN_VALUE;

			aminr = Integer.MAX_VALUE;
			amaxr = Integer.MIN_VALUE;

			aminp = Integer.MAX_VALUE;
			amaxp = Integer.MIN_VALUE;
			for(Integer target : this.clusterfortargets[clusterindex])
			{
				/**
				 * min condition for defender reward
				 */
				if(this.securitygamedata[target][0]<0)
				{
					System.out.print("OOO");
				}
				if(dminr > this.securitygamedata[target][0])
				{
					dminr = this.securitygamedata[target][0];
				}
				/**
				 * max condition for defender reward
				 */
				if(dmaxr < this.securitygamedata[target][0])
				{
					dmaxr = this.securitygamedata[target][0];
				}
				/**
				 * min condition for defender penalty
				 */
				if(dminp > this.securitygamedata[target][1])
				{
					dminp = this.securitygamedata[target][1];
				}
				/**
				 * max condition for defender penalty
				 */
				if(dmaxp < this.securitygamedata[target][1])
				{
					dmaxp = this.securitygamedata[target][1];
				}

				/**
				 * min condition for defender reward
				 */
				if(aminr > this.securitygamedata[target][2])
				{
					aminr = this.securitygamedata[target][2];
				}
				/**
				 * max condition for defender reward
				 */
				if(amaxr < this.securitygamedata[target][2])
				{
					amaxr = this.securitygamedata[target][2];
				}
				/**
				 * min condition for defender penalty
				 */
				if(aminp > this.securitygamedata[target][3])
				{
					aminp = this.securitygamedata[target][3];
				}
				/**
				 * max condition for defender penalty
				 */
				if(this.securitygamedata[target][3]>0)
				{
					System.out.print("OOOshh");
				}
				if(amaxp < this.securitygamedata[target][3])
				{
					amaxp = this.securitygamedata[target][3];
				}
			}
			securitygame[clusterindex][0][0] = dminr;
			securitygame[clusterindex][0][1] = dmaxr;

			securitygame[clusterindex][1][0] = dminp;
			securitygame[clusterindex][1][1] = dmaxp;

			securitygame[clusterindex][2][0] = aminr;
			securitygame[clusterindex][2][1] = amaxr;

			securitygame[clusterindex][3][0] = aminp;
			securitygame[clusterindex][3][1] = amaxp;
		}


		return securitygame;
	}





	/**
	 * 
	 * @param cluster pass the cluster array containing the actions for a player
	 * @param player player number
	 */
	public void mapActions(List<Integer>[] cluster, int player)
	{
		//double[] actionwithprobability = new double[2]; // [0]<- action, [1]<- probability, which can be set later 


		for(int i=0; i< this.numberofclusters[player]; i++)
		{

			for(Integer x: cluster[i])
			{
				//actionwithprobability[0] = x[0];
				this.clusterforplayerswithstrategy[player][i].add((int)x);
			}


		}
	}


	/**
	 * 
	 * @param abstractedaction abstracted action or pass the cluster number
	 * @param player
	 * @return a list of actions belong to the abstracted action. 
	 */
	public List<Double> getOriginalActionsFromAbstractedAction(int abstractedaction, int player)
	{
		List<Double> elm = new ArrayList<Double>();
		for(Integer x: this.clusterforplayerswithstrategy[player][abstractedaction-1])
		{
			elm.add((Math.floor(x)));
		}

		return elm;
	}



	public Game getOriginalgame() {
		return originalgame;
	}

	public void setOriginalgame(MatrixGame originalgame) {
		this.originalgame = originalgame;
	}



	/*
	 * calculates the max payoff for the actions in the original game in a cluster
	 */

	public double calcMaxPayoffOriginalGame(int[] abstractoutcome, int player)
	{
		double maxpayoff = Double.NEGATIVE_INFINITY;
		int opponent = 1^ player;

		Logger.logit("\n player "+ player+", abstract outcome "+ abstractoutcome[0]+ ", "+abstractoutcome[1]);


		List<Double> originalactions[] = new List[this.originalgame.getNumPlayers()];

		for(int i=0; i<this.originalgame.getNumPlayers(); i++)
		{
			originalactions[i] = new ArrayList<Double>();
			originalactions[i] = getOriginalActionsFromAbstractedAction(abstractoutcome[i], i);
		}



		for(int i=0; i<(this.originalgame.getNumPlayers()-1); i++)
		{

			for(Double x: originalactions[i])
			{
				for(Double y: originalactions[i+1])
				{

					int[] outcome = {(int)Math.floor(x), (int)Math.floor(y)};
					double payoff = this.originalgame.getPayoff(outcome, player);
					Logger.logit("\n Player "+player+" , in original game, payoff for outcome ("+ outcome[0] +", "+outcome[1]+") "+payoff+", maxpayoff "+ maxpayoff) ;

					if(payoff>maxpayoff)
					{


						maxpayoff= payoff;
						Logger.logit("\n maxpayoff changed to "+ maxpayoff );
					}
				}
			}//
		}






		/*List<Double> originalactionsplayer1;// = getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
		List<Double> originalactionsplayer2; //= getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent); 



		originalactionsplayer1 = getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
		originalactionsplayer2 = getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent); 



		if(player==1)
		{

			//here opponent is player 0/// the first player
			originalactionsplayer1 = getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent);
			originalactionsplayer2 = getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
		}


		for(Double x: originalactionsplayer1)
		{
			for(Double y: originalactionsplayer2)
			{

				int[] outcome = {(int)Math.floor(x), (int)Math.floor(y)};
				double payoff = this.originalgame.getPayoff(outcome, player);
				Logger.logit("\n Player "+player+" , in original game, payoff for outcome ("+ outcome[0] +", "+outcome[1]+") "+payoff+", maxpayoff "+ maxpayoff) ;

				if(payoff>maxpayoff)
				{


					maxpayoff= payoff;
					Logger.logit("\n maxpayoff changed to "+ maxpayoff );
				}
			}
		}
		 */


		return maxpayoff;
	}

	public double calcMaxExpectedPayoffOriginalGame(int[] abstractoutcome, int player)
	{

		double maxpayoff = Double.NEGATIVE_INFINITY;
		int opponent = 1^ player;
		List<Double> originalactionsplayer1;// = getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
		List<Double> originalactionsplayer2; //= getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent); 
		if(player==0)
		{
			originalactionsplayer1 = getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
			originalactionsplayer2 = getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent); 
			for(Double x: originalactionsplayer1)
			{
				double expectedpayoff = 0; 
				for(Double y: originalactionsplayer2)
				{
					int[] outcome = {(int)Math.floor(x), (int)Math.floor(y)};
					double payoff = this.originalgame.getPayoff(outcome, player);
					expectedpayoff += payoff;
				}
				expectedpayoff = expectedpayoff/originalactionsplayer2.size();
				if(expectedpayoff>maxpayoff)
				{
					maxpayoff = expectedpayoff;
				}
			}
		}
		else if(player==1)
		{
			//here opponent is player 0/// the first player
			originalactionsplayer1 = getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent);
			originalactionsplayer2 = getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
			for(Double x: originalactionsplayer2)
			{
				double expectedpayoff = 0; 
				for(Double y: originalactionsplayer1)
				{
					int[] outcome = {(int)Math.floor(y), (int)Math.floor(x)};
					double payoff = this.originalgame.getPayoff(outcome, player);
					expectedpayoff += payoff;
				}
				expectedpayoff = expectedpayoff/originalactionsplayer1.size();
				if(expectedpayoff>maxpayoff)
				{
					maxpayoff = expectedpayoff;
				}
			}
		}
		return maxpayoff;
	}









	/*public double[][] getActionsMaxDeviation(MatrixGame absgame, int[] outcome)
	{

	}*/




	/*
	 * returns action with minimum deviation {action, maxdev}
	 */
	public double[] getActionWithMinEpsilon(MatrixGame abstractgame, int opponentoutcome, int player)   
	{

		double[] maxdev= new double[abstractgame.getNumActions(player)] ;
		double[] actionwithminepsilon = new double[2];

		for(double x: maxdev)
		{
			x = Double.NEGATIVE_INFINITY;
		}



		int opponent = 1^player;
		for(int i=0; i<abstractgame.getNumActions(player); i++)
		{
			int[] outcome = {i+1, opponentoutcome};

			if(player==1)
			{
				outcome[0] = opponentoutcome;
				outcome[1] = i+1;
			}

			double payoff =  abstractgame.getPayoff(outcome, player);

			for(int j=0; j<abstractgame.getNumActions(player); j++)
			{
				if(i!=j)
				{
					int[] outcometocompare = {j+1, opponentoutcome};

					if(player==1)
					{
						outcometocompare[0] = opponentoutcome;
						outcometocompare[1] = j+1;
					}


					double payofftocompare = calcMaxPayoffOriginalGame(outcometocompare, player); //abstractgame.getPayoff(outcometocompare, player);
					double tmpdev = payofftocompare - payoff;
					if(tmpdev>maxdev[i])
					{
						maxdev[i] = tmpdev;
						//actionwithmaxdev = i+1;


					}
				}

			}
		}

		double min = Double.POSITIVE_INFINITY;
		int minaction =0;

		for(int k=0; k<maxdev.length; k++)
		{
			if(maxdev[k]<min)
			{
				min = maxdev[k];
				minaction = k+1;
			}
		}


		actionwithminepsilon[0] = minaction;
		actionwithminepsilon[1] = min;


		return actionwithminepsilon;





	}



	public MixedStrategy calcEpsilonBoundedEq(MatrixGame abstractgame, int player)
	{


		OutcomeIterator itr = abstractgame.iterator();


		int equilibrium = 0;
		int opponent = 1^player;
		double[][]  actionwithepsilon = new double[abstractgame.getNumActions(opponent)][2];  //{action, min(maxdev)}


		for(int i=0; i<abstractgame.getNumActions(opponent); i++)
		{
			actionwithepsilon[i] = getActionWithMinEpsilon(abstractgame, i+1, player);

			Logger.log("\n player "+player+" Action "+ actionwithepsilon[i][0]+", with min epsilon "+ actionwithepsilon[i][1]+ " against opponent action "+ i+1, false);

		}


		double minepsilon = Double.POSITIVE_INFINITY;

		for(int i=0; i<actionwithepsilon.length; i++)
		{
			if(minepsilon>actionwithepsilon[i][1])
			{
				minepsilon = actionwithepsilon[i][1];
				equilibrium = (int)actionwithepsilon[i][0];

			}
		}

		Logger.log("\n Player "+ player+ " eqlbrm action"+ equilibrium +" with min epsilon "+ minepsilon, false);






		//return equilibrium;


		double[] prob = new double[abstractgame.getNumActions(player)+1];
		prob[equilibrium] = 1; 
		MixedStrategy strategy = new MixedStrategy(prob);

		return strategy;


	}




	public  int getClusterSize(int action, int player)
	{

		int cluster = this.getClusternumber(action, player);
		int size = this.clusterforplayerswithstrategy[player][cluster].size();
		return size; 
	}

	public int getClusterSize1(int cluster, int player)
	{
		return this.clusterforplayerswithstrategy[player][cluster].size();

	}



	/**
	 * 
	 * @param clustermappingtoaction just one dimensional array containing the cluster number for each action
	 * @param player player number
	 */
	public void mapActions(int[] clustermappingtoaction, int player)
	{




		for(int i=0; i< this.numberofclusters[player]; i++)
		{
			for(int j=0; j < clustermappingtoaction.length; j++)
			{
				if( (i+1) == clustermappingtoaction[j]) //the clustermappingtoaction should contain the cluster number starts from 1
				{
					double[] actionwithprobability = new double[2]; // [0]<- action, [1]<- probability 
					actionwithprobability[0] = j+1;
					int action = j+1;
					this.clusterforplayerswithstrategy[player][i].add(action);
				}

			}

		}

		for(List<Integer> x: this.clusterforplayerswithstrategy[player])
		{
			for(Integer y: x)
			{
				System.out.print(y);
			}
			System.out.print("\n");
		}

	}



	/**
	 * 
	 * @param probability set the probability for an action
	 * @param player
	 */
	/*public void setStrategy(int[] probability, int player ) // there will be  n number of probability where n = number of actions in the abstracted game. 
	{
		for(int i =0; i< this.numberofclusters[player]; i++)
		{
			for(double[] x: this.clusterforplayerswithstrategy[player][i])
			{
				x[1] = probability[i];
			}
		}

	}
	 */

	public MatrixGame makeAbstractGame()
	{


		/*
		 * create an array containing the number of actions for each player in the abstracted game
		 */

		int[] N = new int[this.numberofplayers];

		for(int i=0; i<N.length; i++)
		{
			N[i] = this.numberofclusters[i];
		}

		/*
		 * create the abstracted game
		 */

		MatrixGame abstractedgame  = new MatrixGame(originalgame.getNumPlayers(), N);

		/*
		 * 1.take an iterator for the original game
		 * 2.iterate over the iterator
		 * 3. Check the outcome of the original game's iterator: In which cluster they belong. 
		 * 4. The cluster numbers are the outcome in the abstracted game
		 * 5. Get the payoff in the abstracted game. Sum with the existig one with the new one. 
		 * 6. repeat 2 to 5
		 */


		//	OutcomeIterator iteratorabstractgame = abstractedgame.iterator();
		int[] outcomeabstractgame = new int[this.numberofplayers];
		int[] outcomeoriginalgame = new int[this.numberofplayers]; 



		OutcomeIterator iteratororiginalgame = this.originalgame.iterator();

		while(iteratororiginalgame.hasNext())
		{

			outcomeoriginalgame = iteratororiginalgame.next();

			for(int p = 0; p<this.originalgame.getNumPlayers(); p++)
			{

				outcomeabstractgame[p] = getClusternumber(outcomeoriginalgame[p], p) + 1 ; // plus 1 because the outcomes start from 1

			}




			for(int p = 0; p<this.originalgame.getNumPlayers(); p++)
			{


				double tmppayoff =  originalgame.getPayoff(outcomeoriginalgame, p);

				double oldpayoff = abstractedgame.getPayoff(outcomeabstractgame, p) ; 


				if(oldpayoff == Double.NEGATIVE_INFINITY)
				{
					abstractedgame.setPayoff(outcomeabstractgame, p, tmppayoff);
				}
				else
				{
					abstractedgame.setPayoff(outcomeabstractgame, p, tmppayoff+oldpayoff);
				}



			}






		}



		/*
		 * now average  the payoffs in the abstract game
		 */

		OutcomeIterator iteratorabstractgame = abstractedgame.iterator();

		while(iteratorabstractgame.hasNext())
		{
			outcomeabstractgame = iteratorabstractgame.next();

			int clustersizes = 1;


			for(int p=0; p<this.originalgame.getNumPlayers(); p++)
			{
				clustersizes *= getClusterSize1(outcomeabstractgame[p]-1, p) ; // minus 1 becasue cluster index starts from 0


			}


			for(int p=0; p<this.originalgame.getNumPlayers(); p++)
			{

				double payoff = abstractedgame.getPayoff(outcomeabstractgame, p);

				payoff = payoff/clustersizes;
				abstractedgame.setPayoff(outcomeabstractgame, p, payoff);


			}






		}




		//test
		//	String gamename = Parameters.GAME_FILES_PATH+"k"+this.numberofclusters[0]+"-"+this.gamename+Parameters.GAMUT_GAME_EXTENSION;





		String gamename = Parameters.GAME_FILES_PATH+"k"+this.numberofclusters[0]+"-"+this.gamename+Parameters.GAMUT_GAME_EXTENSION;
		String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		try
		{

			PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			SimpleOutput.writeGame(pw,abstractedgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}


		try{
			PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"k"+this.numberofclusters[0]+"-"+this.gamename+".mapping","UTF-8");

			//test
			// PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+"k"+this.numberofclusters[0]+"-"+this.gamename+".mapping","UTF-8");

			Logger.logit("\nMapping of Actions: ");

			pw.write("Number of players: " + this.numberofplayers + "\n");
			pw.write("Number of actions: "+"\n");

			Logger.logit("\n\n Number of players: " + this.numberofplayers + "\n");
			Logger.logit("\nNumber of actions: "+"\n");


			for(int i =0; i<this.numberofplayers; i++)
			{
				pw.write("Player "+ (i+1) +" : "+this.numberofactions[i]+ "\n");
				Logger.logit("Player "+ (i+1) +" : "+this.numberofactions[i]+ "\n");
			}


			pw.write("\n \n");

			pw.write("Number of clusters: "+"\n");
			Logger.logit("Number of clusters: "+"\n");
			for(int i =0; i<this.numberofplayers; i++)
			{
				pw.write("Player "+ (i+1) +" : "+this.numberofclusters[i]+ "\n");
				Logger.logit("Player "+ (i+1) +" : "+this.numberofclusters[i]+ "\n");
			}


			pw.write("\n \n");
			pw.write("Mapping of actions: "+"\n\n");

			Logger.logit("\n \n");
			Logger.logit("Mapping of actions: "+"\n\n");


			for(int i =0; i< this.numberofplayers; i++)
			{
				pw.write("Player "+ (i+1)+" : \n");

				Logger.logit("Player "+ (i+1)+" : \n");

				int k =1 ;
				for(List<Integer> x: clusterforplayerswithstrategy[i])
				{

					pw.write("Cluster "+ (k) + " Actions: ");
					Logger.logit("Cluster "+ (k) + " Actions: ");
					k++;
					for(Integer y: x)
					{
						pw.write(y+", ");
						Logger.logit(y+", ");
					}
					pw.write("\n");
					Logger.logit("\n");

				}

				pw.write("\n \n");
				Logger.logit("\n \n");
			}





			//SimpleOutput.writeGame(pw,abstractedgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping--- class :something went terribly wrong during clustering abstraction ");
		}





		//return  Referee.experimentdir+"/"+gmname;
		return  abstractedgame;

		//test
		// return gmname;






	}


	/*
	 *//**
	 * Build the abstracted game from original game
	 *//*
	public String buildAbstractedGame()
	{





		for(int i =0; i< this.numberofplayers; i++)
		{
			System.out.print("\n\n For player: "+ i + "\n");
			for(int j =0; j< this.numberofclusters[i]; j++)
			{
				System.out.print("\nCLuster "+ j +": ");
				for(double[] x: this.clusterforplayerswithstrategy[i][j])
				{
					System.out.print(x[0] + " ");
				}
			}
			System.out.print( "\n");
		}





		int[] N = new int[this.numberofplayers];

		for(int i=0; i<N.length; i++)
		{
			N[i] = this.numberofclusters[i];
		}


		MatrixGame abstractedgame  = new MatrixGame(originalgame.getNumPlayers(), N);



		For all players, do the following steps:
	  * 
	  *1. take the abstracted game.
	  *2. take an action X
	  *    3. take the original game
	  *    4. iterate over all the actions and check whether it belongs to action X
	  *    5. SUm the payoffs and take average
	  *    6. Set the payoff for player i.
	  * 7. go to 2. 
	  *    
	  *    
	  * 




		OutcomeIterator iteratorabstractgame = abstractedgame.iterator();
		int[] outcomeabstractgame = new int[this.numberofplayers];
		int[] outcomeoriginalgame = new int[this.numberofplayers]; 

		double payoffsum =0;
		int counter = 0;

		for(int i=0; i< this.numberofplayers; i++)
		{
			iteratorabstractgame = abstractedgame.iterator();

			while(iteratorabstractgame.hasNext())
			{
				outcomeabstractgame = iteratorabstractgame.next();
				OutcomeIterator iteratororiginalgame = this.originalgame.iterator();
				payoffsum = 0;
				counter = 0;

				//now do step 6
				while(iteratororiginalgame.hasNext())
				{ 
					outcomeoriginalgame = iteratororiginalgame.next();
					//  now check
					if(checkIfMappingMatches(outcomeabstractgame, outcomeoriginalgame) == true)
					{
						payoffsum+= this.originalgame.getPayoff(outcomeoriginalgame, i);
						counter++;
					}


				}

				if(counter!=0)
				{

					abstractedgame.setPayoff(outcomeabstractgame, i, payoffsum/counter);
				}
				else if(counter==0)
				{
					abstractedgame.setPayoff(outcomeabstractgame, i, 0);
				}


			}// end of while loop
		} // end of outer for loop

		String gamename = Parameters.GAME_FILES_PATH+Referee.experimentdir+"/"+"k"+this.numberofclusters[0]+"-"+this.gamename+Parameters.GAMUT_GAME_EXTENSION;
		String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		try{

			PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			SimpleOutput.writeGame(pw,abstractedgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}


		try{
			PrintWriter pw = new PrintWriter(Parameters.GAME_FILES_PATH+Referee.experimentdir+"/"+"k"+this.numberofclusters[0]+"-"+this.gamename+".mapping","UTF-8");

			pw.write("Number of players: " + this.numberofplayers + "\n");
			pw.write("Number of actions: "+"\n");
			for(int i =0; i<this.numberofplayers; i++)
			{
				pw.write("Player "+ (i+1) +" : "+this.numberofactions[i]+ "\n");
			}


			pw.write("\n \n");

			pw.write("Number of clusters: "+"\n");
			for(int i =0; i<this.numberofplayers; i++)
			{
				pw.write("Player "+ (i+1) +" : "+this.numberofclusters[i]+ "\n");
			}


			pw.write("\n \n");
			pw.write("Mapping of actions: "+"\n\n");


			for(int i =0; i< this.numberofplayers; i++)
			{
				pw.write("Player "+ (i+1)+" : \n");
				int k =1 ;
				for(List<double[]> x: clusterforplayerswithstrategy[i])
				{
					pw.write("Cluster "+ (k++) + " Actions: ");
					for(double[] y: x)
					{
						pw.write(y[0]+", ");
					}
					pw.write("\n");
				}

				pw.write("\n \n");
			}





			//SimpleOutput.writeGame(pw,abstractedgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}





		return Referee.experimentdir+"/"+gmname;




	}*/


	/**
	 * 
	 * @param abstractactions array of actions in abstracted game
	 * @param originalactions array of actions in original game
	 * @return true or false if the original actions belong to those abstracted actions
	 */
	public boolean checkIfMappingMatches(int[] abstractactions, int[] originalactions)
	{

		for(int i=0; i< this.numberofplayers; i++)
		{
			if(abstractactions[i] != (getClusternumber(originalactions[i], i) + 1)) // plus 1 because cluster number starts from 0 but action starts from 1.
			{
				return false;
			}

		}

		return true;


	}

	/**
	 * 
	 * @param action pass an action of original game
	 * @param player player number
	 * @return the cluster number the action belongs to
	 */
	public int getClusternumber(int action, int player)
	{
		int clusternumber = -1;
		for(int i=0; i<this.numberofclusters[player]; i++)
		{
			for(Integer x: this.clusterforplayerswithstrategy[player][i])
			{
				if(action == x)
				{
					clusternumber = i;
					break;
				}
			}
		}
		return clusternumber;

	}

	/**
	 * Assign each combination of clusters (one from each player) the
	 * equilibrium value when that cluster is viewed as a game, recursively
	 * abstracting it if it is too large.
	 * 
	 * @param game
	 *            the game to abstract
	 * @param mapping
	 *            mapping[p][c] is a list of actions in cluster c of player p
	 * @param clusterBy
	 *            number of clusters for each player when recursing
	 * @return the abstracted game
	 */
	private Game recAbstractBase(Game game, List<Integer>[][] mapping,
			int clusterBy, MixedStrategy[] solSpace) {
		assert solSpace==null || solSpace.length==numberofplayers;
		int p;
		OutcomeDistribution equilibriumDistribution;
		int[] clusters = new int[numberofplayers]; /*
		 * clusters[player] is the
		 * current cluster for that
		 * player
		 */
		Arrays.fill(clusters, 1);
		int[] abstractNumActions = new int[numberofplayers];
		for (p = 0; p < numberofplayers; p++) {
			abstractNumActions[p] = mapping[p].length;
		}
		MatrixGame abstractedGame = new MatrixGame(numberofplayers,
				abstractNumActions); /* to fill in and return */
		Game abstractedSubgame;
		Iterator<int[]> outcomes;
		int[] outcome;
		double[] payoffs = new double[numberofplayers];
		int[] numClusters = new int[numberofplayers];
		int numActions;
		boolean recurse;
		List<Integer>[][] subgameMapping = new List[numberofplayers][];
		GamutModifier gm = new GamutModifier();
		MixedStrategy[] subgameSolution;
		Map<int[], MixedStrategy[]> subgameSols=new HashMap<int[], MixedStrategy[]>();
		List<double[]>[] clusterStruct; /*
		 * returned by
		 * gamutModifier.clusterActions
		 */

		while (true) {
			Game subgame = sliceGame(game, mapping, clusters);

			recurse = false;
			for (p = 0; p < numberofplayers; p++) {
				numActions = subgame.getNumActions(p);
				if (numActions > clusterBy) {
					numClusters[p] = clusterBy;
					recurse = true;
				} else {
					numClusters[p] = numActions;
				}
			}

			if (recurse) {
				MatrixGame mgm = new MatrixGame(subgame);
				gm.setMg(mgm);
				for (p = 0; p < numberofplayers; p++) {
					gm.setMg(mgm);
					clusterStruct = gm.clusterActions(numClusters[p], p);
					subgameMapping[p] = new List[numClusters[p]];
					for (int i = 0; i < numClusters[p]; i++) {
						subgameMapping[p][i] = new ArrayList<Integer>();
						for (double[] action : clusterStruct[i]) {
							subgameMapping[p][i]
									.add((Integer) (int) (action[0] + .5));
						}
					}
				}

				subgameSolution=new MixedStrategy[numberofplayers];
				abstractedSubgame = recAbstractBase(subgame, subgameMapping, clusterBy, subgameSolution);
			}
			else {
				subgameSolution = EquilibriumProfile(subgame);
			}

			/* store subgame solutions for use in the reverse mapping */
			subgameSols.put(clusters.clone(), subgameSolution);

			equilibriumDistribution = new OutcomeDistribution(
					Arrays.asList(subgameSolution));

			/*
			 * use expected utility for each player under the equilibrium in the
			 * subgame as the payoff for the action in the clustered game with
			 * clusters corresponding to the actions in that subgame
			 */
			Arrays.fill(payoffs, 0.0);
			outcomes = new OutcomeIterator(subgame);
			while (outcomes.hasNext()) {
				outcome = outcomes.next();
				for (p = 0; p < numberofplayers; p++) {
					payoffs[p] += subgame.getPayoff(outcome, p)
							* equilibriumDistribution.getProb(outcome);
				}
			}
			abstractedGame.setPayoffs(clusters, payoffs);

			for (p = 0; clusters[p] == mapping[p].length; p++) {
				if (p == numberofplayers-1) {
					/* abstractedGame is now filled in */
					if(game==this.originalgame) {
						this.subgameSols=subgameSols;
					}
					/* if the caller wants a solution, use the QRESolver on the abstracted game and pass the result through the reverse mapping */
					if(solSpace!=null) {
						MixedStrategy[] solution=subgameReverseMap(game, mapping, subgameSols, EquilibriumProfile(abstractedGame));
						for(p=0; p<numberofplayers; p++) {
							solSpace[p]=solution[p];
						}
					}
					return abstractedGame;
				}
				clusters[p] = 1;
			}
			clusters[p]++;
		}
	}





	/**
	 * Gives a solution to original game based on a solution to the abstracted
	 * game. The probability given for each action is the weighted sum of the
	 * probabilities of playing that action in the solution to each cluster,
	 * weighted by the probability, in the solution to the abstracted game, of
	 * playing that cluster.
	 * 
	 * Must be used after a call to recAbstract (because it uses the
	 * solutions to the subgames recorded there).
	 * 
	 * @param sol
	 *            Strategy profile for the abstracted game
	 * @return Strategy profile for the original game
	 */
	public MixedStrategy[] subgameReverseMap(Game game, List<Integer>[][] mapping, Map<int[], MixedStrategy[]> subgameSols, MixedStrategy[] sol)
	{
		assert subgameSols!=null;
		int[] clusters;
		MixedStrategy[] equilibriumStrats;
		double prob;
		double probOfCluster;
		Iterator<int[]> outcomesInCluster;
		MixedStrategy[] subgameProfile;
		double[][] profile = new double[numberofplayers][];
		for(int p=0; p<this.numberofplayers; p++) {
			profile[p]=new double[game.getNumActions(p)+1];
		}

		for (Map.Entry<int[], MixedStrategy[]> entry : subgameSols.entrySet()) {
			clusters = entry.getKey();
			subgameProfile = entry.getValue();

			/*
			 * calculates the probability of reaching a given cluster as the
			 * product over players of the probability of their corresponding
			 * actions in the solution to the abstracted game
			 */
			probOfCluster = 1;
			for (int p = 0; p < numberofplayers; p++) {
				probOfCluster *= sol[p].getProb(clusters[p]);
			}

			/*
			 * for each outcome in the cluster and each player, adds the
			 * probability that that player plays towards that ouctcome in the
			 * subgame AND that subgame (cluster) is reached in the abstracted
			 * game to the probability that that player plays that action in the
			 * original game
			 */
			for (int p = 0; p < numberofplayers; p++) {
				for(int action=1; action<=subgameProfile[p].getNumActions(); action++) {
					profile[p][mapping[p][clusters[p]-1].get(action-1)] += probOfCluster
							* subgameProfile[p].getProb(action);
				}
			}
		}

		/* converts double[]'s in profile to MixedStrategies before returning */
		MixedStrategy[] ret = new MixedStrategy[numberofplayers];
		for (int p = 0; p < numberofplayers; p++) {
			ret[p] = new MixedStrategy(profile[p]);
		}
		return ret;
	}







	/**
	 * Alternative to buildAbstractedGame, using the algorithm in
	 * recAbstract to assign payoffs to cluster.
	 * 
	 * @param clusterBy
	 *            number of clusters for each player when recursing
	 */
	public Game recAbstract(int clusterBy) {
		Game ret = recAbstractBase(originalgame, this.clusterforplayerswithstrategy, clusterBy, null);

		return ret;
		// @TODO: do outputty stuff like above
	}




	/**
	 * Creates a MatrixSubgame out of the given game with actions for each
	 * player p limited to those in mapping[p][clusters[p]-1].
	 * 
	 * @param parent
	 *            the game to restrict
	 * @param mapping
	 *            contains for each player an array of action clusters
	 * @param clusters
	 *            contains for each player the index of a cluster--in the
	 *            restricted game, only actions from this cluster are allowed
	 * @return
	 */
	private static Game sliceGame(Game parent, List<Integer>[][] mapping,
			int[] clusters) {
		int numberOfPlayers = parent.getNumPlayers();
		int[][] actions = new int[numberOfPlayers][];
		for (int p = 0; p < numberOfPlayers; p++) {
			actions[p] = new int[mapping[p][clusters[p] - 1].size()];
			int i = 0;
			for (int n : mapping[p][clusters[p] - 1]) {
				assert (n > 0); /* actions use 1-based indexing */
				actions[p][i] = n;
				i++;
			}
		}
		return (Game) (new MatrixSubgame(parent, actions));
	}



	/**
	 * 
	 * @param game
	 *            game to solve
	 * @return a quantal response equilibrium profile Uses the QRESolver to find
	 *         an equilibrium strategy profile.
	 */
	private static MixedStrategy[] EquilibriumProfile(Game game) {
		MixedStrategy[] ret = new MixedStrategy[game.getNumPlayers()];
		EmpiricalMatrixGame emg = new EmpiricalMatrixGame(game);
		QRESolver solver = new QRESolver();
		for (int p = 0; p < game.getNumPlayers(); p++) {
			ret[p] = solver.solveGame(emg, p);
		}
		return ret;
	}


	public MixedStrategy[] getStrategySubgameSols(MixedStrategy[] sol) {
		return subgameReverseMap(originalgame, this.clusterforplayerswithstrategy, this.subgameSols, sol);
	}




	public int[][] getSecuritygamedata() {
		return securitygamedata;
	}




	public void setSecuritygamedata(int[][] securitygamedata) {
		this.securitygamedata = securitygamedata;
	}




	







}
