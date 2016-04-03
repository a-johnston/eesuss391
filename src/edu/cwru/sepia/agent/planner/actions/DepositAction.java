package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.agent.planner.Position;
/**
 * Represents the action of a unit depositing what it is carrying at a town hall
 * Action: Deposit(UnitID, Direction)
 * Preconditions: CarryingSomething(UnitID), AdjacentTo(UnitID, Townhall, Direction)
 * Effect: Carrying(UnitID, 0), AddToCollected(100, GetCarriedResource(UnitID)
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

    /**
     * Sometimes move actions are reported as completed before they are in SEPIA
     * This just handles that and allows the plan executer to get the ncessary action to do before executing this one
     * @param state
     * @param unitMap
     * @return
     */
    public Action preconditionAction(GameState state, Map<Integer, Integer> unitMap) {
        Position newPos = new Position(
        		state.getTownHall().x - direction.xComponent(),
        		state.getTownHall().y - direction.yComponent());
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
