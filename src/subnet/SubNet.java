package subnet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

import games.MatrixGame;
import output.SimpleOutput;
import subgame.GameReductionBySubGame;
import subgame.Parameters;



public class SubNet {



	public static Random rand = new Random(20);

	public static void doExp() throws FileNotFoundException {


		int[] naction = {15,15};

		// number of subnet
		int nsubnet = 3;
		int numberofnodes = 15;
		int nodesinsubnet[] = {numberofnodes/nsubnet, numberofnodes/nsubnet, numberofnodes/nsubnet/*, 
				numberofnodes/nsubnet, numberofnodes/nsubnet*//*,
				numberofnodes/nsubnet, numberofnodes/nsubnet, numberofnodes/nsubnet, numberofnodes/nsubnet, numberofnodes/nsubnet*/};
		int intrasubtransmissionprob[] = {80, 90}; //make this 2d
		double intrasubtransmissionprobnoise = 0;
		int intersubtransmissionprob[] = {10, 20}; // make this 2d/3d
		double intersubtransmissionprobnoise = 0;
		int[] nodevaluerange = {7, 10};
		int[] defcostrange = {0,0}; // make this 2d
		int[] attackercostrange = {0,0}; // make this 2d
		int[] intersubnetnumberofedgerange = {1,1}; //make this 3d/2d
		int nsub = numberofnodes/nsubnet;
		int min = (nsub-1)*(nsub-2)/2 + 1;
		int max = nsub*(nsub-1)/2;
		int[] intrasubnetnumberofedgerange = {min, max};
		int iter=0;


		boolean connectsubnets = true;

		HashMap<Integer, Node> nodes = createNetwork(nsubnet, numberofnodes, nodesinsubnet, intrasubtransmissionprob, intrasubtransmissionprobnoise, intersubtransmissionprob, 
				intersubtransmissionprobnoise, nodevaluerange, defcostrange, attackercostrange, intersubnetnumberofedgerange, intrasubnetnumberofedgerange, 0,0, connectsubnets);		

		printNodes(nodes);


		//HashMap<Integer, Node> nodes = createNetwork();
		//printNodes(nodes);

		HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
		HashMap<String, Double> defenderpayoffs = new  HashMap<String, Double>();
		doMonteCarloSimulation(nodes, defenderpayoffs, attackerpayoffs);
		buildGameFile(naction, defenderpayoffs, attackerpayoffs, naction[0], iter);


	}



