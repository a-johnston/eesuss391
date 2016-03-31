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
	private Map<Integer, Stack<StripsAction>> individualPlans;
	private Stack<StripsAction> headquartersActions;
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

			if(unitHasSomethingToDo(fakeID)) {
				StripsAction unitAction = individualPlans.get(fakeID).peek();
				System.out.println(unitAction);
				if(unitAction.preconditionsMet(thisState)) {
					individualPlans.get(fakeID).pop();
					nextActions.put(realID, unitAction.getSepiaAction(peasantIdMap));
				} else {
					if(unitAction instanceof HarvestAction) {
						nextActions.put(realID, ((HarvestAction) unitAction).preconditionAction(thisState, peasantIdMap));
					} else if (unitAction instanceof DepositAction) {
						nextActions.put(realID, ((DepositAction) unitAction).preconditionAction(thisState, peasantIdMap));
					}
				}
			} else {
				nextActions.put(realID, Action.createCompoundMove(realID, 0, 0));
			}
		}

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
		ActionResult result = actionResults.get(unitID);
		
		if (result != null && result.getAction().getUnitId() == unitID) {
			System.out.println(result);
			if(result.getFeedback().equals(ActionFeedback.COMPLETED)){
				// Solving some sepia bug
				if(result.getAction() instanceof LocatedAction) {
					LocatedAction lastAction = (LocatedAction) result.getAction();
					if(stateView.getUnit(unitID).getXPosition() != lastAction.getX() ||
							stateView.getUnit(unitID).getYPosition() != lastAction.getY()) {
						System.err.println("Retrying");
						System.err.println(unitID);
						System.err.println(lastAction.getX());
						System.err.println(lastAction.getY());
						return true;
					}
				}
				return true;
			} else if (result.getAction() instanceof LocatedAction && result.getFeedback().equals(ActionFeedback.FAILED)){
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
		// Unit not in map
		return true;
	}


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

		// Correctly put the stack back together
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
