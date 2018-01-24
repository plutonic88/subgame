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
import subgame.Parameters;



public class SubNet {

	public static void doExp() throws FileNotFoundException {


		int[] naction = {9,9};

		// number of subnet
		int nsubnet = 3;
		int numberofnodes = 9;
		int nodesinsubnet[] = {numberofnodes/nsubnet, numberofnodes/nsubnet, numberofnodes/nsubnet};
		int intrasubtransmissionprob[] = {60, 100};
		double intrasubtransmissionprobnoise = 0;
		int intersubtransmissionprob[] = {10, 20};
		double intersubtransmissionprobnoise = 0;
		int[] nodevaluerange = {5, 10};
		int[] defcostrange = {2,3};
		int[] attackercostrange = {2,3};
		int[] intersubnetnumberofedgerange = {1,2};
		int nsub = numberofnodes/nsubnet;
		int min = (nsub-1)*(nsub-2)/2 + 1;
		int max = nsub*(nsub-1)/2;
		int[] intrasubnetnumberofedgerange = {min, max};



		HashMap<Integer, Node> nodes2 = createNetwork(nsubnet, numberofnodes, nodesinsubnet, intrasubtransmissionprob, intrasubtransmissionprobnoise, intersubtransmissionprob, 
				intersubtransmissionprobnoise, nodevaluerange, defcostrange, attackercostrange, intersubnetnumberofedgerange, intrasubnetnumberofedgerange);		

		printNodes(nodes2);


		HashMap<Integer, Node> nodes = createNetwork();
		printNodes(nodes);

		HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
		HashMap<String, Double> defenderpayoffs = new  HashMap<String, Double>();
		doMonteCarloSimulation(nodes, defenderpayoffs, attackerpayoffs);
		buildGameFile(naction, defenderpayoffs, attackerpayoffs, 9);


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
	 * @return
	 */
	private static HashMap<Integer, Node> createNetwork(int nsubnet, int numberofnodes, int[] nodesinsubnet,
			int[] intrasubtransmissionprob, double intrasubtransmissionprobnoise, int[] intersubtransmissionprob,
			double intersubtransmissionprobnoise, int[] nodevaluerange, int[] defcostrange, int[] attackercostrange,
			int[] intersubnetnumberofedgerange, int[] intrasubnetnumberofedgerange) {

		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();


		// for every subnet create nodes

		int nodeid = 0;

		for(int subnet = 0; subnet<nsubnet; subnet++)
		{
			for(int nodeindex=0; nodeindex<nodesinsubnet[subnet]; nodeindex++)
			{
				int nodeval = randInt(nodevaluerange[0], nodevaluerange[1]);
				int defcost = randInt(defcostrange[0], defcostrange[1]);
				int attackercost = randInt(attackercostrange[0], attackercostrange[1]);
				Node node = new Node(nodeid, subnet, nodeval, false, defcost, attackercost);
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

								n.addNeighbor(nei);
								double prob = randInt(intrasubtransmissionprob[0], intrasubtransmissionprob[1])/100.0;
								n.setTransitionProbs(nei, prob);

								nei.addNeighbor(n);
								nei.setTransitionProbs(n, prob);


								nodeedgecount++;
								totaledgedone++;

								int [] e = {n.id, nei.id};
								donedges.add(e);
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

									n.addNeighbor(nei);
									double prob = randInt(intrasubtransmissionprob[0], intrasubtransmissionprob[1])/100.0;
									n.setTransitionProbs(nei, prob);

									nei.addNeighbor(n);
									nei.setTransitionProbs(n, prob);
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
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	private static void buildGameFile(int[] naction, HashMap<String, Double> defenderpayoffs,
			HashMap<String, Double> attackerpayoffs, int gamenumber) throws FileNotFoundException {


		MatrixGame game = new MatrixGame(2, naction);

		PrintWriter pw1 = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+gamenumber+".csv"),true));


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

				System.out.print(payoffs[0]+","+payoffs[1] + "  ");

				pw1.append(payoffs[0]+"|"+payoffs[1] + ",");

			}
			System.out.println();
			pw1.append("\n");
		}

		pw1.close();




		String gamename = Parameters.GAME_FILES_PATH+gamenumber+Parameters.GAMUT_GAME_EXTENSION;

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

