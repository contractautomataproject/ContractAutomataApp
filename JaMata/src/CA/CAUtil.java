package CA;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import FMCA.FMCATransition;


/**
 * Utilities for CA: product, aproduct
 * 
 * @author Davide Basile
 *
 */
public class CAUtil 
{

	static boolean debug = false;
	/**
	 * compute the product automaton of the CA given in aut
	 * @param aut the operands of the product
	 * @return the composition of aut
	 */
	public static CA composition(CA[] aut)
	{
		if (aut.length==1)
			return aut[0];
		/**
		 * compute rank, states, initial states, final states
		 */
		int prodrank = 0;
		for (int i=0;i<aut.length;i++)
		{
			prodrank = prodrank+(aut[i].getRank()); 
		}
		int[] statesprod = new int[prodrank];
		int[][] finalstatesprod = new int[prodrank][];
		int[] initialprod = new int[prodrank];
		int totnumstates=1;
		int pointerprodrank=0;
		for (int i=0;i<aut.length;i++)
		{
			for (int j=0;j<aut[i].getRank();j++)
			{
				statesprod[pointerprodrank]= aut[i].getStatesCA()[j];		
				totnumstates *= statesprod[pointerprodrank];
				finalstatesprod[pointerprodrank] = aut[i].getFinalStatesCA()[j];
				initialprod[pointerprodrank] = aut[i].getInitialCA().getState()[j];
				pointerprodrank++;
			}
		}
		
		/**
		 * compute transitions, non associative
		 * 
		 * scan all pair of transitions, if there is a match
		 * then generate the match in all possible contexts		 
		 * it also generates the independent moves, then clean from invalid transitions 
		 */
		CATransition[][] prodtr = aut[0].createArrayTransition2(aut.length); //new CATransition[aut.length][];
		int trlength = 0;
		for(int i=0;i<aut.length;i++)
		{
			prodtr[i]= aut[i].getTransition();
			trlength += prodtr[i].length;
		}
		CATransition[] transprod = aut[0].createArrayTransition((trlength*(trlength-1)*totnumstates)); // new CATransition[(trlength*(trlength-1)*totnumstates)]; //Integer.MAX_VALUE - 5];////upper bound to the total transitions 
		//int pointertemp = 0;
		int pointertransprod = 0;
		for (int i=0;i<prodtr.length;i++)// for all the automaton in the product
		{
			CATransition[] t = prodtr[i];
			for (int j=0;j<t.length;j++)  // for all transitions of automaton i
			{
				CATransition[][] temp = aut[0].createArrayTransition2(trlength*(trlength-1)); //new CATransition[trlength*(trlength-1)][];
				//Transition[] trtemp = new CATransition[trlength*(trlength-1)];//stores the other transition involved in the match in temp
				int pointertemp=0; //reinitialized each new transition
				boolean match=false;
				for (int ii=0;ii<prodtr.length;ii++)    //for all others automaton
				{
					if (ii!=i)
					{
						CATransition[] tt = prodtr[ii];
						for (int jj=0;jj<tt.length;jj++)    //for all transitions of other automatons
						{
							if (CATransition.match(t[j].getLabelP() ,tt[jj].getLabelP())) //match found
							{
								match=true;
								CATransition[] gen;
								if (i<ii)
									 gen = generateTransitions(t[j],tt[jj],i,ii,aut);
								else
									gen = generateTransitions(tt[jj],t[j],ii,i,aut);
								temp[pointertemp]=gen; //temp is temporary used for comparing matches and offers/requests
								//trtemp[pointertemp]=tt[jj];
								pointertemp++;
								for (int ind=0;ind<gen.length;ind++)
									//copy all the matches in the transition of the product automaton, if not already in !
								{
									boolean copy=true;
									for (int ind2=0;ind2<pointertransprod;ind2++)
									{
										if (transprod[ind2].equals(gen[ind]))
										{
											copy=false;
											break;
										}
									}
									if(copy)
									{
										transprod[pointertransprod]=gen[ind]; 
										if (debug)
											System.out.println(gen[ind].toString());
										pointertransprod++;
									}
								}
							}
						}
					}
				}
				/*insert only valid transitions of gen, that is a principle moves independently in a state only if it is not involved
				  in matches. The idea is  that firstly all the independent moves are generated, and then we remove the invalid ones.  
				  */	

				CATransition[] gen = generateTransitions(t[j],null,i,-1,aut);
				if ((match)&&(gen!=null))		
				{
					/**
					 * extract the first transition of gen to check the principal who move 
					 * and its state 
					 */
					CATransition tra = gen[0];
					String[] lab = tra.getLabelP(); 
					int pr1=-1;
					for (int ind2=0;ind2<lab.length;ind2++)
					{
						if (lab[ind2]!=CATransition.idle)
						{
							pr1=ind2; //principal
						}
					}
					String label = tra.getLabelP()[pr1];  //the action of the principal who moves
					for (int ind3=0;ind3<gen.length;ind3++)
					{
						for (int ind=0;ind<pointertemp;ind++)
							for(int ind2=0;ind2<temp[ind].length;ind2++)
							{	
								if(gen[ind3]!=null)
								{
									if (Arrays.equals(gen[ind3].getSourceP().getState(),temp[ind][ind2].getSourceP().getState()) &&  //the state is the same
											label==temp[ind][ind2].getLabelP()[pr1]) //pr1 makes the same move
									{
										gen[ind3]=null;
									}
								}
							}
					}
					
					/**
					 * 
					 *
					for (int ind=0;ind<pointertemp;ind++)
					{
						/**
						 * extract the first transition of temp[ind] to check the two principals who move in the match
						 * and their state (they are equal in all inner transitions)
						 *
						CATransition tra = temp[ind][0];
						int[] lab = tra.getLabelP(); 
						int pr1=-1;int pr2=-1;
						for (int ind2=0;ind2<lab.length;ind2++)
						{
							if (lab[ind2]!=0)
							{
								if (pr1==-1)
									pr1=ind2; //principal 1
								else
									pr2=ind2; //principal 2
							}
						}
						int[] source = tra.getInitialP();
						int pr1s = source [pr1];  //principal 1 state
						int pr2s = source [pr2];  //principal 2 state
						/**
						 * in all the non-match transitions generated in gen, if the two principals are in the initial state, then
						 * in that particular state the non-match transition of one of the two was not allowed and it is removed
						 *
						for (int ind2=0;ind2<gen.length;ind2++)
						{
							if(gen[ind2]!=null)
							{
								if ((gen[ind2].getInitialP()[pr1]==pr1s)&&(gen[ind2].getInitialP()[pr2]==pr2s))
									gen[ind2]=null;
							}
						}
					}
					*/					
				}
				/**
				 * finally insert only valid independent moves in transprod
				 */
				for (int ind=0;ind<gen.length;ind++)
				{
					if (gen[ind]!=null)
					{
						try{
						transprod[pointertransprod]=gen[ind];
						}catch(ArrayIndexOutOfBoundsException e){
							e.printStackTrace();
							}
						if (debug)
							System.out.println(gen[ind].toString());
						pointertransprod++;
					}
				}
			}
		}
		/**
		 * remove all unused space in transProd (null at the end of the array)
		 */
		CATransition[] finalTr = aut[0].createArrayTransition(pointertransprod); //new CATransition[pointertransprod];
		for (int ind=0;ind<pointertransprod;ind++)
			finalTr[ind]= (CATransition)transprod[ind];
		
		CA prod = aut[0].createNew(prodrank,new CAState(initialprod, CAState.type.INITIAL),statesprod,finalstatesprod,finalTr);
		
		if (debug)
			System.out.println("Remove unreachable ...");
		prod = removeUnreachableTransitions(prod);
		
		return prod;
	}
	
