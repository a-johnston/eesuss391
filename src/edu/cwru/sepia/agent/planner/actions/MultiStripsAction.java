package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.Map;

import edu.cwru.sepia.action.Action;
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
	public void getSepiaAction(Map<Integer, Action> actionMap, Map<Integer, Integer> unitMap) {
		// TODO Auto-generated method stub
		
	}
}
