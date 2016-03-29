package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.util.Pair;

import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.activation.ActivationID;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {
	private static final long serialVersionUID = 1L;

	// The plan being executed
    private Stack<MultiStripsAction> plan = null;
    private Map<Integer, Stack<Pair<Action, StripsAction>>> individualPlans = null;
    private Stack<Pair<Action, StripsAction>> headquartersActions = new Stack<>();
    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;

    public PEAgent(int playernum, Stack<MultiStripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<>();
        this.plan = plan;

    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(unitId, unitId);
                peasantTemplateId = unit.getTemplateView().getID();
            }
        }

        return middleStep(stateView, historyView);
    }
    
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        Map<Integer, Action> nextActions = new HashMap<Integer, Action>();
        if(individualPlans == null) {
            individualPlans = getIndividualPlans();
        }

        GameState thisState = new GameState(stateView, stateView.getPlayerNumbers()[0], 0, 0, false);
        // Headquarters case
        Pair<Action, StripsAction> hqPair = headquartersActions.peek();
        if(hqPair.b.preconditionsMet(thisState)) {
            headquartersActions.pop();
            nextActions.put(thisState.getTownHallId(), hqPair.a);
        }

        for(Integer fakeID: peasantIdMap.keySet()) {
            Integer realID = peasantIdMap.get(fakeID);
            if(!unitIsFree(stateView, historyView, realID)) {
                continue;
            }

            if(unitHasSomethingToDo(realID)) {
                Pair<Action, StripsAction> actionStripsActionPair = individualPlans.get(realID).pop();
                if(actionStripsActionPair.b.preconditionsMet(thisState)) {
                    nextActions.put(realID, actionStripsActionPair.a);
                }
            }
        }
        return nextActions;
    }

    private boolean unitHasSomethingToDo(int id) {
        return individualPlans.keySet().contains(id) && !individualPlans.get(id).isEmpty();
    }

    private boolean unitIsFree(State.StateView stateView, History.HistoryView historyView, int unitID) {
        if (stateView.getTurnNumber() == 0) {
            return true;
        }

        Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
        for (ActionResult result : actionResults.values()) {
            if (result.getAction().getUnitId() == unitID) {
                return false;
            }

        }
        // Unit not in map
        return true;
    }


    private Map<Integer, Stack<Pair<Action, StripsAction>>> getIndividualPlans() {
        Map<Integer, Stack<Pair<Action, StripsAction>>> unitPlan = new HashMap<>();
        while(!plan.isEmpty()) {
            MultiStripsAction actions = plan.pop();
            for(StripsAction stripsAction: actions) {
                Pair<Integer, Action> sepiaActionPair = stripsAction.getSepiaAction(peasantIdMap).get(0);
                Pair<Action, StripsAction> sepiaStripsPair = new Pair<>(sepiaActionPair.b, stripsAction);
                if(stripsAction instanceof CreatePeasantAction) {
                    headquartersActions.push(sepiaStripsPair);
                } else if(unitPlan.keySet().contains(sepiaActionPair.a)) {
                    unitPlan.get(sepiaActionPair.a).push(sepiaStripsPair);
                } else {
                    Stack<Pair<Action, StripsAction>> newStack = new Stack<>();
                    newStack.push(sepiaStripsPair);
                    unitPlan.put(sepiaActionPair.a, newStack);
                }
            }

        }

        return unitPlan;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action) {
        return null;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {}

    @Override
    public void savePlayerData(OutputStream outputStream) {}

    @Override
    public void loadPlayerData(InputStream inputStream) {}
}