	/**
	 * remove the unreachable transitions from aut
	 * @param at	the CA
	 * @return	a new CA clone of aut with only reachable transitions
	 */
	public static CA removeUnreachableTransitions(CA at)
	{
		CA aut = at.clone();
		CATransition[] finalTr=aut.getTransition();
		
		/**
		 * remove unreachable transitions
		 */
		int removed=0;
		int reachablepointer=1; //era messo a uno forse per lo stato iniziale, ma viene cmq letto nelle transizioni
		int unreachablepointer=0;
		int[][] reachable = new int[at.prodStates()][]; 
		int[][] unreachable = new int[at.prodStates()][];
		reachable[0]=aut.getInitialCA().getState();
		for (int ind=0;ind<finalTr.length;ind++)
		{
			//for each transition t checks if the source state of t is reachable from the initial state of the CA
			CATransition t=(CATransition)finalTr[ind];
			int[] source = t.getSourceP().getState();
//			if (ind==5)
//			{
//				System.out.println("debug");
//			}
			boolean found=false; //source state must not have been already visited (and inserted in either reachable or unreachable)
			for (int i=0;i<unreachablepointer;i++)
			{
				if (Arrays.equals(unreachable[i],source))
				{
					found=true;
					finalTr[ind]=null;
					removed++;
					break;
				}
			}
			if (!found)
			{
				for (int i=0;i<reachablepointer;i++)
				{
					if (Arrays.equals(reachable[i],source))
					{
						found=true;
						break;
					}
				}
			}
			
			/**
			int[] debugg = {0,0,1};
			if (Arrays.equals(s,debugg))
				System.out.println("debug");*/

			if (!found)
			{
				int[] pointervisited = new int[1];
				pointervisited[0]=0;
				if (debug)
					System.out.println("Checking Reachability state "+Arrays.toString(source));
				if(!amIReachable(source,aut,aut.getInitialCA().getState(),new int[aut.prodStates()][],pointervisited,reachable,unreachable,reachablepointer,unreachablepointer))
				{
					finalTr[ind]=null;
					removed++;
//					if (unreachable.length==unreachablepointer)
//					{
//						unreachablepointer++;
//						FMCATransition[] debug=FMCATransition.getTransitionFrom(t.getSourceP(), (FMCATransition[]) finalTr);
//						unreachablepointer++;
//					}
					unreachable[unreachablepointer]=source;
					unreachablepointer++;
				}
				else
				{
					reachable[reachablepointer]=source;
					reachablepointer++;
				}
			}
		}
		
		/**
		 * remove holes (null) in finalTr2
		 */
//		int pointer=0;
//		CATransition[] finalTr2 = new CATransition[finalTr.length-removed];
//		for (int ind=0;ind<finalTr.length;ind++)
//		{
//			if (finalTr[ind]!=null)
//			{
//				finalTr2[pointer]=finalTr[ind];
//				pointer++;
//			}
//		}
		
		finalTr= CAUtil.removeHoles(finalTr, removed);
		aut.setTransition(finalTr);
		return aut;
	}
	
