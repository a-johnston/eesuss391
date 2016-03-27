package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.agent.planner.GameState.DummyResourceSpot;

/**
 * Created by eluan on 3/27/16.
 */
public class HarvestAction implements StripsAction {
    public int getUnitID() {
        return unitID;
    }

    int unitID;

    public int getResourceID() {
        return resourceID;
    }

    int resourceID;

    public HarvestAction(int unitID, int resourceID) {
        this.unitID = unitID;
        this.resourceID = resourceID;
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        DummyUnit unit = state.getUnit(unitID);
        DummyResourceSpot spot = state.getResourceSpot(resourceID);

        return unit != null && spot != null && canHarvest(spot, unit);
    }

    private boolean canHarvest(GameState.DummyResourceSpot harvestSpot, GameState.DummyUnit unit) {
        return !unit.hasSomething() 
        	 && unit.getPosition().isAdjacent(harvestSpot.getPosition());
    }

    @Override
    public GameState apply(GameState state) {
        DummyUnit unit = state.getUnit(unitID);
        DummyResourceSpot spot = state.getResourceSpot(resourceID);

        GameState newState = new GameState(state, this);
        newState.doHarvest();

        return newState;
    }

    @Override
    public Action getSepiaAction(Map<Integer, Integer> unitMap, GameState state) {
        return Action.createCompoundGather(unitMap.get(unitID), resourceID);
    }
}