	private static void doMonteCarloSimulation(HashMap<Integer, Node> nodes, HashMap<String,Double> defenderpayoffs, HashMap<String,Double> attackerpayoffs) {


		int LIMIT = 10000;

		/*HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
		HashMap<String, Double> defenderpayoffs = new  HashMap<String, Double>();*/


		for(Node hardendenode : nodes.values())
		{
			//hardendenode.setHardended(true);

			//System.out.println("\n*****Hardended node "+ hardendenode.id);

			for(Node attackednode: nodes.values())
			{
				// harden the defended node


				double sumatt = 0.0;
				double sumdef = 0.0;		

				for(int iter = 0; iter<=LIMIT; iter++)
				{

					//System.out.println("\n*****ITER no "+ iter);


					Random rand = new Random();
					double attackerpoints = 0.0;
					double defenderpoints = 0.0;


					//defenderpoints += (0.0 - hardendenode.defcost);
					//attackerpoints +=(0.0 -ha)


					//System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") , iter  " + iter);


					Node start = new Node(attackednode);

					PriorityQueue<Node> fringequeue = new PriorityQueue<Node>(1000, new Comparator<Node>() 
					{  
						public int compare(Node w1, Node w2) 
						{                         
							return (int)(w1.prob - w2.prob);
						}      
					});

					if(hardendenode.id==0 && attackednode.id==5)
					{
						int f=1;
					}

					ArrayList<Integer> closednodes = new ArrayList<Integer>();

					fringequeue.add(start);

					boolean[] visited = new boolean[nodes.size()];


					while(fringequeue.size()>0)
					{
						// poll a node
						Node curnode = fringequeue.poll();

						//System.out.println("polling node "+ curnode.id + ", v: "+ curnode.value);


						//System.out.println("Closed nodes ");

						//printClosedNodes(closednodes);



						if(curnode.id == hardendenode.id)
						{
							defenderpoints += - (curnode.getValue()+ curnode.defcost);

							attackerpoints = 0;
							//System.out.println("attackerpoints "+ attackerpoints);
							break;
						}

						defenderpoints += - (curnode.getValue()+ curnode.defcost);
						attackerpoints += (curnode.getValue()- curnode.attackercost);

						//System.out.println("attackerpoints "+ attackerpoints);
						//System.out.println("sumatt "+ sumatt);

						//System.out.println("polled node "+ curnode.id);
						//System.out.println("Adding points for node "+  curnode.id);

						Node curorignode = nodes.get(curnode.id);

						double beta = 1;
						double gamma = 1;

						if(curnode.id == hardendenode.id)
						{
							beta = 0.5;
							gamma = 0.5;
						}

						closednodes.add(curnode.id);

						for(Node nei: curorignode.neighbors.values())
						{

							// for every neighbor of that node make a decision
							// if the pollling node is the hardend one, then every outgoing prob will be reduced by beta
							// and the reward will be reduced by gamma
							// if went through then add the neighbor to the queue and sum the reward


							if(!closednodes.contains(nei.id) && visited[nei.id]==false)
							{

								double prob = curorignode.getTransitionProbs(nei);
								// generate a random double

								double r = rand.nextDouble();
								if(r<(prob*beta))
								{
									// the attack went through
									//attackerpoints += (nei.value*gamma);
									//defenderpoints += -((nei.value*gamma));
									Node newnode = new Node(nei);
									newnode.setValue(nei.value*gamma);
									newnode.depth = curnode.depth + 1;
									fringequeue.add(newnode);
									//System.out.print("Closed node : ");
									//printClosedNodes(closednodes);
									//System.out.println("Adding node "+ newnode.id + "\n");
									visited[newnode.id] = true;
								}
							}
						}

					}

					sumatt += attackerpoints;
					sumdef += defenderpoints;

					//	System.out.println("attackerpoints "+ attackerpoints);
					//	System.out.println("sumatt "+ sumatt);


				}

				sumatt /= LIMIT;
				sumdef /= LIMIT;

				System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") = " + sumatt);

				String key = hardendenode.id + ","+attackednode.id;

				attackerpayoffs.put(key, sumatt);
				defenderpayoffs.put(key, sumdef);


			}
		}



	}

	/*private static void printNodes(HashMap<Integer, Node> nodes) {


		for(Node n: nodes.values())
		{
			System.out.println("\n\n*******Node "+n.id+" *******");
			System.out.println("Id: "+ n.id);
			System.out.println("value: "+ n.value);
			System.out.println("Subnet id: "+ n.subnetid);
			//System.out.println("Neighbors : ");
			for(Node nei: n.transitionprobs.keySet())
			{
				System.out.println("Neighbor: "+ nei.id + ", prob: "+ n.transitionprobs.get(nei) + ", subnet: "+ nei.subnetid);
			}


		}


	}
	 */



	/*private static void doMonteCarloSimulation(HashMap<Integer, Node> nodes, HashMap<String,Double> defenderpayoffs, HashMap<String,Double> attackerpayoffs) {


		int LIMIT = 10000;

		HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
		HashMap<String, Double> defenderpayoffs = new  HashMap<String, Double>();

		double totalattpoints = 0;
		double totaldefpoints = 0;

		for(Node hardendenode : nodes.values())
		{
			//hardendenode.setHardended(true);
			for(Node attstartnode: nodes.values())
			{
				// harden the defended node

				if(hardendenode.id==6 && attstartnode.id==0)
				{
					int f=0;
				}


				double attackerpoints = 0.0;
				double defenderpoints = 0.0;

				for(int iter = 0; iter<=LIMIT; iter++)
				{

					// attackerpoints = 0.0;
					// defenderpoints = 0.0;

					defenderpoints += (0.0 - hardendenode.defcost);
					attackerpoints += (0.0 - hardendenode.attackercost);



					//System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") , iter  " + iter);


					Node start = new Node(attstartnode);
					start.setProb(1.0);

					Queue<Node> fringequeue = new LinkedList<Node>(); 


					ArrayList<Integer> closednodes = new ArrayList<Integer>();

					fringequeue.add(start);
					while(fringequeue.size()>0)
					{
						// poll a node
						Node curnode = fringequeue.poll();

						Node curorignode = nodes.get(curnode.id);

						double beta = 1;
						double gamma = 1;




						if(curnode.id == hardendenode.id)
						{
							beta = 0.5;
							gamma = 0.5;
						}

						double curprob = curnode.getProb();

						Random rand = new Random();
						double r = rand.nextDouble();
						if(r<(curprob*beta))
						{


							if(curnode.id == hardendenode.id)
							{
								defenderpoints += -((curnode.value*gamma));
								attackerpoints = 0;
								break;
							}
							else
							{
								attackerpoints += (curnode.value*gamma);
								defenderpoints += -((curnode.value*gamma));
							}


							closednodes.add(curnode.id);

							for(Node nei: curorignode.neighbors.values())
							{

								// for every neighbor of that node make a decision
								// if the pollling node is the hardend one, then every outgoing prob will be reduced by beta
								// and the reward will be reduced by gamma
								// if went through then add the neighbor to the queue and sum the reward


								if(!closednodes.contains(nei.id))
								{
									double prob = curorignode.getTransitionProbs(nei);
									// generate a random double

										// the attack went through
										//attackerpoints += (nei.value*gamma);
										//defenderpoints += -((nei.value*gamma));
										Node newnode = new Node(nei);
										newnode.depth = curnode.depth + 1;
										newnode.setProb(prob);
										newnode.setValue(nei.value);
										fringequeue.add(newnode);


								}
							}

						}


					}

					//totalattpoints += attackerpoints;
					//totaldefpoints += defenderpoints;

				}

				attackerpoints /= LIMIT;
				defenderpoints /= LIMIT;



				totalattpoints += attackerpoints;
				totaldefpoints += defenderpoints;

				System.out.println("("+hardendenode.id + ","+ attstartnode.id+ ") = " + totalattpoints);

				String key = hardendenode.id + ","+attstartnode.id;

				attackerpayoffs.put(key, totalattpoints);
				defenderpayoffs.put(key, totaldefpoints);


			}
		}



	}
	 */

	private static void printClosedNodes(ArrayList<Integer> closednodes) {


		for(Integer n: closednodes)
		{
			System.out.print(n+" ");
		}

		System.out.println();

	}



	/**
	 * 
	 * @param nsubnet
	 * @param numberofnodes
	 * @param nodesinsubnet
	 * @param intrasubtransmissionprob
	 * @param intrasubtransmissionprobnoise
	 * @param intersubtransmissionprob
	 * @param intersubtransmissionprobnoise
	 * @param nodevaluerange
	 * @param defcostrange
	 * @param attackercostrange
	 * @param intersubnetnumberofedgerange
	 * @param intrasubnetnumberofedgerange
	 * @param attackercostnoise 
	 * @param defendercostnoise 
	 * @param connectsubnets 
	 * @return
	 */
	private static HashMap<Integer, Node> createNetwork(int nsubnet, int numberofnodes, int[] nodesinsubnet,
			int[] intrasubtransmissionprob, double intrasubtransmissionprobnoise, int[] intersubtransmissionprob,
			double intersubtransmissionprobnoise, int[] nodevaluerange, int[] defcostrange, int[] attackercostrange,
			int[] intersubnetnumberofedgerange, int[] intrasubnetnumberofedgerange, int defendercostnoise, int attackercostnoise, boolean connectsubnets) {

		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();


		// for every subnet create nodes

		int nodeid = 0;

		for(int subnet = 0; subnet<nsubnet; subnet++)
		{
			for(int nodeindex=0; nodeindex<nodesinsubnet[subnet]; nodeindex++)
			{
				int nodeval = randInt(nodevaluerange[0], nodevaluerange[1]);
				int defcost = randInt(defcostrange[0], defcostrange[1]);
				int defcostnoise = randInt(0, defendercostnoise);
				int attcostnoise = randInt(0, attackercostnoise);
				int attackercost = randInt(attackercostrange[0], attackercostrange[1]);
				Node node = new Node(nodeid, subnet, nodeval, false, defcost+defcostnoise, attackercost+attcostnoise);
				nodes.put(nodeid, node);
				nodeid++;
			}
		}

		// now make the intra subnet connections

		for(int subnet=0; subnet<nsubnet; subnet++)
		{
			HashMap<Integer, Node> subnetnodes = getSubNetNodes(nodes, subnet);


			int nedge = randInt(intrasubnetnumberofedgerange[1], intrasubnetnumberofedgerange[1]);

			//int edgepernode = (int)Math.ceil(nedge/nodesinsubnet[subnet]);

			int totaledgedone = 0;
			ArrayList<int[]> donedges = new ArrayList<int[]>();

			

			for(Node n: subnetnodes.values())
			{
				int nodeedgecount = 0; 

				ArrayList<Node> notdonenodes = new ArrayList<Node>();

				for(Node t: subnetnodes.values())
				{
					if(t.id != n.id)
					{
						notdonenodes.add(t);
					}
				}

				// prob used for a subnet


				while(true)
				{
					// pick another node randomly

					int index = -1;

					if(notdonenodes.size()>0)
					{
						index = randInt(0, notdonenodes.size()-1);

						Node nei = notdonenodes.get(index);

						boolean isok = isOK(n, nei, donedges);

						if(isok)
						{

							double prob = randInt(intrasubtransmissionprob[0], intrasubtransmissionprob[1])/100.0;
							n.addNeighbor(nei);
							double probnoise = randInt(0, (int)intrasubtransmissionprobnoise)/100.0;
							n.setTransitionProbs(nei, prob-probnoise);

							nei.addNeighbor(n);
							nei.setTransitionProbs(n, prob-probnoise);


							nodeedgecount++;
							totaledgedone++;

							int [] e = {n.id, nei.id};
							donedges.add(e);
							notdonenodes.remove(index);
						}
						else
						{
							notdonenodes.remove(index);
						}
					}
					else
					{
						break;
					}



					if(totaledgedone==nedge)
						break;
				}
			}

			if(totaledgedone<nedge)
			{
				for(Node n: subnetnodes.values())
				{

					while(true)
					{
						// pick another node randomly


						for(Node nei: subnetnodes.values())
						{
							if(nei.id != n.id)
							{

								boolean isok = isOK(n, nei, donedges);
								if(isok)
								{
									double prob = randInt(intrasubtransmissionprob[0], intrasubtransmissionprob[1])/100.0;
									n.addNeighbor(nei);
									double probnoise = randInt(0, (int)intrasubtransmissionprobnoise)/100.0;
									n.setTransitionProbs(nei, prob-probnoise);

									nei.addNeighbor(n);
									nei.setTransitionProbs(n, prob-probnoise);
									totaledgedone++;

									int [] e = {n.id, nei.id};
									donedges.add(e);
									break;
								}
							}

						}
						if(totaledgedone==nedge)
							break;
					}
				}
			}



		}


		if(connectsubnets)
		{

			for(int subnet1=0; subnet1<nsubnet-1; subnet1++)
			{
				for(int subnet2=subnet1+1; subnet2<nsubnet; subnet2++)
				{

					if(subnet1 != subnet2)
					{
						HashMap<Integer, Node> subnet1nodes = getSubNetNodes(nodes, subnet1);
						HashMap<Integer, Node> subnet2nodes = getSubNetNodes(nodes, subnet2);



						ArrayList<Integer> s1nodes = new ArrayList<Integer>(subnet1nodes.keySet());
						ArrayList<Integer> s2nodes = new ArrayList<Integer>(subnet2nodes.keySet());



						int nedge = randInt(intersubnetnumberofedgerange[0], intersubnetnumberofedgerange[1]);


						int edgecount = 0;

						ArrayList<int[]> done = new ArrayList<int[]>();


						if(nedge>0)
						{

							while(true)
							{

								// pick two nodes randomly from two subnet and add them... 
								// add prob

								int index1 = randInt(0, s1nodes.size()-1);
								int index2 = randInt(0, s2nodes.size()-1);

								int n1 = s1nodes.get(index1);
								int  n2 = s2nodes.get(index2);

								boolean ok = isOK(n1, n2, done);

								if(ok)
								{
									Node n1node = subnet1nodes.get(n1);
									Node n2node = subnet2nodes.get(n2);


									double prob = randInt(intersubtransmissionprob[0], intersubtransmissionprob[0])/100.0;

									n1node.addNeighbor(n2node);
									n1node.setTransitionProbs(n2node, prob);


									n2node.addNeighbor(n1node);
									n2node.setTransitionProbs(n1node, prob);

									int[] e = {n1, n2};

									done.add(e);

									edgecount++;
								}


								if(edgecount==nedge)
									break;
							} // end while loop
						}


					}
				}
			}


		}





		return nodes;



	}

	private static boolean isOK(int n1, int n2, ArrayList<int[]> done) {

		for(int[] e: done)
		{
			if((e[0]==n1 && e[1]==n2) || (e[1]==n1 && e[0]==n2))
				return false;
		}


		return true;
	}


	private static boolean isOK(Node n, Node nei, ArrayList<int[]> donedges) {



		for(int[] e: donedges)
		{
			if((e[0]==n.id && e[1]==nei.id) || (e[1]==n.id && e[0]==nei.id))
				return false;
		}


		return true;
	}


	private static HashMap<Integer, Node> getSubNetNodes(HashMap<Integer, Node> nodes, int subnet) {


		HashMap<Integer, Node> subnodes = new HashMap<Integer, Node>();


		for(Node n: nodes.values())
		{
			if(n.subnetid==subnet)
			{
				subnodes.put(n.id, n);
			}
		}

		return subnodes;

	}


	public static int randInt(int min, int max) {

		// Usually this should be a field rather than a method variable so
		// that it is not re-seeded every call.


		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	private static void buildGameFile(int[] naction, HashMap<String, Double> defenderpayoffs,
			HashMap<String, Double> attackerpayoffs, int gamenumber, int iter) throws FileNotFoundException {


		MatrixGame game = new MatrixGame(2, naction);

		PrintWriter pw1 = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+gamenumber+"-"+iter+".csv"),true));


		for(int i=0; i<naction[0]; i++)
		{
			for(int j=0; j<naction[1]; j++)
			{
				String key = (i) +","+(j);
				double defval = defenderpayoffs.get(key);
				double attval = attackerpayoffs.get(key);
				int[] outcome = {i+1, j+1};

				double payoffs[] = {defval, attval};

				game.setPayoffs(outcome, payoffs);

				//System.out.print(payoffs[0]+","+payoffs[1] + "  ");

				pw1.append(payoffs[0]+"|"+payoffs[1] + ",");

			}
			//System.out.println();
			pw1.append("\n");
		}

		pw1.close();




		String gamename = Parameters.GAME_FILES_PATH+gamenumber+"-"+iter+Parameters.GAMUT_GAME_EXTENSION;

		//	String gamename = Parameters.GAME_FILES_PATH+gamenumber+"-"+size+"-"+delta+Parameters.GAMUT_GAME_EXTENSION;
		//String gmname = "k"+this.numberofclusters[0]+"-"+this.gamename;

		try{

			PrintWriter pw = new PrintWriter(gamename,"UTF-8");
			SimpleOutput.writeGame(pw,game);
			pw.close();
		}
		catch(Exception ex){
			System.out.println("StrategyMapping class :something went terribly wrong during clustering abstraction ");
		}



	}



	private static void printNodes(HashMap<Integer, Node> nodes) {


		for(Node n: nodes.values())
		{
			System.out.println("\n\n*******Node "+n.id+" *******");
			System.out.println("Id: "+ n.id);
			System.out.println("value: "+ n.value);
			System.out.println("Subnet id: "+ n.subnetid);
			//System.out.println("Neighbors : ");
			for(Node nei: n.transitionprobs.keySet())
			{
				System.out.println("Neighbor: "+ nei.id + ", prob: "+ n.transitionprobs.get(nei) + ", subnet: "+ nei.subnetid);
			}


		}


	}




	private static HashMap<Integer, Node> createNetwork() {

		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();


		Node a = new Node(0, 0, 10, false, 0, 0);
		Node b = new Node(1, 0, 9, false, 0, 0);
		Node c = new Node(2, 0, 8, false, 0, 0);

		a.addNeighbor(b);
		a.setTransitionProbs(b, 0.7);
		b.addNeighbor(a);
		b.setTransitionProbs(a, 0.7);


		a.addNeighbor(c);
		a.setTransitionProbs(c, 0.8);
		c.addNeighbor(a);
		c.setTransitionProbs(a, 0.8);



		c.addNeighbor(b);
		c.setTransitionProbs(b, 0.9);
		b.addNeighbor(c);
		b.setTransitionProbs(c, 0.9);







		Node d = new Node(3, 1, 7, false, 0, 0);
		Node e = new Node(4, 1, 8, false, 0, 0);
		Node f = new Node(5, 1, 9, false, 0, 0);

		d.addNeighbor(e);
		d.setTransitionProbs(e, 0.7);
		e.addNeighbor(d);
		e.setTransitionProbs(d, 0.7);



		d.addNeighbor(f);
		d.setTransitionProbs(f, 0.7);
		f.addNeighbor(d);
		f.setTransitionProbs(d, 0.7);


		e.addNeighbor(f);
		e.setTransitionProbs(f, 0.8);
		f.addNeighbor(e);
		f.setTransitionProbs(e, 0.8);







		Node g = new Node(6, 2, 7, false, 0, 0);
		Node h = new Node(7, 2, 9, false, 0, 0);
		Node i = new Node(8, 2, 8, false, 0, 0);



		g.addNeighbor(h);
		g.setTransitionProbs(h, 0.7);
		h.addNeighbor(g);
		h.setTransitionProbs(g, 0.7);


		g.addNeighbor(i);
		g.setTransitionProbs(i, 0.8);
		i.addNeighbor(g);
		i.setTransitionProbs(g, 0.8);



		h.addNeighbor(i);
		h.setTransitionProbs(i, 0.9);
		i.addNeighbor(h);
		i.setTransitionProbs(h, 0.9);



		c.addNeighbor(d);
		c.setTransitionProbs(d, 0.2);
		d.addNeighbor(c);
		d.setTransitionProbs(c, 0.2);



		f.addNeighbor(g);
		f.setTransitionProbs(g, 0.1);
		g.addNeighbor(f);
		g.setTransitionProbs(f, 0.1);



		nodes.put(a.id, a);
		nodes.put(b.id, b);
		nodes.put(c.id, c);
		nodes.put(d.id, d);

		nodes.put(e.id, e);
		nodes.put(f.id, f);
		nodes.put(g.id, g);
		nodes.put(h.id, h);
		nodes.put(i.id, i);






		return nodes;

	}



	public static void transmissionExp() throws Exception {


		int ITER_LIMIT = 1;
		int naction = 9;
		int nplayer = 2;
		int ncluster = 3;
		boolean connectsubgames = true;
		// set up the game parameters before experiments
		buildExperimentGames(ITER_LIMIT, naction, nplayer, ncluster, connectsubgames);

		// do the exp

		GameReductionBySubGame.transmissionExp(ITER_LIMIT, naction, nplayer, ncluster);


	}





	public static void buildExperimentGames(int ITER_LIMIT, int naction2, int nplayer, int ncluster, boolean connectsubnets) throws FileNotFoundException
	{
		// create game files

		//int ITER_LIMIT = 5;
		int[] naction = {naction2,naction2};

		// number of subnet
		int nsubnet = ncluster;
		int numberofnodes = naction2;
		int nodesinsubnet[] = new int[nsubnet];
		int intrasubtransmissionprob[] = {60, 100}; //make this 2d
		double intrasubtransmissionprobnoise = 0;
		int intersubtransmissionprob[] = {10, 30}; // make this 2d/3d
		double intersubtransmissionprobnoise = 0; // no need
		int[] nodevaluerange = {5, 10};
		int[] defcostrange = {1,3}; // make this 2d
		int defendercostnoise = 0;
		int[] attackercostrange = {1,3}; // make this 2d
		int attackercostnoise = 0;
		int[] intersubnetnumberofedgerange = {1,1}; //make this 3d/2d
		int nsub = numberofnodes/nsubnet;
		int min = (nsub-1)*(nsub-2)/2 + 1;
		int max = nsub*(nsub-1)/2;
		int[] intrasubnetnumberofedgerange = {min, max};
		//int iter=0;

		for(int i=0; i<nsubnet; i++)
		{
			nodesinsubnet[i] = numberofnodes/nsubnet;
		}


		//boolean connectsubnets = false;

		for(int iter=0; iter<ITER_LIMIT; iter++)
		{

			HashMap<Integer, Node> nodes = createNetwork(nsubnet, numberofnodes, nodesinsubnet, intrasubtransmissionprob, intrasubtransmissionprobnoise, intersubtransmissionprob, 
					intersubtransmissionprobnoise, nodevaluerange, defcostrange, attackercostrange, 
					intersubnetnumberofedgerange, intrasubnetnumberofedgerange, defendercostnoise, attackercostnoise, connectsubnets);		

			//printNodes(nodes);
			printSubNets(nodes, nsubnet);

			//HashMap<Integer, Node> nodes = createNetwork();
			//printNodes(nodes);

			HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
			HashMap<String, Double> defenderpayoffs = new  HashMap<String, Double>();
			doMonteCarloSimulation(nodes, defenderpayoffs, attackerpayoffs);
			buildGameFile(naction, defenderpayoffs, attackerpayoffs, naction[0], iter);
		}

	}



	private static void printSubNets(HashMap<Integer, Node> nodes, int nsubnet) {


		for(int isub=0; isub<nsubnet; isub++)
		{

			for(Node n: nodes.values())
			{
				if(n.subnetid==isub)
				{
					System.out.println("\n\n*******Node "+n.id+" *******");
					System.out.println("Id: "+ n.id);
					System.out.println("value: "+ n.value);
					System.out.println("Subnet id: "+ n.subnetid);
					//System.out.println("Neighbors : ");
					for(Node nei: n.transitionprobs.keySet())
					{
						System.out.println("Neighbor: "+ nei.id + ", prob: "+ n.transitionprobs.get(nei) + ", subnet: "+ nei.subnetid);
					}
				}


			}
		}

	}



	public static void deltaExp() throws Exception {


		int ITER_LIMIT = 1;
		int naction = 100;
		int nplayer = 2;
		int ncluster = 10;
		boolean connectsubnets = true;
		// set up the game parameters before experiments
		buildExperimentGames(ITER_LIMIT, naction, nplayer, ncluster, connectsubnets);


		GameReductionBySubGame.deltaExp(nplayer, ncluster, naction, ITER_LIMIT);
		
		
		GameReductionBySubGame.transmissionExp(ITER_LIMIT, naction, nplayer, ncluster);


	}






}


