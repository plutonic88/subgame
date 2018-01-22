package subgame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import games.MatrixGame;
import parsers.GamutParser;

public class LouvainClusteringActions {





	public static ArrayList<HierCommunity> clusterActions(Action[][] actions, int clusternumber) throws Exception
	{
		ArrayList<ArrayList<Action>> communities = new ArrayList<ArrayList<Action>>();
		int comcounter =0;
		for(int player=0; player<2; player++)
		{
			for(Action a: actions[player])
			{
				a.communityindex = comcounter++;
				ArrayList<Action> tmpnode = new ArrayList<Action>();
				tmpnode.add(a);
				communities.add(tmpnode);
			}
		}
		//System.out.println();
		//printcommunity(communities);
		/**
		 * for each node i calculate the modularity change if it's 
		 * moved to neighbor j
		 */
		/**
		 * for a node we need to calculate the neighbor first
		 * then for the neighbors we need to calculate the modularity change
		 */

		/**
		 * 
		 * 
		 * first ITER
		 */

		int it = 0;
		int IETR = 20;
		boolean done= false;
		while(it++<IETR)
		{
			boolean deltaincreased = false;
			for(ArrayList<Action> node : communities)
			{
				for(int action=0; action<node.size(); action++)
				{
					Action a = node.get(action);


					//	System.out.println("Evaluating Action "+a.action + ", player "+a.player + ", community "+a.communityindex);
					/**
					 * calculate the neighbors which are arraylist<action> in community
					 * return the indexes from the community
					 */
					/**
					 * index of communities
					 */
					ArrayList<Integer> neighborcommys = getNeighbors(a, communities);

					/*for(Integer nindex: neighborcommys)
					{
						System.out.println("Neighbor community "+ nindex);
					}*/


					/**
					 * for each of this neighborcommunity calcualte the change in delta
					 */
					double[] deltamod = new double[neighborcommys.size()];
					int deltacounter = 0;
					for(Integer neighborcommy : neighborcommys)
					{
						/**
						 * sumin sum of all weights of all links of neighborcommunity
						 */
						ArrayList<Action> neighborcommunity = communities.get(neighborcommy);
						double sumin = calculateSumIn(neighborcommunity);
						double sumtot = calculateSumTot(neighborcommunity);
						/**
						 * degree of node, number of edges incident on node
						 */
						int ki = calculateKi(a);
						double ki_in = calculateKiIn(a, neighborcommunity);
						double m = calculateM(communities);


						double f = (sumin+(2*ki_in))/(2*m);
						double b =  Math.pow((sumtot+ki)/(2*m),2);
						double c = sumin/(2*m);
						double d = Math.pow(sumtot/(2*m), 2);
						double e = Math.pow(ki/(2*m), 2);
						//deltamod[deltacounter++] = (a-b) - (c-d-e);
						deltamod[deltacounter++] = (f-b) - (c-d-e);
						/*System.out.println("considering neighbor community "+ neighborcommy +
								", mod "+ deltamod[deltacounter-1]);*/
					}
					/**
					 *calculate the modularity when node i is removed from it's community 
					 */
					double modifremoved = calcModIfRemoved(node, a, communities);

					/*System.out.println("action "+a.action+", player "+a.player+", community "+ a.communityindex +
							", mod if action is removed"+ modifremoved);*/

					for(int i=0; i<deltamod.length; i++)
					{
						deltamod[i] = deltamod[i] -  modifremoved ;
						/*System.out.println(" changed mod if action "+ a.action + " is moved to "
								+ " community "+ neighborcommys.get(i) +
								": "+ deltamod[i] );*/

					}


					/**
					 * move node i to the community with   deltamod increased
					 */
					int maxmodindex = calculateMaxIncreaseIndex(deltamod);
					int moveincommy = -1;
					if(maxmodindex!=-1)
					{
						deltaincreased = true;
						moveincommy = neighborcommys.get(maxmodindex);
						//System.out.println("action "+a.action + " is being moved to "+ moveincommy);

						moveNodeToCommunity(a, communities.indexOf(node) , moveincommy, communities);
						//printcommunity(communities);
					}
					int countcom = 0;
					for(int c=0; c<communities.size(); c++)
					{
						if(communities.get(c).size()>0)
						{
							countcom++;
						}
					}

					System.out.println("iter "+it + ", com size "+ countcom);
					/*
					if(countcom<=clusternumber)
					{
						done = true;
						break;
					}*/
				}
				/*if(done)
					break;*/


			}




			if(!deltaincreased || done/*|| countcom<=clusternumber*/)
			{
				break;
			}
		}
		//printcommunity(communities);


		////////// end of first iter


		/**
		 * check if there is any empty community
		 */



		/**
		 * build hierarchical community
		 */

		ArrayList<HierCommunity> hcommunities = buildHierArchicalCommunities(communities);

		if(hcommunities.size()>clusternumber)
		{
			//printHCommunity(hcommunities);


			int subgame = clusternumber;
			int finaliter = 0;


			while(true)
			{
				boolean countreached = false;
				finaliter++;
				if(finaliter==10)
					break;


				int ITER = 100;
				int itr = 0;
				while(itr++<ITER)
				{
					//while(true)
					//{
					boolean deltaincreased = false;

					for(HierCommunity hcom: hcommunities)
					{
						//countreached = false;
						for(int nodeindex = 0; nodeindex<hcom.nodes.size(); nodeindex++)
						{

							Node node = hcom.nodes.get(nodeindex);

							//System.out.println("Processing node "+ node.id + ", community "+ node.community);
							/**
							 * for every neighbor of node, calcualte the gain for moving it to the neighbor community
							 */
							ArrayList<Integer> neicoms = getNeighbors(node, hcommunities);

							/*for(Integer ind: neicoms)
						{
							System.out.println("Neighbor "+ ind);
						}*/

							double[] deltamod = new double[neicoms.size()];
							int deltacounter = 0;
							for(Integer neicom: neicoms)
							{
								/**
								 * calcualte gain
								 */

								int neiindex = getComIndex(neicom, hcommunities);

								HierCommunity neihcom = hcommunities.get(neiindex);

								if(neihcom.community!=neicom)
								{
									throw new Exception("Not match");
								}

								double sumin = calculateSumIn(neihcom);

								//System.out.println("nei com "+ neicom + ", sumin "+ sumin);


								double sumtot = calculateSumTot(neihcom);

								//	System.out.println("nei com "+ neicom + ", sumtot "+ sumtot);

								double ki = calculateKi(node); // is it correct ? loop counted twice ?
								//System.out.println("nei com "+ neicom + ", ki "+ ki);
								double ki_in = calculateKiIn(node, neihcom);
								//	System.out.println("nei com "+ neicom + ", ki_in "+ ki_in);
								double m = calculateMH(hcommunities);

								//	System.out.println("nei com "+ neicom + ", m "+ m);



								double a = (sumin+(2*ki_in))/(2*m);
								double b =  Math.pow((sumtot+ki)/(2*m),2);
								double c = sumin/(2*m);
								double d = Math.pow(sumtot/(2*m), 2);
								double e = Math.pow(ki/(2*m), 2);
								deltamod[deltacounter++] = (a-b) - (c-d-e);

								/*System.out.println("considering neighbor community "+ neihcom.community +
									", mod "+ deltamod[deltacounter-1]);*/


							}

							//double modifremoved = calcModIfRemoved(node, hcom, hcommunities);

							int maxmodindex = calculateMaxIncreaseIndex(deltamod);
							int moveincommunity = -1;

							/*for(int i=0; i<deltamod.length; i++)
						{
							//deltamod[i] = deltamod[i] -  modifremoved ;
							System.out.println(" changed mod if node "+ node.id + " is moved to "
									+ " community "+ neicoms.get(i) +
									": "+ deltamod[i] );

						}*/

							if(maxmodindex!=-1)
							{
								deltaincreased = true;
								moveincommunity = neicoms.get(maxmodindex);
								//System.out.println("node "+node.id + " is being moved to "+ moveincommunity);
								moveNodeToCommunity(hcom, node, moveincommunity, hcommunities);
							}



						}

						int countcom = 0;
						for(int c=0; c<hcommunities.size(); c++)
						{
							if(hcommunities.get(c).nodes.size()>0)
							{
								countcom++;
							}
						}

						System.out.println("finalitr "+finaliter+", iterrrr "+ itr + ", com size "+ countcom);
						/*if(countcom<=clusternumber)
						{
							countreached = true;
							break;
						}
						 */



					}
					if(!deltaincreased /*|| hcommunities.size()<=subgame*/ /*|| countreached*/)
					{
						break;
					}

					//}
				}

				//printHCommunity(hcommunities);

				//System.out.println();


				/**
				 * process the hcommunity
				 */


				processHCommunity(hcommunities);

				//printHCommunity(hcommunities);
				System.out.println("Com size "+ hcommunities.size());
				if(hcommunities.size()<=subgame)
					break;

			}
		}

		//printHCommunity(hcommunities);
		//	System.out.println();







		//int ITER = 100;
		//int itr = 0;
		/*	while(itr++<ITER)
		{
			while(true)
			{
				boolean deltaincreased = false;
				for(ArrayList<Action> node : communities)
				{
		 *//**
		 * calculate the neighbors which are arraylist<action> in community
		 * return the indexes from the community
		 *//*
		 *//**
		 * index of communities
		 *//*
					ArrayList<Integer> neighborcommunityindexes = getNeighbors(node, communities);
		  *//**
		  * for each of this neighborcommunity calcualte the change in delta
		  *//*
					double[] deltamod = new double[neighborcommunityindexes.size()];
					int deltacounter = 0;
					for(Integer neighborcommunityindex : neighborcommunityindexes)
					{
		   *//**
		   * sumin sum of all weights of all links of neighborcommunity
		   *//*
						ArrayList<Action> neighborcommunity = communities.get(neighborcommunityindex);
						double sumin = calculateSumIn(neighborcommunity);
						double sumtot = calculateSumTot(neighborcommunity);
		    *//**
		    * degree of node, number of edges incident on node
		    *//*
						int ki = calculateKi(node);
						double ki_in = calculateKiIn(node, neighborcommunity);
						double m = calculateM(communities);
						double a = (sumin+ki_in)/(2*m);
						double b = ((sumtot+ki)*(sumtot+ki))/(2*m);
						double c = sumin/(2*m);
						double d = Math.pow(sumtot/(2*m), 2);
						double e = Math.pow(ki/(2*m), 2);
						deltamod[deltacounter++] = (a-b) - (c-d-e);
					}
		     *//**
		     * move node i to the community with   deltamod increased
		     *//*
					int maxmodindex = calculateMaxIncreaseIndex(deltamod);
					int moveincommunityindex = -1;
					if(maxmodindex!=-1)
					{
						deltaincreased = true;
						moveincommunityindex = neighborcommunityindexes.get(maxmodindex);
						moveNodeToCommunity(communities.indexOf(node) , moveincommunityindex, communities);
					}
				}
				if(!deltaincreased)
				{
					break;
				}
			}// inner while 
		} // outer while
		      */		return hcommunities;

	}