	/**
	 * remove transitions who do not reach a final state
	 * @param at	the CA
	 * @return	CA without hanged transitions
	 */
	public static CA removeDanglingTransitions(CA at)
	{
		CA aut = at.clone();
		CATransition[] finalTr=aut.getTransition();
		int removed=0;
		int pointerreachable=0;
		int pointerunreachable=0;
		int[][] reachable = new int[at.prodStates()][]; 
		int[][] unreachable = new int[at.prodStates()][];
		int[][] fs = aut.allFinalStates();
		
		for (int ind=0;ind<finalTr.length;ind++)
		{
			// for each transition checks if the arrival state s is reachable from one of the final states of the ca
			CATransition t=(CATransition)finalTr[ind];
			int[] arr = t.getTargetP().getState();

			boolean remove=true;
			
			for (int i=0;i<fs.length;i++)
			{
				int[] pointervisited = new int[1];
				pointervisited[0]=0;
				if(amIReachable(fs[i],aut,arr,new int[aut.prodStates()][],pointervisited,reachable,unreachable,pointerreachable,pointerunreachable)) //if final state fs[i] is reachable from arrival state arr
					remove = false;
			}
			//if t does not reach any final state then remove
			if(remove)
			{
				finalTr[ind]=null;
				removed++;
			}			
		}
			
			
		/**
		 * remove holes (null) in finalTr2
		 */
		int pointer=0;
		CATransition[] finalTr2 = new CATransition[finalTr.length-removed];
		for (int ind=0;ind<finalTr.length;ind++)
		{
			if (finalTr[ind]!=null)
			{
				finalTr2[pointer]=finalTr[ind];
				pointer++;
			}
		}
		aut.setTransition(finalTr2);
		return aut;
	}
	
	/**
	 * true if state[] is reachable from  from[]  in aut
	 * @param state
	 * @param aut
	 * @param visited
	 * @param pointervisited
	 * @return  true if state[] is reachable from  from[]  in aut
	 */

