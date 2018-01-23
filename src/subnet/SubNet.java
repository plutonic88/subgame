package subnet;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;



public class SubNet {

	public static void doExp() {
		
		
		HashMap<Integer, Node> nodes = createNetwork();
		printNodes(nodes);
		doMonteCarloSimulation(nodes);
		
		
	}

	private static void doMonteCarloSimulation(HashMap<Integer, Node> nodes) {
		
		
		int LIMIT = 100000;
		
		HashMap<String, Double> attackerpayoffs = new  HashMap<String, Double>();
		
		
		for(Node hardendenode : nodes.values())
		{
			//hardendenode.setHardended(true);
			for(Node attackednode: nodes.values())
			{
				// harden the defended node
				
				double attackerpoints = 0.0;
				
				
				
				for(int iter = 0; iter<=LIMIT; iter++)
				{
					
					//System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") , iter  " + iter);
					
					
					Node start = new Node(attackednode);
					
					PriorityQueue<Node> fringequeue = new PriorityQueue<Node>(1000, new Comparator<Node>() {  
					    
			            public int compare(Node w1, Node w2) {                         
			                return (int)(w1.depth - w2.depth);
			            }      
			        }); 
					
					fringequeue.add(start);
					while(fringequeue.size()>0)
					{
						// poll a node
						Node curnode = fringequeue.poll();
						
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
							
							double prob = curorignode.getTransitionProbs(nei);
							// generate a random double
							Random rand = new Random();
							double r = rand.nextDouble();
							if(r<(prob*beta))
							{
								// the attack went through
								attackerpoints += (nei.value*gamma);
								Node newnode = new Node(nei);
								newnode.depth = curnode.depth + 1;
								fringequeue.add(newnode);
							}
						}
						
					}
					
				}
				
				attackerpoints /= LIMIT;
				
				System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") = " + attackerpoints);
				
				String key = hardendenode.id + ","+attackednode.id;
				
				attackerpayoffs.put(key, attackerpoints);
				

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
		
		
		Node a = new Node(0, 0, 10, false);
		Node b = new Node(1, 0, 4, false);
		Node c = new Node(2, 0, 6, false);
		
		a.addNeighbor(b);
		a.setTransitionProbs(b, 0.4);
		b.addNeighbor(a);
		b.setTransitionProbs(a, 0.2);
		
		
		a.addNeighbor(c);
		a.setTransitionProbs(c, 0.4);
		c.addNeighbor(a);
		c.setTransitionProbs(a, 0.1);
		
		
		
		c.addNeighbor(b);
		c.setTransitionProbs(b, 0.5);
		b.addNeighbor(c);
		b.setTransitionProbs(c, 0.2);
		
		
		
		
		
		
		
		Node d = new Node(3, 1, 7, false);
		Node e = new Node(4, 1, 8, false);
		Node f = new Node(5, 1, 9, false);
		
		d.addNeighbor(e);
		d.setTransitionProbs(e, 0.5);
		e.addNeighbor(d);
		e.setTransitionProbs(d, 0.2);
		
		
		
		d.addNeighbor(f);
		d.setTransitionProbs(f, 0.3);
		f.addNeighbor(d);
		f.setTransitionProbs(d, 0.1);
		
		
		e.addNeighbor(f);
		e.setTransitionProbs(f, 0.2);
		f.addNeighbor(e);
		f.setTransitionProbs(e, 0.4);
		
		
		
		
		
		
		
		Node g = new Node(6, 2, 5, false);
		Node h = new Node(7, 2, 9, false);
		Node i = new Node(8, 2, 6, false);
		
		
		
		g.addNeighbor(h);
		g.setTransitionProbs(h, 0.6);
		h.addNeighbor(g);
		h.setTransitionProbs(g, 0.5);
		
		
		g.addNeighbor(i);
		g.setTransitionProbs(i, 0.2);
		i.addNeighbor(g);
		i.setTransitionProbs(g, 0.3);
		
		
		
		h.addNeighbor(i);
		h.setTransitionProbs(i, 0.3);
		i.addNeighbor(h);
		i.setTransitionProbs(h, 0.3);
		
		
		
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
	
	
	
	

}


class Node {
	
	
	int id;  // unique across the network
	int subnetid;
	int value;
	boolean hardended= false;
	
	int depth = 0;
	

	HashMap<Integer, Node> neighbors =  new HashMap<Integer, Node>();
	HashMap<Node, Double> transitionprobs = new HashMap<Node, Double>();
	
	
	
	public Node(int id, int subnetid, int value, boolean hardended) {
		super();
		this.id = id;
		this.subnetid = subnetid;
		this.value = value;
		this.hardended = hardended;
	}
	
	public Node(Node node)
	{
		this.id = node.id;
		this.subnetid = node.subnetid;
		this.value = node.value;
		this.hardended = node.hardended;
		
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
