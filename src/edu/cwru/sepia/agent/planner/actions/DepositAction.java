package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.util.Direction;

/**
 * Represents the action of a unit depositing what it is carrying at a town hall
 */
public class DepositAction implements StripsAction {

    public int getId() {
        return unitId;
    }

    private int unitId;
	private Direction direction;
    
    public DepositAction(int unitID, Direction direction) {
        this.unitId	   = unitID;
        this.direction = direction;
    }

    @Override
    public boolean preconditionsMet(GameState state) {
    	DummyUnit unit = state.getUnit(unitId);
    	
        return unit != null
           && !unit.hasSomething()
           &&  unit.getPosition().move(direction).equals(state.getTownHall());
    }

    @Override
    public GameState apply(GameState state) {
        GameState newState = new GameState(state, this);
        newState.doDeposit();
        return newState;
    }

	@Override
	public void getSepiaAction(Map<Integer, Action> actionMap, Map<Integer, Integer> unitMap) {
		actionMap.put(unitId, Action.createPrimitiveDeposit(unitId, direction));
	}
}
