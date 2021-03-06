import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

public class BTSolver
{

	// =================================================================
	// Properties
	// =================================================================

	private ConstraintNetwork network;
	private SudokuBoard sudokuGrid;
	private Trail trail;

	private boolean hasSolution = false;

	public String varHeuristics;
	public String valHeuristics;
	public String cChecks;

	// =================================================================
	// Constructors
	// =================================================================

	public BTSolver ( SudokuBoard sboard, Trail trail, String val_sh, String var_sh, String cc )
	{
		this.network    = new ConstraintNetwork( sboard );
		this.sudokuGrid = sboard;
		this.trail      = trail;

		varHeuristics = var_sh;
		valHeuristics = val_sh;
		cChecks       = cc;
	}

	// =================================================================
	// Consistency Checks
	// =================================================================

	// Basic consistency check, no propagation done
	private boolean assignmentsCheck ( )
	{
		for ( Constraint c : network.getConstraints() )
			if ( ! c.isConsistent() )
				return false;

		return true;
	}

	// =================================================================
	// Arc Consistency
	// =================================================================
	public boolean arcConsistency ( )
    {
        List<Variable> toAssign = new ArrayList<Variable>();
        List<Constraint> RMC = network.getModifiedConstraints();
        for(int i = 0; i < RMC.size(); ++i)
        {
            List<Variable> LV = RMC.get(i).vars;
            for(int j = 0; j < LV.size(); ++j)
            {
                if(LV.get(j).isAssigned())
                {
                    List<Variable> Neighbors = network.getNeighborsOfVariable(LV.get(j));
                    int assignedValue = LV.get(j).getAssignment();
                    for(int k = 0; k < Neighbors.size(); ++k)
                    {
                        Domain D = Neighbors.get(k).getDomain();
                        if(D.contains(assignedValue))
                        {
                            if(D.size() == 1)
                                return false;
                            if(D.size() == 2)
                                toAssign.add(Neighbors.get(k));
                            trail.push(Neighbors.get(k));
                            Neighbors.get(k).removeValueFromDomain(assignedValue);
                        }
                    }
                }
            }
        }
        if(!toAssign.isEmpty())
        {
            for(int i = 0; i < toAssign.size(); ++i)
            {
                Domain D = toAssign.get(i).getDomain();
                ArrayList<Integer> assign = D.getValues();
                trail.push(toAssign.get(i));
                toAssign.get(i).assignValue(assign.get(0));
            }
            return arcConsistency();
        }
        return network.isConsistent();
    }


	/**
	 * Part 1 TODO: Implement the Forward Checking Heuristic
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 *
	 * Return: a pair of a HashMap and a Boolean. The map contains the pointers to all MODIFIED variables, mapped to their MODIFIED domain. 
	 *         The Boolean is true if assignment is consistent, false otherwise.
	 */
	public Map.Entry<HashMap<Variable,Domain>, Boolean> forwardChecking ( )
	{
		HashMap<Variable,Domain> fcMap = new HashMap<Variable,Domain>();
		Boolean fc_consistent = true;
		for ( Variable v : network.getVariables() ){
			if (v.isAssigned())
				{
					List<Variable> neighbours = network.getNeighborsOfVariable(v);
					int key = v.getAssignment();
					for(Variable vn: neighbours){
						if(vn.getDomain().contains(key))
						{
							if(vn.getDomain().size()==1)
								fc_consistent = false;
							trail.push(vn);
							vn.removeValueFromDomain(key);
						}
					}	
				}	
		}

		for ( Variable v : network.getVariables() ){
			if(!v.isAssigned()&&v.isModified())
				fcMap.put(v,v.getDomain());
		}
		
		for ( Constraint c : network.getConstraints() )
			if ( ! c.isConsistent() )
				fc_consistent =  false;

		return Pair.of(fcMap, fc_consistent);
	}