	//TODO pointerunreachable is never updated, probably is not needed. In case this method is called multiple times from another method, substitute 
	//it with a method which computes a forward visit of the graph only once
	public static boolean amIReachable( int[] state, CA aut, int[] from, int[][] visited, int[] pointervisited,int[][] reachable,int[][] unreachable, int pointerreachable,int pointerunreachable )
	{
		if (Arrays.equals(state,from))
			return true;
		for (int i=0;i<pointerunreachable;i++)
		{
			if (Arrays.equals(unreachable[i],state))
				return false;
		}
		for (int i=0;i<pointerreachable;i++)
		{
			if (Arrays.equals(reachable[i],state))
				return true;
		}
		
		for (int j=0;j<pointervisited[0];j++)
		{
			if (Arrays.equals(visited[j],state))
			{
				return false;		//detected a loop, state has not been reached 
			}
		}
		visited[pointervisited[0]]=state;
		pointervisited[0]++;
		
		//if (debug)
		//	System.out.println("Visited "+pointervisited[0]+" "+Arrays.toString(visited[pointervisited[0]-1]));
		CATransition[] t = aut.getTransition();
		for (int i=0;i<t.length;i++)
		{
			if (t[i]!=null)
			{
				if (Arrays.equals(state,t[i].getTargetP().getState()))
				{
					if (amIReachable(t[i].getSourceP().getState(),aut,from,visited,pointervisited,reachable,unreachable,pointerreachable,pointerunreachable))
						return true;
				}
			}
		}
		return false;
	}
		
