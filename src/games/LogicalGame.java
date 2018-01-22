package games;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import output.SimpleOutput;
import subgame.Parameters;

public class LogicalGame {
	public  int numberofclause;
	public  int literalperclause;
	public  int numberofvariables;
	public  int numberofformula;
	
	private static LogicalGame instance = null;

	public   static List<ArrayList<Integer>>[][] randompayoffformula = new List[2][];// = new ArrayList<ArrayList<Integer>>();
	public   static ArrayList<ArrayList<Integer>> actions = new ArrayList<ArrayList<Integer>>();
	public static int[][] payoffs = new int[2][];
	
	
	
	public static LogicalGame getInstance(int numberofclause, int literalperclause, int numberofvariables, int numberofformula) {
	      if(instance == null) {
	         instance = new LogicalGame(numberofclause, literalperclause, numberofvariables, numberofformula);
	      }
	      return instance;
	   }

	protected LogicalGame(int numberofclause, int literalperclause, int numberofvariables, int numberofformula) {
		super();
		this.numberofclause = numberofclause;
		this.numberofvariables = numberofvariables;
		this.literalperclause = literalperclause;
		this.numberofformula = numberofformula;


		for(int j=0; j<2; j++)
		{
			this.payoffs[j] = new int[this.numberofformula];
		}

		//int[] N = {(int)Math.pow(2, numberofvariables), (int)Math.pow(2, numberofvariables)};
		//public static MatrixGame logicalgame = new MatrixGame(2, N);


		for(int j=0; j<2; j++)
		{
			randompayoffformula[j] = new List[this.numberofformula];
		}

		for(int i=0;i<2; i++)
		{
			for(int j=0; j<this.numberofformula; j++)
			{
				randompayoffformula[i][j] = new ArrayList<ArrayList<Integer>>();
			}
		}




	}

	public static void generateGame(int numberofplayer, int numberofaction, int gamenumber)
	{

		int[] N = {numberofaction, numberofaction}; 

		MatrixGame logicalgame  = new MatrixGame(numberofplayer, N);


		for(int i =0; i< numberofplayer; i++)
		{
			for(int j=0; j< LogicalGame.randompayoffformula[i].length; j++)
			{



				for(ArrayList<Integer> y: LogicalGame.actions)
				{
					for(ArrayList<Integer> z: LogicalGame.actions)
					{
						int payoffindex =0;
						int payoffsum =0;
						for(List<ArrayList<Integer>> x : LogicalGame.randompayoffformula[i])
						{



							int tmppayoff = LogicalGame.calculatePayoffFromAFormula(i, y, z, x, payoffindex);
							payoffsum = payoffsum + tmppayoff;
							payoffindex++;	
						}

						int outcomeplayer1 = LogicalGame.returnActionVal(y)+1;
						int outcomeplayer2 = LogicalGame.returnActionVal(z)+1;

						int[] outcome = {outcomeplayer1, outcomeplayer2};

						if(payoffsum>100)
							payoffsum = 100; //limit the payoff by 100

						logicalgame.setPayoff(outcome, i, payoffsum);



					}
				}





			}
		}


		try{
			PrintWriter pw = new PrintWriter(gamenumber+Parameters.GAMUT_GAME_EXTENSION,"UTF-8");
			SimpleOutput.writeGame(pw,logicalgame);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("LogicalGame class :something went terribly wrong during logical game  creation ");
		}





	}



	public static int calculatePayoffFromAFormula(int player, ArrayList<Integer> action1, ArrayList<Integer> action2, List<ArrayList<Integer>> payoffformula, int payoffindex)
	{

		int payoff = 0;

		boolean flag = false;

		for(ArrayList<Integer> x: payoffformula)
		{
			flag=false;
			for(Integer y: x)
			{
				flag= false;

				if(LogicalGame.isTrue(action1, action2, y)== true)
				{
					flag = true;
					break;
				}

			}

			if(flag==false)
			{
				break;
			}



		}

		if(flag==true)
		{
			payoff = LogicalGame.payoffs[player][payoffindex];
		}

		return payoff;
	}


	public static int returnActionVal(ArrayList<Integer> action)
	{
		int x =0;

		for(int i=0; i<action.size(); i++)
		{
			x = (x| (action.get(i) << ( (action.size()-1) - i) ));
			//x = x<<1;
		}
		return x;

	}

