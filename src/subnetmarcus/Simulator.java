package subnetmarcus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;




public class Simulator {

	public static ArrayList<Node> nodes;
	public static ArrayList<ArrayList<Edge>> edges;
	public static Random rand = new Random();

	public static void initialize(){
		nodes = new ArrayList<Node>();
		nodes.add(new Node(0, 1, 6.0, 0.0, 1.0)); //a1
		nodes.add(new Node(1, 1, 3.0, 0.0, 1.0)); //a2
		nodes.add(new Node(2, 1, 5.0, 0.0, 1.0)); //a3
		nodes.add(new Node(3, 2, 9.0, 0.0, 2.0)); //b1
		nodes.add(new Node(4, 2, 4.0, 0.0, 2.0)); //b2
		nodes.add(new Node(5, 2, 8.0, 0.0, 2.0)); //b3
		nodes.add(new Node(6, 3, 5.0, 0.0, 3.0)); //c1
		nodes.add(new Node(7, 3, 5.0, 0.0, 3.0)); //c2
		nodes.add(new Node(8, 3, 7.0, 0.0, 3.0)); //c3

		edges = new ArrayList<ArrayList<Edge>>();
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());
		edges.add(new ArrayList<Edge>());

		//Subnet A
		edges.get(0).add(new Edge(0, 1, 0.80));
		edges.get(1).add(new Edge(1, 0, 0.80));
		edges.get(0).add(new Edge(0, 2, 0.60));
		edges.get(2).add(new Edge(2, 0, 0.60));
		edges.get(1).add(new Edge(1, 2, 0.90));
		edges.get(2).add(new Edge(2, 1, 0.90));

		//Subnet B
		edges.get(3).add(new Edge(3, 4, 1.00));
		edges.get(4).add(new Edge(4, 3, 1.00));
		edges.get(3).add(new Edge(3, 5, 0.75));
		edges.get(5).add(new Edge(5, 3, 0.75));
		edges.get(4).add(new Edge(4, 5, 0.90));
		edges.get(5).add(new Edge(5, 4, 0.90));

		//Subnet C
		edges.get(6).add(new Edge(6, 7, 0.80));
		edges.get(7).add(new Edge(7, 6, 0.80));
		edges.get(6).add(new Edge(6, 8, 0.90));
		edges.get(8).add(new Edge(8, 6, 0.90));
		edges.get(7).add(new Edge(7, 8, 0.80));
		edges.get(8).add(new Edge(8, 7, 0.80));

		//Edge(a2,b2)
		edges.get(1).add(new Edge(1, 4, 0.2));
		edges.get(4).add(new Edge(4, 1, 0.2));

		//Edge(a2,c2)
		edges.get(1).add(new Edge(1, 7, 0.2));
		edges.get(7).add(new Edge(7, 1, 0.2));

