package subnetmarcus;


public class Node {

	public int nodeId;
	public int subnet;
	public double value;
	public double attackerCost;
	public double defenderCost;
	
	public Node(int nodeId, int subnet, double value, double aCost, double dCost){
		this.nodeId = nodeId;
		this.subnet = subnet;
		this.value = value;
		this.attackerCost = aCost;
		this.defenderCost = dCost;
	}
	
	public Node clone(){
		return new Node(nodeId, subnet, value, attackerCost, defenderCost);
	}
	
}
