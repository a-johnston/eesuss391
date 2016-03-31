package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.*;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.BirthLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.*;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {
	private static final long serialVersionUID = 1L;

	// The plan being executed
    private Stack<MultiStripsAction> plan = null;
    private Map<Integer, Stack<StripsAction>> individualPlans = null;
    private Stack<StripsAction> headquartersActions = new Stack<>();
    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private Integer lastFakeId = null;

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
            
            if (unitType.equals("peasant")) {
                peasantIdMap.put(unitId, unitId);
            }
        }

        return middleStep(stateView, historyView);
    }
    
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {

        this.addToUnitMap(stateView, historyView);

        Map<Integer, Action> nextActions = new HashMap<Integer, Action>();
        if(individualPlans == null) {
            individualPlans = getIndividualPlans();
        }

        GameState thisState = new GameState(stateView, playernum, 0, 0, false);
        replaceRealWithFake(thisState);

        // Headquarters case
        if(!headquartersActions.isEmpty()) {
            StripsAction hqStripsAction = headquartersActions.peek();
            if (hqStripsAction.preconditionsMet(thisState)) {
                headquartersActions.pop();
                CreatePeasantAction temp = (CreatePeasantAction) hqStripsAction;
                this.lastFakeId = temp.getFakeId();
                nextActions.put(thisState.getTownHallId(), hqStripsAction.getSepiaAction(peasantIdMap));
            }
        }
        System.out.println(stateView.getTurnNumber());

        for(Integer fakeID: peasantIdMap.keySet()) {
            Integer realID = peasantIdMap.get(fakeID);
            if(!unitIsFree(stateView, historyView, realID, nextActions)) {
                continue;
            }
//            System.out.println("Unit:" + fakeID.toString());
//            try {
//                System.out.println(individualPlans.get(fakeID).isEmpty());
//            } catch (Exception e) {
//                System.err.println(e);
//                System.err.println(individualPlans.get(fakeID));
//                for(Integer fake: peasantIdMap.keySet()) {
//                    System.err.println(fake);
//                }
//                System.exit(0);
//            }
            if(unitHasSomethingToDo(fakeID)) {
                StripsAction unitAction = individualPlans.get(fakeID).peek();
//                System.out.println(unitAction);
//                System.out.println(unitAction.preconditionsMet(thisState));
                if(unitAction.preconditionsMet(thisState)) {
                    individualPlans.get(fakeID).pop();
                    nextActions.put(realID, unitAction.getSepiaAction(peasantIdMap));
                }
            }
        }
//        System.out.println(stateView.getTurnNumber());
//        for(Integer i: nextActions.keySet()) {
//            System.out.println(nextActions.get(i));
//        }
        return nextActions;
    }

    private void replaceRealWithFake(GameState state) {
        for (GameState.DummyUnit unit : state.getPeasants()) {
            for (Integer fakeID: peasantIdMap.keySet()) {
                if (unit.getId() == peasantIdMap.get(fakeID)) {
                    unit.setId(fakeID);
                }
            }
        }
    }

    private void addToUnitMap(State.StateView stateView, History.HistoryView historyView) {
        if(this.lastFakeId == null || stateView.getTurnNumber() == 0) {
            return;
        }

        if(historyView.getBirthLogs(stateView.getTurnNumber()).size() > 1) {
            System.err.println("Something wonky happened");
        }

        for(BirthLog log: historyView.getBirthLogs(stateView.getTurnNumber() - 1)) {
            peasantIdMap.put(lastFakeId, log.getNewUnitID());
            lastFakeId = null;
            System.out.println("Added unit to map");
            break;
        }

        lastFakeId = null;
    }
    private boolean unitHasSomethingToDo(int id) {

        return individualPlans.keySet().contains(id) && !individualPlans.get(id).isEmpty();
    }

    private boolean unitIsFree(State.StateView stateView, History.HistoryView historyView, int unitID, Map<Integer, Action> actionMap) {
        if (stateView.getTurnNumber() == 0) {
            return true;
        }

        Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
        for (ActionResult result : actionResults.values()) {
            if (result.getAction().getUnitId() == unitID) {
                System.out.println(result);
                if(result.getFeedback().equals(ActionFeedback.COMPLETED)){
                    // Solving some sepia bug
                    if(result.getAction() instanceof LocatedAction) {
                        LocatedAction lastAction = (LocatedAction) result.getAction();
                        if(stateView.getUnit(unitID).getXPosition() != lastAction.getX() ||
                            stateView.getUnit(unitID).getYPosition() != lastAction.getY()) {
                            actionMap.put(unitID, Action.createCompoundMove(
                                unitID,
                                lastAction.getX(),
                                lastAction.getY()
                            ));
                            return false;
                        }
                    }
                    return true;
                }
                else if(result.getFeedback().equals(ActionFeedback.FAILED)){
                    LocatedAction lastAction = (LocatedAction) result.getAction();

                    actionMap.put(unitID, Action.createCompoundMove(
                            unitID,
                            lastAction.getX(),
                            lastAction.getY()
                    ));
                    return false;
                }
                else {
                    return false;
                }
            }

        }
        // Unit not in map
        return true;
    }


    private Map<Integer, Stack<StripsAction>> getIndividualPlans() {
        Map<Integer, Stack<StripsAction>> unitPlan = new HashMap<>();
        Map<Integer, List<StripsAction>> reversedPlan = new HashMap<>();
        Stack<StripsAction> reversedHQPlan = new Stack<>();
        while(!plan.isEmpty()) {
            for(StripsAction stripsAction: plan.pop()) {
                if(stripsAction instanceof CreatePeasantAction) {
                    reversedHQPlan.push(stripsAction); // The headquarters can only do one thing....so we don't really care.
                } else if(reversedPlan.keySet().contains(stripsAction.getID())) {
                    reversedPlan.get(stripsAction.getID()).add(stripsAction);
                } else {
                    List<StripsAction> newArray = new ArrayList<>();
                    newArray.add(stripsAction);
                    reversedPlan.put(stripsAction.getID(), newArray);
                }
            }
        }

        // Correctly put the stack back together
        System.out.println("Plan ids:");
        for(Integer id: reversedPlan.keySet()) {
            System.out.println(id);
            System.out.println(reversedPlan.get(id).size());
            unitPlan.put(id, new Stack<StripsAction>());
            for(int i = reversedPlan.get(id).size() - 1; i >= 0; i --) {
                System.out.println(reversedPlan.get(id).get(i));
                unitPlan.get(id).push(reversedPlan.get(id).get(i));
            }
        }
        System.out.println("New built fake ids: ");
        while(!reversedHQPlan.isEmpty()) {
            System.out.println(((CreatePeasantAction) reversedHQPlan.peek()).getFakeId());
            headquartersActions.push(reversedHQPlan.pop());
        }

        return unitPlan;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {}

    @Override
    public void savePlayerData(OutputStream outputStream) {}

    @Override
    public void loadPlayerData(InputStream inputStream) {}
}
