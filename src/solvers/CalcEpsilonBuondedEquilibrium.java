package solvers;

import java.util.List;

import Log.Logger;
import games.DeviationIterator;
import games.Game;
import games.MixedStrategy;
import subgame.StrategyMapping;

public class CalcEpsilonBuondedEquilibrium {


	public static int numberoofactionsplayer1;
	public static int numberoofactionsplayer2;
	public static StrategyMapping strategymapping;
	public static Game game;
	public static double[][] epsilon;// = new double[numberoofactionsplayer1][numberoofactionsplayer2];  
	//public static double[][] actionwith = new double[numberoofactionsplayer1][numberoofactionsplayer2];
	public static int[] epsiloneq;// = new int[CalcEpsilonBuondedEquilibrium.game.getNumPlayers()];

	public static boolean isepsilonbounded = false;


	public CalcEpsilonBuondedEquilibrium(StrategyMapping strategymapping,
			Game game) 
	{
		super();
		CalcEpsilonBuondedEquilibrium.strategymapping = strategymapping;
		CalcEpsilonBuondedEquilibrium.game = game;
		CalcEpsilonBuondedEquilibrium.numberoofactionsplayer1 = game.getNumActions(0);
		CalcEpsilonBuondedEquilibrium.numberoofactionsplayer2 = game.getNumActions(1);

		CalcEpsilonBuondedEquilibrium.epsilon = new double[numberoofactionsplayer1][numberoofactionsplayer2];
		epsiloneq = new int[CalcEpsilonBuondedEquilibrium.game.getNumPlayers()];



		for(int i=0; i<CalcEpsilonBuondedEquilibrium.epsilon.length; i++)
		{
			for(int j=0; j<CalcEpsilonBuondedEquilibrium.epsilon[i].length; j++)
			{
				CalcEpsilonBuondedEquilibrium.epsilon[i][j] = Double.NEGATIVE_INFINITY;
			}
		}


	}


	public  MixedStrategy getEpsilonBoundedEq(int player)
	{


		double[] prob = new double[CalcEpsilonBuondedEquilibrium.game.getNumActions(player)+1];
		prob[CalcEpsilonBuondedEquilibrium.epsiloneq[player]] = 1; 
		MixedStrategy strategy = new MixedStrategy(prob);
		
		

		Logger.log("\n player "+ player+ " min epsilon profile "+CalcEpsilonBuondedEquilibrium.epsiloneq[player] , false);


		return strategy;



	}