	public static boolean isTrue(ArrayList<Integer> action1, ArrayList<Integer> action2, int literal)
	{


		if(Math.abs(literal)>action1.size())
		{
			int val = action2.get( (Math.abs(literal)-1) % action2.size());
			if((val>0 && literal >0) || (val==0 && literal < 0))
			{
				return true;
			}
			else 
			{
				return false;
			}

		}
		else if(Math.abs(literal)<=action1.size())
		{
			int val = action1.get( (Math.abs(literal)-1));
			if((val>0 && literal >0)   || (val==0 && literal < 0))
			{
				return true;
			}
			else 
			{
				return false;
			}
		}


		return true;

	}



	public static void generatePayoffs(int numberofformula)
	{
		for(int i=0;i<2; i++)
		{
			for(int j=0; j<numberofformula; j++)
			{


				Random ran = new Random();

				LogicalGame.payoffs[i][j] = ran.nextInt(10) + 1;
			}
		}
	}




	public static void generateActions(int numberofvariables, int player)
	{
		
		LogicalGame.actions.clear();

		int maxvalue = (int)Math.pow(2, numberofvariables) - 1;
		ArrayList<Integer> tmpaction = new ArrayList<Integer>();
		//int actioninbit = 0;


		for(int i=0; i<=maxvalue; i++)
		{
			tmpaction = new ArrayList<Integer>(); // clearing is not enough. 
			for(int j=numberofvariables-1; j>=0; j--)
			{
				if(LogicalGame.returnBitValue(i, j)==0)
				{
					tmpaction.add(0);
					System.out.print(0);
				}
				else if(LogicalGame.returnBitValue(i, j)==1)
				{
					tmpaction.add(1);
					System.out.print(1);
				}

			}
			LogicalGame.actions.add(tmpaction);
			System.out.println();

		}




	}

	public static int returnBitValue(int var, int bitposition)
	{
		int x = var & (1<<bitposition);
		if(x>0)
			return 1;


		return 0; 
	}


	public static void generateRandomFormulas(int numberofclause, int literalperclause, int numberofvariables, int numberofformula)
	{


		for(int n =0; n< 2; n++)
		{
			System.out.println("\n\nPlayer : "+ n+" payoff formula");
			for(int m=0; m<numberofformula; m++)

			{
				System.out.println("\n\nPlayer : "+ n+" payoff formula "+ m);

				for(int i=0; i< numberofclause; i++)
				{
					Random ran = new Random();
					int clauselength = ran.nextInt(literalperclause) + 3;//this.literalperclause;


					int literalcount = 0;
					ArrayList<Integer> tempclause = new ArrayList<Integer>();

					while(true)
					{
						// generate a number between [-numberofvariables, numberofvariables]

						int newliteral  = LogicalGame.randInt(-numberofvariables, numberofvariables);

						if(newliteral!=0)
						{

							//	System.out.println(newliteral);
							//check if the potential literal already exists or it's negation already exists

							boolean isallowed = LogicalGame.allowedInClause(tempclause, newliteral);
							if(isallowed== true)
							{
								tempclause.add(newliteral);
								literalcount++;
								if(literalcount==clauselength)
								{

									// check if the clause already exists
									boolean alreadyexists = LogicalGame.doesClauseExist(LogicalGame.randompayoffformula[n][m], tempclause);
									if(alreadyexists == false)
									{
										LogicalGame.randompayoffformula[n][m].add(tempclause);
										System.out.println(tempclause);
										break;
									}
									else if(alreadyexists== true)
									{
										literalcount=0;
									}


								}//inner if
							} //outer if
						}


					} // outer most while loop




				} 
			}
		}// outermost for loop
	}


	public static boolean doesClauseExist(List<ArrayList<Integer>> clauselist, ArrayList<Integer> clause)
	{
		if(clauselist.isEmpty())
			return false;




		for(ArrayList<Integer> x: clauselist)
		{
			if(x.size() > 0 && (x.size() == clause.size()) )
			{

				boolean issame = LogicalGame.checkIfSame(clause, x);
				if(issame==true)
				{
					return true;
				}

			}
		}

		return false;


	}


	public static boolean checkIfSame(ArrayList<Integer> clause1, ArrayList<Integer> clause2)
	{

		for(int i=0; i< clause1.size(); i++)
		{
			if(clause1.get(i)!=clause2.get(i))
				return false;
		}

		return true;


	}