	public static ArrayList<HierCommunity> clusterActionsFixedSize(Action[][] actions, int clusternumber, int limit_comsize) throws Exception
	{
		ArrayList<ArrayList<Action>> communities = new ArrayList<ArrayList<Action>>();
		int comcounter =0;
		for(int player=0; player<2; player++)
		{
			for(Action a: actions[player])
			{
				a.communityindex = comcounter++;
				ArrayList<Action> tmpnode = new ArrayList<Action>();
				tmpnode.add(a);
				communities.add(tmpnode);
			}
		}
		//System.out.println();
		//printcommunity(communities);
		/**
		 * for each node i calculate the modularity change if it's 
		 * moved to neighbor j
		 */
		/**
		 * for a node we need to calculate the neighbor first
		 * then for the neighbors we need to calculate the modularity change
		 */

		/**
		 * 
		 * 
		 * first ITER
		 */

		int it = 0;
		int IETR = 20;
		boolean done= false;
		while(it++<IETR)
		{
			boolean deltaincreased = false;
			for(ArrayList<Action> node : communities)
			{
				for(int action=0; action<node.size(); action++)
				{
					Action a = node.get(action);


					//	System.out.println("Evaluating Action "+a.action + ", player "+a.player + ", community "+a.communityindex);
					/**
					 * calculate the neighbors which are arraylist<action> in community
					 * return the indexes from the community
					 */
					/**
					 * index of communities
					 */
					ArrayList<Integer> neighborcommys = getNeighbors(a, communities);

					/*for(Integer nindex: neighborcommys)
					{
						System.out.println("Neighbor community "+ nindex);
					}*/


					/**
					 * for each of this neighborcommunity calcualte the change in delta
					 */
					double[] deltamod = new double[neighborcommys.size()];
					int deltacounter = 0;
					for(Integer neighborcommy : neighborcommys)
					{
						/**
						 * sumin sum of all weights of all links of neighborcommunity
						 */
						ArrayList<Action> neighborcommunity = communities.get(neighborcommy);
						/**
						 * count number of member inside the community
						 * if it's greater => limit don't assign or calculate the modularity
						 */
						int numberofmember = countMembers(neighborcommunity);

						if(numberofmember<limit_comsize)
						{
							double sumin = calculateSumIn(neighborcommunity);
							double sumtot = calculateSumTot(neighborcommunity);
							/**
							 * degree of node, number of edges incident on node
							 */
							int ki = calculateKi(a);
							double ki_in = calculateKiIn(a, neighborcommunity);
							double m = calculateM(communities);


							double f = (sumin+(2*ki_in))/(2*m);
							double b =  Math.pow((sumtot+ki)/(2*m),2);
							double c = sumin/(2*m);
							double d = Math.pow(sumtot/(2*m), 2);
							double e = Math.pow(ki/(2*m), 2);
							//deltamod[deltacounter++] = (a-b) - (c-d-e);
							deltamod[deltacounter++] = (f-b) - (c-d-e);
							/*System.out.println("considering neighbor community "+ neighborcommy +
								", mod "+ deltamod[deltacounter-1]);*/
						}
						else
						{
							deltamod[deltacounter++] = Double.NEGATIVE_INFINITY;
						}
					}
					/**
					 *calculate the modularity when node i is removed from it's community 
					 */
					double modifremoved = calcModIfRemoved(node, a, communities);

					/*System.out.println("action "+a.action+", player "+a.player+", community "+ a.communityindex +
							", mod if action is removed"+ modifremoved);*/

					for(int i=0; i<deltamod.length; i++)
					{
						deltamod[i] = deltamod[i] -  modifremoved ;
						/*System.out.println(" changed mod if action "+ a.action + " is moved to "
								+ " community "+ neighborcommys.get(i) +
								": "+ deltamod[i] );*/

					}


					/**
					 * move node i to the community with   deltamod increased
					 */
					int maxmodindex = calculateMaxIncreaseIndex(deltamod);
					int moveincommy = -1;
					if(maxmodindex!=-1)
					{
						deltaincreased = true;
						moveincommy = neighborcommys.get(maxmodindex);
						//System.out.println("action "+a.action + " is being moved to "+ moveincommy);

						moveNodeToCommunity(a, communities.indexOf(node) , moveincommy, communities);
						//printcommunity(communities);
					}
					int countcom = 0;
					for(int c=0; c<communities.size(); c++)
					{
						if(communities.get(c).size()>0)
						{
							countcom++;
						}
					}

					System.out.println("iter "+it + ", # of com  "+ countcom);
					/*
					if(countcom<=clusternumber)
					{
						done = true;
						break;
					}*/
				}
				/*if(done)
					break;*/


			}




			if(!deltaincreased || done/*|| countcom<=clusternumber*/)
			{
				break;
			}
		}
		//printcommunity(communities);


		////////// end of first iter


		/**
		 * check if there is any empty community
		 */



		/**
		 * build hierarchical community
		 */

		ArrayList<HierCommunity> hcommunities = buildHierArchicalCommunities(communities);

		if(hcommunities.size()>clusternumber)
		{
			//printHCommunity(hcommunities);


			int subgame = clusternumber;
			int finaliter = 0;


			while(true)
			{
				boolean countreached = false;
				finaliter++;
				if(finaliter==10)
					break;


				int ITER = 100;
				int itr = 0;
				while(itr++<ITER)
				{
					//while(true)
					//{
					boolean deltaincreased = false;

					for(HierCommunity hcom: hcommunities)
					{
						//countreached = false;
						for(int nodeindex = 0; nodeindex<hcom.nodes.size(); nodeindex++)
						{

							Node node = hcom.nodes.get(nodeindex);

							//System.out.println("Processing node "+ node.id + ", community "+ node.community);
							/**
							 * for every neighbor of node, calcualte the gain for moving it to the neighbor community
							 */
							ArrayList<Integer> neicoms = getNeighbors(node, hcommunities);

							/*for(Integer ind: neicoms)
						{
							System.out.println("Neighbor "+ ind);
						}*/

							double[] deltamod = new double[neicoms.size()];
							int deltacounter = 0;
							for(Integer neicom: neicoms)
							{
								/**
								 * calcualte gain
								 */

								int neiindex = getComIndex(neicom, hcommunities);

								HierCommunity neihcom = hcommunities.get(neiindex);


								int numberofmember = countMembers(neihcom);

								if(numberofmember<limit_comsize)
								{


									if(neihcom.community!=neicom)
									{
										throw new Exception("Not match");
									}

									double sumin = calculateSumIn(neihcom);

									//System.out.println("nei com "+ neicom + ", sumin "+ sumin);


									double sumtot = calculateSumTot(neihcom);

									//	System.out.println("nei com "+ neicom + ", sumtot "+ sumtot);

									double ki = calculateKi(node); // is it correct ? loop counted twice ?
									//System.out.println("nei com "+ neicom + ", ki "+ ki);
									double ki_in = calculateKiIn(node, neihcom);
									//	System.out.println("nei com "+ neicom + ", ki_in "+ ki_in);
									double m = calculateMH(hcommunities);

									//	System.out.println("nei com "+ neicom + ", m "+ m);



									double a = (sumin+(2*ki_in))/(2*m);
									double b =  Math.pow((sumtot+ki)/(2*m),2);
									double c = sumin/(2*m);
									double d = Math.pow(sumtot/(2*m), 2);
									double e = Math.pow(ki/(2*m), 2);
									deltamod[deltacounter++] = (a-b) - (c-d-e);

									/*System.out.println("considering neighbor community "+ neihcom.community +
									", mod "+ deltamod[deltacounter-1]);*/
								}
								else
								{
									deltamod[deltacounter++] = Double.NEGATIVE_INFINITY;
								}


							}

							//double modifremoved = calcModIfRemoved(node, hcom, hcommunities);

							int maxmodindex = calculateMaxIncreaseIndex(deltamod);
							int moveincommunity = -1;

							/*for(int i=0; i<deltamod.length; i++)
						{
							//deltamod[i] = deltamod[i] -  modifremoved ;
							System.out.println(" changed mod if node "+ node.id + " is moved to "
									+ " community "+ neicoms.get(i) +
									": "+ deltamod[i] );

						}*/

							if(maxmodindex!=-1)
							{
								deltaincreased = true;
								moveincommunity = neicoms.get(maxmodindex);
								//System.out.println("node "+node.id + " is being moved to "+ moveincommunity);
								moveNodeToCommunity(hcom, node, moveincommunity, hcommunities);
							}



						}

						int countcom = 0;
						for(int c=0; c<hcommunities.size(); c++)
						{
							if(hcommunities.get(c).nodes.size()>0)
							{
								countcom++;
							}
						}

						System.out.println("finalitr "+finaliter+", iterrrr "+ itr + ", # of com  "+ countcom);
						/*if(countcom<=clusternumber)
						{
							countreached = true;
							break;
						}
						 */



					}
					if(!deltaincreased /*|| hcommunities.size()<=subgame*/ /*|| countreached*/)
					{
						break;
					}

					//}
				}

				//printHCommunity(hcommunities);

				//System.out.println();


				/**
				 * process the hcommunity
				 */


				processHCommunity(hcommunities);

				//printHCommunity(hcommunities);
				System.out.println("Com size "+ hcommunities.size());
				if(hcommunities.size()<=subgame)
					break;

			}
		}

		//printHCommunity(hcommunities);
		//	System.out.println();







		//int ITER = 100;
		//int itr = 0;
		/*	while(itr++<ITER)
		{
			while(true)
			{
				boolean deltaincreased = false;
				for(ArrayList<Action> node : communities)
				{
		 *//**
		 * calculate the neighbors which are arraylist<action> in community
		 * return the indexes from the community
		 *//*
		 *//**
		 * index of communities
		 *//*
					ArrayList<Integer> neighborcommunityindexes = getNeighbors(node, communities);
		  *//**
		  * for each of this neighborcommunity calcualte the change in delta
		  *//*
					double[] deltamod = new double[neighborcommunityindexes.size()];
					int deltacounter = 0;
					for(Integer neighborcommunityindex : neighborcommunityindexes)
					{
		   *//**
		   * sumin sum of all weights of all links of neighborcommunity
		   *//*
						ArrayList<Action> neighborcommunity = communities.get(neighborcommunityindex);
						double sumin = calculateSumIn(neighborcommunity);
						double sumtot = calculateSumTot(neighborcommunity);
		    *//**
		    * degree of node, number of edges incident on node
		    *//*
						int ki = calculateKi(node);
						double ki_in = calculateKiIn(node, neighborcommunity);
						double m = calculateM(communities);
						double a = (sumin+ki_in)/(2*m);
						double b = ((sumtot+ki)*(sumtot+ki))/(2*m);
						double c = sumin/(2*m);
						double d = Math.pow(sumtot/(2*m), 2);
						double e = Math.pow(ki/(2*m), 2);
						deltamod[deltacounter++] = (a-b) - (c-d-e);
					}
		     *//**
		     * move node i to the community with   deltamod increased
		     *//*
					int maxmodindex = calculateMaxIncreaseIndex(deltamod);
					int moveincommunityindex = -1;
					if(maxmodindex!=-1)
					{
						deltaincreased = true;
						moveincommunityindex = neighborcommunityindexes.get(maxmodindex);
						moveNodeToCommunity(communities.indexOf(node) , moveincommunityindex, communities);
					}
				}
				if(!deltaincreased)
				{
					break;
				}
			}// inner while 
		} // outer while
		      */		return hcommunities;

	}