	/**
	 * 
	 * @param t  first transition made by one CA
	 * @param tt second transition if it is a match, otherwise null
	 * @param i  the index of the CA whose transition is t
	 * @param ii the index of the CA whose transition is tt or -1
	 * @param aut all the CA to be in the transition
	 * @return an array of transitions where i (and ii) moves and the other stays idle in each possible state 
	 */
	protected static CATransition[] generateTransitions(CATransition t, CATransition tt, int i, int ii, CA[] aut)
	{
		/**
		 * preprocessing to the recursive method recgen:
		 * it computes  the values firstprinci,firstprincii,numtransitions,states
		 */
		int prodrank = 0; //the sum of rank of each CA in aut, except i and ii
		int firstprinci=-1; //index of first principal in aut[i] in the list of all principals in aut
		int firstprincii=-1; //index of first principal in aut[ii] in the list of all principals in aut
		int[] states=null; //the number of states of each principal, except i and ii
		int productNumberOfStatesExceptIandII=1; //contains the product of the number of states of each principals, except for those of i and ii
		

		if (tt!= null) //if is a match
		{			
			/**
			 * first compute prodrank, firstprinci,firstprincii
			 */
			for (int ind=0;ind<aut.length;ind++)
			{
				if ((ind!=i)&&(ind!=ii))
					prodrank += (aut[ind].getRank()); 
				else 
				{
					if (ind==i)
						firstprinci=prodrank; //these values are handled inside generateATransition static method
					else 
						firstprincii=prodrank; //note that firstprinci and firstprincii could be equal
				}
					
			}
			if (prodrank!=0)
			{				
				states = new int[prodrank]; 
				int indstates=0;
				//filling the array states with number of states of all principals of CA in aut except of i and ii
				for (int ind=0;ind<aut.length;ind++) 
				{
					if ((ind!=i)&&(ind!=ii)) 
					{
						int[] statesprinc=aut[ind].getStatesCA();
						for(int ind2=0;ind2<statesprinc.length;ind2++)
							{						
								states[indstates]=statesprinc[ind2];
								productNumberOfStatesExceptIandII*=states[indstates];
								indstates++;
							}
					}
				}		
			}
		}
		else	//is not a match
		{
			for (int ind=0;ind<aut.length;ind++)
			{
				
				if (ind!=i) 
					prodrank = prodrank+(aut[ind].getRank()); 
				else if (ind==i)
					firstprinci=prodrank;					
			}
			if(prodrank!=0)
			{
				states = new int[prodrank]; //the number of states of each principal except i 
				int indstates=0;
				//filling the array states
				for (int ind=0;ind<aut.length;ind++)
				{
					if (ind!=i)
					{
						int[] statesprinc=aut[ind].getStatesCA();
						for(int ind2=0;ind2<statesprinc.length;ind2++)
							{						
								states[indstates]=statesprinc[ind2];
								productNumberOfStatesExceptIandII*=states[indstates];
								indstates++;
							}
					}
				}	
			}
		}
		CATransition[] tr = aut[0].createArrayTransition(productNumberOfStatesExceptIandII); 
		
		aut=CAUtil.extractAllPrincipals(aut); //TODO this must be shift to method composition, to be called only once!
		
		
		if(prodrank!=0)
		{
			int[] insert= new int[states.length];
			//initialize insert to zero in all component
			for (int ind=0;ind<insert.length;ind++)
				insert[ind]=0;
			recGen(t,tt,firstprinci, firstprincii,tr,states,0, states.length-1, insert,aut); //first call insert = [0,0,0, ...,0]
		}
		else
			tr[0]=t.generateATransition(t,tt,0,0,new int[0],aut);
		return tr;
	}
	
	
	/**
	 * 
	 * recursive method that generates all combinations of transitions with all possible states of principals that are idle 
	 * it must start from the end of array states
	 * 
	 * 
	 * example of evolution considering  array insert where states = [2,2,2], the dot is indstates the array is insert
	 *  
	 * [0,0,0.] -> C1 : [0,0,1.] -> C1 : [0,0,2.] -> C2 : [0,0.,0] -> C3 : [0,1,0.]     
	 * -> C1 : [0,1,1.] -> C1 : [0,1,2.] -> C2 : [0,1.,0] -> C3 : [0,2,0.]
	 * -> C1 : [0,2,1.] -> C1 : [0,2,2.] -> C2 : [0,2.,0] -> C2 : [0.,0, 0] -> C3 : [1, 0, 0.]
	 * -> C1 : [1, 0, 1.] ->  ... -> C2 [2, 2, 2] (indstates=-1) -> C4 termination
	 * 
	 * 
	 * @param t		first transition who moves
	 * @param tt	second transition who moves or null if it is not a match
	 * @param fi	offset of first CA who moves in list of principals
	 * @param fii	offset of second CA who moves in list of principals or empty
	 * @param cat	side effect: modifies cat by adding the generated transitions   -->modified at each iteration
	 * @param states	the number of states of each idle principal
	 * @param indcat	pointer in the array cat, the first call must be 0			-->modified at each iteration
	 * @param indstates	pointer in the array states, the first call must be states.length-1 	-->modified at each iteration
	 * @param insert    it is used to generate all the combinations of states of idle principals, the first must be all zero  -->modified at each iteration
	 * @param aut		array of automata, it is used in generateATransition of FMCA to retrieve the states of idle principals using insert as pointer  --> not modified
	 */
	private static void recGen(CATransition t, CATransition tt, int fi, int fii, CATransition[] cat,  int[] states, int indcat, int indstates, int[] insert,CA[] aut)
	{
		if (indstates==-1) //C4
			return;
		if (insert[indstates]==states[indstates]) /// C2
		{
			insert[indstates]=0;
			indstates--;
			recGen(t,tt,fi,fii,cat,states,indcat,indstates,insert,aut);
		}
		else
		{
			if (indstates==states.length-1)//C1   first calls
			{
				cat[indcat]=t.generateATransition(t,tt,fi,fii,insert,aut); //here insert contains the states of the idle principals in the transition
				indcat++;
				insert[indstates]++;
				recGen(t,tt,fi,fii,cat,states,indcat,indstates,insert,aut);
			}
			else  //C3
			{
				insert[indstates]++; 
				if (insert[indstates]!=states[indstates])
					indstates=states.length-1;
				recGen(t,tt,fi,fii,cat,states,indcat,indstates,insert,aut);				
			}
		}
	}
	
	
	/**
	 * 
	 * Generates all possible combinations of the states in fin, stored in modif. Here for indmod I used 
	 * an array where I always read the element indmod[0] instead of directly passing an integer.
	 * 
	 * very similar to recGen for transitions. The only difference is that here instead of generateATransition a novel state 
	 * is added to modif, where basically the array insert generates all combinations of final states.
	 * 
	 * @param fin	the array of final states of each principal
	 * @param modif		the array of final states of the composition, modified by side effect
	 * @param states	states[i] = fin[i].length
	 * @param indmod	index in modif, the first call must be 0		modified by the method
	 * @param indstates		the index in states, the first call must be states.length-1		modified by the method
	 * @param insert	it is used to generate all the combinations of final states, the first call must be all zero		modified by the method
	 */
	public static void recGen(int[][] fin, int[][] modif,  int[] states, int indmod[], int indstates[], int[] insert)
	{
		if (indstates[0]==-1)
			return;
		if (insert[indstates[0]]==states[indstates[0]])
		{
			insert[indstates[0]]=0;
			indstates[0]--;
			recGen(fin,modif,states,indmod,indstates,insert);
		}
		else
		{
			if (indstates[0]==states.length-1)
			{
				modif[indmod[0]]=new int[insert.length];
				for(int i=0;i<insert.length;i++)
				{
					modif[indmod[0]][i]=fin[i][insert[i]];
				}
				indmod[0]++;
				insert[indstates[0]]++;
				recGen(fin,modif,states,indmod,indstates,insert);
			}
			else
			{
				insert[indstates[0]]++; 
				if (insert[indstates[0]]!=states[indstates[0]])
					indstates[0]=states.length-1;
				recGen(fin,modif,states,indmod,indstates,insert);				
			}
		}
	}
	
	
	
