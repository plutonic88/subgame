package subnet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

public class MonteCarloParallel implements Runnable{



	Random rand = new Random();
	public double sumatt = 0.0;
	public double sumdef = 0.0;		

	public Thread t;
	public String threadName;
	public int LIMIT;
	public NodeX hardendenode;
	public NodeX attackednode;

	public HashMap<Integer, NodeX> nodes = new HashMap<Integer, NodeX>();

	public boolean suspend = true;
	public boolean waiting = false;








	public MonteCarloParallel(Random rand, String threadName, int iTER, NodeX hardendenode, NodeX attackednode, HashMap<Integer, NodeX> nodes) {
		super();
		this.rand = rand;
		this.threadName = threadName;
		LIMIT = iTER;
		this.hardendenode = hardendenode;
		this.attackednode = attackednode;
		this.nodes = nodes;
	}

	@Override
	public void run() {

		for(int iter = 0; iter<=LIMIT; iter++)
		{

			//System.out.println("\n*****ITER no "+ iter);


			Random rand = new Random();
			double attackerpoints = 0.0;
			double defenderpoints = 0.0;


			//defenderpoints += (0.0 - hardendenode.defcost);
			//attackerpoints +=(0.0 -ha)


			//System.out.println("("+hardendenode.id + ","+ attackednode.id+ ") , iter  " + iter);


			NodeX start = new NodeX(attackednode);

			PriorityQueue<NodeX> fringequeue = new PriorityQueue<NodeX>(1000, new Comparator<NodeX>() 
			{  
				public int compare(NodeX w1, NodeX w2) 
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
				NodeX curnode = fringequeue.poll();

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

				NodeX curorignode = nodes.get(curnode.id);

				double beta = 1;
				double gamma = 1;

				if(curnode.id == hardendenode.id)
				{
					beta = 0.5;
					//gamma = 0.5;
				}

				closednodes.add(curnode.id);

				for(NodeX nei: curorignode.neighbors.values())
				{

					// for every neighbor of that node make a decision
					// if the pollling node is the hardend one, then every outgoing prob will be reduced by beta
					// and the reward will be reduced by gamma
					// if went through then add the neighbor to the queue and sum the reward


					if(!closednodes.contains(nei.id) && visited[nei.id]==false)
					{
						if(nei.id == hardendenode.id)
						{
							beta = 0.5;
							gamma = 0.5;
						}


						double prob = curorignode.getTransitionProbs(nei);
						// generate a random double

						double r = rand.nextDouble();
						if(r<(prob*beta))
						{
							// the attack went through
							//attackerpoints += (nei.value*gamma);
							//defenderpoints += -((nei.value*gamma));
							NodeX newnode = new NodeX(nei);
							newnode.setValue(nei.value*gamma);
							newnode.setProb(prob);
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


		

	}

	public void start () 
	{
		System.out.println("Starting " +  threadName );
		if (t == null) 
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}

}
