package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;

/**
 * A MultiStripsAction represents a collection of actions being performed
 * simultaneously. A GameState generating children first generates all
 * possible MultiStripsAction instances to check the validity as well as to
 * generate the resulting GameState instances.
 * 
 * Fundamentally, this class is an ArrayList of StripsActions that can check
 * preconditions and be applied to a GameState.
 */
public class MultiStripsAction extends ArrayList<StripsAction> implements StripsAction {
	
	private static final long serialVersionUID = 1L;
	
	public MultiStripsAction() {
		super();
	}
	
	public MultiStripsAction(List<StripsAction> actions) {
		super(actions);
	}

	/**
	 * Iterates over the actions represented by this MultiStripsAction and
	 * checks for compatibility with one another as well as compatibility with
	 * the existing state.
	 * 
	 * This should be the only StripsAction that does not mutate the given
	 * state instance.
	 * 
	 * @param state
	 * @return New state with actions applied to the given state
	 */
	@Override
	public boolean preconditionsMet(GameState state) {
		GameState child = new GameState(state, this);
		return stream().allMatch(action -> {
			boolean met = action.preconditionsMet(child);
			if (met) action.apply(child);
			return met;
		});
	}

	@Override
	public GameState apply(GameState state) {
		// Java 8 Streams API doesn't cover this use case..
		GameState child = new GameState(state, this);
		stream().forEach(action -> action.apply(child));
		return child;
	}

	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return null; // No single action can be returned
	}

	@Override
	public int getID() {
		return -1; // No single actor for this action
	}

	@Override
	public String toString() {
		return "MultiStripsAction" + super.toString();
	}
}
