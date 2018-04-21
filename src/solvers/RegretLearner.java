package solvers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import Log.Logger;
/*import ega.KmeanClustering;
import ega.RegretClustering;*/
import games.MatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import parsers.GamutParser;
import regret.RegretClustering;
import subgame.KmeanClustering;
import subgame.Parameters;
import subgame.StrategyMapping;

/**
 * @author sunny
 *
 */

public class RegretLearner 
{
	public static String currentgame = "";
	public static final Random random = new Random();


	public static void doExPeriments()
	{
		//TODO set kmean parameters

		double[] sumdelta = new double[3];
		double[] sumepsilon = new double[3];
		final int ITERATION = 100;
		int numberofclusters = 16;
		int clusterlimit = 2;
		//double[][] result = new double[4][2];

	 //   int[] percentages = {1,5,10,20,50,70,90,100};
	  int[] percentages = {100};
		for(int perc: percentages)
		{

			for(int clusternumber=numberofclusters; clusternumber>=clusterlimit; clusternumber=clusternumber/2)
			{
				for(int k=0; k<sumdelta.length; k++)
				{
					sumdelta[k] = 0;
					sumepsilon[k] = 0;
				}
				for(int i=0; i<ITERATION; i++)
				{
					System.out.println("Game"+ i);
					RegretLearner.currentgame = Integer.toString(i);
					MatrixGame mg = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+i+Parameters.GAMUT_GAME_EXTENSION));

					/*
					 * test for regret based clustering
					 */
					//with regret capping
					int[] solvers = {0,2,3};
					double[][] result = evaluateRegretLearning(clusternumber, mg, Integer.toString(i), false, solvers, perc);
					
					//without regret capping
					//double[][] result = evaluateRegretLearning(clusternumber, mg, Integer.toString(i),0);
					for(int j=0; j<3; j++)
					{
						sumdelta[j] = sumdelta[j] + result[j][0];
						sumepsilon[j] = sumepsilon[j] + result[j][1];
						//Logger.log("\n Running Instance "+ i+ " player "+ j + " delta: "+ result[j][0]+ " epsilon: "+result[j][1], false);

					}
				}
				for(int j=0; j<3; j++)
				{
					sumdelta[j] = sumdelta[j]/ITERATION;
					sumepsilon[j] = sumepsilon[j]/ITERATION ;
					Logger.log("\n Player: "+j+ " final delta: "+ sumdelta[j]+  " Player: "+j+ " final epsilon: "+ sumepsilon[j], false);

				}
				try{
					PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"result"+perc+".csv"),true));
					for(int j=0; j<3; j++)
					{

						String x = " ";
						if(j==0)
						{
							x = "PSNE";
							pw.append("\n "+x+","+sumdelta[j] + ","+ sumepsilon[j]);
						}
						else if(j==5)
						{
							x = "CFR";
							pw.append("\n "+x+","+sumdelta[j] + ","+ sumepsilon[j]);
						}
						else if(j==1)
						{
							x = "MinEpsilonbounded";
							pw.append("\n "+x+","+sumdelta[j] + ","+ sumepsilon[j]);
						}
						else if(j==2)
						{
							x = "QRE";
							pw.append("\n "+x+","+sumdelta[j] + ","+ sumepsilon[j]);
						}

						Logger.logit("\nFor clustering "+clusternumber+" and "+x+" profile, Final Average delta and epsilon "+sumdelta[j] + " "+ sumepsilon[j] + "  ");

					}
					pw.append("\n\n");
					pw.close();

				}
				catch(Exception e)
				{

				}

			}
		}

	}

	/**
	 * 
	 * @param numberofclusters number of clusters
	 * @param mg game to perform test on
	 * @param gamename game name
	 * * @param payoffclustering payoff cluster is where payoffs are used to find similar actions, if false regret clustering is used. 
	 * @param cappedval the value above which there should not be any payoff
	 * @return returns delta and epsilon
	 */
	public static double[][] evaluateRegretLearning(int numberofclusters, MatrixGame mg, String gamename, boolean payoffclustering, int[] solvers, int cappedval )
	{
		double[][] result = new double[solvers.length][2];
		/*
		 * for random restart we need to save the clusterings... and deltas...
		 */
		HashMap<Integer,List<Integer>[]> clustersplayer1 = new HashMap<Integer, List<Integer>[]>();
		HashMap<Integer,List<Integer>[]> clustersplayer2 = new HashMap<Integer, List<Integer>[]>();
		HashMap <Integer, Double> deltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> deltasplayer2 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer1 = new HashMap<Integer, Double>();
		HashMap <Integer, Double> maxdeltasplayer2 = new HashMap<Integer, Double>();
		final  int RANDOM_RESTART_ITERATION = 6; // 
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

			if(payoffclustering==true)
			{

				clusterforplayers[0] = KmeanClustering.clusterActions(numberofclusters, 0, mg);
				clusterforplayers[1] = KmeanClustering.clusterActions(numberofclusters, 1, mg);
			}
			else
			{
				MatrixGame rgrtgm = RegretClustering.doRegretTable(mg, cappedval);
				clusterforplayers[0] = KmeanClustering.clusterActions(numberofclusters, 0, rgrtgm);
				clusterforplayers[1] = KmeanClustering.clusterActions(numberofclusters, 1, rgrtgm);
			}
			

			if( KmeanClustering.isMaxDelta())
			{
				double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, KmeanClustering.isMaxDelta());
				double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, KmeanClustering.isMaxDelta());
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

			/*
			 * save the clusterings and deltas
			 */
			clustersplayer1.put(randomitr, clusterforplayers[0]);
			clustersplayer2.put(randomitr, clusterforplayers[1]);

		} // end of random iteration loop
		Logger.log("\n Selecting minimum delta", false);
		double[] mindeltas = new double[mg.getNumPlayers()]; // will contain the minimum delta for 2 players
		double[] maxdeltas = new double[mindeltas.length]; 
		// find the cluster with minimum delta
		clusterforplayers[0] = KmeanClustering.getBestCluster(deltasplayer1, maxdeltasplayer1,clustersplayer1, 0, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		clusterforplayers[1] = KmeanClustering.getBestCluster(deltasplayer2, maxdeltasplayer2,clustersplayer2, 1, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);
		int[] numberofclustersforeachplayer = new int[mg.getNumPlayers()];
		for(int i =0; i< mg.getNumPlayers(); i++)
		{
			numberofclustersforeachplayer[i] = numberofclusters;
		}
		/* For the strategy map
		 * 1. give the constructor appropriate variables.
		 * 2. pass the cluster mapping to the strategy map or pass the array, which contain the cluster number for each actions, for each player

		 */	
		StrategyMapping strategymap = new StrategyMapping(mg.getNumPlayers(), mg.getNumActions(), numberofclustersforeachplayer,mg, gamename);
		strategymap.mapActions(clusterforplayers[0], 0);
		strategymap.mapActions(clusterforplayers[1], 1);
		MatrixGame abstractedgame = strategymap.makeAbstractGame();
		
		// solvers[] = {0,2,3};

		//epsilons contains the deviations for each solution concept
		ArrayList<Double> epsilons = SolverCombo.computeStabilityWithMultipleSolversForAbstraction(solvers, abstractedgame, mg, strategymap);


		if(KmeanClustering.isAvrgDelta())
		{
			double maxd = maxdeltas[0]>maxdeltas[1]?maxdeltas[0]:maxdeltas[1];
			for(int i=0; i<epsilons.size(); i++)
			{
				result[i][0] = maxd; // max deltas
				result[i][1] = epsilons.get(i); //qre epsilon

			}

		}
		else
		{
			double mind = mindeltas[0]<mindeltas[1]?mindeltas[0]: mindeltas[1];
			for(int i=0; i<epsilons.size(); i++)
			{
				result[i][0] = mind; // min over max delta
				result[i][1] = epsilons.get(i);   //qre epsilon

			}

		}


		return result;

	}

	public static void startSolvingGameByRegret()
	{
		MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"k8-0"+Parameters.GAMUT_GAME_EXTENSION)); 
		RegretLearner.solveGame(tstgame);
	}

	public static MixedStrategy[] solveGame(MatrixGame matrixgame)
	{	
		/*
		 * hashmap for saving the strategy for each player for each time the game has been played
		 */
		//HashMap<Integer,MixedStrategy[]> player1history = new HashMap<Integer, MixedStrategy[]>();
		MixedStrategy[] finalstrategy = new MixedStrategy[2];


		/*
		 * holds the sum of regrets after t-th repetition
		 */
		double[][] sumofregrets = new double[matrixgame.getNumPlayers()][];
		for(int j=0; j<matrixgame.getNumPlayers(); j++)
		{
			sumofregrets[j] = new double[matrixgame.getNumActions(j)];
		}
		double[][] sumofstrategy = new double[matrixgame.getNumPlayers()][];
		for(int j=0; j<matrixgame.getNumPlayers(); j++)
		{
			sumofstrategy[j] = new double[matrixgame.getNumActions(j)];
		}

		int repeptition_limit = 50000;
		int repetition_counter = 0;
		while(repetition_counter<repeptition_limit)
		{

			/*
			 * choose action for round t
			 *  
			 */
			int[] outcome = new int[matrixgame.getNumPlayers()];
			if(repetition_counter>0)
			{
				outcome = chooseActions(finalstrategy);
			}
			MixedStrategy[] newstrategy = new MixedStrategy[2];
			newstrategy	= regrettMatching(matrixgame, repetition_counter, outcome, sumofregrets, sumofstrategy);
			finalstrategy = newstrategy;
			List<MixedStrategy> originalstrategylist = new ArrayList<MixedStrategy>();
			originalstrategylist.add(finalstrategy[0]);
			originalstrategylist.add(finalstrategy[1]);
			OutcomeDistribution origregretdistro = new OutcomeDistribution(originalstrategylist);
			double regretepsilon = SolverUtils.computeOutcomeStability(matrixgame, origregretdistro);
			repetition_counter++;
			//System.out.println("iter "+ repetition_counter);
		}
		MixedStrategy[] ultimatestrategy = getUltimateStrategy(sumofstrategy, repeptition_limit);
		List<MixedStrategy> originalstrategylist = new ArrayList<MixedStrategy>();
		originalstrategylist.add(ultimatestrategy[0]);
		originalstrategylist.add(ultimatestrategy[1]);
		OutcomeDistribution origregretdistro = new OutcomeDistribution(originalstrategylist);
		double regretepsilon = SolverUtils.computeOutcomeStability(matrixgame, origregretdistro);
		return ultimatestrategy;

	}




	private static MixedStrategy[] getUltimateStrategy(
			double[][] sumofstrategy, int repetition_limit) {

		MixedStrategy[] strategy = new MixedStrategy[sumofstrategy.length];
		for(int i=0; i<sumofstrategy.length; i++)
		{
			strategy[i] = new MixedStrategy(sumofstrategy[i].length);
			for(int j=0; j<sumofstrategy[i].length; j++)
			{
				double prob = sumofstrategy[i][j]/(repetition_limit);
				strategy[i].setProb(j+1, prob);
			}
			strategy[i].normalize();
		}
		return strategy;
	}

	private static MixedStrategy[] regrettMatching(MatrixGame matrixgame,
			int repetition_counter, int[] outcome, double[][] sumofregrets,
			double[][] sumofstrategy) 
	{

		int numberofplayer = matrixgame.getNumPlayers();
		int[] numberofactions = matrixgame.getNumActions();
		MixedStrategy[] strategy = new MixedStrategy[numberofplayer];
		for(int n=0; n<numberofplayer; n++)
		{
			strategy[n] = new MixedStrategy(numberofactions[n]);
		}
		double[][] regrets = new double[numberofplayer][];
		double[] sumofpositiveregrets = new double[numberofplayer]; // sum of positive regrets for every players. 
		for(int i=0; i<numberofplayer; i++)
		{
			regrets[i] = new double[numberofactions[i]];
			for(int j=0; j<numberofactions[i]; j++)
			{
				regrets[i][j] = calculateRegrett(matrixgame, repetition_counter, i, j+1, outcome, sumofregrets);

				/*
				 * take the positive regret
				 */
				sumofpositiveregrets[i] += (max(regrets[i][j], 0));
				regrets[i][j] = max(regrets[i][j], 0);
			}

		}

		if(repetition_counter==0)
		{
			updateSumOfStrategy(sumofstrategy, strategy);
			return strategy;
		}

		for(int i=0; i<numberofplayer; i++)
		{
			for(int j=0; j<numberofactions[i]; j++)
			{
				if(sumofpositiveregrets[i]>0)
				{
					double prob = regrets[i][j]/sumofpositiveregrets[i];
					strategy[i].setProb(j+1, prob);
				}
				else
				{
					double prb = 1.0/numberofactions[i];
					strategy[i].setProb(j+1, prb);
				}

			}

			strategy[i].normalize();


		}
		updateSumOfStrategy(sumofstrategy, strategy);

		return strategy;

	}

	private static void updateSumOfStrategy(double[][] sumofstrategy,
			MixedStrategy[] strategy) {

		for(int i=0; i<sumofstrategy.length; i++)
		{
			for(int j=0; j<sumofstrategy[i].length; j++)
			{
				sumofstrategy[i][j] += strategy[i].getProb(j+1);
			}
		}

	}

	private static double calculateRegrett(MatrixGame matrixgame,
			int repetition_counter, int player, int action, int[] outcome,
			double[][] sumofregrets) {

		// for the first iteration there is no regret. 
		if(repetition_counter==0)
		{
			return 0;
		}
		else
		{
			double tmpsumofregret = 0;
			/*
			 * calculate the regret only for (repetition_counter-1)th iteration, which is the previous iteration
			 */
			for(int i=(repetition_counter-1); i<=(repetition_counter-1); i++)
			{
				/*
				 * get the expected payoff for outcome
				 */
				double expectedpayof = matrixgame.getPayoff(outcome, player);


				/*
				 * build an outcome for action for player
				 */
				int[] regretoutcome = new int[outcome.length];
				for(int k=0; k<matrixgame.getNumPlayers(); k++)
				{
					if(k != player)
					{
						regretoutcome[k] = outcome[k];
					}
					else if(k==player)
					{
						regretoutcome[player] = action;
					}
				}

				double  payoff = matrixgame.getPayoff(regretoutcome, player);
				double tmpregret = payoff - expectedpayof;
				tmpsumofregret += tmpregret;
				sumofregrets[player][action-1] += tmpsumofregret;
			}
			if((repetition_counter-1) > 0)
			{
				/*
				 * return the average regret. 
				 */
				return tmpsumofregret = sumofregrets[player][action-1]/(repetition_counter);
			}
			else if((repetition_counter-1) ==0)
			{
				return  sumofregrets[player][action-1];
			}
			else
			{
				System.out.print("Something wrong");
			}
		}
		return 0;

	}

	private static int[] chooseActions(MixedStrategy[] finalstrategy) 
	{
		int[] outcome = new int[2];

		for(int i=0; i<2; i++)
		{
			double[] probs = finalstrategy[i].getProbs();
			double probsum = 0.0;
			double r = random.nextDouble();
			for(int j=0; j<(probs.length-1); j++)
			{
				probsum += probs[j+1];
				if(r<probsum)
				{
					outcome[i] = j+1;
					break;
				}
			}
		}

		return outcome;
	}


	private static double max(double d, int i) {
		if(d>i)
			return d;

		return i;
	}

}