	/**
	 * Part 2 TODO: Implement both of Norvig's Heuristics
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * (2) If a constraint has only one possible place for a value
	 *     then put the value there.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 * Return: a pair of a map and a Boolean. The map contains the pointers to all variables that were assigned during the whole 
	 *         NorvigCheck propagation, and mapped to the values that they were assigned. 
	 *         The Boolean is true if assignment is consistent, false otherwise.
	 */
	public Map.Entry<HashMap<Variable,Integer>,Boolean> norvigCheck ( )
	{
		HashMap<Variable,Integer> NorvigMap = new HashMap<Variable,Integer>();
		Boolean fc_consistent = true;
		int count = 0;
		do{	//System.out.println(count);
			//System.out.println("egfagargearg");
			
			for ( Variable v : network.getVariables() ){
				if (v.isAssigned())
					{
						List<Variable> neighbours = network.getNeighborsOfVariable(v);
						int key = v.getAssignment();
						for(Variable vn: neighbours){
							if(vn.getDomain().contains(key))
							{
								if(vn.getDomain().size()==1)
									fc_consistent = false;
								trail.push(vn);
								vn.removeValueFromDomain(key);
							}
							
						}	
					}	
			}
			count = 0;
			for(Constraint c : network.getModifiedConstraints()){
				HashMap<Integer,Variable> A = new HashMap<Integer,Variable>();
				HashSet<Integer> B = new HashSet<Integer>();
				
				for(Variable v : c.vars){

					for(Integer i : v.getValues()){
						if(A.containsKey(i)){
							B.add(i);
						}
						else A.put(i,v);
					}
				}
				//count = 0;
				for(Map.Entry<Integer,Variable> entry : A.entrySet()){
					if(B.contains(entry.getKey())) continue;
					else if(!(entry.getValue().isAssigned())){
						count++;
						trail.push(entry.getValue());
						entry.getValue().assignValue(entry.getKey());
						NorvigMap.put(entry.getValue(),entry.getKey());
					}
				}

			}
			//System.out.println(count);
			//System.out.println(NorvigMap.size());
		}while(count > 0);

		for ( Constraint c : network.getConstraints() )
			if ( ! c.isConsistent() )
				fc_consistent =  false;
		
		//System.out.println("ended");
		return Pair.of(NorvigMap, network.isConsistent());
	}

	/**
	 * Optional TODO: Implement your own advanced Constraint Propagation
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private boolean getTournCC ( )
	{
		return arcConsistency();
	}

	// =================================================================
	// Variable Selectors
	// =================================================================

	// Basic variable selector, returns first unassigned variable
	private Variable getfirstUnassignedVariable()
	{
		for ( Variable v : network.getVariables() )
			if ( ! v.isAssigned() )
				return v;

		// Everything is assigned
		return null;
	}

	/**
	 * Part 1 TODO: Implement the Minimum Remaining Value Heuristic
	 *
	 * Return: The unassigned variable with the smallest domain
	 */
	public Variable getMRV ( )
	{
		int domain_size = Integer.MAX_VALUE;
		Variable mrv = null;
		for ( Variable v : network.getVariables() )
			if ( ! v.isAssigned() && v.getDomain().size() < domain_size)
				{
					mrv = v;
					domain_size = v.getDomain().size();
				}
		return mrv;
	}

	/**
	 * Part 2 TODO: Implement the Minimum Remaining Value Heuristic
	 *                with Degree Heuristic as a Tie Breaker
	 *
	 * Return: The unassigned variable with the smallest domain and affecting the most unassigned neighbors.
	 *         If there are multiple variables that have the same smallest domain with the same number 
	 *         of unassigned neighbors, add them to the list of Variables.
	 *         If there is only one variable, return the list of size 1 containing that variable.
	 */
	public List<Variable> MRVwithTieBreaker ( )
	{
		
		Variable mrv = getMRV();  // unassigned variable with the smallest domain
		Map<Integer,List<Variable>> degree = new HashMap<Integer,List<Variable>>();
		int ans = -1;
		for(Variable v: network.getVariables()){  
			if(!v.isAssigned()&&(v.getDomain().size()==mrv.getDomain().size())){ // unassigned VARIABLES with the smallest domain
 				int count = 0;
				List<Variable> neighbours = network.getNeighborsOfVariable(v);
				for(Variable n: neighbours){
					if(!n.isAssigned())
						count++;			// count of unassigned neighbours
				}
				ans = Math.max(ans,count);
				if(degree.containsKey(count))
					degree.get(count).add(v);
				else
				{
					List<Variable> l = new ArrayList<Variable>();
					l.add(v);
					degree.put(count,l);
				}			
			}
		}
		if(degree.get(ans)!=null)
			return degree.get(ans);
		else{
			List<Variable> l = new ArrayList<Variable>();
			l.add(mrv);
			return l;
		}
    }

