package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.agent.planner.GameState.DummyResourceSpot;

/**
 * Represents the action of harvesting from some resource location
 */
public class HarvestAction implements StripsAction {
    int unitId;
    int resourceId;

    public HarvestAction(int unitID, int resourceID) {
        this.unitId = unitID;
        this.resourceId = resourceID;
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
        return unit.getPosition().isAdjacent(harvestSpot.getPosition());
    }
    
    public int getUnitID() {
        return unitId;
    }
    
    public int getResourceID() {
        return resourceId;
    }

    @Override
    public GameState apply(GameState state) {
        return new GameState(state, this).doHarvest(this);
    }

	@Override
	public void getSepiaAction(Map<Integer, Action> actionMap, Map<Integer, Integer> unitMap) {
		actionMap.put(unitMap.get(unitId), Action.createCompoundGather(unitMap.get(unitId), resourceId));
	}
}
