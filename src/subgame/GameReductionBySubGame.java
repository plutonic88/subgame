package subgame;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
//import org.apache.commons.math3.stat.StatUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import games.EmpiricalMatrixGame;
import games.Game;
import games.MatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import games.OutcomeIterator;
import output.SimpleOutput;
import parsers.GamutParser;
import solvers.IEDSMatrixGames;
import solvers.QRESolver;
import solvers.SolverCombo;
import solvers.SolverExperiments;
import solvers.SolverUtils;



public class GameReductionBySubGame {




	/*
	 * partition contains the partitioning for all the players. 
	 * partition contains the partitioning for all the players. 
	 * partition contains the partitioning for all the players. 
	 */
	public static List<Integer>[][] partition;
	public static int numberofsubgames;
	public static ArrayList<MatrixGame> subgames = new ArrayList<MatrixGame>(); // holds the subgames
	public  static MatrixGame originalgame;
	public static int[] subgamesize= new int[2];
	public static double hillclimbingdelta = Double.POSITIVE_INFINITY;
	public static List<Integer>[][] bestpartitionyet = new List[2][];
	public static boolean buildtestgame = false;
	public static ArrayList<Integer> bestepsilons = new ArrayList<Integer>();
	public static int partitionplayer = 1;
	public static int maxpayofflimit = 100;
	public static long  temperature = 1000000000;
	public static double deltaDistance = 0;
	public static double coolingRate = 0.99;
	public static List<List<Integer>[][]> paritionsforgames = new ArrayList<List<Integer>[][]>();
	//public static double absoluteTemperature = 0.00001;
	public static MixedStrategy[] finalstrategy = new MixedStrategy[2]; // holds final strategy after the ending of an iteration
	public static MixedStrategy[] oldfinalstrategy = new MixedStrategy[2]; // holds strategy for previous rounds iteration
	public static ArrayList<ArrayList<MixedStrategy>> subgamestrategy = new ArrayList<ArrayList<MixedStrategy>>();
	public static ArrayList<ArrayList<MixedStrategy>> prevsubgamestrategy = new ArrayList<ArrayList<MixedStrategy>>();
	public static ArrayList<MixedStrategy[]> reducedgamestrategy = new ArrayList<MixedStrategy[]>(); // holds the strategy of reduced game for the previous iteration
	/**
	 * timer parameters
	 */
	public static long subgamesolvingtime = 0; 
	public static long hierarchicalsolvingtime = 0;
	public static int subgamesolvingcounter = 0;
	public static int hierarchicalsolvingcounter = 0;
	public static long kmeantimer = 0;
	public static long qretimer = 0;
	public static int qretimecounter = 0;
	public static long psnetimer = 0;
	public static int psnetimecounter = 0;
	public static long mebtimer = 0;
	public static int mebtimecounter = 0;
	public static boolean isfirstiteration = true; 
	public GameReductionBySubGame(List<Integer>[][] partition, int numberofsubgames, MatrixGame originalgame, int numberofactions) 
	{
		super();
		GameReductionBySubGame.partition = partition;
		if(numberofsubgames!= partition[0].length)
		{
			System.out.println("Error number of subgame");
		}
		GameReductionBySubGame.numberofsubgames = numberofsubgames;
		GameReductionBySubGame.originalgame = originalgame;
		GameReductionBySubGame.subgamesize[0] = partition[0][0].size();
		GameReductionBySubGame.subgamesize[1] = partition[1][0].size();
		GameReductionBySubGame.finalstrategy[0] = new MixedStrategy(numberofactions);
		GameReductionBySubGame.finalstrategy[1] = new MixedStrategy(numberofactions);

		MakeGameForPartition.partition = partition;

	}






	public List<Integer>[][] getPartition() {
		return partition;
	}






	public void setPartition(List<Integer>[][] partition) {
		this.partition = partition;
	}






	public MatrixGame getOriginalgame() {
		return originalgame;
	}






	public void setOriginalgame(MatrixGame originalgame) {
		GameReductionBySubGame.originalgame = originalgame;
	}






	public static int[] getSubgamesize() {
		return subgamesize;
	}






	public static void setSubgamesize(int[] subgamesize) {
		GameReductionBySubGame.subgamesize = subgamesize;
	}






	public int getNumberofsubgames() {
		return numberofsubgames;
	}



	public void setNumberofsubgames(int numberofsubgames) {
		this.numberofsubgames = numberofsubgames;
	}



	public GameReductionBySubGame(List<Integer>[][] partition) 
	{
		super();
		this.partition = partition;
	}


	public static MixedStrategy[] buidMixedStrategyForOriginalGame()
	{

		MixedStrategy[] mxdstrategy = new MixedStrategy[2];
		for(int i=0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		{
			//get the player's list of strategy
			ArrayList<MixedStrategy> tmpstr = GameReductionBySubGame.subgamestrategy.get(i);
			mxdstrategy[i] = new MixedStrategy(GameReductionBySubGame.originalgame.getNumActions(i));


			for(int j=0; j< GameReductionBySubGame.numberofsubgames; j++)
			{
				//get the jth games strategy for ith player
				MixedStrategy tmpmxdstr = tmpstr.get(j);

				int k = 0;

				for(Integer action : GameReductionBySubGame.partition[i][j])
				{

					double prob = tmpmxdstr.getProb(k+1);
					mxdstrategy[i].setProb(action, prob);
					k++;

				}

			}

		}

		return mxdstrategy;

	}


	/**
	 * Collapse the original game using  subgame's strategy. Average payoff is used to build the hierarchical game
	 * @return
	 * @throws Exception
	 */
	public static MatrixGame collapseOriginalGame() throws Exception
	{
		MixedStrategy[] originalgamemixedstrategy = GameReductionBySubGame.buidMixedStrategyForOriginalGame();
		int[] N = {GameReductionBySubGame.numberofsubgames, GameReductionBySubGame.numberofsubgames};
		MatrixGame reducedgame = new MatrixGame(GameReductionBySubGame.originalgame.getNumPlayers(), N);
		Iterator itr = reducedgame.iterator();
		while(itr.hasNext())
		{
			int[] outcome = (int[])itr.next();
			double[] payoff = computeExpectedPayoff(outcome, originalgamemixedstrategy);
			//	double[] maxpayoff = computeMaxPayoff(outcome);
			reducedgame.setPayoffs(outcome, payoff);
		}
		return reducedgame;

	}


	/**
	 * 
	 * @param outcome outcome of the reduced game.
	 * @return max payoff in the original game after mapping back from the outcome of hierarchical game 
	 * to the original game
	 */
	private static double[] computeMaxPayoff(int[] outcome) 
	{
		double[] maxpayoff = { Double.MIN_VALUE,  Double.MIN_VALUE};
		for(int actionplayer1: GameReductionBySubGame.partition[0][outcome[0]-1])
		{
			for(int actionplayer2: GameReductionBySubGame.partition[1][outcome[1]-1])
			{
				int[] originaloutcome = {actionplayer1, actionplayer2};
				double[] tmppayoff = GameReductionBySubGame.originalgame.getPayoffs(originaloutcome);
				for(int player =0; player<GameReductionBySubGame.originalgame.getNumPlayers(); player++)
				{
					if(maxpayoff[player]<tmppayoff[player])
					{
						maxpayoff[player] = tmppayoff[player];

					}
				}

			}
		}

		return maxpayoff;
	}






	private static double[] computeExpectedPayoff(int[] outcome,  MixedStrategy[] originalgamemixedstrategy) throws Exception 
	{
		MixedStrategy[] tmpstrategy = new MixedStrategy[2];
		List<MixedStrategy> list = new ArrayList<MixedStrategy>();
		for(int j=0; j<GameReductionBySubGame.originalgame.getNumPlayers(); j++)
		{
			tmpstrategy[j] = new MixedStrategy(GameReductionBySubGame.originalgame.getNumActions(j));
			/*
			 * I am iterating over all the payoffs bcz you need to set zero to other strategies
			 * if iterating over only the actions in the partition.
			 * and setting zero to all of the actions also requires time. 
			 * 
			 */
			for(int k=0 ; k<  GameReductionBySubGame.originalgame.getNumActions(j); k++)
			{
				if(GameReductionBySubGame.partition[j][outcome[j]-1].contains(k+1))
				{
					tmpstrategy[j].setProb(k+1, originalgamemixedstrategy[j].getProb(k+1));
				}
				else
				{
					tmpstrategy[j].setProb(k+1, 0);

				}
			}
			if(tmpstrategy[j].isValid() != true)
			{
				//throw new Exception("Not valid strategy, building the reduced game " + tmpstrategy[j].checkIfNormalized());
			}
			list.add(tmpstrategy[j]);

		}
		if(list.size() != 2)
		{
			throw new Exception("Not appropriate size");
		}
		for(int i =0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		{
			if(tmpstrategy[i].isValid() != true)
			{
				tmpstrategy[i].normalize();

			}
		}
		MatrixGame g = new MatrixGame(GameReductionBySubGame.originalgame);
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(g, distro);
		//System.out.println("\n Exited from expected payoff");
		return expectedpayoff;
	}



	/**
	 * solving sobgames using qre
	 */
	public static void  solveSubGames()
	{

		ArrayList<MixedStrategy> player1strategies = new ArrayList<MixedStrategy>();
		ArrayList<MixedStrategy> player2strategies = new ArrayList<MixedStrategy>();
		//int subgame = 0;
		Date start = new Date();
		long l1 = start.getTime();
		for(MatrixGame x: GameReductionBySubGame.subgames)
		{
			int[] solver = {3}; // QRE
			MixedStrategy[] subgameprofile = SolverCombo.computeStrategyWithMultipleSolvers(solver, x);
			player1strategies.add(subgameprofile[0]);
			player2strategies.add(subgameprofile[1]);
			//subgame++;
		}
		Date stop = new Date();
		long l2 = stop.getTime();
		long diff = l2 - l1;
		GameReductionBySubGame.subgamesolvingtime += diff;
		GameReductionBySubGame.subgamesolvingcounter++;
		if( (GameReductionBySubGame.numberofsubgames != player1strategies.size()) || (GameReductionBySubGame.numberofsubgames != player2strategies.size()))
		{
			try {
				throw new Exception("Error in subgame");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		GameReductionBySubGame.subgamestrategy.clear();
		GameReductionBySubGame.subgamestrategy.add(player1strategies);
		GameReductionBySubGame.subgamestrategy.add(player2strategies);

	}





	/**
	 * 
	 * @param gameindex
	 * @return true if either of the strategy is positive
	 */
	private static boolean needToSolve(int gameindex) 
	{


		if(GameReductionBySubGame.reducedgamestrategy.size() > 1 || GameReductionBySubGame.reducedgamestrategy.size()==0)
		{
			try 
			{
				throw new Exception("reduced game strategy size wrong");
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if((GameReductionBySubGame.reducedgamestrategy.get(0)[0].getProb(gameindex+1) > 0) || (GameReductionBySubGame.reducedgamestrategy.get(0)[1].getProb(gameindex+1) > 0))
		{
			return true;
		}

		return false;
	}






	/**
	 * Builds subgames from the original game
	 * In iteration 0 it builds the subgame from the original game
	 * In interation>0 it builds the subgame from the original game and also 
	 * calculates the error terms using previous round's strategy and updates the subgame
	 * 
	 * @param gamenumber the game number 
	 * @param iteration the iteration number in the process
	 */
	public  static void buildSubGames(int gamenumber, int iteration)
	{
		List<Integer>[] player1parition = GameReductionBySubGame.partition[0];
		List<Integer>[] player2parition = GameReductionBySubGame.partition[1];
		/**
		 *Here we need to modify the subgames when the iteration is
		 *not the first one. 
		 *Remember to set(true) it at the very end of all the iterations for one game. 
		 */
		if(GameReductionBySubGame.isIsfirstiteration())
		{
			MatrixGame[] tmpsubgame = new MatrixGame[GameReductionBySubGame.numberofsubgames];

			if(GameReductionBySubGame.subgames.size()>0)
			{
				GameReductionBySubGame.subgames.clear();
			}
			for(int i=0; i< GameReductionBySubGame.numberofsubgames; i++)
			{
				// define subgame size
				int[] size = {GameReductionBySubGame.partition[0][i].size(), GameReductionBySubGame.partition[1][i].size()};
				tmpsubgame[i]  = new MatrixGame(originalgame.getNumPlayers(), size/*GameReductionBySubGame.subgamesize*/);
				GameReductionBySubGame.buildSubGame(player1parition[i], player2parition[i], tmpsubgame[i]);
				String gamename = Parameters.GAME_FILES_PATH+gamenumber+"subgame-"+iteration+"-"+"-"+i+Parameters.GAMUT_GAME_EXTENSION;
				//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;
				/*try
				{
					//PrintWriter pw = new PrintWriter(gamename,"UTF-8");
					//SimpleOutput.writeGame(pw,tmpsubgame[i]);
					//pw.close();
				}
				catch(Exception ex){
					System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
				}*/
				GameReductionBySubGame.subgames.add(tmpsubgame[i]);

			}
			if(GameReductionBySubGame.numberofsubgames!=GameReductionBySubGame.subgames.size())
			{
				System.out.print("Error subgame");
			}
			//reset(false) 
			GameReductionBySubGame.setIsfirstiteration(false);
		}
		else if(!GameReductionBySubGame.isIsfirstiteration()) // if the iteration is not the first one. 
		{
			/*
			 * we need to modify the subgames.
			 * For every subgame :
			 *  1. Take the partitions from both players.
			 *  2. For a player, calculate the expected payoff for each individual strategy
			 *  when opponent does not play from the partition's strategy. 
			 *  3. Add the expected values to the payoffs in the subgame when opponent plays 
			 *  the partition's strategy
			 *  4. Partition's strategy can be found from the last obtained final strategy. 
			 *  
			 */

			if(GameReductionBySubGame.subgames.size()>0)
			{
				GameReductionBySubGame.subgames.clear();
			}

			MatrixGame[] tmpsubgame = new MatrixGame[GameReductionBySubGame.numberofsubgames];
			for(int i=0; i< GameReductionBySubGame.numberofsubgames; i++)
			{
				int[] size = {GameReductionBySubGame.partition[0][i].size(), GameReductionBySubGame.partition[1][i].size()};
				//System.out.println("tmpsubgame size "+ size[0] + ","+size[1]);
				tmpsubgame[i]  = new MatrixGame(originalgame.getNumPlayers(), size/*GameReductionBySubGame.subgamesize*/);
				/*
				 * get the original subgames
				 then update them
				 */

				GameReductionBySubGame.buildSubGame(player1parition[i], player2parition[i], tmpsubgame[i]);
				String gamename = Parameters.GAME_FILES_PATH+gamenumber+"subgame-"+iteration+"-"+"-"+i+Parameters.GAMUT_GAME_EXTENSION;
				/*try
				{

					//	PrintWriter pw = new PrintWriter(gamename,"UTF-8");
					//	SimpleOutput.writeGame(pw,tmpsubgame[i]);
					//	pw.close();
				}
				catch(Exception ex){
					System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
				}*/

				/*
				 * before updating a subgame make sure that opponent's strategy is not zero
				 */
				boolean needtoupdate = checkIfSubGameNeedsToBeUpdated(i,player1parition[i], player2parition[i]);
				if(needtoupdate==true)
				{
					//do step 2 as previously mentioned 
					try 
					{
						updateSubgame(tmpsubgame[i], player1parition[i], player2parition[i]);
					} 
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				/*else // copy previous subgame
				{
					//MatrixGame tmpgame = new MatrixGame(previoussubgame);
					//tmpsubgame[i] = tmpgame;
				}*/
				/*try
				{
					//	PrintWriter pw = new PrintWriter(gamename,"UTF-8");
					//	SimpleOutput.writeGame(pw,tmpsubgame[i]);
					//	pw.close();
				}
				catch(Exception ex){
					System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
				}*/
				GameReductionBySubGame.subgames.add(tmpsubgame[i]);

			}

		}


	}



	/**
	 * 
	 * @param previoussubgame 
	 * @param tmpsubgame new  subgame, updated
	 * @param player1parition
	 * @param player2parition
	 * @return true if sum of probs are not zero
	 */
	private static boolean checkIfSubGameNeedsToBeUpdated(
			int subgameindex, 
			List<Integer> player1parition, List<Integer> player2parition) 
	{
		List<Double>[] sumofprobsforplayers = new List[2];
		sumofprobsforplayers[0] = new ArrayList<Double>();
		sumofprobsforplayers[1] = new ArrayList<Double>();
		List<Integer>[] partitions = new List[2];
		partitions[0] = player1parition;
		partitions[1] = player2parition;
		for(int i=0; i<originalgame.getNumPlayers(); i++)
		{

			int opponent = 1^i;
			double sumofprobs = 0;
			//update = true;
			for(int j=0 ; j<originalgame.getNumActions(opponent); j++) //actions for opponent
			{
				if(!partitions[opponent].contains(j+1))
				{
					double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(j+1);
					sumofprobs += prob;
				}

			}
			if(sumofprobs>0)
			{
				return true;

			}

		}
		return false;

	}


	/**
	 * 
	 * @param previoussubgame the subgame game that was built in the previous iteration
	 * @param tmpsubgame needs to be updated from the previous subgame
	 * @param player1parition partition for player 1 for a subgame
	 * @param player2parition partition for player 2 for a subgame
	 * @throws Exception 
	 * 
	 */
	private static void updateSubgameV3(
			MatrixGame tmpsubgame,
			List<Integer> player1parition, List<Integer> player2parition) throws Exception 
	{
		List<Double>[] expectedpayoffs = new List[2];
		expectedpayoffs[0] = new ArrayList<Double>();
		expectedpayoffs[1] = new ArrayList<Double>();

		List<Integer>[] partitions = new List[2];
		partitions[0] = player1parition;
		partitions[1] = player2parition;


		for(int player=0; player<originalgame.getNumPlayers(); player++)
		{
			int opponent = 1^player;
			double expectedpayoofforx = 0;
			double sumofprob = 0.0;
			for(int k=0; k<originalgame.getNumActions(opponent); k++)
			{
				if(!partitions[opponent].contains(k+1))
				{
					double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(k+1);
					sumofprob += prob;
					if(prob>0)
					{
						break;
					}
				}
			}
			if(sumofprob==0.0) // 0.0 means there is no support for  actions
			{
				expectedpayoofforx = 0;
				for(Integer x: partitions[player])
				{
					expectedpayoffs[player].add(expectedpayoofforx);
				}
			}
			else
			{
				/**
				 * strategies will be built then compute the expected payoff
				 */
				for(Integer x: partitions[player])
				{
					MixedStrategy[] strategy = new MixedStrategy[2];
					strategy[player] = new MixedStrategy(originalgame.getNumActions(player));
					strategy[player].setZeros();
					strategy[player].setProb(x, 1);
					strategy[opponent] = new MixedStrategy(originalgame.getNumActions(opponent));
					strategy[opponent].setZeros();
					expectedpayoofforx = 0;
					for(int j=0; j<originalgame.getNumActions(opponent); j++) //actions for opponent
					{
						if(!partitions[opponent].contains(j+1)) //action j+1
						{
							//get the probability
							double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(j+1);
							if(prob>0)
							{
								strategy[opponent].setProb(j+1, prob);
							}
						}

					}
					/**
					 * now compute the expected payoff
					 */

					/*if(!strategy[0].isValid())
					{
						throw new Exception("Update games strategy not valid"+ strategy[0].checkIfNormalized());
					}
					if(!strategy[1].isValid())
					{
						throw new Exception("Update games strategy not valid"+ strategy[1].checkIfNormalized());
					}*/
					//MatrixGame g = new MatrixGame(GameReductionBySubGame.originalgame);
					List<MixedStrategy> list = new ArrayList<MixedStrategy>();
					list.add(strategy[0]);
					list.add(strategy[1]);
					OutcomeDistribution distro = new OutcomeDistribution(list);
					double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(originalgame, distro);
					expectedpayoffs[player].add(expectedpayoff[player]);
				}
			}

		}
		//now update the game
		OutcomeIterator itr = new OutcomeIterator(tmpsubgame);
		int index = 0; // keeping track of which expected payoff
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			for(int i=0; i<2; i++)
			{
				double tmppayoff = tmpsubgame.getPayoff(outcome, i);
				index = outcome[i]-1;
				double modifiedpayoff = tmppayoff + expectedpayoffs[i].get(index);
				tmpsubgame.setPayoff(outcome, i, modifiedpayoff);

			}

		}

	}




	/**
	 * 
	 * @param previoussubgame the subgame game that was built in the previous iteration
	 * @param tmpsubgame needs to be updated from the previous subgame
	 * @param player1parition partition for player 1 for a subgame
	 * @param player2parition partition for player 2 for a subgame
	 * 
	 */
	private static void updateSubgameV2(
			MatrixGame tmpsubgame,
			List<Integer> player1parition, List<Integer> player2parition) 
	{
		List<Double>[] expectedpayoffs = new List[2];
		expectedpayoffs[0] = new ArrayList<Double>();
		expectedpayoffs[1] = new ArrayList<Double>();

		List<Integer>[] partitions = new List[2];
		partitions[0] = player1parition;
		partitions[1] = player2parition;


		for(int player=0; player<originalgame.getNumPlayers(); player++)
		{
			int opponent = 1^player;
			double expectedpayoofforx = 0;
			double sumofprob = 0.0;
			for(int k=0; k<originalgame.getNumActions(opponent); k++)
			{
				if(!partitions[opponent].contains(k+1))
				{
					double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(k+1);
					sumofprob += prob;
					if(prob>0)
					{
						break;
					}
				}
			}
			if(sumofprob==0.0) // 0.0 means there is no support for  actions
			{
				expectedpayoofforx = 0;
				for(Integer x: partitions[player])
				{
					expectedpayoffs[player].add(expectedpayoofforx);
				}
			}
			else
			{
				for(Integer x: partitions[player])
				{

					expectedpayoofforx = 0;
					for(int j=0; j<originalgame.getNumActions(opponent); j++) //actions for opponent
					{
						if(!partitions[opponent].contains(j+1)) //action j+1
						{
							//get the probability
							double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(j+1);
							if(prob>0)
							{
								int[] outcome = {x, j+1};
								if(player==1)
								{
									outcome[0] = j+1;
									outcome[1] = x;
								}
								double tmppayoff = originalgame.getPayoff(outcome, player);
								expectedpayoofforx += tmppayoff*prob;
							}


						}
					}

					expectedpayoffs[player].add(expectedpayoofforx);
				}
			}

		}
		//now update the game
		OutcomeIterator itr = new OutcomeIterator(tmpsubgame);
		int index = 0; // keeping track of which expected payoff
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			for(int i=0; i<2; i++)
			{
				double tmppayoff = tmpsubgame.getPayoff(outcome, i);
				index = outcome[i]-1;
				double modifiedpayoff = tmppayoff + expectedpayoffs[i].get(index);
				tmpsubgame.setPayoff(outcome, i, modifiedpayoff);

			}

		}

	}






	/**
	 * 
	 * @param previoussubgame the subgame game that was built in the previous iteration
	 * @param tmpsubgame needs to be updated from the previous subgame
	 * @param player1parition partition for player 1 for a subgame
	 * @param player2parition partition for player 2 for a subgame
	 * 
	 */
	private static void updateSubgame(
			MatrixGame tmpsubgame,
			List<Integer> player1parition, List<Integer> player2parition) 
	{
		List<Double>[] expectedpayoffs = new List[2];
		expectedpayoffs[0] = new ArrayList<Double>();
		expectedpayoffs[1] = new ArrayList<Double>();

		List<Integer>[] partitions = new List[2];
		partitions[0] = player1parition;
		partitions[1] = player2parition;


		for(int i=0; i<originalgame.getNumPlayers(); i++)
		{
			int opponent = 1^i;
			double expectedpayoofforx = 0;
			double sumofprob = 0.0;
			for(int k=0; k<originalgame.getNumActions(opponent); k++)
			{
				if(!partitions[opponent].contains(k+1))
				{
					// can be improved
					/*
					 * break when ever there is a support
					 * no need to sum up all
					 */
					double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(k+1);
					sumofprob += prob;
					if(prob>0)
					{
						break;
					}
				}
			}
			if(sumofprob==0.0) // 0.0 means there is no support for  actions
			{
				expectedpayoofforx = 0;
				for(Integer x: partitions[i])
				{
					expectedpayoffs[i].add(expectedpayoofforx);
				}
			}
			else
			{
				for(Integer x: partitions[i])
				{

					expectedpayoofforx = 0;
					for(int j=0; j<originalgame.getNumActions(opponent); j++) //actions for opponent
					{
						if(!partitions[opponent].contains(j+1)) //action j+1
						{
							//get the probability
							double prob = GameReductionBySubGame.finalstrategy[opponent].getProb(j+1);
							if(prob>0)
							{
								int[] outcome = {x, j+1};
								if(i==1)
								{
									outcome[0] = j+1;
									outcome[1] = x;
								}
								double tmppayoff = originalgame.getPayoff(outcome, i);
								expectedpayoofforx += tmppayoff*prob;
							}


						}
					}

					expectedpayoffs[i].add(expectedpayoofforx);
				}
			}

		}
		//now update the game
		OutcomeIterator itr = new OutcomeIterator(tmpsubgame);
		int index = 0; // keeping track of which expected payoff
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			for(int i=0; i<2; i++)
			{
				try{
					double tmppayoff = tmpsubgame.getPayoff(outcome, i);
					//System.out.println("Game size "+);
					index = outcome[i]-1;
					double modifiedpayoff = tmppayoff + expectedpayoffs[i].get(index);
					tmpsubgame.setPayoff(outcome, i, modifiedpayoff);
				}
				catch (Exception ex)
				{
					System.out.println("");
				}


			}

		}

	}






	/*
	 * 
	 * keeps track of whether it's a first iteration for a game
	 * so we need to reset(false) it after the first iteration
	 * If iteration is the first(true) one, then building the subgames will be same as before
	 * If iteration is not the first one, then subgames need to be modified.
	 * At the very end of all iterations for one game, we need to set(true) it again.  
	 */	
	public static boolean isIsfirstiteration() {
		return isfirstiteration;
	}

	public static void setIsfirstiteration(boolean isfirstiteration) {
		GameReductionBySubGame.isfirstiteration = isfirstiteration;
	}

	public static void buildSubGame(List<Integer> player1actions, List<Integer> player2actions, MatrixGame subgame)
	{

		int i=0; 
		int j=0;

		for(Integer x: player1actions)
		{
			j=0;
			for(Integer y: player2actions)
			{
				int[] outcome = {x, y };
				int[] subgameoutcome = {i+1, j+1};
				double payoff1 = GameReductionBySubGame.originalgame.getPayoff(outcome, 0);
				double payoff2 = GameReductionBySubGame.originalgame.getPayoff(outcome, 1);
				double[] payoff = {payoff1, payoff2}; 
				subgame.setPayoffs(subgameoutcome, payoff);
				j++;
			}

			i++;
		}

	}



	public static MatrixGame makeTestGame(int gamenumber, int size, double delta)
	{
		int[] N = {size, size};
		MakeGameForPartition mkgm = new MakeGameForPartition(N, 2, delta);
		mkgm.buildTestGame();
		MatrixGame testgm = mkgm.getOriginalgame();

		/*for(int i=0; i<N[0];i++)
		{
			for(int j=0;j<N[1]; j++)
			{

				int[] outcome = {i+1, j+1};
				double p1 = testgm.getPayoff(outcome, 0);
				double p2 = testgm.getPayoff(outcome, 1);
				//System.out.print("("+p1 + ", "+p2+")  ");
			}
			//System.out.println();
		}*/

		//for regret
		String gamename = Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION;

		//	String gamename = Parameters.GAME_FILES_PATH+gamenumber+"-"+size+"-"+delta+Parameters.GAMUT_GAME_EXTENSION;
		//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		try{

			PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			SimpleOutput.writeGame(pw,testgm);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}


		return testgm;


	}


	public static void testSubGameMethod()
	{
		int numberofplayers = 2;
		int numberofcluster = 3;
		int numberofaction = 6;
		double delta = 5;

		List<Integer>[][] dummypartition = new List[numberofplayers][];

		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}


		for(int i=0; i< 2; i++){

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}

		for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(5);
				dummypartition[i][0].add(1);
				dummypartition[i][1].add(6);
				dummypartition[i][1].add(4);
				dummypartition[i][2].add(2);
				dummypartition[i][2].add(3);



			}
			else
			{
				dummypartition[i][0].add(2);
				dummypartition[i][0].add(6);
				dummypartition[i][1].add(1);
				dummypartition[i][1].add(5);
				dummypartition[i][2].add(3);
				dummypartition[i][2].add(4);
			}

		}
		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);

		MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"0"+Parameters.GAMUT_GAME_EXTENSION));
		gmr.setOriginalgame(tstgame);
		for(int iteration =0; iteration<10; iteration++)
		{
			double epsilon;
			try {
				epsilon = gmr.startProcessing(0,iteration);
				System.out.println("Epsilon: "+ epsilon);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


	}

	/**
	 * solving sobgames using qre and IED, iterative elimination of dominance
	 * Also check whether we need to solve the subgame depending on the strategy 
	 * If iteration =0, every subgame is solved. 
	 * If iteration>0 subgame is solved if either of the support for playing a cluster >0 in the strategy 
	 * for previous iteration.
	 * If a subgame is not solved then [prevois iterations strategy for the subgame is copied. 
	 * To solve a subgame first PSNE is applied. If epsilon!=0, then qre is applied.
	 * @throws Exception 
	 */
	public static void  solveSubGamesV1(int iteration) throws Exception
	{

		//	int subgame = 0;
		int players = originalgame.getNumPlayers();
		ArrayList<MixedStrategy> player1strategies = new ArrayList<MixedStrategy>();
		ArrayList<MixedStrategy> player2strategies = new ArrayList<MixedStrategy>();
		Date start = new Date();
		long l1 = start.getTime();
		int subgamegameindex = 0;
		for(MatrixGame subgamex: GameReductionBySubGame.subgames)
		{
			MixedStrategy[] subgameprofile = new MixedStrategy[players];
			if(subgamex.getNumActions(0)==0 || subgamex.getNumActions(1)==0)
			{
				subgameprofile[0] = new MixedStrategy(subgamex.getNumActions(0));
				subgameprofile[1] = new MixedStrategy(subgamex.getNumActions(1));



			}
			else
			{



				if(iteration==-1)
				{
					/*
					 * for each subgame set uniform random
					 */

					//MixedStrategy[] subgameprofile = new MixedStrategy[players];
					subgameprofile[0] = new MixedStrategy(subgamex.getNumActions(0));
					subgameprofile[1] = new MixedStrategy(subgamex.getNumActions(1));

					for(int player=0; player<2; player++)
					{
						subgameprofile[player].setZeros();

						/*for(int i=0; i<; i++)
						{
							subgameprofile[0].setUniform();
							subgameprofile[1].setUniform();
						}*/
					}

				}
				else if(iteration>=0)
				{
					/*
					 * Check if the subgame needs to be solved at all in the next iterations
					 * To do that take previous iterations's reducedgames strategy. 
					 * Do not solve the game if one or the other strategy has zero probability 
					 */

					boolean needstosolve = false;
					if(iteration==0)
					{
						needstosolve = true;
					}
					else
					{
						needstosolve = needToSolve(subgamegameindex);
					}
					/*
					 * 1. first remove actions using IED
					 * 2. Put the removed and remaining actions in a hashmap. 
					 * 3. After using IED, create a mapping between remaining and the reduced game's actions.
					 * 4. AFter solving using qre, use the mapping to assign probabilities to appropriate actions. 
					 */
					//int players = x.getNumPlayers();

					if(true==needstosolve)
					{

						/**
						 * Use PSNE first, see if  e==0,
						 * if not then do IED and QRE
						 */
						int psnesolver = 0; // psne
						MixedStrategy[] subgamepsne =  SolverCombo.computeStrategyWithOneSolver(psnesolver, subgamex);
						ArrayList<MixedStrategy> list = new ArrayList<MixedStrategy>();
						list.add(subgamepsne[0]);
						list.add(subgamepsne[1]);
						OutcomeDistribution distro = new OutcomeDistribution(list);
						double epsilonpsne = SolverUtils.computeOutcomeStability(subgamex, distro);
						//double epsilonpsne = 5;
						if(epsilonpsne<=0)
						{
							subgameprofile[0] = new MixedStrategy(subgamepsne[0].getProbs()) ;
							subgameprofile[1] = new MixedStrategy(subgamepsne[1].getProbs());
						}
						else if(epsilonpsne>0)
						{
							Set<Integer>[] remaining = new HashSet[players];
							Set<Integer>[] removed = new HashSet[players];
							for(int player = 0; player<players; player++)
							{
								remaining[player] = new HashSet<Integer>();
								removed[player] = new HashSet<Integer>();
							}
							//step 1,2
							MatrixGame IEDmat = IEDSMatrixGames.IEDS(subgamex, remaining, removed);

							/*
							 * assign 0 to removed actions
							 */
							for(int player = 0; player<players; player++)
							{
								subgameprofile[player] = new MixedStrategy(subgamex.getNumActions(player));
								for(int removedaction : removed[player])
								{
									subgameprofile[player].setProb(removedaction, 0);
								}

							}
							int[] solver = {3}; // QRE and use IEDmat
							MixedStrategy[] tmpsubgameprofile =  SolverCombo.computeStrategyWithMultipleSolvers(solver, IEDmat);
							if( (tmpsubgameprofile[0].getProbs().length-1) != remaining[0].size()   ||  (tmpsubgameprofile[1].getProbs().length-1) != remaining[1].size() )
							{
								throw new Exception("SOmething wrong in IED, tmpsubgameprofile");
							}
							if( (subgameprofile[0].getProbs().length-1) != (remaining[0].size()+removed[0].size())   ||  (subgameprofile[1].getProbs().length-1) != (remaining[1].size()+removed[1].size()) )
							{
								throw new Exception("SOmething wrong in IED, subgameprofile");
							}
							/*
							 * from the IEDmat solution assign probabilities to the subgameprofile
							 */
							for(int player = 0; player<players; player++)
							{
								int action = 1;
								for(int remainingaction : remaining[player]) //mapping
								{
									double prob = tmpsubgameprofile[player].getProb(action);
									subgameprofile[player].setProb(remainingaction, prob);
									action++; // go to next action of IEDmat
								}
							}
						}
					}
					else // subgame was not solved. 
					{
						// copy previous iteration's strategy
						subgameprofile[0] = new MixedStrategy(subgamex.getNumActions(0));
						subgameprofile[1] = new MixedStrategy(subgamex.getNumActions(1));
						subgameprofile[0].setProbs(GameReductionBySubGame.prevsubgamestrategy.get(0).get(subgamegameindex).getProbs());
						subgameprofile[1].setProbs(GameReductionBySubGame.prevsubgamestrategy.get(1).get(subgamegameindex).getProbs());

					}
				}
			}
			player1strategies.add(subgameprofile[0]);
			player2strategies.add(subgameprofile[1]);
			subgamegameindex++;
		}
		Date stop = new Date();
		long l2 = stop.getTime();
		long diff = l2 - l1;
		GameReductionBySubGame.subgamesolvingtime += diff;
		GameReductionBySubGame.subgamesolvingcounter++;

		if( (GameReductionBySubGame.numberofsubgames != player1strategies.size()) || (GameReductionBySubGame.numberofsubgames != player2strategies.size()))
		{
			try {
				throw new Exception("Error in subgame");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		GameReductionBySubGame.subgamestrategy.clear();
		GameReductionBySubGame.subgamestrategy.add(player1strategies);
		GameReductionBySubGame.subgamestrategy.add(player2strategies);
		GameReductionBySubGame.prevsubgamestrategy.clear();
		//always saves previous round's subgame strategy
		GameReductionBySubGame.prevsubgamestrategy.add(player1strategies);
		GameReductionBySubGame.prevsubgamestrategy.add(player2strategies);

	}



	/**
	 * Does support monitoring, builds subgames, solve them using PSNE+QRE, builds the hierarchical game, solves it using PSNE + qre, returns the epsilon
	 * @param gamenumber game number
	 * @param iteration iteration number for a game
	 * @param lastiterationepsilon for a game it's the epsilon of the last iteration
	 * @return epsilon
	 * @throws Exception 
	 */
	public  double startProcessingV4(int gamenumber, int iteration, double lastiterationepsilon) throws Exception
	{

		//int subiteration=0;
		double epsilonz= -1;
		GameReductionBySubGame.buildSubGames(gamenumber, iteration);
		if(GameReductionBySubGame.numberofsubgames != GameReductionBySubGame.subgames.size())
		{
			try {
				throw new Exception("Error");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		GameReductionBySubGame.solveSubGamesV1(iteration);
		MatrixGame reducedgame = GameReductionBySubGame.collapseOriginalGame();


		//String gamename = Parameters.GAME_FILES_PATH+"reducedgame-"+gamenumber+Parameters.GAMUT_GAME_EXTENSION;
		//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;
		/*
		try
		{

			//PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			//SimpleOutput.writeGame(pw,reducedgame);
			//pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}*/



		MixedStrategy[] reducedgamestrategy = new MixedStrategy[2];
		Date start = new Date();
		long l1 = start.getTime();
		int psnesolver = 0; //psne
		MixedStrategy[] hierarchgamepsne =  SolverCombo.computeStrategyWithOneSolver(psnesolver, reducedgame);
		ArrayList<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(hierarchgamepsne[0]);
		list.add(hierarchgamepsne[1]);
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double epsilonpsne = SolverUtils.computeOutcomeStability(reducedgame, distro);
		if(epsilonpsne<=0)
		{
			reducedgamestrategy[0] = new MixedStrategy(hierarchgamepsne[0].getProbs()) ;
			reducedgamestrategy[1] = new MixedStrategy(hierarchgamepsne[1].getProbs());
		}
		else if(epsilonpsne>0)
		{
			QRESolver qresubgame = new QRESolver(100);
			EmpiricalMatrixGame emsubgame = new EmpiricalMatrixGame(reducedgame);
			qresubgame.setDecisionMode(QRESolver.DecisionMode.RAW);
			for(int i =0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
			{
				reducedgamestrategy[i] = qresubgame.solveGame(emsubgame, i);

			}
			/**
			 * what if psneepsilon is less than qre epsilon
			 */
		}

		Date stop = new Date();
		long l2 = stop.getTime();
		long diff = l2 - l1;
		GameReductionBySubGame.reducedgamestrategy.clear();
		GameReductionBySubGame.reducedgamestrategy.add(reducedgamestrategy);
		GameReductionBySubGame.hierarchicalsolvingtime += diff;
		GameReductionBySubGame.hierarchicalsolvingcounter++;
		MixedStrategy[] subgamestrategy = GameReductionBySubGame.buidMixedStrategyForOriginalGame(); 
		for(int j =0; j< reducedgame.getNumPlayers(); j++)
		{

			for(int k=0; k< reducedgamestrategy[j].getNumActions(); k++)
			{
				List<Integer> actions = GameReductionBySubGame.getOriginalActions(k+1, j);
				double prob = reducedgamestrategy[j].getProb(k+1);
				for(Integer x: actions)
				{
					double subgmprob = subgamestrategy[j].getProb(x);
					GameReductionBySubGame.finalstrategy[j].setProb(x, subgmprob*prob);

				}

			}

		}
		ArrayList<MixedStrategy> finalstrategylist = new ArrayList<MixedStrategy>();
		finalstrategylist.add(GameReductionBySubGame.finalstrategy[0]);
		finalstrategylist.add(GameReductionBySubGame.finalstrategy[1]);
		//System.out.println("Final strategy 0 normalized : " + GameReductionBySubGame.finalstrategy[0].checkIfNormalized());
		//System.out.println("Final strategy 1 normalized : " + GameReductionBySubGame.finalstrategy[1].checkIfNormalized());
		OutcomeDistribution finalstrategydistro = new OutcomeDistribution(finalstrategylist);
		//double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(GameReductionBySubGame.originalgame, distro);
		//System.out.println("\n Expected payoff "+ expectedpayoff[0]+" " +expectedpayoff[1]) ;
		epsilonz = SolverUtils.computeOutcomeStability(GameReductionBySubGame.originalgame, finalstrategydistro);

		/**
		 * print higher level games epsilon with support for both players.
		 */

		/*
		 * printing results for support monitoring
		 */
		/*
		int[] supportsize = new int[2];
		for(int i=0; i<2; i++)
		{
			supportsize[i] = getSupport(reducedgamestrategy[i]);
		}
		int maxsupportsize = supportsize[0]>supportsize[1]? supportsize[0]: supportsize[1]; 
		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"support.csv"),true));
			if(iteration>0)
			{
				pw.append(maxsupportsize+","+( (lastiterationepsilon>epsilonz)? 1 : 0) +","+( (lastiterationepsilon>epsilonz) ? (lastiterationepsilon-epsilonz) : 0) + "\n");
			}
			else
			{
				pw.append(maxsupportsize+ "," + 0 +"," + 0 + "\n");
			}
			pw.close();

		}
		catch(Exception e)
		{

		}*/
		return epsilonz;

	}	




	/**
	 * Does support monitoring, builds subgames, solve them, builds the hierarchical game, solves it, returns the epsilon
	 * @param gamenumber game number
	 * @param iteration iteration number for a game
	 * @param lastiterationepsilon for a game it's the epsilon of the last iteration
	 * @return epsilon
	 * @throws Exception 
	 */
	public  double startProcessingV3(int gamenumber, int iteration, double lastiterationepsilon) throws Exception
	{

		//int subiteration=0;
		double epsilonz= -1;
		GameReductionBySubGame.buildSubGames(gamenumber, iteration);
		if(GameReductionBySubGame.numberofsubgames != GameReductionBySubGame.subgames.size())
		{
			try {
				throw new Exception("Error");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		GameReductionBySubGame.solveSubGamesV1(iteration);
		MatrixGame reducedgame = GameReductionBySubGame.collapseOriginalGame();


		//String gamename = Parameters.GAME_FILES_PATH+"reducedgame-"+gamenumber+Parameters.GAMUT_GAME_EXTENSION;
		//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;
		/*
		try
		{

			//PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			//SimpleOutput.writeGame(pw,reducedgame);
			//pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}*/



		MixedStrategy[] reducedgamestrategy = new MixedStrategy[2];
		QRESolver qresubgame = new QRESolver();
		EmpiricalMatrixGame emsubgame = new EmpiricalMatrixGame(reducedgame);
		qresubgame.setDecisionMode(QRESolver.DecisionMode.RAW);
		Date start = new Date();
		long l1 = start.getTime();
		/**
		 * we can try MEB
		 */
		//int solver = 2; // MEB
		//reducedgamestrategy = SolverCombo.computeStrategyWithOneSolver(solver, reducedgame);


		/**
		 * QRE
		 */
		for(int i =0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		{
			reducedgamestrategy[i] = qresubgame.solveGame(emsubgame, i);

		}

		Date stop = new Date();
		long l2 = stop.getTime();
		long diff = l2 - l1;
		GameReductionBySubGame.reducedgamestrategy.clear();
		GameReductionBySubGame.reducedgamestrategy.add(reducedgamestrategy);
		GameReductionBySubGame.hierarchicalsolvingtime += diff;
		GameReductionBySubGame.hierarchicalsolvingcounter++;
		MixedStrategy[] subgamestrategy = GameReductionBySubGame.buidMixedStrategyForOriginalGame(); 
		for(int j =0; j< reducedgame.getNumPlayers(); j++)
		{

			for(int k=0; k< reducedgamestrategy[j].getNumActions(); k++)
			{
				List<Integer> actions = GameReductionBySubGame.getOriginalActions(k+1, j);
				double prob = reducedgamestrategy[j].getProb(k+1);
				for(Integer x: actions)
				{
					double subgmprob = subgamestrategy[j].getProb(x);
					GameReductionBySubGame.finalstrategy[j].setProb(x, subgmprob*prob);

				}

			}

		}
		ArrayList<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(GameReductionBySubGame.finalstrategy[0]);
		list.add(GameReductionBySubGame.finalstrategy[1]);
		//System.out.println("Final strategy 0 normalized : " + GameReductionBySubGame.finalstrategy[0].checkIfNormalized());
		//System.out.println("Final strategy 1 normalized : " + GameReductionBySubGame.finalstrategy[1].checkIfNormalized());
		OutcomeDistribution finalstrategydistro = new OutcomeDistribution(list);
		//double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(GameReductionBySubGame.originalgame, distro);
		//System.out.println("\n Expected payoff "+ expectedpayoff[0]+" " +expectedpayoff[1]) ;
		epsilonz = SolverUtils.computeOutcomeStability(GameReductionBySubGame.originalgame, finalstrategydistro);

		/**
		 * print higher level games epsilon with support for both players.
		 */

		/*
		 * printing results for support monitoring
		 */
		/*
		int[] supportsize = new int[2];
		for(int i=0; i<2; i++)
		{
			supportsize[i] = getSupport(reducedgamestrategy[i]);
		}
		int maxsupportsize = supportsize[0]>supportsize[1]? supportsize[0]: supportsize[1]; 
		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"support.csv"),true));
			if(iteration>0)
			{
				pw.append(maxsupportsize+","+( (lastiterationepsilon>epsilonz)? 1 : 0) +","+( (lastiterationepsilon>epsilonz) ? (lastiterationepsilon-epsilonz) : 0) + "\n");
			}
			else
			{
				pw.append(maxsupportsize+ "," + 0 +"," + 0 + "\n");
			}
			pw.close();

		}
		catch(Exception e)
		{

		}*/



		/*try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"itr_result"+".csv"),true));
			pw.append("\n"+iteration+","+epsilonz);
			pw.close();

		}
		catch(Exception e)
		{

		}*/
		return epsilonz;

	}	



	public static void LouvainVsKmean() throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = 2;
		int numberofcluster = 3;
		int numberofaction = 6;
		double[] deltas = {0, 5, 20, 50};
		double margin = 1; // margin to include best responses for the graph for louvain method
		int totalgames = 50;//3
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createRandomPartition(numberofcluster, numberofaction, dummypartition);
		/*
		 * create a predefined partition
		 */

		/*for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(7);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(9);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(8);



			}
			else
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(9);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(8);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(7);
			}

		}
		 */

		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;
		double resdeltas[] = new double[deltas.length];
		double reskmeandeltas[] = new double[deltas.length];

		int deltcnt = 0;
		for(double delta: deltas)
		{

			if(GameReductionBySubGame.buildtestgame== true)
			{
				createTestGames(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition);

			} // end of if
			/*
			 * test games are built. partitions are stored.
			 * now do test
			 */

			for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
			{
				MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION));

				gmr.setOriginalgame(tstgame);
				//printgame(tstgame, gamenumber);



				/**
				 * copy partition from louvain method
				 */

				//makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), GameReductionBySubGame.partition);
				System.out.println("doing louvain, game "+ gamenumber + "...");
				System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);
				List<Integer>[][] tmppartition = LouvainClusteringActions.getLouvainClustering(tstgame, numberofcluster, margin); 
				GameReductionBySubGame.numberofsubgames = tmppartition[0].length;
				makeDeepCopyPartition(tmppartition, GameReductionBySubGame.partition);

				System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);

				double[] delta1 = calculateDelta(tstgame, dummypartition, 0, true);
				double[] delta2 = calculateDelta(tstgame, dummypartition, 1, true);
				System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);
				double delt = Math.max(delta1[0], delta2[0]);
				resdeltas[deltcnt] += delt;


				/**
				 * kmeans
				 */
				List<Integer>[][] clusterforplayers = new List[tstgame.getNumPlayers()][tmppartition[0].length];
				clusterforplayers[0] = KmeanClustering.clusterActions(tmppartition[0].length, 0, tstgame);
				clusterforplayers[1] = KmeanClustering.clusterActions(tmppartition[0].length, 1, tstgame);

				double deltak1 = KmeanClustering.calculateDelta(tstgame, clusterforplayers, 0, true);
				double deltak2 = KmeanClustering.calculateDelta(tstgame, clusterforplayers, 1, true);
				reskmeandeltas[deltcnt] += Math.max(deltak1, deltak2);
				printPartition(clusterforplayers, 1);
				System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);


				////////



			}
			deltcnt++;
		}


		for(int i=0; i<deltas.length; i++)
		{
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"deltaresult.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				pw.append(resdeltas[i]/totalgames+","+reskmeandeltas[i]/totalgames+",");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}

	}







	public static void clusterDistributionExperiment() throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = 2;
		int numberofcluster = 5;
		int numberofaction = 50;
		double delta = 10;
		//double margin = 0; // margin to include best responses for the graph for louvain method
		int totalgames = 20;//3
		int limit_comsize = 2*(numberofaction/numberofcluster);
		double [] margins = {0, 1, 2, 3};
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createRandomPartition(numberofcluster, numberofaction, dummypartition);
		/*
		 * create a predefined partition
		 */

		/*for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(7);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(9);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(8);



			}
			else
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(9);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(8);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(7);
			}

		}
		 */

		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;





		if(GameReductionBySubGame.buildtestgame== true)
		{
			createTestGames(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition);

		} // end of if
		/*
		 * test games are built. partitions are stored.
		 * now do test
		 */



		double[] avgnumberofcommunity = new double[margins.length];
		double[] avrgcommunitysize = new double[margins.length];
		double[] avgepsilon = new double[margins.length];


		int[][] distribution = new int[margins.length][numberofaction*2];


		int margincounter = 0;



		for(double margin: margins)
		{


			int ITERATION = 100;
			double sumlpsneepsilon = 0;
			double sumlmebepsilon = 0;
			double sumlqreepsilon = 0;
			//double sum


			double sumsubgameepsilon = 0;
			double sumqreepsilon = 0;
			double sumpsneepsilon = 0;
			double summebepsilon = 0;
			resetTimerParameters();
			long totaltimesubgame = 0;
			int totalsubgametimecounter = 0;
			double[] eps = new double[160];
			long louviantime = 0;
			long louvainclustertime = 0;
			long kmeantime = 0;
			for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
			{
				MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION));
				GameReductionBySubGame.setIsfirstiteration(true);
				gmr.setOriginalgame(tstgame);
				//printgame(tstgame, gamenumber);



				/**
				 * copy partition from louvain method
				 */

				//makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), GameReductionBySubGame.partition);
				System.out.println("doing louvain, game "+ gamenumber + "...");
				System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);
				System.out.println("Margin  "+ margin);
				Date start = new Date();
				long l1 = start.getTime();
				//List<Integer>[][] tmppartition = LouvainClusteringActions.getLouvainClustering(tstgame, numberofcluster, margin); 
				List<Integer>[][] tmppartition = LouvainClusteringActions.getFixedLouvainClustering(tstgame, numberofcluster, margin, limit_comsize);

				//List<Integer>[][] tmppartition = SolverExperiments.getKmeanCLusters(numberofcluster, tstgame, true); 


				avgnumberofcommunity[margincounter] += Math.max(tmppartition[0].length, tmppartition[1].length);
				avrgcommunitysize[margincounter] += getAvgCommunitySize(tmppartition);

				for(int b=0; b<tmppartition[0].length; b++)
				{

					int s = tmppartition[0][b].size()+ tmppartition[1][b].size();
					distribution[margincounter][s]++;
				}




				Date stop = new Date();
				long l2 = stop.getTime();
				long diff = l2 - l1;
				louvainclustertime += diff;
				GameReductionBySubGame.numberofsubgames = tmppartition[0].length;
				makeDeepCopyPartition(tmppartition, GameReductionBySubGame.partition);

				System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);

				/////////////////////////////////////////




				ArrayList<MixedStrategy[]> finalstrategies = new ArrayList<MixedStrategy[]>();
				GameReductionBySubGame.finalstrategy[0].setUniform();
				GameReductionBySubGame.finalstrategy[1].setUniform();
				/*
				 * create a hashmap to save all the strategies and all the epsilon
				 */
				HashMap<Integer,MixedStrategy[]> strategycontainer = new HashMap<Integer,MixedStrategy[]>();
				HashMap<Integer,Double> epsiloncontainer = new HashMap<Integer,Double>();
				Double minimumepsilonyet = Double.MAX_VALUE;
				Double oldepsilon = -1.0;

				int lastiter = -1; 
				for(int iteration = 0;iteration<160; iteration++)
				{
					/*
					 * copy new strategies to old ones. 
					 * 
					 */
					copyNewStrategies(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
					double epsilon = -1;
					// send the epsilon from the last iteration, need it for support monitoring
					start = new Date();
					l1 = start.getTime();
					if(iteration>0)
					{
						double lastiterationepsilon = epsiloncontainer.get(iteration-1);
						if(oldepsilon != lastiterationepsilon)
						{
							throw new Exception();
						}
						epsilon = gmr.startProcessingV3(gamenumber, iteration, epsiloncontainer.get(iteration-1));
					}
					else
					{	
						epsilon = gmr.startProcessingV3(gamenumber, iteration, -1);
					}
					stop = new Date();
					l2 = stop.getTime();
					diff = l2 - l1;
					totaltimesubgame += diff;
					totalsubgametimecounter++;

					louviantime += (diff+louvainclustertime);


					oldepsilon = epsilon;

					if(minimumepsilonyet>epsilon)
					{
						minimumepsilonyet = epsilon;
					}
					/*
					 * print the epsilon for iterations
					 */

					try
					{
						//PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"itr_result"+".csv"),true));
						eps[iteration] += minimumepsilonyet;
						lastiter = iteration;
						//pw.append("\n"+iteration+","+minimumepsilonyet);
						//pw.close();

					}
					catch(Exception e)
					{

					}

					//test
					epsiloncontainer.put(iteration, epsilon);
					if(epsilon==0)
					{
						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
						lastiter = iteration;
						break;

					}
					System.out.println("Epsilon: "+ epsilon);
					/*
					 * check if the new strategy changed
					 */
					boolean changed = checkIfNewStrategyChanged(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
					if(iteration>0 && (changed != true))
					{

						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
						lastiter = iteration;
						break;
					}
					/*
					 * check if any repetition occured
					 */


					if(finalstrategies.size()==0)
					{
						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						finalstrategies.add(tmpstr);
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
					}
					else
					{
						/*
						 * check for repetition.
						 */
						boolean repeat = checkForRepetition(finalstrategies, finalstrategy);
						if(repeat==false)
						{
							MixedStrategy[] tmpstr = new MixedStrategy[2];
							tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
							tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
							finalstrategies.add(tmpstr);
							strategycontainer.put(iteration, tmpstr);
							epsiloncontainer.put(iteration, epsilon);
						}
						else
						{
							lastiter = iteration;
							break;
						}
					}



				}

				/**
				 * fill the rest of the iteration
				 */

				for(int pp=lastiter+1; pp<160; pp++)
				{
					eps[pp] += minimumepsilonyet;
				}

				/*
				 * find the strategy with minimum epsilon
				 */
				Double minepsilon = getMinEpsilon(epsiloncontainer);
				//double qreeps = gmr.solveUsingQRE();
				sumsubgameepsilon += minepsilon;


				/*int solvers[] = {0,2,3};
				ArrayList<Double> luvainwithothers = doLouvainWithOthers(tmppartition, tstgame, solvers);
			//double res[] = new double[luvainwithothers.size()];
			int c =0;
			for(Double x: luvainwithothers)
			{
				//res[c++]= x;
				if(c==0)
				{
					sumlpsneepsilon += x;
				}
				else if(c==1)
				{
					sumlmebepsilon += x;
				}
				else if(c==2)
				{
					sumlqreepsilon+= x;
				}
				c++;


			}





				 *//**
				 * use other solvers with clustering to find stability
				 * 0.PSNE
				 * 1.CFR
				 * 2.MEB
				 * 3.QRE
				 *//*


				double[][] result = SolverExperiments.evaluateSolutionConcepts(GameReductionBySubGame.numberofsubgames, tstgame, Integer.toString(gamenumber) , true, solvers, 100);
				//	System.out.println("\nDone doing evaluating solution concepts ");

				System.out.println(louviantime+","+ kmeantimer + ", "+ psnetimer+","+mebtimer+","+qretimer);


				for(int j=0; j<result.length; j++)
				{
					if(j==0)
					{
						sumpsneepsilon = sumpsneepsilon + result[j][1];
					}
					else if(j==1)
					{
						summebepsilon += result[j][1];
					}
					else if(j==2)
					{
						sumqreepsilon += result[j][1];
					}
					//Logger.log("\n Running Instance "+ i+ " player "+ j + " delta: "+ result[j][0]+ " epsilon: "+result[j][1], false);

				}




				try
				{


					PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"result.csv"),true));
					// gamenumber, subgame, psne, meb,qre
					pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");
					pw.close();
				}
				catch(Exception ex){
					System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
				}
				  */
				//System.out.println("QRE Epsilon: "+ qreeps);


			}

			/*

		if(GameReductionBySubGame.subgamesolvingcounter != GameReductionBySubGame.hierarchicalsolvingcounter)
		{
			throw new Exception("error in timer counter");
		}

		calculateTimes(totaltimesubgame, totalsubgametimecounter, "totalsubgamemethod");

		calculateTimes(GameReductionBySubGame.subgamesolvingtime, GameReductionBySubGame.subgamesolvingcounter,"subgame");
		calculateTimes(GameReductionBySubGame.hierarchicalsolvingtime, GameReductionBySubGame.hierarchicalsolvingcounter,"hierarchical");
		calculateTimes(GameReductionBySubGame.psnetimer, GameReductionBySubGame.psnetimecounter,"psne");
		calculateTimes(GameReductionBySubGame.mebtimer, GameReductionBySubGame.mebtimecounter,"meb");
		calculateTimes(GameReductionBySubGame.qretimer, GameReductionBySubGame.qretimecounter,"qre");
			 */

			/*	for(int k=0; k<160; k++)
		{
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"iteration_result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				//pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");

				pw.append(k + ", "+(eps[k]/totalgames) + "\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}*/


			avgepsilon[margincounter] = sumsubgameepsilon/totalgames;
			avgnumberofcommunity[margincounter] = avgnumberofcommunity[margincounter]/totalgames;
			avrgcommunitysize[margincounter] = avrgcommunitysize[margincounter]/totalgames;
			margincounter++;

			/*System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
					sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","
					+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );*/

			/*System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
				sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","
				+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );*/





			/*System.out.println(delta+","+ louviantime/totalgames+","+ 
				(louvainclustertime+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.qretimer/2.0))/totalgames + ","

				+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.qretimer/2.0))/totalgames );
			 */



			/*System.out.println(delta+","+ louviantime/totalgames+","+ 


				+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.qretimer/2.0))/totalgames );*/

		}


		String output = "Element\tValue\tHistogram";
		/* Format histogram */

		// For each array element, output a bar in histogram

		margincounter=0;
		//for(double mar: margins)	
		{
			output = "";

			for ( int counter = 0; counter < distribution[0].length; counter++ ) {
				output += counter+",";

				for(int m=0; m<margins.length; m++)
				{
					output +=  distribution[m][ counter ] + ",";
				}
				output += "\n" ;


				// Print bar of asterisks                              

				/*for ( int stars = 0; stars < distribution[margincounter][ counter ]; stars++ ) {

					output += "*";  

				}*/

			}


			/* Print histogram */

			System.out.println(output);


			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"hist.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				//pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");

				pw.append(output);
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
			margincounter++;
		}







		for(int i=0; i<margins.length; i++)
		{
			System.out.println(margins[i] + ", " + avgepsilon[i] + "," + avrgcommunitysize[i] + "," + avgnumberofcommunity[i]);
		}






	}






	private static double getAvgCommunitySize(List<Integer>[][] tmppartition) {


		double sum =0;
		int size = Math.max(tmppartition[0].length, tmppartition[1].length);

		for(int i=0; i<size; i++)
		{
			//for(int j=0; j<tmppartition[0][i].size(); j++)
			{

				sum += tmppartition[0][i].size()+ tmppartition[1][i].size();
			}
		}
		return sum/tmppartition[0].length;
	}






	public static void deltaExperiment() throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = 2;
		int numberofcluster = 3;
		int numberofaction = 6;
		double[] deltas = {0, 5, 20, 50};
		double margin = 0; // margin to include best responses for the graph for louvain method
		int totalgames = 50;//3
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createRandomPartition(numberofcluster, numberofaction, dummypartition);
		/*
		 * create a predefined partition
		 */

		/*for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(7);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(9);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(8);



			}
			else
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(9);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(8);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(7);
			}

		}
		 */

		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;
		double resdeltas[] = new double[deltas.length];

		int deltcnt = 0;
		for(double delta: deltas)
		{

			if(GameReductionBySubGame.buildtestgame== true)
			{
				createTestGames(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition);

			} // end of if
			/*
			 * test games are built. partitions are stored.
			 * now do test
			 */

			for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
			{
				MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION));

				gmr.setOriginalgame(tstgame);
				//printgame(tstgame, gamenumber);



				/**
				 * copy partition from louvain method
				 */

				//makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), GameReductionBySubGame.partition);
				System.out.println("doing louvain, game "+ gamenumber + "...");
				System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);
				List<Integer>[][] tmppartition = LouvainClusteringActions.getLouvainClustering(tstgame, numberofcluster, margin); 
				GameReductionBySubGame.numberofsubgames = tmppartition[0].length;
				makeDeepCopyPartition(tmppartition, GameReductionBySubGame.partition);

				System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);

				double[] delta1 = calculateDelta(tstgame, dummypartition, 0, true);
				double[] delta2 = calculateDelta(tstgame, dummypartition, 1, true);
				System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);
				double delt = Math.max(delta1[0], delta2[0]);
				resdeltas[deltcnt] += delt;


			}
			deltcnt++;
		}


		for(int i=0; i<deltas.length; i++)
		{
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"deltaresult.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				pw.append(resdeltas[i]/totalgames+",");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}


	}




	/**
	 * this method performs test on subgame solution technique and QRE, PSNE,MEB
	 * 
	 * @throws Exception
	 */
	public static void testSubGameSolverV3() throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = 2;
		int numberofcluster = 3;
		int limit_comsize=10;
		int numberofaction = 9;
		double delta = 0;
		double margin = 1; // margin to include best responses for the graph for louvain method
		int totalgames = 1;//3
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createRandomPartition(numberofcluster, numberofaction, dummypartition);
		/*
		 * create a predefined partition
		 */

		/*for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(7);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(9);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(8);



			}
			else
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				//dummypartition[i][0].add(9);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				//dummypartition[i][1].add(8);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);
				//dummypartition[i][2].add(7);
			}

		}
		 */

		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;

		if(GameReductionBySubGame.buildtestgame== true)
		{
			createTestGames(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition);

		} // end of if
		/*
		 * test games are built. partitions are stored.
		 * now do test
		 */
		int ITERATION = 1;
		double sumlpsneepsilon = 0;
		double sumlmebepsilon = 0;
		double sumlqreepsilon = 0;
		//double sum


		double sumsubgameepsilon = 0;
		double sumqreepsilon = 0;
		double sumpsneepsilon = 0;
		double summebepsilon = 0;
		resetTimerParameters();
		long totaltimesubgame = 0;
		int totalsubgametimecounter = 0;
		double[] eps = new double[160];
		long louviantime = 0;
		long louvainclustertime = 0;
		long kmeantime = 0;
		for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
		{
			MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION));
			GameReductionBySubGame.setIsfirstiteration(true);
			gmr.setOriginalgame(tstgame);
			//printgame(tstgame, gamenumber);



			/**
			 * copy partition from louvain method
			 */

			makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), GameReductionBySubGame.partition);
			//System.out.println("doing louvain, game "+ gamenumber + "...");
			System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);
			Date start = new Date();
			long l1 = start.getTime();

			//List<Integer>[][] tmppartition = LouvainClusteringActions.getLouvainClustering(tstgame, numberofcluster, margin); 
			//List<Integer>[][] tmppartition = LouvainClusteringActions.getFixedLouvainClustering(tstgame, numberofcluster, margin, limit_comsize);
			//List<Integer>[][] tmppartition = SolverExperiments.getKmeanCLusters(numberofcluster, tstgame, true); 


			Date stop = new Date();
			long l2 = stop.getTime();
			long diff = l2 - l1;
			louvainclustertime += diff;
			GameReductionBySubGame.numberofsubgames = GameReductionBySubGame.partition[0].length;
			//makeDeepCopyPartition(tmppartition, GameReductionBySubGame.partition);

			//System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);

			/////////////////////////////////////////




			ArrayList<MixedStrategy[]> finalstrategies = new ArrayList<MixedStrategy[]>();
			GameReductionBySubGame.finalstrategy[0].setUniform();
			GameReductionBySubGame.finalstrategy[1].setUniform();
			/*
			 * create a hashmap to save all the strategies and all the epsilon
			 */
			HashMap<Integer,MixedStrategy[]> strategycontainer = new HashMap<Integer,MixedStrategy[]>();
			HashMap<Integer,Double> epsiloncontainer = new HashMap<Integer,Double>();
			Double minimumepsilonyet = Double.MAX_VALUE;
			Double oldepsilon = -1.0;

			int lastiter = -1; 
			for(int iteration = 0;iteration<160; iteration++)
			{
				/*
				 * copy new strategies to old ones. 
				 * 
				 */
				copyNewStrategies(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				double epsilon = -1;
				// send the epsilon from the last iteration, need it for support monitoring
				start = new Date();
				l1 = start.getTime();
				if(iteration>0)
				{
					double lastiterationepsilon = epsiloncontainer.get(iteration-1);
					if(oldepsilon != lastiterationepsilon)
					{
						throw new Exception();
					}
					epsilon = gmr.startProcessingV3(gamenumber, iteration, epsiloncontainer.get(iteration-1));
				}
				else
				{	
					epsilon = gmr.startProcessingV3(gamenumber, iteration, -1);
				}
				stop = new Date();
				l2 = stop.getTime();
				diff = l2 - l1;
				totaltimesubgame += diff;
				totalsubgametimecounter++;

				louviantime += (diff+louvainclustertime);


				oldepsilon = epsilon;

				if(minimumepsilonyet>epsilon)
				{
					minimumepsilonyet = epsilon;
				}
				/*
				 * print the epsilon for iterations
				 */

				try
				{
					//PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"itr_result"+".csv"),true));
					eps[iteration] += minimumepsilonyet;
					lastiter = iteration;
					//pw.append("\n"+iteration+","+minimumepsilonyet);
					//pw.close();

				}
				catch(Exception e)
				{

				}

				//test
				epsiloncontainer.put(iteration, epsilon);
				if(epsilon==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					lastiter = iteration;
					break;

				}
				System.out.println("Epsilon: "+ epsilon);
				/*
				 * check if the new strategy changed
				 */
				boolean changed = checkIfNewStrategyChanged(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				if(iteration>0 && (changed != true))
				{

					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					lastiter = iteration;
					break;
				}
				/*
				 * check if any repetition occured
				 */


				if(finalstrategies.size()==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					finalstrategies.add(tmpstr);
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
				}
				else
				{
					/*
					 * check for repetition.
					 */
					boolean repeat = checkForRepetition(finalstrategies, finalstrategy);
					if(repeat==false)
					{
						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						finalstrategies.add(tmpstr);
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
					}
					else
					{
						lastiter = iteration;
						break;
					}
				}



			}

			/**
			 * fill the rest of the iteration
			 */

			for(int pp=lastiter+1; pp<160; pp++)
			{
				eps[pp] += minimumepsilonyet;
			}

			/*
			 * find the strategy with minimum epsilon
			 */
			Double minepsilon = getMinEpsilon(epsiloncontainer);
			//double qreeps = gmr.solveUsingQRE();
			sumsubgameepsilon += minepsilon;


			int solvers[] = {0,2,3};
			/*ArrayList<Double> luvainwithothers = doLouvainWithOthers(tmppartition, tstgame, solvers);
			//double res[] = new double[luvainwithothers.size()];
			int c =0;
			for(Double x: luvainwithothers)
			{
				//res[c++]= x;
				if(c==0)
				{
					sumlpsneepsilon += x;
				}
				else if(c==1)
				{
					sumlmebepsilon += x;
				}
				else if(c==2)
				{
					sumlqreepsilon+= x;
				}
				c++;


			}*/





			/**
			 * use other solvers with clustering to find stability
			 * 0.PSNE
			 * 1.CFR
			 * 2.MEB
			 * 3.QRE
			 */


			double[][] result = SolverExperiments.evaluateSolutionConcepts(GameReductionBySubGame.numberofsubgames, tstgame, Integer.toString(gamenumber) , true, solvers, 100);
			//	System.out.println("\nDone doing evaluating solution concepts ");

			System.out.println(louviantime+","+ kmeantimer + ", "+ psnetimer+","+mebtimer+","+qretimer);


			for(int j=0; j<result.length; j++)
			{
				if(j==0)
				{
					sumpsneepsilon = sumpsneepsilon + result[j][1];
				}
				else if(j==1)
				{
					summebepsilon += result[j][1];
				}
				else if(j==2)
				{
					sumqreepsilon += result[j][1];
				}
				//Logger.log("\n Running Instance "+ i+ " player "+ j + " delta: "+ result[j][0]+ " epsilon: "+result[j][1], false);

			}




			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}

			//System.out.println("QRE Epsilon: "+ qreeps);


		}

		/*

		if(GameReductionBySubGame.subgamesolvingcounter != GameReductionBySubGame.hierarchicalsolvingcounter)
		{
			throw new Exception("error in timer counter");
		}

		calculateTimes(totaltimesubgame, totalsubgametimecounter, "totalsubgamemethod");

		calculateTimes(GameReductionBySubGame.subgamesolvingtime, GameReductionBySubGame.subgamesolvingcounter,"subgame");
		calculateTimes(GameReductionBySubGame.hierarchicalsolvingtime, GameReductionBySubGame.hierarchicalsolvingcounter,"hierarchical");
		calculateTimes(GameReductionBySubGame.psnetimer, GameReductionBySubGame.psnetimecounter,"psne");
		calculateTimes(GameReductionBySubGame.mebtimer, GameReductionBySubGame.mebtimecounter,"meb");
		calculateTimes(GameReductionBySubGame.qretimer, GameReductionBySubGame.qretimecounter,"qre");
		 */

		/*	for(int k=0; k<160; k++)
		{
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"iteration_result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				//pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");

				pw.append(k + ", "+(eps[k]/totalgames) + "\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}*/


		System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
				/*sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","*/
				+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );

		/*System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
				sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","
				+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );*/





		/*System.out.println(delta+","+ louviantime/totalgames+","+ 
				(louvainclustertime+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.qretimer/2.0))/totalgames + ","

				+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.qretimer/2.0))/totalgames );
		 */



		System.out.println(delta+","+ louviantime/totalgames+","+ 


				+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.qretimer/2.0))/totalgames );

	}


	public static void testCyberSubGame() throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = 2;
		int numberofcluster = 3;
		int limit_comsize=10;
		int numberofaction = 15;
		double delta = 0;
		double margin = 1; // margin to include best responses for the graph for louvain method
		int totalgames = 1;//3
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		List<List<Integer>[][]> allparitions = new ArrayList<List<Integer>[][]>();
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createPartition(numberofcluster, numberofaction, dummypartition);
		/*
		 * create a predefined partition
		 */

		/*for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				dummypartition[i][0].add(3);
				dummypartition[i][1].add(4);
				dummypartition[i][1].add(5);
				dummypartition[i][1].add(6);
				dummypartition[i][2].add(7);
				dummypartition[i][2].add(8);
				dummypartition[i][2].add(9);



			}
			else
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				dummypartition[i][0].add(3);
				dummypartition[i][1].add(4);
				dummypartition[i][1].add(5);
				dummypartition[i][1].add(6);
				dummypartition[i][2].add(7);
				dummypartition[i][2].add(8);
				dummypartition[i][2].add(9);
			}

		}*/


		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;

		if(GameReductionBySubGame.buildtestgame== true)
		{
			setupPartitionV2(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition, allparitions);

		} // end of if
		/*
		 * test games are built. partitions are stored.
		 * now do test
		 */
		int ITERATION = 1;
		double sumlpsneepsilon = 0;
		double sumlmebepsilon = 0;
		double sumlqreepsilon = 0;
		//double sum


		double sumsubgameepsilon = 0;
		double sumqreepsilon = 0;
		double sumpsneepsilon = 0;
		double summebepsilon = 0;
		resetTimerParameters();
		long totaltimesubgame = 0;
		int totalsubgametimecounter = 0;
		double[] eps = new double[160];
		//long louviantime = 0;
		//long louvainclustertime = 0;
		//long kmeantime = 0;
		for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
		{
			MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+numberofaction+"-"+gamenumber+Parameters.GAMUT_GAME_EXTENSION));
			GameReductionBySubGame.setIsfirstiteration(true);
			gmr.setOriginalgame(tstgame);
			printgame(tstgame, gamenumber);



			/**
			 * copy partition from louvain method
			 */

			makeDeepCopyPartition(allparitions.get(gamenumber), GameReductionBySubGame.partition);
			//makeDeepCopyPartition(dummypartition, GameReductionBySubGame.partition);
			//System.out.println("doing louvain, game "+ gamenumber + "...");
			System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);
			Date start = new Date();
			long l1 = start.getTime();

			//List<Integer>[][] tmppartition = LouvainClusteringActions.getLouvainClustering(tstgame, numberofcluster, margin); 
			//List<Integer>[][] tmppartition = LouvainClusteringActions.getFixedLouvainClustering(tstgame, numberofcluster, margin, limit_comsize);
			//List<Integer>[][] tmppartition = SolverExperiments.getKmeanCLusters(numberofcluster, tstgame, true); 


			Date stop = new Date();
			long l2 = stop.getTime();
			long diff = l2 - l1;
			//louvainclustertime += diff;
			GameReductionBySubGame.numberofsubgames = GameReductionBySubGame.partition[0].length;
			//makeDeepCopyPartition(tmppartition, GameReductionBySubGame.partition);

			//System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);

			/////////////////////////////////////////




			ArrayList<MixedStrategy[]> finalstrategies = new ArrayList<MixedStrategy[]>();
			GameReductionBySubGame.finalstrategy[0].setUniform();
			GameReductionBySubGame.finalstrategy[1].setUniform();
			/*
			 * create a hashmap to save all the strategies and all the epsilon
			 */
			HashMap<Integer,MixedStrategy[]> strategycontainer = new HashMap<Integer,MixedStrategy[]>();
			HashMap<Integer,Double> epsiloncontainer = new HashMap<Integer,Double>();
			Double minimumepsilonyet = Double.MAX_VALUE;
			Double oldepsilon = -1.0;

			int lastiter = -1; 
			for(int iteration = 0;iteration<160; iteration++)
			{
				/*
				 * copy new strategies to old ones. 
				 * 
				 */
				copyNewStrategies(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				double epsilon = -1;
				// send the epsilon from the last iteration, need it for support monitoring
				start = new Date();
				l1 = start.getTime();
				if(iteration>0)
				{
					double lastiterationepsilon = epsiloncontainer.get(iteration-1);
					if(oldepsilon != lastiterationepsilon)
					{
						throw new Exception();
					}
					epsilon = gmr.startProcessingV3(gamenumber, iteration, epsiloncontainer.get(iteration-1));
				}
				else
				{	
					epsilon = gmr.startProcessingV3(gamenumber, iteration, -1);
				}
				stop = new Date();
				l2 = stop.getTime();
				diff = l2 - l1;
				totaltimesubgame += diff;
				totalsubgametimecounter++;



				oldepsilon = epsilon;

				if(minimumepsilonyet>epsilon)
				{
					minimumepsilonyet = epsilon;
				}
				/*
				 * print the epsilon for iterations
				 */

				try
				{
					//PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"itr_result"+".csv"),true));
					eps[iteration] += minimumepsilonyet;
					lastiter = iteration;
					//pw.append("\n"+iteration+","+minimumepsilonyet);
					//pw.close();

				}
				catch(Exception e)
				{

				}

				//test
				epsiloncontainer.put(iteration, epsilon);
				if(epsilon==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					lastiter = iteration;
					break;

				}
				System.out.println("Epsilon: "+ epsilon);
				/*
				 * check if the new strategy changed
				 */
				boolean changed = checkIfNewStrategyChanged(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				if(iteration>0 && (changed != true))
				{

					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					lastiter = iteration;
					break;
				}
				/*
				 * check if any repetition occured
				 */


				if(finalstrategies.size()==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					finalstrategies.add(tmpstr);
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
				}
				else
				{
					/*
					 * check for repetition.
					 */
					boolean repeat = checkForRepetition(finalstrategies, finalstrategy);
					if(repeat==false)
					{
						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						finalstrategies.add(tmpstr);
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
					}
					else
					{
						lastiter = iteration;
						break;
					}
				}



			}

			/**
			 * fill the rest of the iteration
			 */

			for(int pp=lastiter+1; pp<160; pp++)
			{
				eps[pp] += minimumepsilonyet;
			}

			/*
			 * find the strategy with minimum epsilon
			 */
			Double minepsilon = getMinEpsilon(epsiloncontainer);
			//double qreeps = gmr.solveUsingQRE();
			sumsubgameepsilon += minepsilon;


			int solvers[] = {0,2,3};





			/**
			 * use other solvers with clustering to find stability
			 * 0.PSNE
			 * 1.CFR
			 * 2.MEB
			 * 3.QRE
			 */


			double[][] result = SolverExperiments.evaluateSolutionConcepts(GameReductionBySubGame.numberofsubgames, tstgame, Integer.toString(gamenumber) , true, solvers, 100);
			//	System.out.println("\nDone doing evaluating solution concepts ");



			for(int j=0; j<result.length; j++)
			{
				if(j==0)
				{
					sumpsneepsilon = sumpsneepsilon + result[j][1];
				}
				else if(j==1)
				{
					summebepsilon += result[j][1];
				}
				else if(j==2)
				{
					sumqreepsilon += result[j][1];
				}
				//Logger.log("\n Running Instance "+ i+ " player "+ j + " delta: "+ result[j][0]+ " epsilon: "+result[j][1], false);

			}




			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}

			//System.out.println("QRE Epsilon: "+ qreeps);


		}

		/*

		if(GameReductionBySubGame.subgamesolvingcounter != GameReductionBySubGame.hierarchicalsolvingcounter)
		{
			throw new Exception("error in timer counter");
		}

		calculateTimes(totaltimesubgame, totalsubgametimecounter, "totalsubgamemethod");

		calculateTimes(GameReductionBySubGame.subgamesolvingtime, GameReductionBySubGame.subgamesolvingcounter,"subgame");
		calculateTimes(GameReductionBySubGame.hierarchicalsolvingtime, GameReductionBySubGame.hierarchicalsolvingcounter,"hierarchical");
		calculateTimes(GameReductionBySubGame.psnetimer, GameReductionBySubGame.psnetimecounter,"psne");
		calculateTimes(GameReductionBySubGame.mebtimer, GameReductionBySubGame.mebtimecounter,"meb");
		calculateTimes(GameReductionBySubGame.qretimer, GameReductionBySubGame.qretimecounter,"qre");
		 */

		/*	for(int k=0; k<160; k++)
		{
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"iteration_result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				//pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");

				pw.append(k + ", "+(eps[k]/totalgames) + "\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}*/


		System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
				/*sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","*/
				+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );

		/*System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
				sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","
				+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );*/





		/*System.out.println(delta+","+ louviantime/totalgames+","+ 
				(louvainclustertime+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.qretimer/2.0))/totalgames + ","

				+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.qretimer/2.0))/totalgames );
		 */



		/*System.out.println(delta+","+ 


				+
				((GameReductionBySubGame.psnetimer))/totalgames+","+
				((GameReductionBySubGame.mebtimer))/totalgames+","+
				((GameReductionBySubGame.qretimer))/totalgames );*/

	}



	public static void transmissionExp(int iTER_LIMIT, int naction, int nplayer, int ncluster) throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = nplayer;
		int numberofcluster = ncluster;
		int limit_comsize=10;
		int numberofaction = naction;
		double delta = 0;
		double margin = 1; // margin to include best responses for the graph for louvain method
		int totalgames = iTER_LIMIT;//3
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		List<List<Integer>[][]> allparitions = new ArrayList<List<Integer>[][]>();
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createPartition(numberofcluster, numberofaction, dummypartition);


		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;


		setupPartitionV2(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition, allparitions);

		// end of if
		/*
		 * test games are built. partitions are stored.
		 * now do test
		 */
		int ITERATION = 1;
		double sumlpsneepsilon = 0;
		double sumlmebepsilon = 0;
		double sumlqreepsilon = 0;
		//double sum
		double sumdelta = 0;

		double sumsubgameepsilon = 0;
		double sumqreepsilon = 0;
		double sumpsneepsilon = 0;
		double summebepsilon = 0;
		resetTimerParameters();
		long totaltimesubgame = 0;
		int totalsubgametimecounter = 0;
		double[] eps = new double[160];
		//long louviantime = 0;
		//long louvainclustertime = 0;
		//long kmeantime = 0;
		for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
		{
			MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+numberofaction+"-"+numberofcluster+"-"+gamenumber+Parameters.GAMUT_GAME_EXTENSION));






			GameReductionBySubGame.setIsfirstiteration(true);
			gmr.setOriginalgame(tstgame);
			printgame(tstgame, gamenumber);

			makeDeepCopyPartition(allparitions.get(0), GameReductionBySubGame.partition);

			double tmpdelta = computeDelta(tstgame, GameReductionBySubGame.partition);

			sumdelta += tmpdelta;

			System.out.println("NUmber of subgames "+ GameReductionBySubGame.numberofsubgames);
			Date start = new Date();
			long l1 = start.getTime();




			Date stop = new Date();
			long l2 = stop.getTime();
			long diff = l2 - l1;
			//louvainclustertime += diff;
			GameReductionBySubGame.numberofsubgames = GameReductionBySubGame.partition[0].length;


			/////////////////////////////////////////




			ArrayList<MixedStrategy[]> finalstrategies = new ArrayList<MixedStrategy[]>();
			GameReductionBySubGame.finalstrategy[0].setUniform();
			GameReductionBySubGame.finalstrategy[1].setUniform();
			/*
			 * create a hashmap to save all the strategies and all the epsilon
			 */
			HashMap<Integer,MixedStrategy[]> strategycontainer = new HashMap<Integer,MixedStrategy[]>();
			HashMap<Integer,Double> epsiloncontainer = new HashMap<Integer,Double>();
			Double minimumepsilonyet = Double.MAX_VALUE;
			Double oldepsilon = -1.0;

			int lastiter = -1; 
			for(int iteration = 0;iteration<160; iteration++)
			{
				/*
				 * copy new strategies to old ones. 
				 * 
				 */
				copyNewStrategies(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				double epsilon = -1;
				// send the epsilon from the last iteration, need it for support monitoring
				start = new Date();
				l1 = start.getTime();
				if(iteration>0)
				{
					double lastiterationepsilon = epsiloncontainer.get(iteration-1);
					if(oldepsilon != lastiterationepsilon)
					{
						throw new Exception();
					}
					epsilon = gmr.startProcessingV3(gamenumber, iteration, epsiloncontainer.get(iteration-1));
				}
				else
				{	
					epsilon = gmr.startProcessingV3(gamenumber, iteration, -1);
				}
				stop = new Date();
				l2 = stop.getTime();
				diff = l2 - l1;
				totaltimesubgame += diff;
				totalsubgametimecounter++;



				oldepsilon = epsilon;

				if(minimumepsilonyet>epsilon)
				{
					minimumepsilonyet = epsilon;
				}
				/*
				 * print the epsilon for iterations
				 */

				try
				{
					//PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"itr_result"+".csv"),true));
					eps[iteration] += minimumepsilonyet;
					lastiter = iteration;
					//pw.append("\n"+iteration+","+minimumepsilonyet);
					//pw.close();

				}
				catch(Exception e)
				{

				}

				//test
				epsiloncontainer.put(iteration, epsilon);
				if(epsilon==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					lastiter = iteration;
					break;

				}
				System.out.println("Epsilon: "+ epsilon);
				/*
				 * check if the new strategy changed
				 */
				boolean changed = checkIfNewStrategyChanged(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				if(iteration>0 && (changed != true))
				{

					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					lastiter = iteration;
					break;
				}
				/*
				 * check if any repetition occured
				 */


				if(finalstrategies.size()==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					finalstrategies.add(tmpstr);
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
				}
				else
				{
					/*
					 * check for repetition.
					 */
					boolean repeat = checkForRepetition(finalstrategies, finalstrategy);
					if(repeat==false)
					{
						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						finalstrategies.add(tmpstr);
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
					}
					else
					{
						lastiter = iteration;
						break;
					}
				}



			}

			/**
			 * fill the rest of the iteration
			 */

			for(int pp=lastiter+1; pp<160; pp++)
			{
				eps[pp] += minimumepsilonyet;
			}

			/*
			 * find the strategy with minimum epsilon
			 */
			Double minepsilon = getMinEpsilon(epsiloncontainer);
			//double qreeps = gmr.solveUsingQRE();
			sumsubgameepsilon += minepsilon;


			int solvers[] = {0,2,3};





			/**
			 * use other solvers with clustering to find stability
			 * 0.PSNE
			 * 1.CFR
			 * 2.MEB
			 * 3.QRE
			 */


			double[][] result = SolverExperiments.evaluateSolutionConceptsSamePartition(GameReductionBySubGame.numberofsubgames, tstgame, Integer.toString(gamenumber) , true, solvers, 100, GameReductionBySubGame.partition);
			//	System.out.println("\nDone doing evaluating solution concepts ");



			for(int j=0; j<result.length; j++)
			{
				if(j==0)
				{
					sumpsneepsilon = sumpsneepsilon + result[j][1];
				}
				else if(j==1)
				{
					summebepsilon += result[j][1];
				}
				else if(j==2)
				{
					sumqreepsilon += result[j][1];
				}
				//Logger.log("\n Running Instance "+ i+ " player "+ j + " delta: "+ result[j][0]+ " epsilon: "+result[j][1], false);

			}

			//sumdelta /= iTER_LIMIT;

			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"game-result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				pw.append(gamenumber+","+tmpdelta+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}



		}



		sumdelta /= iTER_LIMIT;


		if(GameReductionBySubGame.subgamesolvingcounter != GameReductionBySubGame.hierarchicalsolvingcounter)
		{
			throw new Exception("error in timer counter");
		}

		//calculateTimes(totaltimesubgame, totalsubgametimecounter, "totalsubgamemethod");

		calculateTimes(GameReductionBySubGame.subgamesolvingtime, GameReductionBySubGame.subgamesolvingcounter,"subgame");
		//calculateTimes(GameReductionBySubGame.hierarchicalsolvingtime, GameReductionBySubGame.hierarchicalsolvingcounter,"hierarchical");
		calculateTimes(GameReductionBySubGame.psnetimer, GameReductionBySubGame.psnetimecounter,"psne");
		calculateTimes(GameReductionBySubGame.mebtimer, GameReductionBySubGame.mebtimecounter,"meb");
		calculateTimes(GameReductionBySubGame.qretimer, GameReductionBySubGame.qretimecounter,"qre");


		/*	for(int k=0; k<160; k++)
		{
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"iteration_result.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				//pw.append(gamenumber+","+minepsilon+","+result[0][1]+","+result[1][1]+","+result[2][1]+"\n");

				pw.append(k + ", "+(eps[k]/totalgames) + "\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}*/



		try
		{


			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"full-result.csv"),true));
			// gamenumber, subgame, psne, meb,qre
			pw.append(sumdelta+","+sumsubgameepsilon/totalgames+","+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames+"\n");
			pw.close();
		}
		catch(Exception ex){
			System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
		}




		System.out.println(sumdelta+","+ sumsubgameepsilon/totalgames+","+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );

		/*System.out.println(delta+","+ sumsubgameepsilon/totalgames+","+ 
				sumlpsneepsilon/totalgames+ "," + sumlmebepsilon/totalgames+","+ sumlqreepsilon/totalgames+","
				+sumpsneepsilon/totalgames+","+summebepsilon/totalgames+","+sumqreepsilon/totalgames );*/





		/*System.out.println(delta+","+ louviantime/totalgames+","+ 
				(louvainclustertime+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(louvainclustertime+(GameReductionBySubGame.qretimer/2.0))/totalgames + ","

				+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.psnetimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.mebtimer/2.0))/totalgames+","+
				(GameReductionBySubGame.kmeantimer+(GameReductionBySubGame.qretimer/2.0))/totalgames );
		 */



		/*System.out.println(delta+","+ 


				+
				((GameReductionBySubGame.psnetimer))/totalgames+","+
				((GameReductionBySubGame.mebtimer))/totalgames+","+
				((GameReductionBySubGame.qretimer))/totalgames );*/

	}


	public static void deltaExp(int nplayer, int ncluster, int naction, int iTER_LIMIT) throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = nplayer;
		int numberofcluster = ncluster;

		int numberofaction = naction;
		double delta = 0;

		int totalgames = iTER_LIMIT;//3
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		List<List<Integer>[][]> allparitions = new ArrayList<List<Integer>[][]>();
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createPartition(numberofcluster, numberofaction, dummypartition);


		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;


		setupPartitionV2(numberofaction, numberofcluster, numberofplayers, totalgames, size, delta, dummypartition, allparitions);


		double[] sumdelta = { 0, 0};


		for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
		{
			MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+numberofaction+"-"+numberofcluster+"-"+gamenumber+Parameters.GAMUT_GAME_EXTENSION));
			GameReductionBySubGame.setIsfirstiteration(true);
			gmr.setOriginalgame(tstgame);
			//printgame(tstgame, gamenumber);
			makeDeepCopyPartition(allparitions.get(0), GameReductionBySubGame.partition);
			double[] tmpdelta = computeAllDelta(tstgame, GameReductionBySubGame.partition);
			sumdelta[0] += tmpdelta[0];
			sumdelta[1] += tmpdelta[1];

			try
			{
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"game-delta.csv"),true));
				// gamenumber, subgame, psne, meb,qre
				pw.append(gamenumber+","+tmpdelta[0]+","+tmpdelta[1]+"\n");
				pw.close();
			}
			catch(Exception ex)
			{
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}
		}


		sumdelta[0] /= iTER_LIMIT;
		sumdelta[1] /= iTER_LIMIT;
		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"full-delta.csv"),true));
			// gamenumber, subgame, psne, meb,qre
			pw.append(sumdelta[0]+","+sumdelta[1]+"\n");
			pw.close();
		}
		catch(Exception ex)
		{
			System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
		}
		System.out.println("delta:  "+ sumdelta[0]+","+sumdelta[1]);


	}














	private static double[] computeAllDelta(MatrixGame tstgame, List<Integer>[][] partition) {



		//MatrixGame testgm = GameReductionBySubGame.makeTestGame(i, size, delta);
		double[] delta1 = calculateDelta(tstgame, partition, 0, true);
		double[] delta2 = calculateDelta(tstgame, partition, 1, true);
		//System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);

		return new double[]{delta1[0], delta2[0]};


	}


	private static double computeDelta(MatrixGame tstgame, List<Integer>[][] partition) {



		//MatrixGame testgm = GameReductionBySubGame.makeTestGame(i, size, delta);
		double[] delta1 = calculateDelta(tstgame, partition, 0, true);
		double[] delta2 = calculateDelta(tstgame, partition, 1, true);
		//System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);

		return Math.max(delta1[0], delta2[0]);


	}







	private static ArrayList<Double> doLouvainWithOthers(List<Integer>[][] clusterforplayers,
			MatrixGame mg, int[] solvers) {

		int[] N = {clusterforplayers[0].length, clusterforplayers[1].length};



		StrategyMapping strategymap = new StrategyMapping(mg.getNumPlayers(), mg.getNumActions(), N,mg, "1");
		strategymap.mapActions(clusterforplayers[0], 0);
		strategymap.mapActions(clusterforplayers[1], 1);
		MatrixGame abstractedgame = strategymap.makeAbstractGame();


		/*
		 * USe different kinds of solution concepts
		 * 0. PSNE
		 * 1. Counter Factual Regret
		 * 2. MinEPsilonBounded Profile
		 * 3. QRE
		 */

		//int solvers[] = {0,2,3};

		//epsilons contains the deviations for each solution concept



		ArrayList<Double> epsilons = SolverCombo.computeStabilityWithMultipleSolversForAbstraction(solvers, abstractedgame, mg, strategymap);
		return epsilons;






	}






	/**
	 * creates all the test games. 
	 * @param numberofaction number of action 
	 * @param numberofcluster number of cluster
	 * @param numberofplayers number of players
	 * @param totalgames total number of games need to be created
	 * @param size number of actions
	 * @param delta delta
	 * @param dummypartition an initial partition
	 * @throws Exception
	 */
	private static void createTestGames(int numberofaction,
			int numberofcluster, int numberofplayers, int totalgames, int size,
			double delta, List<Integer>[][] dummypartition) throws Exception 
	{
		int f = 0; 
		for(int i=0; i<totalgames; i++)
		{
			if(f==0)
			{
				List<Integer>[][] randpart = new List[numberofplayers][];
				for(int j=0; j< randpart.length; j++)
				{
					randpart[j] = new List[numberofcluster];
				}
				for(int j=0; j< 2; j++){

					for(int k =0; k< numberofcluster; k++)
					{
						randpart[j][k] = new ArrayList<Integer>(); 
					}
				}
				makeDeepCopyPartition(dummypartition, randpart);
				GameReductionBySubGame.paritionsforgames.add(randpart);
				//f=1;
			}
			MatrixGame testgm = GameReductionBySubGame.makeTestGame(i, size, delta);
			double[] delta1 = calculateDelta(testgm, dummypartition, 0, true);
			double[] delta2 = calculateDelta(testgm, dummypartition, 1, true);
			System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);
			if(delta1[0]>delta || delta2[0]>delta)
			{
				throw new Exception("Delta exceeds...");
				//System.out.print();
			}
			List<Integer>[][] randpartition = new List[numberofplayers][];
			for(int j=0; j< randpartition.length; j++)
			{
				randpartition[j] = new List[numberofcluster];
			}
			for(int j=0; j< 2; j++){

				for(int k =0; k< numberofcluster; k++)
				{
					randpartition[j][k] = new ArrayList<Integer>(); 
				}
			}
			createRandomPartition(numberofcluster, numberofaction, randpartition);
			if(f==1)
			{
				List<Integer>[][] randpart = new List[numberofplayers][];

				for(int j=0; j< randpart.length; j++)
				{
					randpart[j] = new List[numberofcluster];
				}
				for(int j=0; j< 2; j++){

					for(int k =0; k< numberofcluster; k++)
					{
						randpart[j][k] = new ArrayList<Integer>(); 
					}
				}
				makeDeepCopyPartition(MakeGameForPartition.partition, randpart);
				GameReductionBySubGame.paritionsforgames.add(randpart);
			}
			f=1;
			makeDeepCopyPartition(randpartition, GameReductionBySubGame.partition);
			makeDeepCopyPartition(randpartition, dummypartition);
			makeDeepCopyPartition(randpartition, MakeGameForPartition.partition);

		}

	}



	/**
	 * creates all the test games. 
	 * @param numberofaction number of action 
	 * @param numberofcluster number of cluster
	 * @param numberofplayers number of players
	 * @param totalgames total number of games need to be created
	 * @param size number of actions
	 * @param delta delta
	 * @param dummypartition an initial partition
	 * @param allparitions 
	 * @throws Exception
	 */
	private static void setupPartitionV2(int numberofaction,
			int numberofcluster, int numberofplayers, int totalgames, int size,
			double delta, List<Integer>[][] dummypartition, List<List<Integer>[][]> allparitions) throws Exception 
	{
		int f = 0; 
		for(int i=0; i<1; i++)
		{
			if(f==0)
			{
				List<Integer>[][] randpart = new List[numberofplayers][];
				for(int j=0; j< randpart.length; j++)
				{
					randpart[j] = new List[numberofcluster];
				}
				for(int j=0; j< 2; j++){

					for(int k =0; k< numberofcluster; k++)
					{
						randpart[j][k] = new ArrayList<Integer>(); 
					}
				}
				makeDeepCopyPartition(dummypartition, randpart);
				//GameReductionBySubGame.paritionsforgames.add(randpart);
				allparitions.add(randpart);
				//f=1;
			}
			//MatrixGame testgm = GameReductionBySubGame.makeTestGame(i, size, delta);
			//double[] delta1 = calculateDelta(testgm, dummypartition, 0, true);
			//double[] delta2 = calculateDelta(testgm, dummypartition, 1, true);
			//System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);
			/*if(delta1[0]>delta || delta2[0]>delta)
			{
				throw new Exception("Delta exceeds...");
				//System.out.print();
			}*/
			List<Integer>[][] randpartition = new List[numberofplayers][];
			for(int j=0; j< randpartition.length; j++)
			{
				randpartition[j] = new List[numberofcluster];
			}
			for(int j=0; j< 2; j++){

				for(int k =0; k< numberofcluster; k++)
				{
					randpartition[j][k] = new ArrayList<Integer>(); 
				}
			}
			createPartition(numberofcluster, numberofaction, randpartition);
			if(f==1)
			{
				List<Integer>[][] randpart = new List[numberofplayers][];

				for(int j=0; j< randpart.length; j++)
				{
					randpart[j] = new List[numberofcluster];
				}
				for(int j=0; j< 2; j++){

					for(int k =0; k< numberofcluster; k++)
					{
						randpart[j][k] = new ArrayList<Integer>(); 
					}
				}
				makeDeepCopyPartition(MakeGameForPartition.partition, randpart);
				//GameReductionBySubGame.paritionsforgames.add(randpart);
				allparitions.add(randpart);
			}
			f=1;
			makeDeepCopyPartition(randpartition, GameReductionBySubGame.partition);
			makeDeepCopyPartition(randpartition, dummypartition);
			makeDeepCopyPartition(randpartition, MakeGameForPartition.partition);

		}

	}







	/**
	 * calcualtes time for subgame, psne, meb, qre and print them.
	 * @param totaltimesubgame
	 * @param totalsubgametimecounter
	 */
	private static void calculateTimes(long totaltime,
			int timecounter, String solvertype)
	{

		long secondInMillis = 1000;
		long minuteInMillis = secondInMillis * 60;
		long hourInMillis = minuteInMillis * 60;
		long dayInMillis = hourInMillis * 24;
		/*
		 * subgames
		 */

		long time = totaltime/timecounter;
		/*long elapsedDays = time / dayInMillis;
		time = time % dayInMillis;
		long elapsedHours = time / hourInMillis;
		time = time % hourInMillis;
		long elapsedMinutes = time / minuteInMillis;
		time = time % minuteInMillis;
		long elapsedSeconds = time / secondInMillis;*/
		try
		{


			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"time.csv"),true));
			// gamenumber, subgame, psne, meb,qre
			//pw.append(solvertype+","+elapsedMinutes+"."+elapsedSeconds+"\n");
			pw.append(solvertype+","+time+"\n");
			pw.close();
		}
		catch(Exception ex){
			System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
		}



	}









	private static void resetTimerParameters() {
		GameReductionBySubGame.subgamesolvingtime = 0;
		GameReductionBySubGame.subgamesolvingcounter = 0;
		GameReductionBySubGame.hierarchicalsolvingtime = 0;
		GameReductionBySubGame.hierarchicalsolvingcounter = 0;
		GameReductionBySubGame.qretimer = 0;
		GameReductionBySubGame.qretimecounter = 0;
		GameReductionBySubGame.psnetimer = 0;
		GameReductionBySubGame.psnetimecounter = 0;
		GameReductionBySubGame.mebtimer = 0;
		GameReductionBySubGame.mebtimecounter = 0;

	}






	/**
	 * this method performs test on subgame solution technique and QRE
	 * @throws Exception
	 */
	public static void testSubGameSolverV2() throws Exception
	{
		/*
		 * first test games are built
		 */
		int numberofplayers = 2;
		int numberofcluster = 5;
		int numberofaction = 20;
		double delta = 50;
		int totalgames = 100;
		List<Integer>[][] dummypartition = new List[numberofplayers][];
		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}
		for(int i=0; i< 2; i++){

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}
		createRandomPartition(numberofcluster, numberofaction, dummypartition);
		/*
		 * create a predefined partition
		 */
		/*	for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(5);
				dummypartition[i][1].add(6);
				dummypartition[i][1].add(2);
				dummypartition[i][2].add(3);
				dummypartition[i][2].add(4);



			}
			else
			{
				dummypartition[i][0].add(3);
				dummypartition[i][0].add(4);
				dummypartition[i][1].add(2);
				dummypartition[i][1].add(1);
				dummypartition[i][2].add(6);
				dummypartition[i][2].add(5);
			}

		}
		 */
		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);
		buildtestgame = true;
		int size = numberofaction;
		if(GameReductionBySubGame.buildtestgame== true)
		{
			int f = 0; 
			for(int i=0; i<totalgames; i++)
			{
				if(f==0)
				{
					List<Integer>[][] randpart = new List[numberofplayers][];
					for(int j=0; j< randpart.length; j++)
					{
						randpart[j] = new List[numberofcluster];
					}
					for(int j=0; j< 2; j++){

						for(int k =0; k< numberofcluster; k++)
						{
							randpart[j][k] = new ArrayList<Integer>(); 
						}
					}
					makeDeepCopyPartition(dummypartition, randpart);
					GameReductionBySubGame.paritionsforgames.add(randpart);
					//f=1;
				}
				MatrixGame testgm = GameReductionBySubGame.makeTestGame(i, size, delta);
				double[] delta1 = calculateDelta(testgm, dummypartition, 0, true);
				double[] delta2 = calculateDelta(testgm, dummypartition, 1, true);
				System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);
				if(delta1[0]>delta || delta2[0]>delta)
				{
					throw new Exception("Delta exceeds...");
					//System.out.print();
				}
				List<Integer>[][] randpartition = new List[numberofplayers][];
				for(int j=0; j< randpartition.length; j++)
				{
					randpartition[j] = new List[numberofcluster];
				}
				for(int j=0; j< 2; j++){

					for(int k =0; k< numberofcluster; k++)
					{
						randpartition[j][k] = new ArrayList<Integer>(); 
					}
				}
				createRandomPartition(numberofcluster, numberofaction, randpartition);
				if(f==1)
				{
					List<Integer>[][] randpart = new List[numberofplayers][];

					for(int j=0; j< randpart.length; j++)
					{
						randpart[j] = new List[numberofcluster];
					}
					for(int j=0; j< 2; j++){

						for(int k =0; k< numberofcluster; k++)
						{
							randpart[j][k] = new ArrayList<Integer>(); 
						}
					}
					makeDeepCopyPartition(MakeGameForPartition.partition, randpart);
					GameReductionBySubGame.paritionsforgames.add(randpart);
				}
				f=1;
				makeDeepCopyPartition(randpartition, GameReductionBySubGame.partition);
				makeDeepCopyPartition(randpartition, dummypartition);
				makeDeepCopyPartition(randpartition, MakeGameForPartition.partition);

			}

		} // end of if
		/*
		 * test games are built. partitions are stored.
		 * now do test
		 */
		int ITERATION = 100;
		double summmebepsilon = 0;
		double sumqreepsilon = 0;
		for(int gamenumber = 0; gamenumber < totalgames; gamenumber++)
		{
			MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION));
			GameReductionBySubGame.setIsfirstiteration(true);
			gmr.setOriginalgame(tstgame);
			//printgame(tstgame, gamenumber);
			makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), GameReductionBySubGame.partition);
			ArrayList<MixedStrategy[]> finalstrategies = new ArrayList<MixedStrategy[]>();
			GameReductionBySubGame.finalstrategy[0].setUniform();
			GameReductionBySubGame.finalstrategy[1].setUniform();
			/*
			 * create a hashmap to save all the strategies and all the epsilon
			 */
			HashMap<Integer,MixedStrategy[]> strategycontainer = new HashMap<Integer,MixedStrategy[]>();
			HashMap<Integer,Double> epsiloncontainer = new HashMap<Integer,Double>();
			for(int iteration = 0;; iteration++)
			{
				/*
				 * copy new strategies to old ones. 
				 * 
				 */
				copyNewStrategies(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				double epsilon = gmr.startProcessingV2(gamenumber, iteration);
				//test
				epsiloncontainer.put(iteration, epsilon);
				if(epsilon==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					break;

				}
				System.out.println("Epsilon: "+ epsilon);
				/*
				 * check if the new strategy changed
				 */
				boolean changed = checkIfNewStrategyChanged(GameReductionBySubGame.finalstrategy, GameReductionBySubGame.oldfinalstrategy);
				if(iteration>0 && (changed != true))
				{

					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
					break;
				}
				/*
				 * check if any repetition occured
				 */


				if(finalstrategies.size()==0)
				{
					MixedStrategy[] tmpstr = new MixedStrategy[2];
					tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
					tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
					finalstrategies.add(tmpstr);
					strategycontainer.put(iteration, tmpstr);
					epsiloncontainer.put(iteration, epsilon);
				}
				else
				{
					/*
					 * check for repetition.
					 */
					boolean repeat = checkForRepetition(finalstrategies, finalstrategy);
					if(repeat==false)
					{
						MixedStrategy[] tmpstr = new MixedStrategy[2];
						tmpstr[0] = new MixedStrategy(finalstrategy[0].getProbs());
						tmpstr[1] = new MixedStrategy(finalstrategy[1].getProbs());
						finalstrategies.add(tmpstr);
						strategycontainer.put(iteration, tmpstr);
						epsiloncontainer.put(iteration, epsilon);
					}
					else
					{
						break;
					}
				}



			}
			/*
			 * find the strategy with minimum epsilon
			 */
			Double minepsilon = getMinEpsilon(epsiloncontainer);
			double qreeps = gmr.solveUsingQRE();
			summmebepsilon += minepsilon;
			sumqreepsilon += qreeps;
			try
			{


				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"result.csv"),true));

				pw.append(gamenumber+","+minepsilon+","+qreeps+"\n");
				pw.close();
			}
			catch(Exception ex){
				System.out.println("Gamereductionclass class :something went terribly wrong during file writing ");
			}

			System.out.println("QRE Epsilon: "+ qreeps);


		}

		System.out.println(delta+","+ summmebepsilon/totalgames+","+ sumqreepsilon/totalgames );


	}

	/**
	 * finds minimum epsilon from all iterations
	 * @param epsiloncontainer containing all epsilons for all iterations
	 * @return minimum epsilon
	 */
	private static Double getMinEpsilon(
			HashMap<Integer, Double> epsiloncontainer) {
		Double min = Double.MAX_VALUE;
		for(Integer x: epsiloncontainer.keySet())
		{
			Double tmp = epsiloncontainer.get(x);
			if(tmp<min)
			{
				min = tmp;
			}
		}
		return min;
	}






	private static boolean checkForRepetition(
			ArrayList<MixedStrategy[]> finalstrategies,
			MixedStrategy[] finalstrategy2) {

		for(MixedStrategy[] x: finalstrategies)
		{
			boolean changed = checkIfNewStrategyChanged(x, finalstrategy2);
			if(changed==false)
			{
				return true;
			}
		}

		return false;
	}






	/**
	 * 
	 * @param finalstrategy2 new strategy
	 * @param oldfinalstrategy2 old strategy
	 * @return true if new strategy changed
	 */
	private static boolean checkIfNewStrategyChanged(
			MixedStrategy[] finalstrategy2, MixedStrategy[] oldfinalstrategy2) {

		for(int i=0; i<2; i++)
		{
			for(int j=0; j<finalstrategy2[i].getNumActions(); j++)
			{
				double prob1 = finalstrategy2[i].getProb(j+1);
				double prob2 = oldfinalstrategy2[i].getProb(j+1);
				if(prob1 != prob2)
					return true;
			}
		}
		return false;

	}






	/**
	 * 
	 * @param finalstrategy2 source  new final strategy
	 * @param oldfinalstrategy2 destination 
	 */
	private static void copyNewStrategies(MixedStrategy[] finalstrategy2,
			MixedStrategy[] oldfinalstrategy2) {

		for(int i=0; i<2; i++)
		{
			oldfinalstrategy2[i] = new MixedStrategy(finalstrategy2[i].getNumActions());
			for(int j=0; j<finalstrategy2[i].getNumActions(); j++)
			{
				double prob = finalstrategy2[i].getProb(j+1);
				oldfinalstrategy2[i].setProb(j+1, prob);
			}
		}

	}






	public static void printgame(MatrixGame tstgame, int gamenumber) throws FileNotFoundException 
	{

		int[] N = tstgame.getNumActions();
		PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+gamenumber+".csv"),true));

		for(int i=0; i<N [0];i++)
		{
			for(int j=0;j<N[1]; j++)
			{

				int[] outcome = {i+1, j+1};
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(2);

				String p1 = df.format(tstgame.getPayoff(outcome, 0));
				String p2 = df.format(tstgame.getPayoff(outcome, 1));

				pw.append(p1 + " | "+p2+ "," );
				System.out.print("("+p1 + ", "+p2+")  ");
			}
			pw.append("\n");
			System.out.println();
		}
		pw.close();
	}






	public static void startGameSolving( ) throws Exception
	{


		int numberofplayers = 2;
		int numberofcluster = 3;
		int numberofaction = 6;
		double delta = 5;
		int totalgames = 1;


		/*
		 * create a dummy partition and load the game.
		 * 
		 */






		List<Integer>[][] dummypartition = new List[numberofplayers][];

		for(int i=0; i< dummypartition.length; i++)
		{
			dummypartition[i] = new List[numberofcluster];
		}


		for(int i=0; i< 2; i++){

			for(int j =0; j< numberofcluster; j++)
			{
				dummypartition[i][j] = new ArrayList<Integer>(); 
			}
		}

		createRandomPartition(numberofcluster, numberofaction, dummypartition);



		/*for(int i=0; i<2; i++)
		{

			if(i==0)
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				dummypartition[i][1].add(3);
				dummypartition[i][1].add(4);
				dummypartition[i][2].add(5);
				dummypartition[i][2].add(6);



			}
			else
			{
				dummypartition[i][0].add(1);
				dummypartition[i][0].add(2);
				dummypartition[i][1].add(5);
				dummypartition[i][1].add(6);
				dummypartition[i][2].add(3);
				dummypartition[i][2].add(4);
			}

		}*/
		/*
		 * load the game
		 */

		//MatrixGame testgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"dummy"+Parameters.GAMUT_GAME_EXTENSION)); 
		GameReductionBySubGame gmr = new GameReductionBySubGame(dummypartition, numberofcluster, null, numberofaction);

		buildtestgame = true;


		int size = numberofaction;






		if(GameReductionBySubGame.buildtestgame== true)
		{
			int f = 0; 

			for(int i=0; i<totalgames; i++)
			{
				if(f==0)
				{
					List<Integer>[][] randpart = new List[numberofplayers][];

					for(int j=0; j< randpart.length; j++)
					{
						randpart[j] = new List[numberofcluster];
					}


					for(int j=0; j< 2; j++){

						for(int k =0; k< numberofcluster; k++)
						{
							randpart[j][k] = new ArrayList<Integer>(); 
						}
					}
					makeDeepCopyPartition(dummypartition, randpart);


					GameReductionBySubGame.paritionsforgames.add(randpart);
					//f=1;
				}


				MatrixGame testgm = GameReductionBySubGame.makeTestGame(i, size, delta);
				double[] delta1 = calculateDelta(testgm, dummypartition, 0, true);
				double[] delta2 = calculateDelta(testgm, dummypartition, 1, true);
				System.out.println("Deltas: "+ delta1[0]+" "+delta2[0]);

				if(delta1[0]>delta || delta2[0]>delta)
				{
					//	throw new Exception("Delta exceeds...");
					//System.out.print();
				}


				List<Integer>[][] randpartition = new List[numberofplayers][];

				for(int j=0; j< randpartition.length; j++)
				{
					randpartition[j] = new List[numberofcluster];
				}


				for(int j=0; j< 2; j++){

					for(int k =0; k< numberofcluster; k++)
					{
						randpartition[j][k] = new ArrayList<Integer>(); 
					}
				}

				createRandomPartition(numberofcluster, numberofaction, randpartition);
				if(f==1)
				{
					List<Integer>[][] randpart = new List[numberofplayers][];

					for(int j=0; j< randpart.length; j++)
					{
						randpart[j] = new List[numberofcluster];
					}


					for(int j=0; j< 2; j++){

						for(int k =0; k< numberofcluster; k++)
						{
							randpart[j][k] = new ArrayList<Integer>(); 
						}
					}
					makeDeepCopyPartition(MakeGameForPartition.partition, randpart);

					GameReductionBySubGame.paritionsforgames.add(randpart);
				}
				f=1;
				makeDeepCopyPartition(randpartition, GameReductionBySubGame.partition);
				makeDeepCopyPartition(randpartition, dummypartition);
				makeDeepCopyPartition(randpartition, MakeGameForPartition.partition);

			}

		}
		else
		{

			List<Integer>[][] startingpartition = new List[numberofplayers][];

			for(int i=0; i< startingpartition.length; i++)
			{
				startingpartition[i] = new List[numberofcluster];
			}


			for(int i=0; i< 2; i++){

				for(int j =0; j< numberofcluster; j++)
				{
					startingpartition[i][j] = new ArrayList<Integer>(); 
				}
			}


			// create the random starting partition to use for every game
			createRandomPartition(numberofcluster, size, startingpartition);


			double kmax = 500000;

			//int totalgames = 2;


			double[] settings = {0.999997};

			boolean flag = false;

			for(double cr: settings)
			{

				GameReductionBySubGame.coolingRate = cr;
				Double[] alldeltas = new Double[(int)kmax];
				Double allepsilons = 0.0;
				Double qreespsilons = 0.0;

				for(int i=0; i<alldeltas.length; i++)
				{
					alldeltas[i] = 0.0;
				}


				for(int gamenumber =0; gamenumber<totalgames; gamenumber++)
				{

					MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+gamenumber+"-"+size+"-"+delta+Parameters.GAMUT_GAME_EXTENSION)); 

					gmr.setOriginalgame(tstgame);

					List<Integer>[][] neighborpartition = new List[2][];
					for(int i=0; i< neighborpartition.length; i++)
					{
						neighborpartition[i] = new List[GameReductionBySubGame.numberofsubgames];
					}
					for(int i=0; i< 2; i++)
					{

						for(int j =0; j< GameReductionBySubGame.numberofsubgames; j++)
						{
							neighborpartition[i][j] = new ArrayList<Integer>(); 
						}
					}
					List<Integer>[][] currentpartition = new List[2][];
					for(int i=0; i< currentpartition.length; i++)
					{
						currentpartition[i] = new List[GameReductionBySubGame.numberofsubgames];
					}
					for(int i=0; i< 2; i++){

						for(int j =0; j< GameReductionBySubGame.numberofsubgames; j++)
						{
							currentpartition[i][j] = new ArrayList<Integer>(); 
						}
					}
					for(int i=0; i< currentpartition.length; i++)
					{
						GameReductionBySubGame.bestpartitionyet[i] = new List[GameReductionBySubGame.numberofsubgames];
					}
					for(int i=0; i< 2; i++){

						for(int j =0; j< GameReductionBySubGame.numberofsubgames; j++)
						{
							GameReductionBySubGame.bestpartitionyet[i][j] = new ArrayList<Integer>(); 
						}
					}
					//makeDeepCopyPartition(startingpartition, currentpartition);
					//copy the partition from list, with which the game is made
					makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), currentpartition);
					makeDeepCopyPartition(currentpartition, GameReductionBySubGame.partition);
					double k = 0.0;
					double dlta = 1000.0;
					GameReductionBySubGame.temperature = 1000000;
					double[] delta1 = calculateDelta(tstgame, currentpartition, 0, true);
					double[] delta2 = calculateDelta(tstgame, currentpartition, 1, true);
					int clustertochange  = -1;
					if(delta1[0]>delta2[0])
					{
						dlta = delta1[0];
						clustertochange = (int)delta1[1];
						GameReductionBySubGame.partitionplayer = 0;
					}
					else
					{
						dlta = delta2[0];
						clustertochange = (int)delta2[1];
						GameReductionBySubGame.partitionplayer = 1;
					}

					GameReductionBySubGame.hillclimbingdelta = dlta;
					int ratecounter = 0;
					double pastdelta = GameReductionBySubGame.hillclimbingdelta;
					int deltaindex = 0;
					flag = false;

					while((k<kmax) && (dlta > delta))
					{



						/*if(ratecounter>=4000)
						{
							double decrease = (pastdelta- GameReductionBySubGame.hillclimbingdelta)/100000;
							if(decrease<0.000000005)
							{
								flag = true;

								//System.out.println(decrease+" Exiting loop...rate of decreasing delta is too slow");
							//	break;

							}
							else
							{
								ratecounter = 0;
								pastdelta = GameReductionBySubGame.hillclimbingdelta;
							}

						}*/
						alldeltas[(int)k] = alldeltas[(int)k] + GameReductionBySubGame.hillclimbingdelta;
						System.out.println("k: "+k+ ", kmax: "+ kmax);
						List<Integer>[][]  tmpneighborpartition = new List[2][];
						double T = GameReductionBySubGame.temperature(k/kmax);
						System.out.println("\n Temperature "+ T);
						tmpneighborpartition =  gmr.createNeighborPartition(currentpartition, clustertochange);
						makeDeepCopyPartition(tmpneighborpartition, neighborpartition);
						double[] newdelta1 = calculateDelta(tstgame, neighborpartition, 0, true);
						double[] newdelta2 = calculateDelta(tstgame, neighborpartition, 1, true);
						double newdelta = 0;

						if(newdelta1[0]>newdelta2[0])
						{
							newdelta = newdelta1[0];
							clustertochange = (int)newdelta1[1];
							GameReductionBySubGame.partitionplayer = 0;
						}
						else
						{
							newdelta = newdelta2[0];
							clustertochange = (int)newdelta2[1];
							GameReductionBySubGame.partitionplayer = 1;
						}
						double p = GameReductionBySubGame.P(dlta, newdelta, T);

						System.out.println("P : "+ p);
						try 
						{
							PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Parameters.GAME_FILES_PATH+"p.txt", true)));
							out.print(k+", "+p + ", "+ GameReductionBySubGame.temperature+ " "+GameReductionBySubGame.hillclimbingdelta+"\n");
							out.close();
						} 
						catch (IOException ex) 
						{
							//oh noes!
						}

						if(p==0)
						{
							System.out.print("x");
						}

						if(p > MakeGameForPartition.randomInRange(0.0, 1.0))
						{
							dlta = newdelta;
							makeDeepCopyPartition(neighborpartition, currentpartition);
						}

						if(newdelta < GameReductionBySubGame.hillclimbingdelta)
						{
							makeDeepCopyPartition(neighborpartition, bestpartitionyet);
							GameReductionBySubGame.hillclimbingdelta = newdelta;
						}
						System.out.println("Best delta "+ GameReductionBySubGame.hillclimbingdelta);
						k += 1;
						ratecounter++;


					}



					makeDeepCopyPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), GameReductionBySubGame.partition);
					printPartition(GameReductionBySubGame.paritionsforgames.get(gamenumber), gamenumber);
					double epsilon = gmr.startProcessing(gamenumber,0);
					System.out.println("Subgame Epsilom for game "+gamenumber+" : "+ epsilon);
					allepsilons += epsilon;
					double qreeps = gmr.solveUsingQRE();
					qreespsilons+= qreeps;
					if(qreeps==0 && epsilon!=0)
					{
						System.out.print("x");
					}


				}

				/*if(flag==false)
				{
					for(int m =0; m<alldeltas.length; m++)
					{
						alldeltas[m] = alldeltas[m]/totalgames;
						try 
						{
							PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Parameters.GAME_FILES_PATH+"settings"+cr+".txt", true)));
							out.print(alldeltas[m] + "  ");
							out.close();
						} 
						catch (IOException ex) 
						{
							//oh noes!
						}

					}
				}*/


				allepsilons = allepsilons/totalgames;
				qreespsilons = qreespsilons/totalgames;
				System.out.println("Subgame Epsilon "+allepsilons);
				System.out.println("QRE Epsilon "+qreespsilons);


			}


		}




	}

	public static void printPartition(List<Integer>[][] lists, int gamenumber) {

		System.out.println("Printing game "+ gamenumber + " partition");
		for(int i=0; i<2; i++)
		{
			for(List<Integer> x: lists[i])
			{
				System.out.print("[");
				for(Integer y: x)
				{
					System.out.print(y + " ");
				}
				System.out.print("]");
			}
			System.out.println();
		}

	}






	private static void createRandomPartition(int numberofcluster,
			int numberofaction, List<Integer>[][] partition) {

		int actionpercluster = numberofaction/ numberofcluster;

		ArrayList<Integer> alreadydone = new ArrayList<Integer>();


		for(int i =0; i< 2; i++)
		{
			alreadydone.clear();
			for(int j=0; j<numberofcluster; j++)
			{
				for(int k =0; k<actionpercluster; k++)
				{
					while(true)
					{
						int x = MakeGameForPartition.randInt(1, numberofaction);
						if(alreadydone.size()>=0 && (!alreadydone.contains(x)))
						{
							partition[i][j].add(x);
							alreadydone.add(x);
							break;
						}
					}
				}
			}
		}


	}


	private static void createPartition(int numberofcluster,
			int numberofaction, List<Integer>[][] partition) {

		int actionpercluster = numberofaction/ numberofcluster;

		ArrayList<Integer> alreadydone = new ArrayList<Integer>();


		for(int i =0; i< 2; i++)
		{
			//alreadydone.clear();
			int x = 1;
			for(int j=0; j<numberofcluster; j++)
			{
				int count = 0;
				while(count<actionpercluster)
				{
					partition[i][j].add(x);
					x++;
					count++;
				}


			}
		}


	}





	public static double[] calculateDelta(Game game, List<Integer>[][] cluster, int player, boolean max)
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
			int worstcluster =-1;
			double maximum = Double.NEGATIVE_INFINITY;

			for(int i=0; i< deltas.length; i++)
			{
				if(deltas[i]>maximum)
				{
					worstcluster = i;
					maximum = deltas[i];
				}
			}
			double[] deltawithcluster = {maximum, worstcluster};
			return deltawithcluster;

		}
		else
		{

			int worstcluster =-1;
			double maximum = Double.NEGATIVE_INFINITY;

			for(int i=0; i< deltas.length; i++)
			{
				if(deltas[i]>maximum)
				{
					worstcluster = i;
					maximum = deltas[i];
				}
			}


			double sum = 0.0;

			for(int i=0; i< deltas.length; i++)
			{
				sum+=deltas[i];
			}

			sum= sum/deltas.length;

			double[] deltawithcluster = {sum, worstcluster};
			return deltawithcluster;

			//return deltawithcluster;


		}





		//return deltas;
	}






	private static double temperature(double x)
	{

		if(x==0)
			return GameReductionBySubGame.temperature;
		else
		{

			GameReductionBySubGame.temperature *= GameReductionBySubGame.coolingRate; 
			return GameReductionBySubGame.temperature;
		}

	}


	private static double P(double e, double eprime, double T)
	{
		if(eprime<e)
		{
			return 1.0;
		}

		else
		{
			double x = Math.exp(-(eprime-e)/T);
			return x;
		}
	}





	private static void makeDeepCopyPartition(List<Integer>[][] sourcepartition,
			List<Integer>[][] destpartition) {






		for(int i=0; i< 2; i++)
		{
			destpartition[i] = new List[sourcepartition[i].length];
		}
		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< sourcepartition[i].length; j++)
			{
				destpartition[i][j] = new ArrayList<Integer>(); 
			}
		}


		for(int i=0; i<2; i++)
		{
			for(int j=0; j<sourcepartition[i].length; j++)
			{
				if(destpartition[i][j].size()>0)
					destpartition[i][j].clear();
			}
		}



		for(int i=0; i< 2; i++)
		{

			for(int j =0; j< sourcepartition[i].length; j++)
			{
				for(int k =0; k<sourcepartition[i][j].size(); k++)
				{
					int x = sourcepartition[i][j].get(k);
					destpartition[i][j].add(x); 
				}
			}
		}



	}






	private List<Integer>[][] createNeighborPartition(
			List<Integer>[][] currentpartition, int clustertochnage) 
	{


		//	while(true)
		//{



		//	for(int i=0; i<GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		//	{
		//pick two random cluster to swap the first actions.

		List<Integer>[][] neighborpartition = new List[2][];


		for(int i=0; i< neighborpartition.length; i++)
		{
			neighborpartition[i] = new List[GameReductionBySubGame.numberofsubgames];
		}


		for(int i=0; i< 2; i++){

			for(int j =0; j< GameReductionBySubGame.numberofsubgames; j++)
			{
				neighborpartition[i][j] = new ArrayList<Integer>(); 
			}
		}


		makeDeepCopyPartition(currentpartition, neighborpartition);



		int i = GameReductionBySubGame.partitionplayer ;

		int cluster1 = clustertochnage;
		int cluster2 = -1;
		while(true)
		{
			//cluster1 = MakeGameForPartition.randInt(0, GameReductionBySubGame.numberofsubgames-1);
			cluster2 = MakeGameForPartition.randInt(0, GameReductionBySubGame.numberofsubgames-1);
			if(cluster1 != cluster2)
			{
				break;
			}

		}

		//choose two indexes randomly


		//instead of finding the actions in  cluster1 and cluster2 randomly, choose the action
		//which is the farthest from the average of min and max 


		int huristicaction1 = findFurthestAction(neighborpartition,cluster1, i);
		int huristicaction2 = findFurthestAction(neighborpartition,cluster2, i);




		//	int indx1 = MakeGameForPartition.randInt(0, GameReductionBySubGame.partition[i][cluster1].size()-1);
		//	int indx2 = MakeGameForPartition.randInt(0, GameReductionBySubGame.partition[i][cluster2].size()-1);


		neighborpartition[i][cluster1].remove(neighborpartition[i][cluster1].indexOf(huristicaction1));
		neighborpartition[i][cluster2].remove(neighborpartition[i][cluster2].indexOf(huristicaction2));

		/*int tmp2 = neighborpartition[i][cluster2].get(indx2);
		neighborpartition[i][cluster1].remove(indx1);
		neighborpartition[i][cluster2].remove(indx2);*/
		neighborpartition[i][cluster1].add(huristicaction2);
		neighborpartition[i][cluster2].add(huristicaction1);

		return neighborpartition;


		//	}


		/*	boolean f = false;

			if(neighbors.size()>0)
			{	

				for(List<Integer>[][] z: neighbors)
				{

					f = false;

					if(this.CheckIfMatch(currentpartition, z, 0) && this.CheckIfMatch(currentpartition, z, 1))
					{
						f = true;
						break;
					}
				}

				if(f==false){
					return currentpartition;
				}


			}
			else
			{
				return currentpartition;
			}
		 */

		//	}



	}






	private int findFurthestAction(List<Integer>[][] partition, int cluster, int player) {

		int opponent =0;
		if(player==0)
		{
			opponent =1;
		}
		int numactionsplayer =  GameReductionBySubGame.originalgame.getNumActions(player);
		int numactionsopponent =  GameReductionBySubGame.originalgame.getNumActions(opponent);
		//select the cluster to find an action which is far away from center. 
		//int cluster = MakeGameForPartition.randInt(0, GameReductionBySubGame.numberofsubgames-1);
		ArrayList<Double> meanvector = new ArrayList<Double>();
		for(int j=0; j<numactionsopponent; j++)
		{
			if(!partition[opponent][cluster].contains(j+1))
			{

				double minpayoff = (int)Double.POSITIVE_INFINITY;
				double maxpayoff = (int)Double.NEGATIVE_INFINITY;
				for(int x: partition[player][cluster])
				{
					int[] outcome = new int[2];
					if(player==0)
					{
						outcome[0] = x;
						outcome[1] = j+1;
					}
					if(player==1)
					{
						outcome[0] = j+1;
						outcome[1] = x;
					}
					double payoff = GameReductionBySubGame.originalgame.getPayoff(outcome, player);
					if(payoff>maxpayoff)
					{
						maxpayoff = payoff;
					}
					if(payoff<minpayoff){
						minpayoff = payoff;
					}
				}
				double y = (maxpayoff+minpayoff)/2.0;
				meanvector.add(y);
			}
		}

		int furthestactionsofar = -1;
		double furthestval = Double.NEGATIVE_INFINITY;
		for(int x: partition[player][cluster])
		{
			int index = 0;
			for(int j=0; j<numactionsopponent; j++)
			{

				if(!partition[opponent][cluster].contains(j+1))
				{
					int[] outcome = new int[2];
					outcome[player] = x;
					outcome[opponent] = j+1;
					double payoff = GameReductionBySubGame.originalgame.getPayoff(outcome, player);
					double diff = Math.abs(meanvector.get(index++)-payoff);
					if(diff>furthestval)
					{
						furthestval = diff;
						furthestactionsofar = x;
					}
				}
			}

		}



		return furthestactionsofar;
	}






	private boolean CheckIfMatch(List<Integer>[][] currentpartition,
			List<Integer>[][] lastneighborpartition, int player) {


		boolean flag = false;

		//	for(int i=0; i< 2; i++)
		//{
		//flag = false;
		for(List <Integer> x: currentpartition[player])
		{
			flag = false;
			for(List<Integer> y: lastneighborpartition[player])
			{
				if(checkSImilaritOfTwoCluster(x, y))
				{
					flag = true;
					break;
				}
			}
			if(flag==false)
			{
				return false;
			}
		}
		//}

		return true;
	}






	private boolean checkSImilaritOfTwoCluster(List<Integer> cluster1, List<Integer> cluster2) {

		boolean flag = false;
		for(Integer x: cluster1)
		{
			flag = false;

			for(Integer y: cluster2)
			{
				if(x==y)
				{
					flag = true;
					break;
				}
			}
			if(flag==false)
			{
				return false;
			}
		}

		return true;
	}






	public  double startProcessingV2(int gamenumber, int iteration) throws Exception
	{



		//int subiteration=0;
		double epsilonz= -1;
		GameReductionBySubGame.buildSubGames(gamenumber, iteration);
		GameReductionBySubGame.solveSubGames();
		MatrixGame reducedgame = GameReductionBySubGame.collapseOriginalGame();


		String gamename = Parameters.GAME_FILES_PATH+"reducedgame-"+gamenumber+Parameters.GAMUT_GAME_EXTENSION;
		//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		try
		{

			PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			//SimpleOutput.writeGame(pw,reducedgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}


		QRESolver qresubgame = new QRESolver(100);
		EmpiricalMatrixGame emsubgame = new EmpiricalMatrixGame(reducedgame);
		qresubgame.setDecisionMode(QRESolver.DecisionMode.RAW);
		MixedStrategy[] reducedgamestrategy = new MixedStrategy[2];
		for(int i =0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		{
			reducedgamestrategy[i] = qresubgame.solveGame(emsubgame, i);
			System.out.println("\nplayer  "+ i + " reduced game strategy");
			for(double y: reducedgamestrategy[i].getProbs())
			{
				System.out.print(y+ " ");
			}
			System.out.println("\n");
		}
		MixedStrategy[] subgamestrategy = GameReductionBySubGame.buidMixedStrategyForOriginalGame(); 
		for(int j =0; j< reducedgame.getNumPlayers(); j++)
		{

			for(int k=0; k< reducedgamestrategy[j].getNumActions(); k++)
			{
				List<Integer> actions = GameReductionBySubGame.getOriginalActions(k+1, j);
				double prob = reducedgamestrategy[j].getProb(k+1);
				for(Integer x: actions)
				{
					double subgmprob = subgamestrategy[j].getProb(x);
					GameReductionBySubGame.finalstrategy[j].setProb(x, subgmprob*prob);

				}

			}

		}

		for(double x: GameReductionBySubGame.finalstrategy[0].getProbs())
		{
			System.out.print(x + " ");
		}

		System.out.println();

		for(double x: GameReductionBySubGame.finalstrategy[1].getProbs())
		{
			System.out.print(x + " ");
		}

		/*
		 * test: set uniform when iteration is the very first one. 
		 */
		if(iteration==0)
		{
			//GameReductionBySubGame.finalstrategy[0].setRandom();
			//GameReductionBySubGame.finalstrategy[1].setRandom();
		}



		ArrayList<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(GameReductionBySubGame.finalstrategy[0]);
		list.add(GameReductionBySubGame.finalstrategy[1]);
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(GameReductionBySubGame.originalgame, distro);
		System.out.println("\n Expected payoff "+ expectedpayoff[0]+" " +expectedpayoff[1]) ;
		epsilonz = SolverUtils.computeOutcomeStability(GameReductionBySubGame.originalgame, distro);
		//System.out.println("\n Epsilon "+ epsilonz) ;

		//System.out.println(GameReductionBySubGame.finalstrategy[0].getProbs());
		//System.out.println(GameReductionBySubGame.finalstrategy[1].getProbs());





		//	}

		/**
		 * print higher level games epsilon with support for both players.
		 */
		int[] support = new int[2];
		for(int i=0; i<2; i++)
		{
			support[i] = getSupport(reducedgamestrategy[i]);
		}
		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"support.csv"),true));
			pw.append(iteration+","+epsilonz+","+support[0]+","+support[1]+"\n");
			pw.close();

		}
		catch(Exception e)
		{

		}
		return epsilonz;

	}	



	/*
	 * finds the support of a mixed sttrategy
	 */
	private int getSupport(MixedStrategy mixedStrategy) {

		int support=0;
		for(int i=0; i<mixedStrategy.getNumActions(); i++)
		{
			if(mixedStrategy.getProb(i+1)>0)
			{
				support++;
			}
		}
		return support;
	}






	public  double startProcessing(int gamenumber, int iteration) throws Exception
	{
		GameReductionBySubGame.buildSubGames(gamenumber,iteration);
		GameReductionBySubGame.solveSubGames();
		MatrixGame reducedgame = GameReductionBySubGame.collapseOriginalGame();


		String gamename = Parameters.GAME_FILES_PATH+"reducedgame-"+Parameters.GAMUT_GAME_EXTENSION;
		//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		try{

			PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			SimpleOutput.writeGame(pw,reducedgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}


		QRESolver qresubgame = new QRESolver(100);
		EmpiricalMatrixGame emsubgame = new EmpiricalMatrixGame(reducedgame);
		qresubgame.setDecisionMode(QRESolver.DecisionMode.RAW);
		MixedStrategy[] reducedgamestrategy = new MixedStrategy[2];
		for(int i =0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		{
			reducedgamestrategy[i] = qresubgame.solveGame(emsubgame, i);
			System.out.println("\nplayer  "+ i + " reduced game strategy");
			for(double y: reducedgamestrategy[i].getProbs())
			{
				System.out.print(y+ " ");
			}
			System.out.println("\n");
		}
		MixedStrategy[] subgamestrategy = GameReductionBySubGame.buidMixedStrategyForOriginalGame(); 
		for(int j =0; j< reducedgame.getNumPlayers(); j++)
		{

			for(int k=0; k< reducedgamestrategy[j].getNumActions(); k++)
			{
				List<Integer> actions = GameReductionBySubGame.getOriginalActions(k+1, j);
				double prob = reducedgamestrategy[j].getProb(k+1);
				for(Integer x: actions)
				{
					double subgmprob = subgamestrategy[j].getProb(x);
					GameReductionBySubGame.finalstrategy[j].setProb(x, subgmprob*prob);

				}

			}

		}

		for(double x: GameReductionBySubGame.finalstrategy[0].getProbs())
		{
			System.out.print(x + " ");
		}

		System.out.println();

		for(double x: GameReductionBySubGame.finalstrategy[1].getProbs())
		{
			System.out.print(x + " ");
		}



		ArrayList<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(GameReductionBySubGame.finalstrategy[0]);
		list.add(GameReductionBySubGame.finalstrategy[1]);
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(GameReductionBySubGame.originalgame, distro);
		System.out.println("\n Expected payoff "+ expectedpayoff[0]+" " +expectedpayoff[1]) ;
		double epsilonz = SolverUtils.computeOutcomeStability(GameReductionBySubGame.originalgame, distro);
		//System.out.println("\n Epsilon "+ epsilonz) ;

		//System.out.println(GameReductionBySubGame.finalstrategy[0].getProbs());
		//System.out.println(GameReductionBySubGame.finalstrategy[1].getProbs());


		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"result"+gamenumber+".csv"),true));
			pw.append("\n"+iteration+","+epsilonz);
			pw.close();

		}
		catch(Exception e)
		{

		}
		return epsilonz;

	}


	public double solveUsingQRE()
	{
		System.out.println("\n Using QRE ");
		QRESolver qresubgame = new QRESolver(100);
		EmpiricalMatrixGame emsubgame = new EmpiricalMatrixGame(GameReductionBySubGame.originalgame);
		qresubgame.setDecisionMode(QRESolver.DecisionMode.RAW);
		MixedStrategy[] gamestrategy = new MixedStrategy[2];


		for(int i =0; i< GameReductionBySubGame.originalgame.getNumPlayers(); i++)
		{
			gamestrategy[i] = qresubgame.solveGame(emsubgame, i);
		}


		for(double x: gamestrategy[0].getProbs())
		{
			System.out.print(x + " ");
		}

		System.out.println();

		for(double x: gamestrategy[1].getProbs())
		{
			System.out.print(x + " ");
		}


		ArrayList<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(gamestrategy[0]);
		list.add(gamestrategy[1]);
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double[]  expectedpayoff = SolverUtils.computeOutcomePayoffs(GameReductionBySubGame.originalgame, distro);
		System.out.println("\n Expected payoff "+ expectedpayoff[0]+" " +expectedpayoff[1]) ;
		double epsilonz = SolverUtils.computeOutcomeStability(GameReductionBySubGame.originalgame, distro);
		System.out.println("\n Epsilon "+ epsilonz) ;

		return epsilonz;




	}





	public static List<Integer> getOriginalActions(int action, int player)
	{

		List<Integer> actions = GameReductionBySubGame.partition[player][action-1];
		return actions; 

	}





	/**
	 * Returns a game with maxexpected payoffs using the partition object
	 * @param abstractgame
	 * @param partition2
	 * @return
	 */
	public static Game getGameWithMaxExpectedPayoff(MatrixGame abstractgame, List<Integer>[][] partition) 
	{

		MatrixGame gamewithmaxexpectedpayoff  = new MatrixGame(abstractgame.getNumPlayers(), abstractgame.getNumActions());
		OutcomeIterator itr = gamewithmaxexpectedpayoff.iterator();
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			for(int i=0; i<gamewithmaxexpectedpayoff.getNumPlayers(); i++)
			{
				double payoff = GameReductionBySubGame.calcMaxExpectedPayoffOriginalGame(outcome, i);
				gamewithmaxexpectedpayoff.setPayoff(outcome, i, payoff);
			}

		}
		return gamewithmaxexpectedpayoff;
	}






	private static double calcMaxExpectedPayoffOriginalGame(int[] abstractoutcome, int player) {

		double maxpayoff = Double.NEGATIVE_INFINITY;
		int opponent = 1^ player;
		List<Integer> originalactionsplayer1;
		List<Integer> originalactionsplayer2; 
		if(player==0)
		{
			originalactionsplayer1 = GameReductionBySubGame.getOriginalActionsFromSubgamePartition(abstractoutcome[player], player);
			originalactionsplayer2 = GameReductionBySubGame.getOriginalActionsFromSubgamePartition(abstractoutcome[opponent], opponent); 
			for(Integer x: originalactionsplayer1)
			{
				double expectedpayoff = 0; 
				for(Integer y: originalactionsplayer2)
				{
					int[] outcome = {x, y};
					double payoff = GameReductionBySubGame.originalgame.getPayoff(outcome, player);
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
			originalactionsplayer1 = GameReductionBySubGame.getOriginalActionsFromSubgamePartition(abstractoutcome[opponent], opponent);
			originalactionsplayer2 = GameReductionBySubGame.getOriginalActionsFromSubgamePartition(abstractoutcome[player], player);
			for(Integer x: originalactionsplayer2)
			{
				double expectedpayoff = 0; 
				for(Integer y: originalactionsplayer1)
				{
					int[] outcome = {y, x};
					double payoff = GameReductionBySubGame.originalgame.getPayoff(outcome, player);
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






	/**
	 * returns list of original actions belong to a partition
	 * @param action action in the abstracted game. 
	 * @param player player
	 * @return returns list of original actions belong to a partition/action 
	 */
	private static List<Integer> getOriginalActionsFromSubgamePartition(int action, int player) 
	{

		return GameReductionBySubGame.partition[player][action-1];

	}

}



class MakeGameForPartition{

	private MatrixGame originalgame;
	public static List<Integer>[][] partition;
	public int[] numberofactions;
	int numberofplayers;
	public static boolean issubgame = true;

	private double delta;

	/*static
	{

		partition = GameReductionBySubGame.partition;
	}*/



	public MakeGameForPartition( int[] numberofactions,
			int numberofplayers, double delta) {
		super();

		this.numberofactions = numberofactions;
		this.numberofplayers = numberofplayers;
		this.delta = delta;
	}



	public MatrixGame getOriginalgame() {
		return originalgame;
	}



	public void setOriginalgame(MatrixGame originalgame) {
		this.originalgame = originalgame;
	}



	public void buildTestGame()
	{
		List<Integer>[] player1parition = MakeGameForPartition.partition[0]; 
		List<Integer>[] player2parition = MakeGameForPartition.partition[1]; 
		MatrixGame game = new MatrixGame(this.numberofplayers, this.numberofactions);


		for(int i= 0; i< MakeGameForPartition.partition[0].length; i++) // numberofsubgames
		{

			boolean flag = true; 

			for(Integer x: player1parition[i])
			{

				for(int j = 0; j< this.numberofactions[1]; j++)
				{
					double payoff = MakeGameForPartition.randInt(0, GameReductionBySubGame.maxpayofflimit);
					if(flag==true)
					{

						boolean ok = checkIfActionIsOK(j+1, player2parition[i]); // if outside of subgame
						int[] outcome = {x, j+1};
						if(ok==true) // 
						{
							game.setPayoff(outcome, 0, payoff);

						}
						else if(ok==false) // if in subgame
						{
							double randpayoff = 0;
							/*
							 * assign random payoff if making game for subgame
							 * else assign same payoff
							 */
							if(MakeGameForPartition.issubgame)
							{
								randpayoff = MakeGameForPartition.randInt(1, GameReductionBySubGame.maxpayofflimit);
							}
							else
							{
								randpayoff = payoff;
							}
							game.setPayoff(outcome, 0, randpayoff);
						}

					}
					else if(flag==false)
					{
						boolean ok = checkIfActionIsOK(j+1, player2parition[i]); // if outside of subgame
						int[] outcome = {x, j+1};

						if(ok==true) // if outside subgame
						{
							int index = player1parition[i].indexOf(player1parition[i].get(0));
							int prevaction = player1parition[i].get(index);
							int[] prevoutcome = {prevaction, j+1};
							payoff = (int)game.getPayoff(prevoutcome, 0);
							payoff = MakeGameForPartition.randomInRange(payoff, payoff+this.delta);
							game.setPayoff(outcome, 0, payoff); // set the same payoff as previous

						}
						else if(ok==false)
						{

							int randpayoff = MakeGameForPartition.randInt(1, GameReductionBySubGame.maxpayofflimit);
							if(MakeGameForPartition.issubgame)
							{
								game.setPayoff(outcome, 0, randpayoff);
							}
							else
							{
								int index = player1parition[i].indexOf(player1parition[i].get(0));
								int prevaction = player1parition[i].get(index);
								int[] prevoutcome = {prevaction, j+1};
								payoff = (int)game.getPayoff(prevoutcome, 0);
								payoff = MakeGameForPartition.randomInRange(payoff, payoff+this.delta);
								game.setPayoff(outcome, 0, payoff); // set the same payoff as previous

							}
						}


					}


				}
				flag = false; // set it false so that next time payoffs are copied/same as previous action in the same cluster

			}

		}




		for(int i= 0; i< MakeGameForPartition.partition[1].length; i++) // numberofsubgames
		{

			boolean flag = true; 

			for(Integer x: player2parition[i])
			{

				for(int j = 0; j< this.numberofactions[0]; j++)
				{
					double payoff = MakeGameForPartition.randInt(0, GameReductionBySubGame.maxpayofflimit);
					if(flag==true)
					{

						boolean ok = checkIfActionIsOK(j+1, player1parition[i]);
						int[] outcome = {j+1, x};
						if(ok==true) // 
						{
							game.setPayoff(outcome, 1, payoff);

						}
						else if(ok==false)
						{

							int randpayoff = MakeGameForPartition.randInt(1, GameReductionBySubGame.maxpayofflimit);
							if(MakeGameForPartition.issubgame)
							{
								game.setPayoff(outcome, 1, randpayoff);
							}
							else
							{
								game.setPayoff(outcome, 1, payoff);
							}
						}

					}
					else if(flag==false)
					{
						boolean ok = checkIfActionIsOK(j+1, player1parition[i]);
						int[] outcome = {j+1, x};

						if(ok==true) // 
						{
							int index = player2parition[i].indexOf(player2parition[i].get(0));
							int prevaction = player2parition[i].get(index);
							int[] prevoutcome = {j+1, prevaction};
							payoff = (int)game.getPayoff(prevoutcome, 1);
							payoff = MakeGameForPartition.randomInRange(payoff, payoff+this.delta);
							game.setPayoff(outcome, 1, payoff); // set the same payoff as previous

						}
						else if(ok==false)
						{
							int randpayoff = MakeGameForPartition.randInt(1, GameReductionBySubGame.maxpayofflimit);
							if(MakeGameForPartition.issubgame)
							{
								game.setPayoff(outcome, 1, randpayoff);
							}
							else
							{
								int index = player2parition[i].indexOf(player2parition[i].get(0));
								int prevaction = player2parition[i].get(index);
								int[] prevoutcome = {j+1, prevaction};
								payoff = (int)game.getPayoff(prevoutcome, 1);
								game.setPayoff(outcome, 1, payoff);
							}
						}


					}


				}
				flag = false;

			}

		}


		this.setOriginalgame(game);




	}


	public static boolean checkIfActionIsOK(int action, List<Integer> parition)
	{


		for(Integer x: parition)
		{
			if(action==x)
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


	public static double randomInRange(double min, double max) {
		double range = max - min;
		Random random = new Random();
		double scaled = random .nextDouble() * range;
		double shifted = scaled + min;
		return shifted; // == (rand.nextDouble() * (max-min)) + min;
	}






}
