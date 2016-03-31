package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.util.Pair;

public class NullAction implements StripsAction {
	@Override
	public boolean preconditionsMet(GameState state) {
		return true;
	}

	@Override
	public void apply(GameState state) {
	}

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
		return Collections.emptyList();
	}

	@Override
	public int getID() {
		return -1;
	}

}
