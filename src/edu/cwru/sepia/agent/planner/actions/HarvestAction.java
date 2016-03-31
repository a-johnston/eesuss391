package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.agent.planner.GameState.DummyResourceSpot;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

/**
 * Represents the action of harvesting from some resource location
 */
public class HarvestAction implements StripsAction {
    int unitId;
    int resourceId;
    Direction direction;

    public HarvestAction(int unitID, int resourceID, Direction direction) {
        this.unitId 	= unitID;
        this.resourceId = resourceID;
        this.direction	= direction;
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        DummyUnit unit = state.getUnit(unitId);
        DummyResourceSpot spot = state.getResourceSpot(resourceId);

        if (unit == null || spot == null || unit.hasSomething() || spot.getAmount() == 0) {
        	return false;
        }
        
        return canHarvest(spot, unit);
    }

    public Action preconditionAction(GameState state, Map<Integer, Integer> unitMap) {
        Position spot = state.getResourceSpot(resourceId).getPosition();
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

    private boolean canHarvest(GameState.DummyResourceSpot harvestSpot, GameState.DummyUnit unit) {
        return unit.getPosition().move(direction).equals(harvestSpot.getPosition());
    }
    
    public int getUnitID() {
        return unitId;
    }
    
    public int getResourceID() {
        return resourceId;
    }

    @Override
    public GameState apply(GameState state) {
        state.doHarvest(unitId, resourceId);
        return state;
    }

	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap) {
		return Action.createPrimitiveGather(unitMap.get(unitId), direction);
	}

    @Override
    public int getID() {
        return this.unitId;
    }

    @Override
    public String toString() {
        return "HarvestAction{" +
                "unitId=" + unitId +
                ", resourceId=" + resourceId +
                ", direction=" + direction +
                '}';
    }
}
