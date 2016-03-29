package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.util.Pair;

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
	public void apply(GameState state) {
		state.getUnit(unitID).moveTo(end);
	}

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
		return Collections.singletonList(
				new Pair<>(unitMap.get(unitID), Action.createCompoundMove(unitMap.get(unitID), end.x, end.y)));
	}
}