	private static int countMembers(HierCommunity neihcom) {

		int count =0;

		for(Node n: neihcom.nodes)
		{
			for(Action a: n.actions)
			{
				count++;
			}
		}


		return count;
	}







	private static int countMembers(ArrayList<Action> neighborcommunity) {
		// TODO Auto-generated method stub


		return neighborcommunity.size();
	}







	private static int getComIndex(Integer neicom,
			ArrayList<HierCommunity> hcommunities) {


		int index = 0;
		for(HierCommunity hcom : hcommunities)
		{
			if(hcom.community==neicom)
				return hcommunities.indexOf(hcom);
		}


		return -1;
	}











	private static void processHCommunity(ArrayList<HierCommunity> hcommunities) throws Exception {





		int size = hcommunities.size();

		for(int i=0; i<hcommunities.size(); )
		{
			if(hcommunities.get(i).nodes.size()==0)
			{
				hcommunities.remove(i);
			}
			else
			{
				i++;
			}
		}

		HashMap<Integer[], Double> sumweights = new HashMap<Integer[], Double>();


		for(int i=0; i<hcommunities.size()-1; i++)
		{
			for(int j=i; j<hcommunities.size(); j++)
			{
				HierCommunity ihcom = hcommunities.get(i);
				HierCommunity jhcom = hcommunities.get(j);

				int icomindex = ihcom.community;
				int jcomindex = jhcom.community;

				int iconnectioncounter = 0;
				int jconnectioncounter = 0;
				//int selfloop = 0;

				for(Node n: ihcom.nodes)
				{
					for(Node neibor: n.neighbors)
					{
						if((n.community==icomindex) && (neibor.community==jcomindex) && (neibor.community != n.community))
						{
							iconnectioncounter += n.neiweights.get(neibor);
						}

					}
				}






				for(Node n: jhcom.nodes)
				{
					for(Node neibor: n.neighbors)
					{
						if((n.community==jcomindex) && (neibor.community==icomindex) && (neibor.community != n.community))
						{
							jconnectioncounter += n.neiweights.get(neibor);
						}
					}
				}

				if(iconnectioncounter != jconnectioncounter)
				{
					throw new Exception("icon jcon dont match");
				}

				if(iconnectioncounter>0)
				{
					sumweights.put(new Integer[]{icomindex,  jcomindex}, Math.floor(iconnectioncounter));
				}
			}
			HierCommunity ihcom = hcommunities.get(i);



			int selfloop=0;
			ArrayList<Integer[]> donenodes = new ArrayList<Integer[]>();
			for(Node n: ihcom.nodes)
			{
				for(Node nei: n.neighbors)
				{
					if(n.community==nei.community && !isDoneloops(donenodes, n.id, nei.id))
					{
						donenodes.add(new Integer[]{n.id, nei.id});
						selfloop+= n.neiweights.get(nei) ;
					}
				}
			}
			//System.out.println("ihcom "+ ihcom.community + ", selfloop "+ selfloop) ;

			if(selfloop>0)
			{
				sumweights.put(new Integer[]{ihcom.community,  ihcom.community}, Math.floor(selfloop));
			}

			if(i==hcommunities.size()-2)
			{
				HierCommunity lhcom = hcommunities.get(i+1);
				selfloop=0;

				donenodes = new ArrayList<Integer[]>();

				for(Node n: lhcom.nodes)
				{
					//System.out.println(" node "+ n.id + ",com "+n.community);

					for(Node nei: n.neighbors)
					{
						//System.out.println(" neibor "+ nei.id + ",com "+nei.community);
						if( !(n.community!=nei.community) && !isDoneloops(donenodes, n.id, nei.id))
						{
							donenodes.add(new Integer[]{n.id, nei.id});
							selfloop += n.neiweights.get(nei) ;
						}
					}
				}
				//System.out.println("lhcom "+ lhcom.community + ", selfloop "+ selfloop) ;
				if(selfloop>0)
				{
					sumweights.put(new Integer[]{lhcom.community,  lhcom.community}, Math.floor(selfloop));
				}

			}




		}


		/*	for(Integer[] x: sumweights.keySet())
		{
			System.out.println(x[0] + ","+x[1]+"="+sumweights.get(x));
		}*/


		/**
		 * merge nodes, clear neibors, weights
		 */


		int nodecounter = 0;
		for(int i=0; i<hcommunities.size(); i++)
		{
			//if(hcommunities.get(i).)

			Node newnnode = new Node();
			newnnode.id = nodecounter++;

			for(Node n: hcommunities.get(i).nodes)
			{
				for(Action a: n.actions)
				{
					newnnode.actions.add(a);
				}
			}
			newnnode.community = hcommunities.get(i).community;
			hcommunities.get(i).nodes.clear();
			hcommunities.get(i).nodes.add(newnnode);

		}


		/**
		 * add neighbors and weights
		 */

		ArrayList<Integer[]> doneedge = new ArrayList<Integer[]>();


		for(int i=0; i<hcommunities.size(); i++)
		{
			for(int j=0; j<hcommunities.size(); j++)
			{
				int icomind = hcommunities.get(i).community;
				int jcomind = hcommunities.get(j).community;

				for(Integer[] x: sumweights.keySet())
				{
					if(!done(doneedge, x))
					{

						if((x[0]==icomind && x[1]==jcomind)  || (x[1]==icomind && x[0]==jcomind))
						{
							doneedge.add(x);

							hcommunities.get(i).nodes.get(0).neighbors.add(hcommunities.get(j).nodes.get(0));
							hcommunities.get(i).nodes.get(0).neiweights.put(hcommunities.get(j).nodes.get(0), sumweights.get(x));

							if(x[0] !=x[1])
							{
								hcommunities.get(j).nodes.get(0).neighbors.add(hcommunities.get(i).nodes.get(0));
								hcommunities.get(j).nodes.get(0).neiweights.put(hcommunities.get(i).nodes.get(0), sumweights.get(x));
							}
							break;
						}
					}
				}



			}

		}





		//printHCommunity(hcommunities);

		//System.out.println("hi");






	}