class Node {


	int id;  // unique across the network
	int subnetid;
	double value;
	int defcost;
	int attackercost;
	boolean hardended= false;

	int depth = 0;

	double prob = 0;


	public double getProb() {
		return prob;
	}

	public void setProb(double prob) {
		this.prob = prob;
	}

	HashMap<Integer, Node> neighbors =  new HashMap<Integer, Node>();
	HashMap<Node, Double> transitionprobs = new HashMap<Node, Double>();



	public Node(int id, int subnetid, int value, boolean hardended, int defcost, int attackercost) {
		super();
		this.id = id;
		this.subnetid = subnetid;
		this.value = value;
		this.hardended = hardended;
		this.defcost = defcost;
		this.attackercost = attackercost;
	}

	public Node(Node node)
	{
		this.id = node.id;
		this.subnetid = node.subnetid;
		this.value = node.value;
		this.hardended = node.hardended;
		this.defcost = node.defcost;
		this.attackercost = node.attackercost;

		for(Node nei: node.getNeighbors().values())
		{
			this.neighbors.put(nei.id, nei);
		}

		for(Node nei: node.transitionprobs.keySet())
		{
			this.transitionprobs.put(nei, node.transitionprobs.get(nei));
		}

	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getSubnetid() {
		return subnetid;
	}
	public void setSubnetid(int subnetid) {
		this.subnetid = subnetid;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}

	public void addNeighbor(Node nei)
	{
		this.neighbors.put(nei.id, nei);
	}

	public Node getNeighbor(int id)
	{
		return this.neighbors.get(id);
	}

	public HashMap<Integer, Node> getNeighbors()
	{
		return this.neighbors;
	}

	public boolean isHardended() {
		return hardended;
	}
	public void setHardended(boolean hardended) {
		this.hardended = hardended;
	}

	public double getTransitionProbs(Node node)
	{
		return this.transitionprobs.get(node);
	}

	public void setTransitionProbs(Node node, double prob)
	{
		this.transitionprobs.put(node, prob);
	}

}