		//Edge(a3,b1)
		//edges.get(2).add(new Edge(2, 3, 0.2));
		//edges.get(3).add(new Edge(3, 2, 0.2));
		/*
		//Edge(b2,c2)
		edges.get(4).add(new Edge(4, 7, 0.2));
		edges.get(7).add(new Edge(7, 4, 0.2));

		//Edge(a1,b3)
		edges.get(0).add(new Edge(0, 5, 0.1));
		edges.get(5).add(new Edge(5, 0, 0.1));

		//Edge(b1,c1)
		edges.get(3).add(new Edge(3, 6, 0.3));
		edges.get(6).add(new Edge(6, 3, 0.3));
		 */
	}
	
	
	public static int randInt(int min, int max) {

		// Usually this should be a field rather than a method variable so
		// that it is not re-seeded every call.


		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
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
	public static void createNetworkV2(int nsubnet, int numberofnodes, int[] nodesinsubnet,
			int[] intrasubtransmissionprob, double intrasubtransmissionprobnoise, int[] intersubtransmissionprob,
			double intersubtransmissionprobnoise, int[] nodevaluerange, int[] defcostrange, int[] attackercostrange,
			int[] intersubnetnumberofedgerange, int[] intrasubnetnumberofedgerange, int defendercostnoise, int attackercostnoise, boolean connectsubnets) {

		nodes = new ArrayList<Node>();


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
				Node node = new Node(nodeid, subnet, nodeval, attackercost+attcostnoise, defcost+defcostnoise);
				nodes.add(node);
				nodeid++;
			}
		}
		
		
		edges = new ArrayList<ArrayList<Edge>>();
		
		for (int n=0; n<numberofnodes; n++)
		{
			edges.add(new ArrayList<Edge>());
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
					if(t.nodeId != n.nodeId)
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
							//n.addNeighbor(nei);
							
							double probnoise = randInt(0, (int)intrasubtransmissionprobnoise)/100.0;
							//n.setTransitionProbs(nei, prob-probnoise);
							
							edges.get(n.nodeId).add(new Edge(n.nodeId, nei.nodeId, prob-probnoise));
							

							//nei.addNeighbor(n);
							//nei.setTransitionProbs(n, prob-probnoise);


							nodeedgecount++;
							totaledgedone++;

							int [] e = {n.nodeId, nei.nodeId};
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
							if(nei.nodeId != n.nodeId)
							{

								boolean isok = isOK(n, nei, donedges);
								if(isok)
								{
									double prob = randInt(intrasubtransmissionprob[0], intrasubtransmissionprob[1])/100.0;
									//n.addNeighbor(nei);
									double probnoise = randInt(0, (int)intrasubtransmissionprobnoise)/100.0;
									//n.setTransitionProbs(nei, prob-probnoise);
									
									edges.get(n.nodeId).add(new Edge(n.nodeId, nei.nodeId, prob-probnoise));

									//nei.addNeighbor(n);
									//nei.setTransitionProbs(n, prob-probnoise);
									totaledgedone++;

									int [] e = {n.nodeId, nei.nodeId};
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

									/*n1node.addNeighbor(n2node);
									n1node.setTransitionProbs(n2node, prob);


									n2node.addNeighbor(n1node);
									n2node.setTransitionProbs(n1node, prob);*/
									
									edges.get(n1node.nodeId).add(new Edge(n1node.nodeId, n2node.nodeId, prob));
									

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





		//return nodes;



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
			if((e[0]==n.nodeId && e[1]==nei.nodeId) || (e[1]==n.nodeId && e[0]==nei.nodeId))
				return false;
		}


		return true;
	}
	
	
	private static HashMap<Integer, Node> getSubNetNodes(ArrayList<Node> nodes, int subnet) {


		HashMap<Integer, Node> subnodes = new HashMap<Integer, Node>();


		for(Node n: nodes)
		{
			if(n.subnet==subnet)
			{
				subnodes.put(n.nodeId, n);
			}
		}

		return subnodes;

	}
	

	public ArrayList<Node> cloneNodes(ArrayList<Node> nodes){
		ArrayList<Node> newNodes = new ArrayList<Node>();
		for(Node n : nodes){
			newNodes.add(n.clone());
		}
		return newNodes;
	}

	public static ArrayList<ArrayList<Edge>> hardenNetwork(ArrayList<ArrayList<Edge>> edges, int nodeId){
		ArrayList<ArrayList<Edge>> hardenedNetwork = new ArrayList<ArrayList<Edge>>();
		for(ArrayList<Edge> outList : edges){
			hardenedNetwork.add(new ArrayList<Edge>());
			for(Edge e : outList){
				Edge clonedEdge = e.clone();
				hardenedNetwork.get(e.v1).add(clonedEdge);
				if(e.v1 == nodeId || e.v2 == nodeId){ //Half incoming and outgoing probabilities
					clonedEdge.probability /= 2.0;
				}
			}
		}
		return hardenedNetwork;
	}

	public void simulate(){
		initialize();
		for(int defMove = 0; defMove < nodes.size(); defMove++)
		{
			for(int atkMove = 0; atkMove < nodes.size(); atkMove++)
			{
				ArrayList<Node> nodes = cloneNodes(this.nodes);
				//nodes.get(defMove).value = 0.0 - nodes.get(defMove).value;
				//nodes.get(defMove).value /= 2.0;
				ArrayList<ArrayList<Edge>> edges = hardenNetwork(this.edges, defMove);

				double defenderPayoff = -nodes.get(defMove).defenderCost; //Start with cost
				double attackerPayoff = -nodes.get(atkMove).attackerCost; //Start with cost

				double defSum = 0.0;
				double atkSum = 0.0;
				for(int i = 0; i < 10000; i++)
				{
					boolean[] controlled = Subnetwork.simulate(nodes, edges, atkMove, defMove);
					for(int j = 0; j < controlled.length; j++)
					{
						if(controlled[j] == true)
							defSum -= nodes.get(j).value;
					}
					if(controlled[defMove] == false)
					{
						for(int j = 0; j < controlled.length; j++)
						{
							if(controlled[j] == true && (j!= defMove))
								atkSum += nodes.get(j).value;
						}
					}
					//atkSum += nodes.get(defMove).value;
					//sum += Subnetwork.simulate(nodes, edges, atkMove);
				}
				double defAvg = defSum / 10000.00;
				double atkAvg = atkSum / 10000.00;
				defenderPayoff += defAvg;
				attackerPayoff += atkAvg;
				//System.out.println("Expected of (" + defMove + "," + atkMove + "): " + "(" + defenderPayoff + "," + attackerPayoff + ")");
				System.out.print(defenderPayoff + "," +attackerPayoff +" ");
				if(atkMove < nodes.size()-1)
					System.out.print("\t");
			}
			System.out.println();
		}
	}
	
	
	public void simulateMonteCarlo(HashMap<String,Double> defenderpayoffs, HashMap<String,Double> attackerpayoffs){
		
		for(int defMove = 0; defMove < nodes.size(); defMove++)
		{
			for(int atkMove = 0; atkMove < nodes.size(); atkMove++)
			{
				ArrayList<Node> nodes = cloneNodes(this.nodes);
				//nodes.get(defMove).value = 0.0 - nodes.get(defMove).value;
				//nodes.get(defMove).value /= 2.0;
				ArrayList<ArrayList<Edge>> edges = hardenNetwork(this.edges, defMove);

				double defenderPayoff = -nodes.get(defMove).defenderCost; //Start with cost
				double attackerPayoff = -nodes.get(atkMove).attackerCost; //Start with cost

				double defSum = 0.0;
				double atkSum = 0.0;
				for(int i = 0; i < 10000; i++)
				{
					boolean[] controlled = Subnetwork.simulate(nodes, edges, atkMove, defMove);
					for(int j = 0; j < controlled.length; j++)
					{
						if(controlled[j] == true)
							defSum -= nodes.get(j).value;
					}
					if(controlled[defMove] == false)
					{
						for(int j = 0; j < controlled.length; j++)
						{
							if(controlled[j] == true && (j!= defMove))
								atkSum += nodes.get(j).value;
						}
					}
					//atkSum += nodes.get(defMove).value;
					//sum += Subnetwork.simulate(nodes, edges, atkMove);
				}
				double defAvg = defSum / 10000.00;
				double atkAvg = atkSum / 10000.00;
				defenderPayoff += defAvg;
				attackerPayoff += atkAvg;
				//System.out.println("Expected of (" + defMove + "," + atkMove + "): " + "(" + defenderPayoff + "," + attackerPayoff + ")");
				
				String key = defMove + ","+atkMove;

				attackerpayoffs.put(key, atkAvg);
				defenderpayoffs.put(key, defAvg);
				
				System.out.print(defenderPayoff + "," +attackerPayoff +" ");
				if(atkMove < nodes.size()-1)
					System.out.print("\t");
			}
			System.out.println();
		}
	}

	public static void main(String[] args){
		Simulator s = new Simulator();
		s.simulate();
	}

}