	private static boolean isDoneloops(ArrayList<Integer[]> donenodes, int id,
			int id2) {


		for(Integer[] x: donenodes)
		{
			if((x[0]==id && x[1]==id2) || (x[1]==id && x[0]==id2))
			{
				return true;
			}
		}


		return false;
	}











	private static boolean done(ArrayList<Integer[]> doneedge, Integer[] x) {


		for(Integer y[]: doneedge)
		{
			if((x[0]==y[0] && x[1]==y[1]) || (x[0]==y[1] && x[1]==y[0]))
				return true;
		}


		return false;
	}











	private static void moveNodeToCommunity(HierCommunity hcom, Node node,
			int moveincommunity, ArrayList<HierCommunity> hcommunities) {

		hcom.nodes.remove(node);
		int moveincomindex = getMoveInComIndex(moveincommunity, hcommunities);
		//int moveincommunity = hcommunities.get(moveincomindex).community;
		node.community = moveincommunity;
		for(Action a: node.actions)
		{
			a.communityindex = moveincommunity;
		}
		hcommunities.get(moveincomindex).nodes.add(node);

	}






	private static int getMoveInComIndex(int moveincommunity, ArrayList<HierCommunity> hcommunities) {



		for(HierCommunity hcom: hcommunities)
		{
			if(hcom.community==moveincommunity)
			{
				return hcommunities.indexOf(hcom);
			}
		}


		return -1;
	}











	private static double calcModIfRemoved(Node node, HierCommunity hcom,
			ArrayList<HierCommunity> hcommunities) {

		/*double sumin = calculateSumInIfRemoved(node,hcom);
	double sumtot = calculateSumTotIfRemoved(node,hcom);
		 *//**
		 * degree of node, number of edges incident on node
		 *//*
	int ki = calculateKi(node);
	double ki_in = calculateKiIn(node, hcom);
	double m = calculateM(hcommunities);
	double f = (sumin+(2*ki_in))/(2*m);
	double b = Math.pow((sumtot+ki)/(2*m),2);
	double c = sumin/(2*m);
	double d = Math.pow(sumtot/(2*m), 2);
	double e = Math.pow(ki/(2*m), 2);

	double x = (f-b) - (c-d-e);*/

		return 0;
	}









	/**
	 * sum of the weights of all the links in the network
	 * @param hcommunities
	 * @return
	 */
	private static double calculateMH(ArrayList<HierCommunity> hcommunities) {


		double sum = 0;

		ArrayList<Integer[]> doneedges = new ArrayList<Integer[]>();

		for(HierCommunity hcom: hcommunities)
		{
			for(Node n: hcom.nodes)
			{
				for(Node nei: n.neighbors)
				{
					if(!isDoneEdge(doneedges, n.id,nei.id))
					{
						doneedges.add(new Integer[]{n.id,nei.id});
						sum += n.neiweights.get(nei);

					}
				}
			}

		}


		return sum;
	}











	/**
	 * sum of the weights of the links from i to nodes in C
	 * @param node
	 * @param neihcom
	 * @return
	 */
	private static double calculateKiIn(Node node, HierCommunity neihcom) {

		double sum = 0;

		for(Node nei: node.neighbors)
		{
			if(nei.community==neihcom.community)
			{
				sum += node.neiweights.get(nei);
			}
		}

		return sum;
	}








