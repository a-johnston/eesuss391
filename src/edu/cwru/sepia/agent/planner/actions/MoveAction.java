package edu.cwru.sepia.agent.planner.actions;

import java.util.Map;

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
        this.end = end.move(end.getDirection(start));
    }

	@Override
	public boolean preconditionsMet(GameState state) {
		if (!end.inBounds(GameState.getMapXExtent(), GameState.getMapYExtent())) {
            return false;
        }

        DummyUnit unit = state.getUnit(unitID);
        return unit != null && unit.getPosition().equals(start);
	}

	@Override
	public GameState apply(GameState state) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Action getSepiaAction(Map<Integer, Integer> unitMap, GameState state) {
		// TODO Auto-generated method stub
		return null;
	}
}
