package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

/**
 * Created by eluan on 3/27/16.
 */
public class HarvestAction implements StripsAction {
    int unitID;
    int resourceID;

    public HarvestAction(int unitID, int resourceID) {
        this.unitID = unitID;
        this.resourceID = resourceID;
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        GameState.DummyResourceSpot spot;
        for(GameState.DummyResourceSpot possibleSpot: state.getGoldmines()) {
            if (possibleSpot.getId() == unitID) {
                spot = possibleSpot;
            }
        }

        if (spot == null) {
            for(GameState.DummyResourceSpot possibleSpot: state.getForests()) {
                if (possibleSpot.getId() == unitID) {
                    spot = possibleSpot;
                }
            }
        }

        if(spot == null) {
            return false;
        }

        for(GameState.DummyUnit unit: state.getPeasants()) {
            if (unit.getRandomId() == unitID) {
                canHarvest(spot, unit);
            }
        }

        return false;
    }

    private boolean canHarvest(GameState.DummyResourceSpot harvestSpot, GameState.DummyUnit unit) {
        if (unit.hasSomething()) {
            return false;
        }

        for(Position p: harvestSpot.getPosition().getAdjacentPositions()){
            if (p.equals(unit.getPosition())){
                return true;
            }
        }
        return false;
    }

    @Override
    public GameState apply(GameState state) {
        return null;
    }

    @Override
    public Action getSepiaAction(GameState state) {
        return null;
    }
}