	/**
	 * sum of the weights of the links incident to node
	 * @param node
	 * @return
	 */
	private static double calculateKi(Node node) 
	{

		double sum = 0;

		for(Node nei: node.neighbors)
		{
			if(nei.community==node.community)
			{
				sum += (2*node.neiweights.get(nei));
			}
			else
			{
				sum += node.neiweights.get(nei);
			}


		}


		return sum;
	}









	/**
	 * the sum of the weights of the links incident to nodes in C
	 * @param neihcom
	 * @return
	 */
	private static double calculateSumTot(HierCommunity neihcom) {

		double sum =0;


		for(Node n: neihcom.nodes)
		{
			for(Node nei: n.neighbors)
			{
				sum += n.neiweights.get(nei);
			}
		}

		return sum;
	}










	private static ArrayList<Integer> getNeighbors(Node node,
			ArrayList<HierCommunity> hcommunities) {


		ArrayList<Integer> indexes = new ArrayList<Integer>();

		for(Node nei: node.neighbors)
		{
			if(nei.community!=node.community)
			{
				indexes.add(nei.community);
			}
		}

		return indexes;
	}










	/**
	 * sum of the weights of the links inside C
	 * @param hcom
	 * @return
	 */
	private static double calculateSumIn(HierCommunity hcom) {

		double sum = 0;

		//System.out.println("hcom  "+ hcom.community);

		for(Node n: hcom.nodes)
		{
			//System.out.println("Node  "+ n.id + ", com :"+ n.community);
			for(Node neibor: n.neighbors)
			{
				//	System.out.println("Neibor  "+ neibor.community );
				if(n.community == neibor.community)
				{
					sum += n.neiweights.get(neibor);
				}
			}

			/*for(Node x: n.neiweights.keySet())
			{
				System.out.println(x.community + ","+ n.neiweights.get(x));

			}*/


		}
		return sum;
	}











	private static ArrayList<HierCommunity> buildHierArchicalCommunities(
			ArrayList<ArrayList<Action>> communities) throws Exception {

		ArrayList<Integer> nonemptycommunities = new ArrayList<Integer>();
		for(int i=0; i<communities.size(); i++)
		{
			if(communities.get(i).size()>0)
			{
				nonemptycommunities.add(i);
			}
		}
		/**
		 * determine com to com # of connections
		 */
		int numcom = nonemptycommunities.size();
		HashMap<Integer[], Double> sumweights = new HashMap<Integer[], Double>();
		for(int i=0; i<numcom; i++)
		{
			for(int j=i; j<numcom; j++)
			{
				ArrayList<Action> icom = communities.get(nonemptycommunities.get(i));
				ArrayList<Action> jcom = communities.get(nonemptycommunities.get(j));
				if(icom.size()>0 && jcom.size()>0)
				{
					/**
					 * find out the number of connections
					 */
					int icomindex = icom.get(0).communityindex;
					int jcomindex = jcom.get(0).communityindex;

					int iconnectioncounter = 0;

					for(Action a: icom)
					{
						for(Action n: a.neighbors)
						{
							if((a.communityindex==icomindex) && (n.communityindex==jcomindex) 
									&& (a.communityindex != n.communityindex))
							{
								iconnectioncounter+= a.weights.get(n);
							}
						}
					}

					int jconnectioncounter = 0;
					for(Action a: jcom)
					{
						for(Action n: a.neighbors)
						{
							if((a.communityindex==jcomindex) && (n.communityindex==icomindex) &&
									(a.communityindex != n.communityindex))
							{
								jconnectioncounter+=a.weights.get(n);
							}
						}
					}

					if(iconnectioncounter != jconnectioncounter)
					{
						throw new Exception("icon jcon not same");
					}
					if(iconnectioncounter>0)
					{

						sumweights.put(new Integer[]{icomindex,  jcomindex}, Math.floor(iconnectioncounter));
					}
					//sumweights.put(new Integer[]{jcomindex,  icomindex}, Math.floor(iconnectioncounter));
				}
			}
			ArrayList<Action> icom = communities.get(nonemptycommunities.get(i));
			int icomindex = icom.get(0).communityindex;
			//int jcomindex = jcom.get(0).communityindex;

			int iconnectioncounter = 0;

			ArrayList<Action> done = new ArrayList<Action>();

			for(Action a: icom)
			{
				//System.out.println("Action "+ a.action);
				for(Action n: a.neighbors)
				{
					if((n.communityindex==a.communityindex) && !done.contains(n))
					{
						//System.out.println("Neibor "+ n.action);
						done.add(a);
						iconnectioncounter+= a.weights.get(n);
						//System.out.println("cnt "+ iconnectioncounter);
					}
				}
			}
			if(iconnectioncounter>0)
			{

				sumweights.put(new Integer[]{icomindex,  icomindex}, Math.floor(iconnectioncounter));
			}






		}

		//printConnections(sumweights);


		ArrayList<HierCommunity> newcommunities = new ArrayList<HierCommunity>();
		int comcounter = 0; 
		int ndid=0;
		for(Integer comindex : nonemptycommunities)
		{
			ArrayList<Action> actions = communities.get(comindex);
			HierCommunity tmpcom = new HierCommunity();
			/**
			 * make nodes
			 */
			Node nd = new Node();
			nd.id=ndid++;
			for(Action a: actions)
			{
				nd.actions.add(a);
			}
			nd.community = actions.get(0).communityindex;
			tmpcom.community = nd.community;//comcounter++;
			tmpcom.nodes.add(nd);
			newcommunities.add(tmpcom);

		}

		/**
		 * now add the neighbors and weights
		 */

		int newcomsize = newcommunities.size();

		for(int i=0; i<newcomsize; i++)
		{
			for(int j=i; j<newcomsize; j++)
			{
				/**
				 * 
				 * get a node from both community
				 */
				Action ia = newcommunities.get(i).nodes.get(0).actions.get(0);
				Action ja = newcommunities.get(j).nodes.get(0).actions.get(0);

				double numconnection = -1; 

				for(Integer[] x: sumweights.keySet())
				{
					if((x[0]==ia.communityindex && x[1]==ja.communityindex) /*|| 
							(x[1]==ia.communityindex && x[0]==ja.communityindex)*/)
					{
						numconnection = sumweights.get(x);

						HierCommunity ic = newcommunities.get(i);
						HierCommunity jc = newcommunities.get(j);

						ic.nodes.get(0).neighbors.add(jc.nodes.get(0));
						ic.nodes.get(0).addWeight(jc.nodes.get(0), numconnection);

						if(x[0]!=x[1])
						{

							jc.nodes.get(0).neighbors.add(ic.nodes.get(0));
							jc.nodes.get(0).addWeight(ic.nodes.get(0), numconnection);
						}


						break;
					}
					//sumweights.get(new Integer[]{ia.communityindex, ja.communityindex});
				}

				/**
				 * get the new communities if not -1 and add neighbors
				 */
				/*if(numconnection!=-1)
				{


					//ic.hcomneighbors.add(jc);
					//ic.addWeight(jc, numconnection);
					//jc.hcomneighbors.add(ic);
					//jc.addWeight(ic, numconnection);

				}*/
			}
		}
		/**
		 * now change all the action's communityindex
		 */

		//printHCommunity(newcommunities);

		/*for(HierCommunity com : newcommunities)
		{
			//int communityind = com.c
			for(Node n: com.nodes)
			{
				for(Action a: n.actions)
				{
					a.communityindex= com.community;
				}
			}
		}
		printHCommunity(newcommunities);*/
		return newcommunities;
	}





	private static void printConnections(HashMap<Integer[], Double> sumweights) {



		for(Integer[] x: sumweights.keySet())
		{
			System.out.println(x[0] + ","+x[1]+ "="+sumweights.get(x));
		}

	}











