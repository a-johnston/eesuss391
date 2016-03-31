package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.util.Pair;

/**
 * An action that does nothing. Used for when a unit can be generated because
 * we might not always want to build a unit, so this allows that branch of the
 * graph to be explored.
 * 
 * @author adam
 *
 */
public class NullAction implements StripsAction {
	@Override
	public boolean preconditionsMet(GameState state) {
		return true; // We can always do nothing
	}

	@Override
	public GameState apply(GameState state) {
		return state; // Nothing happens
	}

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
		return Collections.emptyList();
	}

	@Override
	public int getID() {
		return -1; // Nobody does anything (filtered out by MultiStripsAction)
	}

}
