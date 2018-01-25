package subnetmarcus;
import java.util.ArrayList;


public class Simulator {

	public static ArrayList<Node> nodes;
	public static ArrayList<ArrayList<Edge>> edges;

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
				for(int i = 0; i < 10000; i++){
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

	public static void main(String[] args){
		Simulator s = new Simulator();
		s.simulate();
	}

}