	private static double calcModIfRemoved(ArrayList<Action> node, Action a,
			ArrayList<ArrayList<Action>> community) {

		double sumin = calculateSumInIfRemoved(node,a);
		double sumtot = calculateSumTotIfRemoved(node,a);
		/**
		 * degree of node, number of edges incident on node
		 */
		int ki = calculateKi(a);
		double ki_in = calculateKiIn(a, node);
		double m = calculateM(community);
		double f = (sumin+(2*ki_in))/(2*m);
		double b = Math.pow((sumtot+ki)/(2*m),2);
		double c = sumin/(2*m);
		double d = Math.pow(sumtot/(2*m), 2);
		double e = Math.pow(ki/(2*m), 2);

		double x = (f-b) - (c-d-e);

		return x;
	}


	private static void printcommunity(ArrayList<ArrayList<Action>> community)
	{
		/*for(ArrayList<Action> a: community)
		{
			for(Action b: a)
			{
				System.out.println("Action "+b.action+", player "+b.player+",Community "+ b.communityindex);
			}
			System.out.println();
		}*/

	}

	private static void printHCommunity(ArrayList<HierCommunity> hcommunities) 
	{
		for(HierCommunity hcom : hcommunities)
		{
			System.out.println("Community "+ hcom.community);
			int nodecounter = 0;
			for(Node n: hcom.nodes)
			{
				System.out.println("Node "+ nodecounter++);
				for(Action a: n.actions)
				{
					System.out.println("Action "+a.action+", player "+a.player+",Community "+ a.communityindex);
				}
				/**
				 * print neighbors
				 */
				for(Node nbor: n.neighbors)
				{
					//if(nbor.community != n.community)
					{
						System.out.println("neighbor "+ nbor.community + ", weights "+ n.neiweights.get(nbor));
					}
				}
				System.out.println();
			}
			System.out.println();
			System.out.println();

		}



	}





	private static void moveNodeToCommunity(int icommunityindex,
			int moveincommunityindex, ArrayList<ArrayList<Action>> community) 
	{

		ArrayList<Action> nodei = community.remove(icommunityindex);

		for(Action a: nodei)
		{
			a.communityindex = moveincommunityindex;
			community.get(moveincommunityindex).add(a);
		}
	}


	private static void moveNodeToCommunity(Action a, int icommunityindex,
			int moveincommy, ArrayList<ArrayList<Action>> community)
	{

		community.get(icommunityindex).remove(a);
		a.communityindex = moveincommy;
		community.get(moveincommy).add(a);




	}


	private static int calculateMaxIncreaseIndex(double[] deltamod) {

		Double max = Double.NEGATIVE_INFINITY;
		int maxindex = -1;
		for(int i=0; i<deltamod.length; i++)
		{
			if((max<deltamod[i]) && (deltamod[i]>0))
			{
				max = deltamod[i];
				maxindex = i;
			}
		}
		return maxindex;
	}


	private static double calculateM(ArrayList<ArrayList<Action>> community) {

		double sum = 0; 

		ArrayList<Integer[]> doneedges = new ArrayList<Integer[]>();
		for(ArrayList<Action> x: community)
		{
			for(Action a: x)
			{
				for(Action n: a.neighbors)
				{
					if(!isDoneEdge(doneedges, a.action, n.action))
					{
						doneedges.add(new Integer[]{a.action, n.action});
						sum += a.weights.get(n);
					}
				}
			}
		}
		return sum;
	}


	private static boolean isDoneEdge(ArrayList<Integer[]> doneedges,
			int action, int action2) {

		for(Integer[] x: doneedges)
		{
			if((x[0]==action && x[1]==action2) || (x[1]==action && x[0]==action2))
			{
				return true;
			}
		}
		return false;
	}


	private static double calculateKiIn(ArrayList<Action> node, ArrayList<Action> neighborcommunity) 
	{
		int jcommunity = neighborcommunity.get(0).communityindex;
		int icommunity = node.get(0).communityindex;
		double sum = 0;
		for(Action a: node)
		{
			for(Action n: a.neighbors)
			{
				if((n.communityindex==jcommunity) && (a.communityindex==icommunity))
				{
					sum += a.weights.get(n);
				}
			}
		}
		return sum;
	}



	private static double calculateKiInIfRemoved(Action a,
			ArrayList<Action> node) {

		int jcommunity = node.get(0).communityindex;
		int icommunity = a.communityindex;
		double sum = 0;
		//	for(Action a: node)
		{
			for(Action n: a.neighbors)
			{
				if((n.communityindex==jcommunity) /*&& (a.communityindex==icommunity)*/)
				{
					sum += a.weights.get(n);
				}
			}
		}
		return sum;


	}


	private static double calculateKiIn(Action a, ArrayList<Action> neighborcommunity) 
	{
		int jcommunity = neighborcommunity.get(0).communityindex;
		int icommunity = a.communityindex;
		double sum = 0;
		//	for(Action a: node)
		{
			for(Action n: a.neighbors)
			{
				if((n.communityindex==jcommunity) /*&& (a.communityindex==icommunity)*/)
				{
					sum += a.weights.get(n);
				}
			}
		}
		return sum;
	}


	private static int calculateKi(ArrayList<Action> node) {
		int sum = 0;
		int community = node.get(0).communityindex;
		for(Action a: node)
		{
			/**
			 * get the neighbors of a and check if they are in the same community
			 */
			for(Action n: a.neighbors)
			{
				if(n.communityindex!=community)
				{
					sum ++;
				}
			}
		}
		return sum;

	}


	private static int calculateKi(Action a) {
		int sum = 0;
		int community = a.communityindex;
		//for(Action a: node)
		{
			/**
			 * get the neighbors of a and check if they are in the same community
			 */
			for(Action n: a.neighbors)
			{
				//if(n.communityindex!=community)
				{
					sum += a.weights.get(n);
				}
			}
		}
		return sum;

	}



	private static double calculateSumTotIfRemoved(ArrayList<Action> node,
			Action a) {

		double sum = 0;
		//int community = node.get(0).communityindex;
		for(Action x: node)
		{
			/**
			 * get the neighbors of a and check if they are in the same community
			 */
			for(Action n: x.neighbors)
			{
				if((n.action!=a.action) && (n.player!=a.player))
				{
					sum += x.weights.get(n);
				}
			}
		}

		return sum;
	}


	private static double calculateSumTot(ArrayList<Action> neighborcommunity) {
		double sum = 0;
		int community = neighborcommunity.get(0).communityindex;
		for(Action a: neighborcommunity)
		{
			/**
			 * get the neighbors of a and check if they are in the same community
			 */
			for(Action n: a.neighbors)
			{
				//if(n.communityindex!=community)
				{
					sum += a.weights.get(n);
				}
			}
		}

		return sum;
	}




	private static double calculateSumInIfRemoved(Node node, HierCommunity hcom) {
		// TODO Auto-generated method stub
		return 0;
	}




	private static double calculateSumInIfRemoved(ArrayList<Action> node,
			Action a) {

		double sum = 0;
		int community = node.get(0).communityindex;
		for(Action y: node)
		{
			/**
			 * get the neighbors of a and check if they are in the same community
			 */
			for(Action n: y.neighbors)
			{
				if( ((n.action!=a.action) && (n.player!=a.player)) && 
						((y.action!=a.action) && (y.player!=a.player)) && 
						(n.communityindex==community) )
				{
					sum += a.weights.get(n);
				}
			}
		}
		return sum;
	}



	private static double calculateSumIn(ArrayList<Action> neighborcommunity) {
		double sum = 0;
		int community = neighborcommunity.get(0).communityindex;
		for(Action a: neighborcommunity)
		{
			/**
			 * get the neighbors of a and check if they are in the same community
			 */
			for(Action n: a.neighbors)
			{
				if(n.communityindex==community)
				{
					sum += a.weights.get(n);
				}
			}
		}

		return sum;
	}

