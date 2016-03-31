package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.util.Pair;

/**
 * This class checks to see if a new peasant can be made and additionally
 * creates child game states in which that action has been taken.
 */
public class CreatePeasantAction implements StripsAction{

	int townhallId;
	int peasantTemplateId;

	public int getFakeId() {
		System.out.println("Fake id: " + fakeId);
		return fakeId;
	}

	int fakeId;
	
	public CreatePeasantAction(GameState state) {
		townhallId = state.getTownHallId();
		peasantTemplateId = state.getPeasantTemplateId();
		fakeId = (int) (Integer.MAX_VALUE * Math.random());
	}
	
    @Override
    public boolean preconditionsMet(GameState state) {
        return state.getGold() >= 400 && state.getPeasants().size() < 3;
    }

    @Override
    public void apply(GameState state) {
    	state.makePeasant(fakeId);
    }

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
		return Collections.singletonList(
				new Pair<>(townhallId, Action.createPrimitiveBuild(townhallId, peasantTemplateId)));
	}

	@Override
	public int getID() {
		return this.townhallId;
	}
}
