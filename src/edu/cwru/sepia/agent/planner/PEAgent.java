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
 * This PEAgent class takes a stack of MultiStripsActions and unwraps that plan
 * into the action plans for individual units. The class handles translating
 * between the unit IDs used in the plan and the unit IDs in the actual game
 * execution, as well as recovering the current action when preconditions are
 * not met during game execution. 
 */
public class PEAgent extends Agent {
	private static final long serialVersionUID = 1L;

	// The plans being executed
	private Stack<MultiStripsAction> plan = null;
	private Map<Integer, Stack<StripsAction>> individualPlans;
	private Stack<StripsAction> headquartersActions;

	// maps the real unit Ids to the plan's unit ids
	private Map<Integer, Integer> peasantIdMap;
	private Integer lastFakeId = null;

	public PEAgent(int playernum, Stack<MultiStripsAction> plan) {
		super(playernum);
		peasantIdMap = new HashMap<>();
		this.plan = plan;
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
		// maps the existing units within the peasant ID map
		for(int unitId : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(unitId);

			if (unit.getTemplateView().getName().toLowerCase().equals("peasant")) {
				peasantIdMap.put(unitId, unitId);
			}
		}

		return middleStep(stateView, historyView);
	}

	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {

		addToUnitMap(stateView, historyView);

		Map<Integer, Action> nextActions = new HashMap<Integer, Action>();

		if(individualPlans == null) {
			individualPlans = getIndividualPlans();
		}

		GameState thisState = new GameState(stateView, playernum, 0, 0, false);
		replaceRealWithFake(thisState);

		// Handles doing the headquarters actions
		if(!headquartersActions.isEmpty()) {
			StripsAction hqStripsAction = headquartersActions.peek();
			if (hqStripsAction.preconditionsMet(thisState)) {
				headquartersActions.pop();
				CreatePeasantAction temp = (CreatePeasantAction) hqStripsAction;
				this.lastFakeId = temp.getFakeId();
				nextActions.put(thisState.getTownHallId(), hqStripsAction.getSepiaAction(peasantIdMap));
			}
		}

		// Handles doing all of the peasants actions
		for(Integer fakeID: peasantIdMap.keySet()) {
			Integer realID = peasantIdMap.get(fakeID);
			if(!unitIsFree(stateView, historyView, realID, nextActions)) {
				continue;
			}

			if(unitHasSomethingToDo(fakeID)) {
				StripsAction unitAction = individualPlans.get(fakeID).peek();
				if(unitAction.preconditionsMet(thisState)) {
					individualPlans.get(fakeID).pop();
					nextActions.put(realID, unitAction.getSepiaAction(peasantIdMap));
				} else {
					// This state is entered only on a SEPIA bug
					// The specific bug is the history view reporting a move action as completed before actual completion
					// (i.e. the unit is not at the place we told it to go)
					// This handles correcting that bug so that the unit can move on to its next action
					nextActions.put(realID, unitAction.preconditionAction(thisState, peasantIdMap));
				}
			} else {
				// get units that are done acting out of the way
				// our planner reduces the branching factor by not generating
				// child states for each move, so units can finish behind or
				// in front of others time wise

				nextActions.put(realID, Action.createCompoundMove(realID, 0, 0));
			}
		}

		return nextActions;
	}

	/**
	 * Takes a state constructed using the stateView constructor and converts all dummy unit ids to the fake ids used in planning
	 * @param state a gamestate to convert
     */
	private void replaceRealWithFake(GameState state) {
		for (GameState.DummyUnit unit : state.getPeasants()) {
			for (Integer fakeID: peasantIdMap.keySet()) {
				if (unit.getId() == peasantIdMap.get(fakeID)) {
					unit.setId(fakeID);
				}
			}
		}
	}

	/**
	 * Whenever a unit is created, we need to add it to the peasantIdMap
	 * @param stateView the stateView so we can get the turn number
	 * @param historyView the history view so we can get birth logs
     */
	private void addToUnitMap(State.StateView stateView, History.HistoryView historyView) {
		if (this.lastFakeId == null || stateView.getTurnNumber() == 0) {
			return;
		}

		for (BirthLog log : historyView.getBirthLogs(stateView.getTurnNumber() - 1)) {
			peasantIdMap.put(lastFakeId, log.getNewUnitID());
			lastFakeId = null;
		}
	}

	/**
	 * Determines if the specified unit has anything left to accomplish
	 * @param id the unit id to check
	 * @return true if there is work to be done, false otherwise
     */
	private boolean unitHasSomethingToDo(int id) {
		return individualPlans.keySet().contains(id) && !individualPlans.get(id).isEmpty();
	}

	/**
	 * Determines if a unit is free to accept more work
	 * @param stateView the current state
	 * @param historyView the history
	 * @param unitID the unit in question
	 * @param actionMap an action map which we can add actions to
     * @return
     */
	private boolean unitIsFree(State.StateView stateView, History.HistoryView historyView, int unitID, Map<Integer, Action> actionMap) {
		if (stateView.getTurnNumber() == 0) {
			return true;
		}

		Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
		ActionResult result = actionResults.get(unitID);

		if (result != null && result.getAction().getUnitId() == unitID) {
			if(result.getFeedback().equals(ActionFeedback.COMPLETED)){
				return true;
			} else if (result.getAction() instanceof LocatedAction && result.getFeedback().equals(ActionFeedback.FAILED)){
				// Action failed... try again
				LocatedAction lastAction = (LocatedAction) result.getAction();
				actionMap.put(unitID, Action.createCompoundMove(unitID, lastAction.getX(), lastAction.getY()));
			}
			return false;
		}
		// Unit not in map
		return true;
	}

	/**
	 * Breaks the given plan into plans for individual units. Handles the
	 * townhall as a special case. This will also create the acton plan for the townhall (called the headquarters)
	 * @return a map of stakes. Each stake corresponds to one unit
	 */
	private Map<Integer, Stack<StripsAction>> getIndividualPlans() {
		Map<Integer, Stack<StripsAction>> unitPlan = new HashMap<>();
		headquartersActions = new Stack<>();

		while(!plan.isEmpty()) {
			for(StripsAction stripsAction: plan.pop()) {
				if(stripsAction instanceof CreatePeasantAction) {
					headquartersActions.push(stripsAction);
					continue;
				}

				Stack<StripsAction> stack = unitPlan.get(stripsAction.getID());

				if (stack == null) {
					stack = new Stack<>();
					unitPlan.put(stripsAction.getID(), stack);
				}

				unitPlan.get(stripsAction.getID()).add(stripsAction);
			}
		}

		// Reverse the stack so it happens in the correct order.
		for (Stack<StripsAction> stack : unitPlan.values()) {
			Collections.reverse(stack);
		}

		Collections.reverse(headquartersActions);

		return unitPlan;
	}

	@Override
	public void terminalStep(State.StateView stateView, History.HistoryView historyView) {}

	@Override
	public void savePlayerData(OutputStream outputStream) {}

	@Override
	public void loadPlayerData(InputStream inputStream) {}
}