	private static ArrayList<Integer> getNeighbors(Action a,
			ArrayList<ArrayList<Action>> community) 
			{

		ArrayList<Integer> communityindexes = new ArrayList<Integer>();
		for(Action neighbor: a.neighbors)
		{
			/**
			 * neighbors are always opponent actions
			 */
			boolean isin = isInCommunity(neighbor, a);
			if(!isin && (community.get(neighbor.communityindex).size()>0))
			{
				communityindexes.add(neighbor.communityindex);
			}
		}

		return communityindexes;
			}


	/*private static ArrayList<Integer> getNeighbors(ArrayList<Action> node,
			ArrayList<ArrayList<Action>> community) {

		ArrayList<Integer> communityindexes = new ArrayList<Integer>();
		for(Action a: node)
		{
	 *//**
	 * for every action get the neighbors. 
	 * find the neighbors which are not in node
	 * then return the community index where that neighbor belongs
	 *//*
			for(Action neighbor: a.neighbors)
			{
	  *//**
	  * neighbors are always opponent actions
	  *//*
				boolean isin = isInCommunity(neighbor, a);
				if(!isin)
				{
					communityindexes.add(neighbor.communityindex);
				}
			}
		}
		return communityindexes;
	}*/


	private static boolean isInCommunity(Action neighbor, Action action) {

		if(neighbor.communityindex==action.communityindex)
		{
			return true;
		}
		return false;
	}


	public static Action[][] makeDirectedGraphFromNFG(MatrixGame game, double epsilon)
	{
		int naction = game.getNumActions(0);
		Action[][] actions = new Action[2][];


		for(int player=0; player<2; player++)
		{
			actions[player] = new Action[naction];
			for(int action1=0; action1<naction; action1++)
			{
				actions[player][action1] = new Action();
			}
		}




		for(int player=0; player<2; player++)
		{
			//actions[player] = new Action[naction];
			for(int action1=0; action1<naction; action1++)
			{
				//actions[player][action1] = new Action();
				/**
				 * find the best response for action1
				 */
				int maxaction = -1;
				Double maxpayoff = Double.NEGATIVE_INFINITY;
				int[] maxoutcome = new int[2];
				for(int action2=0; action2<naction; action2++)
				{
					int[] outcome = new int[2];
					if(player==0)
					{
						outcome[0] = action1+1;
						outcome[1] = action2+1;
					}
					else
					{
						outcome[1] = action1+1;
						outcome[0] = action2+1;
					}
					double payoff = game.getPayoff(outcome, player^1);
					if(maxpayoff<payoff)
					{
						maxpayoff = payoff;
						maxaction = outcome[player^1];  //(player==0)? (action2+1): (action1+1);
						maxoutcome = outcome;
					}
				}

				/**
				 * some more actions for to add as neighbor
				 */
				//double epsilon = 2;

				ArrayList<Integer> actionstoadd = new ArrayList<Integer>();


				for(int action2=0; action2<naction; action2++)
				{
					int[] outcome = new int[2];
					if(player==0)
					{
						outcome[0] = action1+1;
						outcome[1] = action2+1;
					}
					else
					{
						outcome[1] = action1+1;
						outcome[0] = action2+1;
					}
					double payoff = game.getPayoff(outcome, player^1);
					if(maxpayoff<=(payoff+epsilon) && (outcome[player^1] != maxaction))
					{
						//maxpayoff = payoff;
						//maxaction = outcome[player^1];  //(player==0)? (action2+1): (action1+1);
						actionstoadd.add(outcome[player^1]);

						//maxoutcome = outcome;
					}
				}



				actions[player][action1].bestresponse = maxaction;
				actions[player][action1].action = action1+1;
				actions[player][action1].player = player;
				actions[player][action1].addNeighbor(actions[player^1][maxaction-1]);
				double weight = 1;//game.getPayoff(maxoutcome, player);
				actions[player][action1].addWeight(actions[player^1][maxaction-1], weight);



				actions[player^1][maxaction-1].addNeighbor(actions[player][action1]);
				actions[player^1][maxaction-1].addWeight(actions[player][action1], weight);
				//System.out.println("Player : "+ player + " action "+ action1 + ", best response "+ maxaction);


				for(Integer actoadd: actionstoadd)
				{

					actions[player][action1].addNeighbor(actions[player^1][actoadd-1]);
					//double weight = 1;//game.getPayoff(maxoutcome, player);
					actions[player][action1].addWeight(actions[player^1][actoadd-1], weight);


					actions[player^1][actoadd-1].addNeighbor(actions[player][action1]);
					actions[player^1][actoadd-1].addWeight(actions[player][action1], weight);

				}




			}
		}
		return actions;
	}

