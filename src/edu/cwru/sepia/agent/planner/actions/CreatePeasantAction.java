package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;

/**
 * This class checks to see if a new peasant can be made and additionally
 * creates child game states in which that action has been taken.
 */
public class CreatePeasantAction implements StripsAction{

    @Override
    public boolean preconditionsMet(GameState state) {
        return state.getGold() >= 400 && state.getPeasants().size() < 3;
    }

    @Override
    public GameState apply(GameState state) {
    	GameState child = new GameState(state);
    	child.makePeasant();
        return child;
    }

	@Override
	public Action getSepiaAction(GameState state) {
		Action.createPrimitiveBuild(state.getTownHallId(), state.getPeasantTemplateId());
		return null;
	}
}
