package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

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

    @Override
    public void apply(GameState state) {
        state.doDeposit(unitId);
    }

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {

		return Collections.singletonList(
				new Pair<>(
						unitMap.get(unitId),
						Action.createPrimitiveDeposit(unitMap.get(unitId), direction)));
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