	public static CA[] extractAllPrincipals(CA[] aut)
	{
		CA[][] principals = new CA[aut.length][];
		int allprincipals=0;
		for (int j=0;j<principals.length;j++)
		{
			principals[j]= aut[j].allPrincipals();
			allprincipals+=principals[j].length;
		}
		CA[] onlyprincipal = new CA[allprincipals];
		for (int j=0;j<principals.length;j++)
		{
			for (int z=0;z<principals[j].length;z++)
				onlyprincipal[j+z]=principals[j][z];
		}
		return onlyprincipal;
	}
	
	/**
	 * compute the associative product of the CA in the array a
	 * @param a  array of CA
	 * @return  the associative product
	 */
	public static CA aproduct(CA[] a)
	{
		int tot=0;
		for (int i=0;i<a.length;i++)
			tot+=a[i].getRank();
		if (tot==a.length)
			return composition(a);
		else
		{
			CA[] a2=new CA[tot];
			int pointer=0;
			for(int i=0;i<a.length;i++)
			{
				if(a[i].getRank()>1)
				{
					for (int j=0;j<a[i].getRank();j++)
					{
						a2[pointer]=a[i].proj(j);
						pointer++;
					}
				}
				else
				{
					a2[pointer]=a[i];
					pointer++;
				}
			}
			return composition(a2);
		}
			
	}
	
