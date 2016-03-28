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
		GameState child = new GameState(state, this);
		
		child.getUnit(unitID).moveTo(end);
		
		return child;
	}

	@Override
	public void getSepiaAction(Map<Integer, Action> actionMap, Map<Integer, Integer> unitMap) {
		actionMap.put(unitMap.get(unitID), Action.createCompoundMove(unitMap.get(unitID), end.x, end.y));
	}
}
