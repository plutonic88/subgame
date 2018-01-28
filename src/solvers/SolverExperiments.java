package solvers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import Log.Logger;
import games.MatrixGame;
import regret.RegretClustering;
import subgame.GameReductionBySubGame;
import subgame.KmeanClustering;
import subgame.StrategyMapping;

public class SolverExperiments 
{






	public static List<Integer>[][] getKmeanCLusters(int numberofclusters, MatrixGame mg,  boolean payoffclustering )
	{


		Date start = new Date();
		long kmeanl1 = start.getTime();
		//double[][] result = new double[solvers.length][2];
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

			//if(payoffclustering==true)
			{
				clusterforplayers[0] = KmeanClustering.clusterActions(numberofclusters, 0, mg);
				clusterforplayers[1] = KmeanClustering.clusterActions(numberofclusters, 1, mg);
			}
			/*else
			{
				MatrixGame rgrtgm = RegretClustering.doRegretTable(mg, cappedval);
				clusterforplayers[0] = KmeanClustering.clusterActions(numberofclusters, 0, rgrtgm);
				clusterforplayers[1] = KmeanClustering.clusterActions(numberofclusters, 1, rgrtgm);
			}*/


			if( KmeanClustering.isMaxDelta())
			{
				double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, KmeanClustering.isMaxDelta());
				double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, KmeanClustering.isMaxDelta());
				deltasplayer1.put(randomitr, delta1);
				deltasplayer2.put(randomitr, delta2);
				//Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ delta1, false);
				//Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ delta2, false);

			}
			if(KmeanClustering.isAvrgDelta())
			{
				double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, false);
				double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, false);
				deltasplayer1.put(randomitr, delta1);
				deltasplayer2.put(randomitr, delta2);
				//Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ delta1, false);
				//Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ delta2, false);
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
				//	Logger.log("\nplayer 0 MaxDelta random iteration "+randomitr+" : "+ maxdelta1, false);
				//	Logger.log("\nplayer 1 MaxDelta random iteration "+randomitr+" : "+ maxdelta2, false);

			}

			/*
			 * save the clusterings and deltas
			 */
			clustersplayer1.put(randomitr, clusterforplayers[0]);
			clustersplayer2.put(randomitr, clusterforplayers[1]);

		} // end of random iteration loop
		//Logger.log("\n Selecting minimum delta", false);
		double[] mindeltas = new double[mg.getNumPlayers()]; // will contain the minimum delta for 2 players
		double[] maxdeltas = new double[mindeltas.length]; 
		// find the cluster with minimum delta
		clusterforplayers[0] = KmeanClustering.getBestCluster(deltasplayer1, maxdeltasplayer1,clustersplayer1, 0, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		clusterforplayers[1] = KmeanClustering.getBestCluster(deltasplayer2, maxdeltasplayer2,clustersplayer2, 1, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		//Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);
		int[] numberofclustersforeachplayer = new int[mg.getNumPlayers()];
		for(int i =0; i< mg.getNumPlayers(); i++)
		{
			numberofclustersforeachplayer[i] = numberofclusters;
		}
		/* For the strategy map
		 * 1. give the constructor appropriate variables.
		 * 2. pass the cluster mapping to the strategy map or pass the array, which contain the cluster number for each actions, for each player

		 */
		return clusterforplayers;
	
	
	}

	/**
	 * 
	 * @param numberofclusters number of clusters
	 * @param mg game to perform test on
	 * @param gamename game name
	 * * @param payoffclustering payoff cluster is where payoffs are used to find similar actions, if false regret clustering is used. 
	 * @param cappedval the value above which there should not be any payoff. If payoff clustering is true, then  not used
	 * @return returns delta and epsilon
	 */
	public static double[][] evaluateSolutionConcepts(int numberofclusters, MatrixGame mg, String gamename, boolean payoffclustering, int[] solvers, int cappedval )
	{


		Date start = new Date();
		long kmeanl1 = start.getTime();
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
				//Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ delta1, false);
				//Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ delta2, false);

			}
			if(KmeanClustering.isAvrgDelta())
			{
				double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, false);
				double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, false);
				deltasplayer1.put(randomitr, delta1);
				deltasplayer2.put(randomitr, delta2);
				//Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ delta1, false);
				//Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ delta2, false);
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
				//	Logger.log("\nplayer 0 MaxDelta random iteration "+randomitr+" : "+ maxdelta1, false);
				//	Logger.log("\nplayer 1 MaxDelta random iteration "+randomitr+" : "+ maxdelta2, false);

			}

			/*
			 * save the clusterings and deltas
			 */
			clustersplayer1.put(randomitr, clusterforplayers[0]);
			clustersplayer2.put(randomitr, clusterforplayers[1]);

		} // end of random iteration loop
		//Logger.log("\n Selecting minimum delta", false);
		double[] mindeltas = new double[mg.getNumPlayers()]; // will contain the minimum delta for 2 players
		double[] maxdeltas = new double[mindeltas.length]; 
		// find the cluster with minimum delta
		clusterforplayers[0] = KmeanClustering.getBestCluster(deltasplayer1, maxdeltasplayer1,clustersplayer1, 0, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		clusterforplayers[1] = KmeanClustering.getBestCluster(deltasplayer2, maxdeltasplayer2,clustersplayer2, 1, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		//Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);
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


		/*
		 * USe different kinds of solution concepts
		 * 0. PSNE
		 * 1. Counter Factual Regret
		 * 2. MinEPsilonBounded Profile
		 * 3. QRE
		 */

		//int solvers[] = {0,2,3};

		//epsilons contains the deviations for each solution concept

		Date stop = new Date();
		long kmeanl2 = stop.getTime();
		long diff = kmeanl2 - kmeanl1;

		GameReductionBySubGame.kmeantimer += diff;

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
	
	
	/**
	 * 
	 * @param numberofclusters number of clusters
	 * @param mg game to perform test on
	 * @param gamename game name
	 * * @param payoffclustering payoff cluster is where payoffs are used to find similar actions, if false regret clustering is used. 
	 * @param cappedval the value above which there should not be any payoff. If payoff clustering is true, then  not used
	 * @param partition 
	 * @return returns delta and epsilon
	 */
	public static double[][] evaluateSolutionConceptsSamePartition(int numberofclusters, MatrixGame mg, String gamename, boolean payoffclustering, int[] solvers, int cappedval, List<Integer>[][] partition )
	{


		Date start = new Date();
		long kmeanl1 = start.getTime();
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
				//Logger.log("\nplayer 0 Maxdelta random iteration "+randomitr+" : "+ delta1, false);
				//Logger.log("\nplayer 1 Maxdelta random iteration "+randomitr+" : "+ delta2, false);

			}
			if(KmeanClustering.isAvrgDelta())
			{
				double delta1 = KmeanClustering.calculateDelta(mg, clusterforplayers, 0, false);
				double delta2 = KmeanClustering.calculateDelta(mg, clusterforplayers, 1, false);
				deltasplayer1.put(randomitr, delta1);
				deltasplayer2.put(randomitr, delta2);
				//Logger.log("\nplayer 0 AvgDelta random iteration "+randomitr+" : "+ delta1, false);
				//Logger.log("\nplayer 1 AvgDelta random iteration "+randomitr+" : "+ delta2, false);
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
				//	Logger.log("\nplayer 0 MaxDelta random iteration "+randomitr+" : "+ maxdelta1, false);
				//	Logger.log("\nplayer 1 MaxDelta random iteration "+randomitr+" : "+ maxdelta2, false);

			}

			/*
			 * save the clusterings and deltas
			 */
			clustersplayer1.put(randomitr, clusterforplayers[0]);
			clustersplayer2.put(randomitr, clusterforplayers[1]);

		} // end of random iteration loop
		//Logger.log("\n Selecting minimum delta", false);
		double[] mindeltas = new double[mg.getNumPlayers()]; // will contain the minimum delta for 2 players
		double[] maxdeltas = new double[mindeltas.length]; 
		// find the cluster with minimum delta
		clusterforplayers[0] = partition[0];//KmeanClustering.getBestCluster(deltasplayer1, maxdeltasplayer1,clustersplayer1, 0, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		clusterforplayers[1] = partition[1];//KmeanClustering.getBestCluster(deltasplayer2, maxdeltasplayer2,clustersplayer2, 1, mindeltas, maxdeltas, KmeanClustering.isMaxDelta());
		//Logger.log("\n Player 0 min delta : "+ mindeltas[0]+ " \n player 1 min delta : "+ mindeltas[1], false);
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


		/*
		 * USe different kinds of solution concepts
		 * 0. PSNE
		 * 1. Counter Factual Regret
		 * 2. MinEPsilonBounded Profile
		 * 3. QRE
		 */

		//int solvers[] = {0,2,3};

		//epsilons contains the deviations for each solution concept

		Date stop = new Date();
		long kmeanl2 = stop.getTime();
		long diff = kmeanl2 - kmeanl1;

		GameReductionBySubGame.kmeantimer += diff;

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

}