	/**
	 * Testing the CA
	 */
	public static void CATest()
	{
		try{
			InputStreamReader reader = new InputStreamReader (System.in);
			BufferedReader myInput = new BufferedReader (reader);
			CA prod;
			CA[] aut=null;
			CA a;
			String s="";
			do
			{
				System.out.println("Select an operation");
				System.out.println("1 : product \n2 : projection \n3 : aproduct \n4 : strongly safe \n5 : strong agreement \n6 : safe \n7 : agreement \n8 : strong most permissive controller \n9 : most permissive controller \n10 : branching condition \n11 : mixed choice  \n12 : extended branching condition \n13 : liable \n14 : strongly liable \n15 : exit ");
				s = myInput.readLine();
				if(!s.equals("15"))
				{
					System.out.println("Reset stored automaton...");
					aut=load();
				}
				switch (s)
				{
				case "1":
					System.out.println("Computing the product automaton ... ");
					prod = CAUtil.composition(aut);
					prod.print();
			        //FSA.write(prod);
					prod.printToFile("");
					break;

				case "2":
					System.out.println("Computing the projection of the last CA loaded, insert the index of the principal:");
					s=myInput.readLine();
					int ind = Integer.parseInt(s);
					CA projected = aut[aut.length-1].proj(ind);
					projected.print();
					//FSA.write(projected);
					projected.printToFile("");
					break;

				case "3":
					System.out.println("Computing the associative product automaton ... ");
					prod = CAUtil.aproduct(aut);
					prod.print();
			        //FSA.write(prod);
					prod.printToFile("");
					break;

				case "4":
					a = aut[aut.length-1];
					a.print();
					if (a.strongSafe())
						System.out.println("The CA is strongly safe");
					else
						System.out.println("The CA is not strongly safe");
			        //FSA.write(a);
					a.printToFile("");
					break;

				case "5":
					a = aut[aut.length-1];
					a.print();
					if (a.strongAgreement())
						System.out.println("The CA admits strong agreement");
					else
						System.out.println("The CA does not admit strong agreement");
			        //FSA.write(a);
					a.printToFile("");
					break;

				case "6":
					a = aut[aut.length-1];
					a.print();
					if (a.safe())
						System.out.println("The CA is safe");
					else
						System.out.println("The CA is not safe");
			        //FSA.write(a);
					a.printToFile("");
					break;

				case "7":
					a = aut[aut.length-1];
					a.print();
					if (a.agreement())
						System.out.println("The CA admits agreement");
					else
						System.out.println("The CA does not admit agreement");
			        //FSA.write(a);
					a.printToFile("");
					break;

				case "8":
					System.out.println("The most permissive controller of strong agreement for the last CA loaded is");
					a = aut[aut.length-1];
					CA smpc = a.smpc();
					smpc.print();
					//FSA.write(smpc);
					smpc.printToFile("");
					break;

				case "9":
					System.out.println("The most permissive controller of agreement for the last CA loaded is");
					a = aut[aut.length-1];
					CA mpc = a.mpc();
					mpc.print();
					//FSA.write(mpc);
					mpc.printToFile("");
					break;

				case "10":
					a = aut[aut.length-1];
					a.print();
					String[][] bc = a.branchingCondition();
					if (bc==null)
						System.out.println("The CA enjoys the branching condition");
					else
					{
						System.out.println("The CA does not enjoy the branching condition ");
						System.out.println("State "+bc[2][0]+" violates the branching condition because it has no transition labelled "+bc[1]+" which is instead enabled in state "+bc[0][0]);
					}
			        //FSA.write(a);
			        a.printToFile("");
					break;

				case "11":
					a = aut[aut.length-1];
					a.print();
					int[] st = a.mixedChoice();
					if (st!=null)
						System.out.println("The CA has a mixed choice state  "+Arrays.toString(st));
					else
						System.out.println("The CA has no mixed choice states");
			        //FSA.write(a);
			        a.printToFile("");
					break;

				case "12":
					a = aut[aut.length-1];
					a.print();
					String[][] ebc = a.extendedBranchingCondition();
					if (ebc==null)
						System.out.println("The CA enjoys the extended branching condition");
					else
					{
						System.out.println("The CA does not enjoy the extended branching condition ");
						System.out.println("State "+ebc[2][0]+" violates the branching condition because it has no transition labelled "+ebc[1]+" which is instead enabled in state "+ebc[0][0]);
					}
			        //FSA.write(a);
			        a.printToFile("");
					break;
				case"13":
					a = aut[aut.length-1];
					a.print();
					CATransition[] l = a.liable();
					System.out.println("The liable transitions are:");
					for(int i=0;i<l.length;i++)
						System.out.println(l[i].toString());
					//FSA.write(a);
					a.printToFile("");
					break;
				case"14":
					a = aut[aut.length-1];
					a.print();
					CATransition[] sl = a.strongLiable();
					System.out.println("The strongly liable transitions are:");
					for(int i=0;i<sl.length;i++)
						System.out.println(sl[i].toString());
					//FSA.write(a);
					a.printToFile("");
					break;
				}				
			}while(!s.equals("15"));

		}catch(Exception e){e.printStackTrace();}
	} 
	
	protected static CA[] load()
	{
		try
		{
			CA[] a = new CA[10];
			int i=0;
			CA automa;
			String s="";
			InputStreamReader reader = new InputStreamReader (System.in);
			BufferedReader myInput = new BufferedReader (reader);
			while (!s.equals("no")&&i<10)
			{
				System.out.println("Do you want to create/load other CA? (yes/no)");
				s = myInput.readLine();
				//s = "yes";
				if(!s.equals("no"))
				{
					System.out.println("Insert the name of the automaton (without file extension) to load or leave empty for create a new one");
					s = myInput.readLine();
					//s = "CA1";
			        if (!s.isEmpty())
			        {
			        	automa = CA.load(s);
			        }
			        else
			        	{
				        automa = new CA();
			        	}
			        automa.print();
			        a[i] = automa;
			        //s="no";
			        i++;
				}
			}
			CA[] aut;
			if (i<10)
			{
				aut=new CA[i];
				for (int ind=0;ind<i;ind++)
					aut[ind]=a[ind];
			}
			else
				aut=a;
			return aut;
		}catch(Exception e){e.printStackTrace();return null;}
	}
	
	protected static CATransition[] removeHoles(CATransition[] l, int holes )
	{
		/**
		 * remove holes (null) in t
		 */
		int pointer=0;
		CATransition[] fin = new CATransition[l.length-holes];
		for (int ind=0;ind<l.length;ind++)
		{
			if (l[ind]!=null)
			{
				fin[pointer]=l[ind];
				pointer++;
			}
		}
		return fin;
	}

}
