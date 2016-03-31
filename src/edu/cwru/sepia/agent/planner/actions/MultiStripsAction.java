package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.util.Pair;

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
		GameState child = new GameState(state, this);
		stream().forEach(action -> action.apply(child));
		return child;
	}

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
		return stream()
				.filter(action -> action.getID() != -1)
				.flatMap(action -> action.getSepiaAction(unitMap).stream())
				.collect(Collectors.toList());
	}

	@Override
	public int getID() {
		return -1;
	}

	@Override
	public String toString() {
		String output = "";
		for(StripsAction action: this){
			output += action.toString() + "\n";
		}

		return output;
	}
}