	/**
	 * Optional TODO: Implement your own advanced Variable Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private Variable getTournVar ( )
	{
		return MRVwithTieBreaker().get(0);
	}

	// =================================================================
	// Value Selectors
	// =================================================================

	// Default Value Ordering
	public List<Integer> getValuesInOrder ( Variable v )
	{
		List<Integer> values = v.getDomain().getValues();

		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}

	/**
	 * Part 1 TODO: Implement the Least Constraining Value Heuristic
	 *
	 * The Least constraining value is the one that will knock the least
	 * values out of it's neighbors domain.
	 *
	 * Return: A list of v's domain sorted by the LCV heuristic
	 *         The LCV is first and the MCV is last
	 */
	public List<Integer> getValuesLCVOrder ( Variable v )
	{
		List<int[]> sorted_domain = new ArrayList<>();
		List<Variable> neighbours = network.getNeighborsOfVariable(v);
		List<Integer> A = v.getValues();
		for(int i=0;i<A.size();i++){
			int key = A.get(i);
			int count = 0;
			for(Variable x: neighbours){
				if(x.getDomain().contains(key))
				{
					count++;
				}
			}
			sorted_domain.add(new int[]{count,key});
		}
		Collections.sort(sorted_domain,(a,b)->a[0]!=b[0]?a[0]-b[0]:a[1]-b[1]);
		List<Integer> ans = new ArrayList<Integer>();
		for(int i=0;i<sorted_domain.size();i++)
			ans.add(sorted_domain.get(i)[1]);
		return ans;
	}

	/**
	 * Optional TODO: Implement your own advanced Value Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	public List<Integer> getTournVal ( Variable v )
	{
		return getValuesLCVOrder(v);
	}

	//==================================================================
	// Engine Functions
	//==================================================================

	public void solve ( )
	{
		if ( hasSolution )
			return;

		// Variable Selection
		Variable v = selectNextVariable();

		if ( v == null )
		{
			for ( Variable var : network.getVariables() )
			{
				// If all variables haven't been assigned
				if ( ! var.isAssigned() )
				{
					System.out.println( "Error" );
					return;
				}
			}

			// Success
			hasSolution = true;
			return;
		}

		// Attempt to assign a value
		for ( Integer i : getNextValues( v ) )
		{
			// Store place in trail and push variable's state on trail
			trail.placeTrailMarker();
			trail.push( v );

			// Assign the value
			v.assignValue( i );

			// Propagate constraints, check consistency, recurse
			if ( checkConsistency() )
				solve();

			// If this assignment succeeded, return
			if ( hasSolution )
				return;

			// Otherwise backtrack
			trail.undo();
		}
	}

	public boolean checkConsistency ( )
	{
		switch ( cChecks )
		{
			case "forwardChecking":
				return forwardChecking().getValue();

			case "norvigCheck":
				return norvigCheck().getValue();

			case "tournCC":
				return getTournCC();

			default:
				return assignmentsCheck();
		}
	}

	public Variable selectNextVariable ( )
	{
		switch ( varHeuristics )
		{
			case "MinimumRemainingValue":
				return getMRV();

			case "MRVwithTieBreaker":
				return MRVwithTieBreaker().get(0);

			case "tournVar":
				return getTournVar();

			default:
				return getfirstUnassignedVariable();
		}
	}

	public List<Integer> getNextValues ( Variable v )
	{
		switch ( valHeuristics )
		{
			case "LeastConstrainingValue":
				return getValuesLCVOrder( v );

			case "tournVal":
				return getTournVal( v );

			default:
				return getValuesInOrder( v );
		}
	}

	public boolean hasSolution ( )
	{
		return hasSolution;
	}

	public SudokuBoard getSolution ( )
	{
		return network.toSudokuBoard ( sudokuGrid.getP(), sudokuGrid.getQ() );
	}

	public ConstraintNetwork getNetwork ( )
	{
		return network;
	}
}