	public static boolean allowedInClause(ArrayList<Integer> clause, int literal)
	{
		//check if the potential literal already exists or it's negation already exists

		if(clause.size()>0)
		{
			for(Integer x: clause)
			{
				if((x==literal) || (x==-literal))
					return false;
			}
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

	public static void main(String[] args)
	{	
		//LogicalGame lg = new LogicalGame(5, 5, 14, 10);
		
		
		int NUMBER_OF_CLAUSE_LIMIT = 10;
		int LITERAL_PER_CLAUSE_LIMIT = 10;
		int NUMBER_OF_VARIABLES_LIMIT = 14;
		int NUMBER_OF_FORMULAS_LIMIT = 10;
		
		
		


		for(int i=0; i<100; i++)
		{
			Random ran = new Random();

			int numberofclause = ran.nextInt(NUMBER_OF_CLAUSE_LIMIT) + 1;//NUMBER_OF_CLAUSE_LIMIT;//
			int literalperclause = LITERAL_PER_CLAUSE_LIMIT;//ran.nextInt(LITERAL_PER_CLAUSE_LIMIT) + 1;
			int numberofvariables = NUMBER_OF_VARIABLES_LIMIT;//ran.nextInt(20) + 1;
			int numberofformula = ran.nextInt(NUMBER_OF_FORMULAS_LIMIT) + 5;//NUMBER_OF_FORMULAS_LIMIT;//
			
			LogicalGame lg = LogicalGame.getInstance(numberofclause, literalperclause, numberofvariables, numberofformula);
			
			LogicalGame.generateRandomFormulas(lg.numberofclause, lg.literalperclause, lg.numberofvariables, lg.numberofformula);
			LogicalGame.generateActions(lg.numberofvariables/2, 0); // each player has half the variables
			//	System.out.println("\n"+LogicalGame.actions);
			LogicalGame.generatePayoffs(lg.numberofformula);
			LogicalGame.generateGame(2, (int)Math.pow(2, lg.numberofvariables/2), i+1);
			for(List<ArrayList<Integer>> x: LogicalGame.randompayoffformula[0])
			{
				x.clear();
			}
			
			for(List<ArrayList<Integer>> x: LogicalGame.randompayoffformula[1])
			{
				x.clear();
			}
		}

		/*int[] N = {4, 4};
		MatrixGame gm = new MatrixGame(2, N);



		QRESolver qre = new QRESolver(10);

		GamutModifier absgm = new GamutModifier("k16-Logicalgame2-64");

		EmpiricalMatrixGame emg = new EmpiricalMatrixGame(absgm.returnGame());
		qre.setDecisionMode(QRESolver.DecisionMode.RAW);
		System.out.println(qre.solveGame(emg, 0));
		System.out.println(qre.solveGame(emg, 1));

		MixedStrategy[] mixedstrategy = new MixedStrategy[2];

		 mixedstrategy[0] = qre.solveGame(emg, 0);
		 mixedstrategy[1] = qre.solveGame(emg, 1);

		int[] outcome = new int[2];
		double[] expectedpayoff = {0,0}; 

		int payoffcount =0;

		for(int i=0; i<absgm.returnGame().getNumPlayers(); i++)
		{

			payoffcount =0;
			OutcomeIterator	iteratorabstractgame = absgm.returnGame().iterator();


			while(iteratorabstractgame.hasNext())
			{

				outcome = iteratorabstractgame.next();
				if( (mixedstrategy[0].getProb(outcome[0]) > 0) &&  (mixedstrategy[1].getProb(outcome[1]) > 0))
				{
					System.out.println("Equilibrium outcome: "+ outcome[0] + ", "+ outcome[1]);

					payoffcount++;

					expectedpayoff[i]= expectedpayoff[i] + absgm.returnGame().getPayoff(outcome, i)*mixedstrategy[i].getProb(outcome[i]);


				}
			}

			if(payoffcount>0)
			{
				expectedpayoff[i]= expectedpayoff[i]/payoffcount;
			}
		}

		System.out.println("Payoffplayer 0 : "+ expectedpayoff[0]+ "Payoffplayer 1 : "+ expectedpayoff[1] );
		List<MixedStrategy> list = new ArrayList<MixedStrategy>();
		list.add(mixedstrategy[0]);
		list.add(mixedstrategy[1]);

		MatrixGame g = absgm.returnGame();
		OutcomeDistribution distro = new OutcomeDistribution(list);
		double[]  payoff = SolverUtils.computeOutcomePayoffs(g, distro);


		System.out.println("****Expected Payoff player 0 : "+ payoff[0]+ "****Expected Payoff player 1 : "+ payoff[1] );





		//emg.getEmpiricalpayoffs(outcome)





		 */
	}


}


