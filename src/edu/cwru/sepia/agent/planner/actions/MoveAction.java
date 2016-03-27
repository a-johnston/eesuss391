package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;

public class MoveAction implements StripsAction {

    int unitID;
    Position start;
    Position end;

    public MoveAction(int unitID, Position start, Position end) {
        this.unitID = unitID;
        this.start = start;
        this.end = end;
    }

	@Override
	public boolean preconditionsMet(GameState state) {
        for(DummyUnit unit: state.getPeasants()) {
            if(unit.getRandomId() == this.unitID) {
                canMove(state, unit);
            }
        }
		return false;
	}

    private boolean canMove(GameState state, DummyUnit unit) {
        return unit.getPosition().equals(start);
    }

	@Override
	public GameState apply(GameState state) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Action getSepiaAction(GameState state) {
		// TODO Auto-generated method stub
		return null;
	}
}
