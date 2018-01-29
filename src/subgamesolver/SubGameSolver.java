package subgamesolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import games.MatrixGame;
import games.MixedStrategy;
import games.OutcomeDistribution;
import solvers.IEDSMatrixGames;
import solvers.SolverCombo;
import solvers.SolverUtils;
import subgame.GameReductionBySubGame;

public class SubGameSolver implements Runnable {


	private MatrixGame subgamex;
	int iteration;
	int subgameindex;
	int players = 2;

	private Thread t;
	private String threadName;

	public MixedStrategy[] subgameprofile = new MixedStrategy[2];;





	public Thread getT() {
		return t;
	}


	public void setT(Thread t) {
		this.t = t;
	}


	public SubGameSolver() {
		super();
	}


	public SubGameSolver(MatrixGame subgamex, int iteration, int subgameindex, String threadName) {
		super();
		this.subgamex = subgamex;
		this.iteration = iteration;
		this.subgameindex = subgameindex;
		this.threadName = threadName;

	}
	


	@Override
	public void run() 
	{
		if(subgamex.getNumActions(0)==0 || subgamex.getNumActions(1)==0)
		{
			subgameprofile[0] = new MixedStrategy(subgamex.getNumActions(0));
			subgameprofile[1] = new MixedStrategy(subgamex.getNumActions(1));
		}
		else
		{

			if(this.iteration==-1)
			{
				/*
				 * for each subgame set uniform random
				 */

				//MixedStrategy[] subgameprofile = new MixedStrategy[players];
				this.subgameprofile[0] = new MixedStrategy(this.subgamex.getNumActions(0));
				this.subgameprofile[1] = new MixedStrategy(this.subgamex.getNumActions(1));

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
					needstosolve = GameReductionBySubGame.needToSolve(this.subgameindex);
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
						
						int[] solver = {3};
						MixedStrategy[] tmpsubgameprofile = solve(solver, IEDmat);
						
						//int[] solver = {3}; // QRE and use IEDmat
						//MixedStrategy[] tmpsubgameprofile =  SolverCombo.computeStrategyWithMultipleSolvers(solver, IEDmat);
						
						
						
						if( (tmpsubgameprofile[0].getProbs().length-1) != remaining[0].size()   ||  (tmpsubgameprofile[1].getProbs().length-1) != remaining[1].size() )
						{
							try {
								throw new Exception("SOmething wrong in IED, tmpsubgameprofile");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if( (subgameprofile[0].getProbs().length-1) != (remaining[0].size()+removed[0].size())   ||  (subgameprofile[1].getProbs().length-1) != (remaining[1].size()+removed[1].size()) )
						{
							try {
								throw new Exception("SOmething wrong in IED, subgameprofile");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
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
					subgameprofile[0].setProbs(GameReductionBySubGame.prevsubgamestrategy.get(0).get(this.subgameindex).getProbs());
					subgameprofile[1].setProbs(GameReductionBySubGame.prevsubgamestrategy.get(1).get(this.subgameindex).getProbs());

				}
			}
		}
		//player1strategies.add(subgameprofile[0]);
		//player2strategies.add(subgameprofile[1]);
		//subgamegameindex++;


	}
	
	
	


	private synchronized MixedStrategy[] solve(int[] solver, MatrixGame IEDmat) {
		
		return SolverCombo.computeStrategyWithMultipleSolvers(solver, IEDmat);
	}


	public void start () 
	{
		System.out.println("Starting " +  threadName );
		if (t == null) 
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}

}
