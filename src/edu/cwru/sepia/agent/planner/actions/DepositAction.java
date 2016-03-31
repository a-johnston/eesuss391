package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

/**
 * Represents the action of a unit depositing what it is carrying at a town hall
 */
public class DepositAction implements StripsAction {
    private int unitId;
	private Direction direction;
    
    public DepositAction(int unitID, Direction direction) {
        this.unitId	   = unitID;
        this.direction = direction;
    }

    @Override
    public boolean preconditionsMet(GameState state) {
    	DummyUnit unit = state.getUnit(unitId);
    	if (unit == null) {
            return false;
        }

        if(!unit.hasSomething()) {
            return false;
        }

        if (unit.getPosition().isAdjacent(state.getTownHall())) {
            //this.direction = unit.getPosition().getDirection(state.getTownHall());
            return true;
        }
        return false;
    }

    public Action preconditionAction(GameState state, Map<Integer, Integer> unitMap) {
        Position spot = state.getTownHall();
        DummyUnit unit = state.getUnit(unitId);
        for(Position p: spot.getAdjacentPositions()){
            if(direction.equals(unit.getPosition().getDirection(p))){
                return Action.createCompoundMove(unitMap.get(unitId), p.x, p.y);
            }
        }
        return null;
    }

    @Override
    public GameState apply(GameState state) {
        state.doDeposit(unitId);
        return state;
    }

	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return Action.createPrimitiveDeposit(unitMap.get(unitId), direction);
	}

    @Override
    public int getID() {
        return this.unitId;
    }

    @Override
    public String toString() {
        return "DepositAction{" +
                "unitId=" + unitId +
                ", direction=" + direction +
                '}';
    }
}
