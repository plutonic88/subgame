package regret;

import games.MatrixGame;
import games.OutcomeIterator;

public class RegretCalculator 
{
	
	/**
	 * 
	 * @param matgame the game used to calculate the regret table
	 * 
	 * @return a regret table
	 */
	public static MatrixGame getRegretPayoffs(MatrixGame matgame)
	{
		MatrixGame regretpayoffs = new MatrixGame(matgame.getNumPlayers(), matgame.getNumActions());
		double[][] maxpayoffrowcolumn = new double[matgame.getNumPlayers()][];
		
			maxpayoffrowcolumn[0] = new double[matgame.getNumActions(1)];
			maxpayoffrowcolumn[1] = new double[matgame.getNumActions(0)];
		getMaxPayoffRowAndColumn(matgame, maxpayoffrowcolumn);
		/*
		 * now build the regret table
		 */
		OutcomeIterator itr = new OutcomeIterator(regretpayoffs);
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			for(int i=0; i<2; i++)
			{
				double maxpayoff = maxpayoffrowcolumn[i][outcome[1^i]-1]; // number of columns is equal to the number of action of opponent
				double regret = maxpayoff - matgame.getPayoff(outcome, i);
				/**
				 * extream level of regret
				 * 1 if regret is greater than 0
				 * else 0. 
				 */
				if(regret>0)
				{
					regretpayoffs.setPayoff(outcome, i, 1);
				}
				else if(regret<=0)
				{
					regretpayoffs.setPayoff(outcome, i, 0);
				}
				
			}
		}
		return regretpayoffs;
		
	}
	/**
	 * 
	 * @param matgame
	 * @param maxpayoffrowcolumn updated to contain the max value for every action for every player
	 */
	private static void getMaxPayoffRowAndColumn(MatrixGame matgame,
			double[][] maxpayoffrowcolumn) 
	{
		for(int i=0; i<maxpayoffrowcolumn.length; i++)
		{
			for(int j=0; j<maxpayoffrowcolumn[i].length; j++)
			{
				/*
				 * get the maximum payoff for action j for a player i
				 */
				double maxpayoff = getMaxPayoffForAnAction(i, j+1, matgame);
				maxpayoffrowcolumn[i][j] = maxpayoff;
			}
		}
		
		
	}
	/**
	 * 
	 * @param  player
	 * @param  action of the opponent
	 * @param matgame 
	 * @return the maxpayoff for action j for player i
	 */
	private static double getMaxPayoffForAnAction(int player, int action,
			MatrixGame matgame) {
		int numberofaction = matgame.getNumActions((player));
		double max = Double.NEGATIVE_INFINITY;
		for(int i=0; i< numberofaction; i++ )
		{
			int[] outcome = new int[2];
			if(player==0)
			{
				outcome[0] = i+1;
				outcome[1] = action;
			}
			else if(player==1)
			{
				outcome[0] = action;
				outcome[1] = i+1;
			}
			double tmp = matgame.getPayoff(outcome, player);
			if(tmp>max)
			{
				max = tmp;
			}
			
			
		}
		return max;
	}
	
	/**
	 * returns the maximum regret for both players
	 * @param regrettable
	 * @return an array containing the max regrets
	 */
	public static double[] getMaxRegret(MatrixGame regrettable) 
	{
		
		double max1 = Double.NEGATIVE_INFINITY;
		double max2 = Double.NEGATIVE_INFINITY;
		double[] maxregrets = new double[2]; 
		
		OutcomeIterator itr = new OutcomeIterator(regrettable);
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			double[] tmpregret = regrettable.getPayoffs(outcome);
			if(tmpregret[0]>max1)
			{
				max1 = tmpregret[0];
			}
			if(tmpregret[1]>max2)
			{
				max2 = tmpregret[1];
			}
		}
		maxregrets[0] = max1;
		maxregrets[1] = max2;
		
		return maxregrets;
	}
	
	/**
	 * 
	 * @param regrettable
	 * @param maxregret
	 * @param cappedval percentage of regret that should be kept
	 */
	public static void capTheRegrets(MatrixGame regrettable,
			double[] maxregret, double cappedval) {
		
		double[] cappedlimit = {maxregret[0]*(cappedval/100), maxregret[1]*(cappedval/100)};
		OutcomeIterator itr = new OutcomeIterator(regrettable);
		while(itr.hasNext())
		{
			int[] outcome = itr.next();
			double[] tmpregret = regrettable.getPayoffs(outcome);
			for(int i=0; i<tmpregret.length; i++)
			{
				if(tmpregret[i]>cappedlimit[i])
				{
					double val = cappedlimit[i];
					regrettable.setPayoff(outcome, i, val);
				}
			}
		}
		
	}

}
