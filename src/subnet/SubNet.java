package subnet;

import java.util.HashMap;

public class SubNet {

	public static void doExp() {
		
		
		HashMap<Integer, Node> nodes = createNetwork();
		printNodes(nodes);
		
		
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
		a.setTransitionProbs(b, 0.6);
		b.addNeighbor(a);
		b.setTransitionProbs(a, 0.6);
		
		
		a.addNeighbor(c);
		a.setTransitionProbs(c, 0.7);
		c.addNeighbor(a);
		c.setTransitionProbs(a, 0.7);
		
		
		
		c.addNeighbor(b);
		c.setTransitionProbs(b, 0.5);
		b.addNeighbor(c);
		b.setTransitionProbs(c, 0.5);
		
		
		
		
		
		
		
		Node d = new Node(3, 1, 7, false);
		Node e = new Node(4, 1, 8, false);
		Node f = new Node(5, 1, 9, false);
		
		d.addNeighbor(e);
		d.setTransitionProbs(e, 0.7);
		e.addNeighbor(d);
		e.setTransitionProbs(d, 0.7);
		
		
		
		d.addNeighbor(f);
		d.setTransitionProbs(f, 0.6);
		f.addNeighbor(d);
		f.setTransitionProbs(d, 0.6);
		
		
		e.addNeighbor(f);
		e.setTransitionProbs(f, 0.8);
		f.addNeighbor(e);
		f.setTransitionProbs(e, 0.8);
		
		
		
		
		
		
		
		Node g = new Node(6, 2, 5, false);
		Node h = new Node(7, 2, 9, false);
		Node i = new Node(8, 2, 6, false);
		
		
		
		g.addNeighbor(h);
		g.setTransitionProbs(h, 0.7);
		h.addNeighbor(g);
		h.setTransitionProbs(g, 0.7);
		
		
		g.addNeighbor(i);
		g.setTransitionProbs(i, 0.9);
		i.addNeighbor(g);
		i.setTransitionProbs(g, 0.9);
		
		
		
		h.addNeighbor(i);
		h.setTransitionProbs(i, 0.8);
		i.addNeighbor(h);
		i.setTransitionProbs(h, 0.8);
		
		
		
		c.addNeighbor(d);
		c.setTransitionProbs(d, 0.3);
		d.addNeighbor(c);
		d.setTransitionProbs(c, 0.3);
		
		
		
		f.addNeighbor(g);
		f.setTransitionProbs(g, 0.2);
		g.addNeighbor(f);
		g.setTransitionProbs(f, 0.2);
		
		
		
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
