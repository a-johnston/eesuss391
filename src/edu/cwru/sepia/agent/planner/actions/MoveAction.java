package edu.cwru.sepia.agent.planner.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

public class MoveAction implements StripsAction {

    int unitID;
    Position start;
    Position end;

    public MoveAction(int unitID, Position start, Position end) {
        this.unitID = unitID;
        this.start = start;
        this.end = end.move(Direction.getDirection(start.x - end.x, start.y - end.y));
    }

	@Override
	public boolean preconditionsMet(GameState state) {
		if (!end.inBounds(GameState.getMapXExtent(), GameState.getMapYExtent())) {
            return false;
        }

        DummyUnit unit = state.getUnit(unitID);
        if (unit == null) {
            return false;
        } else {
            return true;
        }


	}

	@Override
	public GameState apply(GameState state) {
		state.getUnit(unitID).moveTo(end);
		return state;
	}

	@Override
	public List<Pair<Integer, Action>> getSepiaAction(Map<Integer, Integer> unitMap) {
        return Collections.singletonList(
				new Pair<>(unitMap.get(unitID), Action.createCompoundMove(unitMap.get(unitID), end.x, end.y)));
	}

    @Override
    public int getID() {
        return this.unitID;
    }

    @Override
    public String toString() {
        return "MoveAction{" +
                "unitID=" + unitID +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
