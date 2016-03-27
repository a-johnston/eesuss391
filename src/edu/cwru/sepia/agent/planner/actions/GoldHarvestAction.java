package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by eluan on 3/24/16.
 */
public class GoldHarvestAction implements StripsAction {

    @Override
    public boolean preconditionsMet(GameState state) {
        return false;
    }

    @Override
    public GameState apply(GameState state) {
        return null;
    }
}
