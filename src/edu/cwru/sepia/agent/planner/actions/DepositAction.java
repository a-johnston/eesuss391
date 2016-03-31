package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.agent.planner.Position;
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
    	
    	if (unit == null || !unit.hasSomething()) {
            return false;
        }

        return unit.getPosition().isAdjacent(state.getTownHall());
    }

    public Action preconditionAction(GameState state, Map<Integer, Integer> unitMap) {
        Position spot = state.getTownHall();
        DummyUnit unit = state.getUnit(unitId);
        Position newPos;
        if(direction.equals(Direction.EAST)){
            newPos = new Position(spot.x-1, spot.y);
        } else if(direction.equals(Direction.SOUTHEAST)) {
            newPos = new Position(spot.x-1, spot.y-1);
        } else if (direction.equals(Direction.SOUTH)) {
            newPos = new Position(spot.x, spot.y-1);
        } else if (direction.equals(Direction.SOUTHWEST)) {
            newPos = new Position(spot.x + 1, spot.y-1);
        } else if (direction.equals(Direction.WEST)) {
            newPos = new Position(spot.x + 1, spot.y);
        } else if (direction.equals(Direction.NORTHWEST)) {
            newPos = new Position(spot.x + 1, spot.y + 1);
        } else if (direction.equals(Direction.NORTHEAST)){
            newPos = new Position(spot.x + 1, spot.y - 1);
        } else { // North
            newPos = new Position(spot.x , spot.y + 1);
        }

        return Action.createCompoundMove(unitMap.get(unitId), newPos.x, newPos.y);
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
