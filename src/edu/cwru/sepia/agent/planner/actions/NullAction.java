package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;

/**
 * An action that does nothing. Used for when a unit can be generated because
 * we might not always want to build a unit, so this allows that branch of the
 * graph to be explored.
 *
 * Represents the action of not building a peasant on the specified turn
 * Action: DoNotBuildPeasant()
 * Preconditions -> None
 * Effects -> None
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
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return null;
	}

	@Override
	public int getID() {
		return -1; // Nobody does anything (filtered out by MultiStripsAction)
	}

	@Override
	public String toString() {
		return "NullAction{NoPeasantBuilts}";
	}
}
