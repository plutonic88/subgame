package subnetmarcus;




import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


public class Subnetwork {
	
	public ArrayList<Node> nodes;
	public ArrayList<ArrayList<Edge>> edges;
	
	public Subnetwork(){
		nodes = new ArrayList<Node>();
		
		nodes.add(new Node(0, 1, 6.0, 0.0, 0.0));
		nodes.add(new Node(1, 1, 8.0, 0.0, 0.0));
		nodes.add(new Node(2, 1, 6.0, 0.0, 0.0));
		nodes.add(new Node(3, 2, 9.0, 0.0, 0.0));
		nodes.add(new Node(4, 2, 6.0, 0.0, 0.0));
		nodes.add(new Node(5, 2, 8.0, 0.0, 0.0));
		nodes.add(new Node(6, 3, 5.0, 0.0, 0.0));
		nodes.add(new Node(7, 3, 10.0, 0.0, 0.0));
		nodes.add(new Node(8, 3, 7.0, 0.0, 0.0));
		
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
		edges.get(0).add(new Edge(0, 1, 0.9));
		edges.get(1).add(new Edge(1, 0, 0.9));
		edges.get(0).add(new Edge(0, 2, 0.8));
		edges.get(2).add(new Edge(2, 0, 0.8));
		edges.get(1).add(new Edge(1, 2, 0.9));
		edges.get(2).add(new Edge(2, 1, 0.9));
		
		//Subnet B
		edges.get(3).add(new Edge(3, 4, 0.7));
		edges.get(4).add(new Edge(4, 3, 0.7));
		edges.get(3).add(new Edge(3, 5, 0.75));
		edges.get(5).add(new Edge(5, 3, 0.75));
		edges.get(4).add(new Edge(4, 5, 0.85));
		edges.get(5).add(new Edge(5, 4, 0.85));
		
		//Subnet C
		edges.get(6).add(new Edge(6, 7, 0.6));
		edges.get(7).add(new Edge(7, 6, 0.6));
		edges.get(6).add(new Edge(6, 8, 0.8));
		edges.get(8).add(new Edge(8, 6, 0.8));
		edges.get(7).add(new Edge(7, 8, 0.9));
		edges.get(8).add(new Edge(8, 7, 0.9));
	}
	
	public Subnetwork(ArrayList<Node> nodes, ArrayList<ArrayList<Edge>> edges){
		this.nodes = new ArrayList<Node>();
		for(Node n : nodes){
			this.nodes.add(n.clone());
		}
		
		this.edges = new ArrayList<ArrayList<Edge>>();
		for(ArrayList<Edge> outList : edges){
			this.edges.add(new ArrayList<Edge>());
			for(Edge e : outList){
				this.edges.get(e.v1).add(e.clone());
			}
		}
	}
	
	public static double[][] transmission = 
			{
//			|------A------||------B------||------C------|
//			  a1   a2   a3   b1   b2   b3   c1   c2   c3
			{0.0, 0.9, 0.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},	//a1
			{0.9, 0.0, 0.9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},	//a2
			{0.8, 0.9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},	//a3
			{0.0, 0.0, 0.0, 0.0, 0.7, 0.75, 0.0, 0.0, 0.0},	//b1
			{0.0, 0.0, 0.0, 0.7, 0.0, 0.85, 0.0, 0.0, 0.0},	//b2
			{0.0, 0.0, 0.0, 0.75, 0.85, 0.0, 0.0, 0.0, 0.0},//b3
			{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.6, 0.8},	//c1
			{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.6, 0.0, 0.9},	//c2
			{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.8, 0.9, 0.0},	//c3
			};
	public static double[] values = {6.0, 8.0, 6.0, 9.0, 6.0, 8.0, 5.0, 10.0, 7.0};
	public static double[] attackerCosts = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	public static double[] defenderCosts = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	public static boolean[] simulate(ArrayList<Node> nodes, ArrayList<ArrayList<Edge>> edges, int startNode, int defMove){
		double totalValue = nodes.get(startNode).value;
		//System.out.println("Starting value: " + totalValue);
		//System.out.println();
		boolean[] controlled = new boolean[nodes.size()];
		controlled[startNode] = true;
		Queue<Edge> queue = new LinkedList<Edge>();
		for(Edge e : edges.get(startNode)){
			queue.add(e);
		}
		
		Random r = new Random();
		while(!queue.isEmpty() && controlled[defMove]==false){
			Edge e = queue.poll();
			if(controlled[e.v2] == false){
				double chance = r.nextDouble();
				if(chance <= e.probability){ //successful transmission
					//System.out.println("SUCCESSFUL Transmission From Node " + e.v1 + " to Node " + e.v2);
					totalValue += nodes.get(e.v2).value;
					controlled[e.v2] = true;
					for(Edge newEdge : edges.get(e.v2)){
						if(controlled[newEdge.v1] && controlled[newEdge.v2] == false){ //Not yet added, so add new edge
							//System.out.println("Adding edge (" + newEdge.v1 + "," + newEdge.v2 + ") to Queue");
							queue.add(newEdge);
						}
					}
				}else{
					//System.out.println("FAILED Transmission From Node " + e.v1 + " to Node " + e.v2);
				}
				//System.out.println("Total Value: " + totalValue);
				//System.out.println();
			}else{
				//System.out.println("Removing edge (" + e.v1 + "," + e.v2 + ") because node " + e.v2 + " has been captured");
			}
		}
		
		return controlled;
	}
	
	/*public static void main(String[] args){
		Simulator sub = new Simulator();
		sub.simulate();
	}*/
	
}
