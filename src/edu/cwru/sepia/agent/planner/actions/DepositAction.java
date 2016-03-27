package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.DummyUnit;
/**
 * Created by eluan on 3/24/16.
 */
public class DepositAction implements StripsAction {

    int id;
    
    public DepositAction(int unitID) {
        this.id = unitID;
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        for (GameState.DummyUnit unit : state.getPeasants()) {
            if (unit.getRandomId() == this.id) {
                return canDeposit(state, unit);
            }
        }
        return false;
    }

    private boolean canDeposit(GameState state, DummyUnit unit) {
        if(!unit.hasSomething()){
            return false;
        }
        for (Position p: state.getTownHall().getAdjacentPositions()) {
            if(p.equals(unit.getPosition())){
                return true;
            }
        }
        return false;
    }


    @Override
    public GameState apply(GameState state) {
        return null;
    }
}