	private static void doMonteCarloSimulation(HashMap<Integer, Node> nodes, HashMap<String,Double> defenderpayoffs, HashMap<String,Double> attackerpayoffs) {


		int LIMIT = 100000;

		/*HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
		HashMap<String, Double> defenderpayoffs = new  HashMap<String, Double>();*/


		for(Node hardendenode : nodes.values())
		{
			//hardendenode.setHardended(true);
			for(Node attackednode: nodes.values())
			{
				// harden the defended node

				double attackerpoints = 0.0;
				double defenderpoints = 0.0;

				for(int iter = 0; iter<=LIMIT; iter++)
				{

					defenderpoints += (0.0 - hardendenode.defcost);



					//System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") , iter  " + iter);


					Node start = new Node(attackednode);

					PriorityQueue<Node> fringequeue = new PriorityQueue<Node>(1000, new Comparator<Node>() 
					{  
						public int compare(Node w1, Node w2) 
						{                         
							return (int)(w1.depth - w2.depth);
						}      
					});

					ArrayList<Integer> closednodes = new ArrayList<Integer>();

					fringequeue.add(start);
					while(fringequeue.size()>0)
					{
						// poll a node
						Node curnode = fringequeue.poll();
						closednodes.add(curnode.id);

						//System.out.println("polled node "+ curnode.id);

						Node curorignode = nodes.get(curnode.id);

						double beta = 1;
						double gamma = 1;

						if(curnode.id == hardendenode.id)
						{
							beta = 0.5;
							gamma = 0.5;
						}

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
								Random rand = new Random();
								double r = rand.nextDouble();
								if(r<(prob*beta))
								{
									// the attack went through
									attackerpoints += (nei.value*gamma);
									defenderpoints += -((nei.value*gamma));
									Node newnode = new Node(nei);
									newnode.depth = curnode.depth + 1;
									fringequeue.add(newnode);
								}
							}
						}

					}

				}

				attackerpoints /= LIMIT;
				defenderpoints /= LIMIT;

				System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") = " + attackerpoints);

				String key = hardendenode.id + ","+attackednode.id;

				attackerpayoffs.put(key, attackerpoints);
				defenderpayoffs.put(key, defenderpoints);


			}
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


		Node a = new Node(0, 0, 10, false, 2, 2);
		Node b = new Node(1, 0, 4, false, 1, 1);
		Node c = new Node(2, 0, 6, false, 3, 3);

		a.addNeighbor(b);
		a.setTransitionProbs(b, 0.7);
		b.addNeighbor(a);
		b.setTransitionProbs(a, 0.7);


		a.addNeighbor(c);
		a.setTransitionProbs(c, 0.8);
		c.addNeighbor(a);
		c.setTransitionProbs(a, 0.8);



		c.addNeighbor(b);
		c.setTransitionProbs(b, 0.4);
		b.addNeighbor(c);
		b.setTransitionProbs(c, 0.4);







		Node d = new Node(3, 1, 7, false, 1, 1);
		Node e = new Node(4, 1, 8, false, 2, 2);
		Node f = new Node(5, 1, 9, false, 3, 3);

		d.addNeighbor(e);
		d.setTransitionProbs(e, 0.5);
		e.addNeighbor(d);
		e.setTransitionProbs(d, 0.5);



		d.addNeighbor(f);
		d.setTransitionProbs(f, 0.7);
		f.addNeighbor(d);
		f.setTransitionProbs(d, 0.7);


		e.addNeighbor(f);
		e.setTransitionProbs(f, 0.65);
		f.addNeighbor(e);
		f.setTransitionProbs(e, 0.65);







		Node g = new Node(6, 2, 5, false, 1, 1);
		Node h = new Node(7, 2, 9, false, 3, 3);
		Node i = new Node(8, 2, 6, false, 2, 2);



		g.addNeighbor(h);
		g.setTransitionProbs(h, 0.6);
		h.addNeighbor(g);
		h.setTransitionProbs(g, 0.6);


		g.addNeighbor(i);
		g.setTransitionProbs(i, 0.8);
		i.addNeighbor(g);
		i.setTransitionProbs(g, 0.8);



		h.addNeighbor(i);
		h.setTransitionProbs(i, 0.9);
		i.addNeighbor(h);
		i.setTransitionProbs(h, 0.9);



		/*c.addNeighbor(d);
		c.setTransitionProbs(d, 0.2);
		d.addNeighbor(c);
		d.setTransitionProbs(c, 0.2);



		f.addNeighbor(g);
		f.setTransitionProbs(g, 0.1);
		g.addNeighbor(f);
		g.setTransitionProbs(f, 0.1);*/



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





}


class Node {


	int id;  // unique across the network
	int subnetid;
	int value;
	int defcost;
	int attackercost;
	boolean hardended= false;

	int depth = 0;


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
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
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
