package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by eluan on 3/24/16.
 */
public class CreatePeasantAction implements StripsAction{
    @Override
    public boolean preconditionsMet(GameState state) {
        return state.getGold() >= 400;
    }

    @Override
    public GameState apply(GameState state) {
        return null;
    }
}
