package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;

public class MultiStripsAction extends ArrayList<StripsAction> implements StripsAction {
	
	private static final long serialVersionUID = 1L;
	
	public MultiStripsAction() {
		super();
	}
	
	public MultiStripsAction(List<StripsAction> actions) {
		super(actions);
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		return stream().allMatch(action -> action.preconditionsMet(state));
	}

	@Override
	public GameState apply(GameState state) {
		// streams API doesn't cover this use case
		GameState child = new GameState(state, this);
		stream().forEach(action -> {
			if (action.preconditionsMet(state)) {
				action.apply(child);
			}
		});
		return child;
	}

	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return null;
	}

	@Override
	public int getID() {
		return -1;
	}

	@Override
	public String toString() {
		return "MultiStripsAction" + super.toString();
	}
}
