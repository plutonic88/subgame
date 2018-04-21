package solvers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import games.EmpiricalMatrixGame;
import games.Game;
import games.MatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import subgame.GameReductionBySubGame;
import subgame.KmeanClustering;
import subgame.StrategyMapping;


/**
 * 
 * @author sunny
 * 
 * This class provides the following solvers. 
 * 0. PSNE
 * 1. Counter Factual Regret
 * 2. MinEPsilonBounded Profile
 * 3. QRE
 *
 */

public class SolverCombo {

	/**
	 * THis method must be called after setting the strategy mapping class
	 * @param solvers indicate which solvers you want
	 * @return returns the epsilons for the solvers.
	 * 3. QRE
	 * 1. Counter Factual Regret
	 * 2. MinEPsilonBounded Profile
	 * 0. PSNE
	 */
	public static ArrayList<Double> computeStabilityWithMultipleSolversForAbstraction(int[] solvers, 
			MatrixGame abstractgame, MatrixGame originalgame, StrategyMapping strategymap)
			{
		/*
		 * inside epsilons the double will contain epsilons for each solution concept
		 */
		ArrayList<Double> epsilons = new ArrayList<Double>();
		for(int x: solvers)
		{
			long l1 = -1;
			MixedStrategy[] abstractgamestrategy = new MixedStrategy[originalgame.getNumPlayers()];
			if(x==0) //PSNE
			{
				Date start = new Date();
				l1 = start.getTime();
				for(int i=0; i< originalgame.getNumPlayers(); i++ )
				{
					abstractgamestrategy[i] = MinEpsilonBoundSolver.getPSNE(abstractgame).get(i);
				}


			}
			else if(x==1) // CFR
			{
				Date start = new Date();
				l1 = start.getTime();
				abstractgamestrategy = RegretLearner.solveGame(abstractgame);

			}
			else if(x==2) // MinEpsilonBounded
			{
				Date start = new Date();
				l1 = start.getTime();
				Game gmwithmaxexpectedpayoff = KmeanClustering.getGameWithMaxExpectedPayoff(abstractgame, strategymap);
				ArrayList<MixedStrategy> strategies = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractgame, gmwithmaxexpectedpayoff);
				for(int i=0; i< originalgame.getNumPlayers(); i++ )
				{
					abstractgamestrategy[i] = strategies.get(i);
				}


			}
			else if(x==3) // QRE
			{
				Date start = new Date();
				l1 = start.getTime();
				QRESolver qre = new QRESolver(100);
				EmpiricalMatrixGame emg = new EmpiricalMatrixGame(abstractgame);
				qre.setDecisionMode(QRESolver.DecisionMode.RAW);
				for(int i=0; i< originalgame.getNumPlayers(); i++ )
				{
					abstractgamestrategy[i] = qre.solveGame(emg, i);
				}

			}
			MixedStrategy[] originalgamestrategy = new MixedStrategy[originalgame.getNumPlayers()];
			for(int i=0; i< originalgame.getNumPlayers(); i++ )
			{
				originalgamestrategy[i] = KmeanClustering.buildOriginalStrategyFromAbstractStrategy(strategymap, abstractgamestrategy[i], originalgame.getNumActions(i), abstractgame.getNumActions(i), i);
			}
			List<MixedStrategy> strategylist = new ArrayList<MixedStrategy>();
			for(int i=0; i<originalgamestrategy.length; i++)
			{
				strategylist.add(originalgamestrategy[i]);
			}
			OutcomeDistribution origdistribution = new OutcomeDistribution(strategylist);
			double[]  originalpayoff = SolverUtils.computeOutcomePayoffs(originalgame, origdistribution);
			double epsilon = SolverUtils.computeOutcomeStability(originalgame, origdistribution);
			if(x==0) //PSNE)
			{
				Date stop = new Date();
				long l2 = stop.getTime();
				long diff = l2 - l1;
				GameReductionBySubGame.psnetimer += diff ;
				GameReductionBySubGame.psnetimecounter++;
			}
			else if(x==1) //cfr)
			{
				Date stop = new Date();
				long l2 = stop.getTime();
				long diff = l2 - l1;
				GameReductionBySubGame.cfrtimer += diff ;
				GameReductionBySubGame.cfrtimecounter++;
			}
			else if(x==2)
			{
				Date stop = new Date();
				long l2 = stop.getTime();
				long diff = l2 - l1;
				GameReductionBySubGame.mebtimer += diff ;
				GameReductionBySubGame.mebtimecounter++;
			}
			else if(x==3)
			{
				Date stop = new Date();
				long l2 = stop.getTime();
				long diff = l2 - l1;
				GameReductionBySubGame.qretimer += diff ;
				GameReductionBySubGame.qretimecounter++;
			}
			epsilons.add(epsilon);
		}
		return epsilons;

			}



	/**
	 * computer mixed strategy for a solver mentioned in solver. Mention only one solver in solvers array
	 * 3. QRE
	 * 1. Counter Factual Regret
	 * 2. MinEPsilonBounded Profile : Can only be used for an abstract game, because we need mapping
	 * 0. PSNE
	 * @param solvers need to mention a solver
	 * @param abstractgame the game that needs to be solved
	 * @param strategymap 
	 * @param player
	 * @return
	 */
	public static MixedStrategy[] computeStrategyWithMultipleSolvers(int[] solvers, 
			MatrixGame abstractgame)
	{
		/*
		 * inside epsilons the double will contain epsilons for each solution concept
		 */

		for(int x: solvers)
		{

			MixedStrategy[] gamestrategy = new MixedStrategy[abstractgame.getNumPlayers()];
			if(x==0) //PSNE
			{

				for(int i=0; i< abstractgame.getNumPlayers(); i++ )
				{
					gamestrategy[i] = MinEpsilonBoundSolver.getPSNE(abstractgame).get(i);
				}


			}
			else if(x==1) // CFR
			{
				gamestrategy = RegretLearner.solveGame(abstractgame);

			}
			else if(x==2) // MinEpsilonBounded
			{

				//Game gmwithmaxexpectedpayoff = KmeanClustering.getGameWithMaxExpectedPayoff(abstractgame, strategymap);
				Game gmwithmaxexpectedpayoff = GameReductionBySubGame.getGameWithMaxExpectedPayoff(abstractgame, GameReductionBySubGame.partition);
				ArrayList<MixedStrategy> strategies = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractgame, gmwithmaxexpectedpayoff);
				for(int i=0; i< abstractgame.getNumPlayers(); i++ )
				{
					gamestrategy[i] = strategies.get(i);
				}


			}
			else if(x==3) // QRE
			{

				QRESolver qre = new QRESolver(100);
				EmpiricalMatrixGame emg = new EmpiricalMatrixGame(abstractgame);
				qre.setDecisionMode(QRESolver.DecisionMode.RAW);
				for(int i=0; i< abstractgame.getNumPlayers(); i++ )
				{
					gamestrategy[i] = qre.solveGame(emg, i);
				}

			}
			return gamestrategy;

		}

		return null;




	}


	

	/**
	 * computer mixed strategy for a solver mentioned in solver. Mention only one solver in solvers array
	 * 3. QRE
	 * 1. Counter Factual Regret
	 * 2. MinEPsilonBounded Profile : only for abstracted(higher level game in subgame) game
	 * 0. PSNE
	 * @param solvers need to mention a solver
	 * @param abstractgame the game that needs to be solved
	 * @param strategymap 
	 * @param player
	 * @return
	 */
	public static MixedStrategy[] computeStrategyWithOneSolver(int solver, 
			MatrixGame abstractgame)
	{
		/*
		 * inside epsilons the double will contain epsilons for each solution concept
		 */

	//	for(int x: solvers)
	//	{

			MixedStrategy[] gamestrategy = new MixedStrategy[abstractgame.getNumPlayers()];
			if(solver==0) //PSNE
			{

				for(int i=0; i< abstractgame.getNumPlayers(); i++ )
				{
					gamestrategy[i] = MinEpsilonBoundSolver.getPSNE(abstractgame).get(i);
				}


			}
			else if(solver==1) // CFR
			{
				gamestrategy = RegretLearner.solveGame(abstractgame);

			}
			else if(solver==2) // MinEpsilonBounded
			{

				Game gmwithmaxexpectedpayoff = GameReductionBySubGame.getGameWithMaxExpectedPayoff(abstractgame, GameReductionBySubGame.partition);
				ArrayList<MixedStrategy> strategies = MinEpsilonBoundSolver.getMinEpsilonBoundProfile(abstractgame, gmwithmaxexpectedpayoff);
				for(int i=0; i< abstractgame.getNumPlayers(); i++ )
				{
					gamestrategy[i] = strategies.get(i);
				}

			}
			else if(solver==3) // QRE
			{

				QRESolver qre = new QRESolver(100);
				EmpiricalMatrixGame emg = new EmpiricalMatrixGame(abstractgame);
				qre.setDecisionMode(QRESolver.DecisionMode.RAW);
				for(int i=0; i< abstractgame.getNumPlayers(); i++ )
				{
					gamestrategy[i] = qre.solveGame(emg, i);
				}

			}
			return gamestrategy;

	//	}

	//	return null;




	}



}