	public  void calcMaxEpsilon()
	{
		//MixedStrategy mstr = new MixedStrategy(numberoofactionsplayer1);


		double minep = Double.POSITIVE_INFINITY; // This variable contains the most recent outcome with smallest epsilon, 

		for(int i=0; i<CalcEpsilonBuondedEquilibrium.numberoofactionsplayer1; i++)
		{
			for(int j=0; j<CalcEpsilonBuondedEquilibrium.numberoofactionsplayer2; j++)
			{

				int[] outcome = {i+1, j+1};
				int[] nActions = {CalcEpsilonBuondedEquilibrium.numberoofactionsplayer1, CalcEpsilonBuondedEquilibrium.numberoofactionsplayer2};
				double outcomepayoff = 0;  



				/*
				 * deviation iterator iterates over the deviated outcomes(outcome)
				 */
				DeviationIterator devitr = new DeviationIterator(outcome, nActions);
				int[] devoutcome = new int[CalcEpsilonBuondedEquilibrium.game.getNumPlayers()];
				double devoutcomepayoff = 0; 




				while(devitr.hasNext())
				{
					devoutcome = devitr.next();
					
					//player is the index where outcome has changed. 
					int player = CalcEpsilonBuondedEquilibrium.getPlayer(outcome, devoutcome);

					outcomepayoff = CalcEpsilonBuondedEquilibrium.game.getPayoff(outcome, player);
					
					
					
					//epsilon bounded takes the maximum payoff in a cluster, otherwise it's the average payoff
					if(CalcEpsilonBuondedEquilibrium.isepsilonbounded)
					{
						devoutcomepayoff = CalcEpsilonBuondedEquilibrium.calcMaxPayoffOriginalGame(devoutcome, player);
					}
					else if(!CalcEpsilonBuondedEquilibrium.isepsilonbounded)
					{
						devoutcomepayoff = CalcEpsilonBuondedEquilibrium.game.getPayoff(devoutcome, player);
					}
					
					
					//compute the deviation 
					double dev = (devoutcomepayoff- outcomepayoff);
					
					int a = i+1;
					int b = j+1;
					
					Logger.log("\n player "+ player+" Action profile ("+ a+", "+b+"), payoff "+outcomepayoff+ ", dev profile ("+ devoutcome[0]+ ", "+devoutcome[1]+") , devpayoff "+ devoutcomepayoff+ " , deviation "+ dev, false );
					
					
					
					
					
					// if deviation(dev) is higher than before(epsilon[i][j]), update the epsilon to most recent max deviation
					if(CalcEpsilonBuondedEquilibrium.epsilon[i][j] <  dev  && (dev >= 0) )
					{
						CalcEpsilonBuondedEquilibrium.epsilon[i][j] = dev; 
						Logger.log("\n CalcEpsilonBuondedEquilibrium.epsilon["+a+"]["+b+"] "+ CalcEpsilonBuondedEquilibrium.epsilon[i][j], false);
						
					}
					else if(dev<0)
					{
						//CalcEpsilonBuondedEquilibrium.epsilon[i][j] = 0;
						
						Logger.log("\n CalcEpsilonBuondedEquilibrium.epsilon["+a+"]["+b+"] "+ CalcEpsilonBuondedEquilibrium.epsilon[i][j], false);
						
					}



				}
				
				
				
				/*
				 * now check whether the deviaiton in epsilon[i][j] is the minimum one. if not update 
				 */

				if(minep > CalcEpsilonBuondedEquilibrium.epsilon[i][j])
				{
					minep = CalcEpsilonBuondedEquilibrium.epsilon[i][j];
					CalcEpsilonBuondedEquilibrium.epsiloneq[0] = i+1;
					CalcEpsilonBuondedEquilibrium.epsiloneq[1] = j+1;
					int a = i+1;
					int b = j+1;
					
					Logger.log("\n min epsilon "+ minep + "profile( "+a+","+b+")", false);
					
					
				}





			}
		}


		// now find the minimum epsilon 




		//return mstr;
		
		Logger.log("\n final Epsilon "+minep, false);






		/*	for(int i=0; i< CalcEpsilonBuondedEquilibrium.epsilon.length; i++)
		{
			for(int j=0; j<CalcEpsilonBuondedEquilibrium.epsilon[i].length; j++)
			{

				if(minep > CalcEpsilonBuondedEquilibrium.epsilon[i][j])
				{
					minep = CalcEpsilonBuondedEquilibrium.epsilon[i][j];
					CalcEpsilonBuondedEquilibrium.epsiloneq[0] = i+1;
					CalcEpsilonBuondedEquilibrium.epsiloneq[1] = j+1;
				}

			}
		}*/
		
		
		
		
		for(int i=0; i<CalcEpsilonBuondedEquilibrium.epsilon.length; i++)
		{
			for(int j=0; j<CalcEpsilonBuondedEquilibrium.epsilon[i].length; j++)
			{
				int l = i+1;
				int m = j+1;
				Logger.log("\n Epsilon for profile ("+ l+", "+m+") = "+CalcEpsilonBuondedEquilibrium.epsilon[i][j] , false);
			}
		}






	}

	public static int getPlayer(int[] outcome, int[] devoutcome)
	{
		int player =0;

		for(int i=0; i<outcome.length; i++)
		{
			if(outcome[i] != devoutcome[i])
			{
				player = i; 
			}
		}

		return player; 

	}




	public static double calcMaxPayoffOriginalGame(int[] abstractoutcome, int player)
	{
		double maxpayoff = Double.NEGATIVE_INFINITY;
		int opponent = 1^ player;


		List<Double> originalactionsplayer1 = CalcEpsilonBuondedEquilibrium.strategymapping.getOriginalActionsFromAbstractedAction(abstractoutcome[player], player);
		List<Double> originalactionsplayer2 = CalcEpsilonBuondedEquilibrium.strategymapping.getOriginalActionsFromAbstractedAction(abstractoutcome[opponent], opponent); 


		for(Double x: originalactionsplayer1)
		{
			for(Double y: originalactionsplayer2)
			{

				int[] outcome = {(int)Math.floor(x), (int)Math.floor(y)};
				double payoff = CalcEpsilonBuondedEquilibrium.strategymapping.getOriginalgame().getPayoff(outcome, player);
				if(payoff>maxpayoff)
				{
					maxpayoff= payoff;
				}
			}
		}



		return maxpayoff;
	}



}
