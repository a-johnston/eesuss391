package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.cwru.sepia.agent.planner.GameState;

public class MultiStripsAction extends ArrayList<StripsAction> implements StripsAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public boolean preconditionsMet(GameState state) {
		return !stream().anyMatch(action -> !action.preconditionsMet(state));
	}

	@Override
	public GameState apply(GameState state) {
		for (StripsAction action : this) {
			state = action.apply(state);
		}
		return state;
	}

	@Override
	public List<ActionPair> getSepiaAction(Map<Integer, Integer> unitMap) {
		return stream()
				.flatMap(action -> action.getSepiaAction(unitMap).stream())
				.collect(Collectors.toList());
	}
}
