package subnetmarcus;


public class Edge {

	public int v1;
	public int v2;
	public double probability;
	
	public Edge(int v1, int v2, double probability){
		this.v1 = v1;
		this.v2 = v2;
		this.probability = probability;
	}
	
	public boolean equals(Object o){
		Edge e = (Edge) o;
		if(probability == e.probability){
			if((v1 == e.v1 && v2 == e.v2) || (v1 == e.v2 && v2 == e.v1))
				return true;
		}
		return false;
	}
	
	public Edge getInverted(){
		return new Edge(v2, v1, probability);
	}
	
	public Edge clone(){
		return new Edge(v1, v2, probability);
	}
	
}
