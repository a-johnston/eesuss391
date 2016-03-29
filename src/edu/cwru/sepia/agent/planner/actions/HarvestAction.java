package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.agent.planner.GameState.DummyResourceSpot;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

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
    public void apply(GameState state) {
        state.doHarvest(unitId, resourceId);
    }

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
		return Collections.singletonList(
				new Pair<>(
						unitMap.get(unitId),
						Action.createPrimitiveGather(unitMap.get(unitId), direction)));
	}
}
