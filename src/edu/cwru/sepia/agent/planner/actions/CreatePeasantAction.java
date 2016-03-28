package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;

/**
 * This class checks to see if a new peasant can be made and additionally
 * creates child game states in which that action has been taken.
 */
public class CreatePeasantAction implements StripsAction{

	int townhallId;
	int peasantTemplateId;
	
	public CreatePeasantAction(GameState state) {
		townhallId = state.getTownHallId();
		peasantTemplateId = state.getPeasantTemplateId();
	}
	
    @Override
    public boolean preconditionsMet(GameState state) {
        return state.getGold() >= 400 && state.getPeasants().size() < 3;
    }

    @Override
    public GameState apply(GameState state) {
        return new GameState(state, this).makePeasant();
    }

	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return Action.createPrimitiveBuild(townhallId, peasantTemplateId);
	}
}
