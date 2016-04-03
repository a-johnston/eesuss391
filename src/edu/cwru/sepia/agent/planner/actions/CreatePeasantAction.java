package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;

/**
 * This class checks to see if a new peasant can be made and additionally
 * creates child game states in which that action has been taken.
 * Action: CreatePeasant(Townhall, fakeID)
 * Precondition: HasAtLeast400Gold(Townhall)
 * Effect: SubtractResource(400, Gold), NewPeasant(fakeID)
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
    public GameState apply(GameState state) {
    	state.makePeasant(fakeId);
    	return state;
    }

	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return Action.createPrimitiveBuild(townhallId, peasantTemplateId);
	}

	@Override
	public int getID() {
		return this.townhallId;
	}

	@Override
	public String toString() {
		return "CreatePeasantAction{" +
				"townhallId=" + townhallId +
				", peasantTemplateId=" + peasantTemplateId +
				", fakeId=" + fakeId +
				'}';
	}
}
