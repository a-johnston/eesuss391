package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;

import edu.cwru.sepia.agent.planner.GameState;

public class MultiStripsAction extends ArrayList<StripsAction> implements StripsAction {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean preconditionsMet(GameState state) {
		return stream()
				.map(action -> action.preconditionsMet(state))
				.reduce(false, (a, b) -> a & b).booleanValue();
	}

	@Override
	public GameState apply(GameState state) {
		for (StripsAction action : this) {
			state = action.apply(state);
		}
		return state;
	}
}