	public static void testLouvainClustering()
	{
		MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"0"+Parameters.GAMUT_GAME_EXTENSION));
		Action[][] actions = makeDirectedGraphFromNFG(tstgame, 1);
		printNeighbors(actions);
		try {
			clusterActions(actions, 3);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}








	public static List<Integer>[][] getFixedLouvainClustering(MatrixGame tstgame, int numberofcluster, double margin, int limit_comsize)
	{
		//MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"0"+Parameters.GAMUT_GAME_EXTENSION));
		Action[][] actions = makeDirectedGraphFromNFG(tstgame, margin);
		printNeighbors(actions);
		try {
			//ArrayList<HierCommunity> hcoms = clusterActions(actions, numberofcluster);
			ArrayList<HierCommunity> hcoms = clusterActionsFixedSize(actions, numberofcluster, limit_comsize);

			checkIfEmptyCommunityAndAdjust(hcoms, numberofcluster);



			int numberofplayers = 2;

			List<Integer>[][] partition = new List[numberofplayers][];
			for(int i=0; i< partition.length; i++)
			{
				partition[i] = new List[hcoms.size()];
			}
			for(int i=0; i< 2; i++)
			{

				for(int j =0; j< hcoms.size(); j++)
				{
					partition[i][j] = new ArrayList<Integer>(); 
				}
			}

			int cluster = 0;
			for(HierCommunity hcom: hcoms)
			{
				/*
				 * contains actions for both player 1 and 2
				 */
				for(Node n: hcom.nodes)
				{
					for(Action a: n.actions)
					{
						partition[a.player][cluster].add(a.action);
					}

				}
				cluster++;
			}

			GameReductionBySubGame.printPartition(partition, 1);

			return partition;






		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}









	public static List<Integer>[][] getLouvainClustering(MatrixGame tstgame, int numberofcluster, double margin)
	{
		//MatrixGame tstgame = new MatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH+"0"+Parameters.GAMUT_GAME_EXTENSION));
		Action[][] actions = makeDirectedGraphFromNFG(tstgame, margin);
		printNeighbors(actions);
		try {
			ArrayList<HierCommunity> hcoms = clusterActions(actions, numberofcluster);

			checkIfEmptyCommunityAndAdjust(hcoms, numberofcluster);



			int numberofplayers = 2;

			List<Integer>[][] partition = new List[numberofplayers][];
			for(int i=0; i< partition.length; i++)
			{
				partition[i] = new List[hcoms.size()];
			}
			for(int i=0; i< 2; i++)
			{

				for(int j =0; j< hcoms.size(); j++)
				{
					partition[i][j] = new ArrayList<Integer>(); 
				}
			}

			int cluster = 0;
			for(HierCommunity hcom: hcoms)
			{
				/*
				 * contains actions for both player 1 and 2
				 */
				for(Node n: hcom.nodes)
				{
					for(Action a: n.actions)
					{
						partition[a.player][cluster].add(a.action);
					}

				}
				cluster++;
			}

			GameReductionBySubGame.printPartition(partition, 1);

			return partition;






		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}






	private static void checkIfEmptyCommunityAndAdjust(
			ArrayList<HierCommunity> hcoms, int numcluster) {


		/*System.out.println("hcom size" + hcoms.size());

		int newcommunity = hcoms.get(hcoms.size()-1).community+1;


		if(hcoms.size()>numcluster)
		{
			int hcomtodisappear = hcoms.size() - numcluster;

			for(int i=0; i<hcomtodisappear; i++)
			{
		 *//**
		 * get the last community and merge it to the community ahead
		 *//*
				HierCommunity htogone = hcoms.get(hcoms.size()-(i+1));
				HierCommunity htogetbiiger = hcoms.get(hcoms.size()-(i+2));

				for(Node n: htogone.nodes)
				{
					for(Action a: n.actions)
					{
						a.communityindex = htogetbiiger.community;
						htogetbiiger.nodes.get(0).actions.add(a);
					}
				}

				hcoms.remove(hcoms.size()-(i+1));



			}


		}


		if(hcoms.size()==(numcluster-1))
		{
		  *//**
		  * create a new hcom
		  *//*
			HierCommunity newcom = new HierCommunity();
		   *//**
		   * extract an action and its best response from the first community
		   *//*
			Action a = hcoms.get(0).nodes.get(0).actions.get(0);
			System.out.println(" action to be extracted "+ a.action + ", com "+hcoms.get(0).community+", pl "+a.player + ", bestr "+ a.bestresponse);

		    *//**
		    * find the best response action and extract that too
		    *//*
			Action ares;
			int comcounter = 0;
			boolean found = false;
			int actionindex = -1;
			for(HierCommunity hc: hcoms)
			{
				for(Node n: hc.nodes)
				{
					actionindex = 0;
					for(Action r: n.actions)
					{
						if(r.action==a.bestresponse && (a.player!=r.player))
						{
							System.out.println("Found best response "+ r.action + " player "+r.player+" in community "+ hc.community);
							ares = r;
							r.communityindex = newcommunity;
							found = true;

							break;
						}
						actionindex++;
					}
				}
				if(found)
					break;
				comcounter++;
			}



			newcom.community = newcommunity;
			a.communityindex = newcommunity;
			Node nnode = new Node();
			nnode.community = newcommunity;
			nnode.actions.add(a);
			nnode.actions.add(hcoms.get(comcounter).nodes.get(0).actions.get(actionindex));

			//if(ares!=null)
			//ares.communityindex = newcommunity;
			newcom.nodes.add(nnode);
			hcoms.get(comcounter).nodes.get(0).actions.remove(actionindex);
			hcoms.get(0).nodes.get(0).actions.remove(0);

			hcoms.add(newcom);






		}*/



		/**
		 * try to find of in one community the number of action is 1. 
		 */

		printHCommunity(hcoms);

		for(int i=0; i<hcoms.size(); i++)
		{
			if(ifoneplayeraction(hcoms.get(i)))
			{
				Action a = hcoms.get(i).nodes.get(0).actions.get(0);
				System.out.println("Action "+ a.action + ", pla "+ a.player + ", com "+ a.communityindex + " needs partner "+ a.bestresponse);

				Action ares;
				int comcounter = 0;
				boolean found = false;
				int actionindex = -1;
				for(HierCommunity hc: hcoms)
				{
					for(Node n: hc.nodes)
					{
						actionindex = 0;
						for(Action r: n.actions)
						{
							if(r.action==a.bestresponse && (a.player!=r.player))
							{
								System.out.println("Found best response "+ r.action + " player "+r.player+" in community "+ hc.community);
								ares = r;
								r.communityindex = a.communityindex;
								found = true;

								break;
							}
							actionindex++;
						}
						if(found)
							break;
						//comcounter++;

					}
					if(found)
						break;
					comcounter++;

				}

				//if(hcoms.get(comcounter).nodes.get(0).actions.size()>1)
				//	{
				//hcoms.get(i).nodes.get(0).actions.add(hcoms.get(comcounter).nodes.get(0).actions.get(actionindex));
				//hcoms.get(comcounter).nodes.get(0).actions.remove(actionindex);
				//	}
				//	else
				{
					boolean b= false;
					for(int k=0; k<hcoms.size(); k++)
					{
						if(hcoms.get(k).nodes.get(0).actions.size()>2)
						{
							for(int m=0; m<hcoms.get(k).nodes.get(0).actions.size(); m++)
							{
								if(hcoms.get(k).nodes.get(0).actions.get(m).player!=a.player)
								{
									hcoms.get(k).nodes.get(0).actions.get(m).communityindex= a.communityindex;
									hcoms.get(i).nodes.get(0).actions.add(hcoms.get(k).nodes.get(0).actions.get(m));

									hcoms.get(k).nodes.get(0).actions.remove(m);
									i=0;
									b= true;
									break;
								}
							}
						}
						if(b)
							break;
					}
				}




			}
		}









	}











	private static boolean ifoneplayeraction(HierCommunity hierCommunity) {


		boolean pl0= false;
		boolean pl1 = false;
		for(int i=0; i<hierCommunity.nodes.get(0).actions.size(); i++)
		{
			if(hierCommunity.nodes.get(0).actions.get(i).player==0)
			{
				pl0=true;
			}
			if(hierCommunity.nodes.get(0).actions.get(i).player==1)
			{
				pl1=true;
			}

			if(pl0 && pl1)
				return false;


		}
		if(pl0 && pl1)
			return false;


		return true;
	}























	public static void printNeighbors(Action[][] actions)
	{
		/*for(int player=0; player<2; player++)
		{
			System.out.println("player "+ player);
			for(int action=0; action<actions[player].length; action++ )
			{
				System.out.println("\naction "+ (action+1) +", player "+actions[player][action].player + "\nNeighbors: ");
				for(Action x: actions[player][action].neighbors)
				{
					System.out.print("player : "+x.player+", action : "+x.action+ ", weight: " + actions[player][action].weights.get(x)+ "\n");
				}
				System.out.println();
			}
		}
		 */
	}




}

class Action{

	int action;
	int bestresponse;
	/**
	 * don't forget to change this when community changes
	 */
	int communityindex;
	ArrayList<Action> neighbors = new ArrayList<Action>();
	ArrayList<ArrayList<ArrayList<Action>>> nodeneighbors = new ArrayList<ArrayList<ArrayList<Action>>>();
	HashMap<Action, Double> weights = new HashMap<Action, Double>();
	HashMap<ArrayList<ArrayList<ArrayList<Action>>>, Double> nodeweights = new HashMap<ArrayList<ArrayList<ArrayList<Action>>>, Double>();
	boolean samenode = false;
	int player;

	public void addNeighbor(Action neighbor)
	{
		if(!neighbors.contains(neighbor))
		{
			neighbors.add(neighbor);
		}
	}

	public void addWeight(Action neighbor, double weight)
	{
		if(!weights.containsKey(neighbor))
		{
			weights.put(neighbor, weight);
		}
	}



}


class Node
{
	int id;
	ArrayList<Action> actions = new ArrayList<Action>();
	ArrayList<Node> neighbors = new ArrayList<Node>();
	HashMap<Node, Double> neiweights = new HashMap<Node, Double>();
	int community ;


	public void addNeighbor(Node neighbor)
	{
		if(!neighbors.contains(neighbor))
		{
			neighbors.add(neighbor);
		}
	}

	public void addWeight(Node neighbor, Double weight)
	{
		if(!neiweights.containsKey(neighbor))
		{
			neiweights.put(neighbor, weight);
		}
	}
}

class HierCommunity
{
	ArrayList<Node> nodes = new ArrayList<Node>();
	//ArrayList<Action> nodeneighbors = new ArrayList<Action>();
	//HashMap<ArrayList<Action>, Double> nodeneighborweights = new HashMap<ArrayList<Action>, Double>();




	//ArrayList<HierCommunity> hcomneighbors = new ArrayList<HierCommunity>();
	//HashMap<HierCommunity, Double> hcomneighborweights = new HashMap<HierCommunity, Double>();
	int community;


	/*public void addNeighbor(HierCommunity neighbor)
	{
		if(!hcomneighbors.contains(neighbor))
		{
			hcomneighbors.add(neighbor);
		}
	}

	public void addWeight(HierCommunity neighbor, double weight)
	{
		if(!hcomneighborweights.containsKey(neighbor))
		{
			hcomneighborweights.put(neighbor, weight);
		}
	}*/



}
