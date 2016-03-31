package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.agent.planner.GameState.DummyResourceSpot;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.util.Direction;

/**
 * Represents the action of harvesting from some resource location
 */
public class HarvestAction implements StripsAction {
    int unitId;
    int resourceId;
    Direction direction;
    ResourceNode.Type type;

    public HarvestAction(int unitID, int resourceID, ResourceNode.Type type, Direction direction) {
        this.unitId 	= unitID;
        this.resourceId = resourceID;
        this.direction	= direction;
        this.type		= type;
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
        DummyResourceSpot resource = state.getResourceSpot(resourceId);
        
        if (resource == null) {
        	resource = state.getResourceOfType(type);
        }
        
        Position spot = resource.getPosition();
        Position newPos = new Position(spot.x - direction.xComponent(), spot.y - direction.yComponent());
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
