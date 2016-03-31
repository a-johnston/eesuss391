package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {

	private abstract class Copyable<T> {
		abstract public T copy();
	}

	public class DummyUnit extends Copyable<DummyUnit> {
		private Position position;
		private int wood = 0;
		private int gold = 0;

		private int id;

		public DummyUnit(Position p, int id) {
			this.position = p;
			this.id = id;
		}

		public DummyUnit(DummyUnit parent) {
			this.position = parent.position;
			this.wood	  = parent.wood;
			this.gold	  = parent.gold;
			this.id = parent.id;
		}

		public boolean hasSomething() {
			return wood + gold != 0;
		}

		public Position getPosition() {
			return position;
		}

		public int getId() {
			return id;
		}

		public void moveTo(Position position) {
			this.position = position;
		}

		public void give(Type type, int amount) {
			if (type == Type.GOLD_MINE) {
				gold = amount;
			} else {
				wood = amount;
			}
		}

		void setId(int id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			int hash = position.hashCode();

			hash ^= 83  * id;
			hash ^= 307 * wood;
			hash ^= 569	* gold;

			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DummyUnit) {
				return this.id == ((DummyUnit) obj).id;
			}
			return false;
		}

		@Override
		public DummyUnit copy() {
			return new DummyUnit(this);
		}
	}

	public class DummyResourceSpot extends Copyable<DummyResourceSpot> {
		private Position position;
		private int amountLeft;
		private int distanceToTownHall;
		private Type type;

		private int id;

		public DummyResourceSpot(ResourceView node, Position townHall) {
			this.position 	= new Position(node.getXPosition(), node.getYPosition());
			this.amountLeft = node.getAmountRemaining();
			this.id 		= node.getID();
			this.type		= node.getType();

			this.distanceToTownHall = this.position.chebyshevDistance(townHall);
		}

		public DummyResourceSpot(DummyResourceSpot parent) {
			this.position 	= parent.position;
			this.amountLeft = parent.amountLeft;
			this.id 		= parent.id;
			this.type		= parent.type;

			this.distanceToTownHall = parent.distanceToTownHall;
		}

		public Position getPosition() {
			return position;
		}

		public int getId() {
			return id;
		}

		public Type getType() {
			return type;
		}

		public int getAmount() {
			return amountLeft;
		}

		@Override
		public DummyResourceSpot copy() {
			return new DummyResourceSpot(this);
		}
	}

	private static final String TOWNHALL = "TownHall";
	private static final String PEASANT = "Peasant";
	private static final int PEASANT_GOLD_COST = 400;
	private static final int MAX_PEASANT_HOLD = 100;

	public static int getMapXExtent() {
		return mapXExtent;
	}

	private static int mapXExtent;

	public static int getMapYExtent() {
		return mapYExtent;
	}

	private static int mapYExtent;
	private static boolean buildPeasants;
	private static int playerNum;
	private static int requiredGold;
	private static int requiredWood;

	private static Position townHall;
	private static int townHallId;

	public List<DummyResourceSpot> getGoldmines() {
		return goldmines;
	}

	private List<DummyResourceSpot> goldmines;

	public List<DummyResourceSpot> getForests() {
		return forests;
	}

	private List<DummyResourceSpot> forests;

	public List<DummyUnit> getPeasants() {
		return peasants;
	}

	private List<DummyUnit> peasants;
	private static int peasantTemplateId;

	private int collectedGold;
	private int collectedWood;

	private GameState parent;			// parent state of this state
	private MultiStripsAction action;	// action turning parent into this state

	private Double cachedHeuristic;
	private double cachedCost;

	/**
	 * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
	 * nodes should be constructed from the another constructor you create or by factory functions that you create.
	 *
	 * @param state The current stateview at the time the plan is being created
	 * @param playernum The player number of agent that is planning
	 * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
	 * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
	 * @param buildPeasants True if the BuildPeasant action should be considered
	 */
	public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
		GameState.buildPeasants = buildPeasants;
		GameState.playerNum		= playernum;
		GameState.requiredGold	= requiredGold;
		GameState.requiredWood	= requiredWood;
		GameState.mapXExtent    = state.getXExtent();
		GameState.mapYExtent    = state.getYExtent();

		this.goldmines = new ArrayList<>();
		this.forests   = new ArrayList<>();

		collectedGold = state.getResourceAmount(playerNum, ResourceType.GOLD);
		collectedWood = state.getResourceAmount(playerNum, ResourceType.WOOD);

		// Initialize town hall and peasants
		peasants = new ArrayList<DummyUnit>();
		for(UnitView unit: state.getUnits(playerNum)) {
			if (unit.getTemplateView().getName().equals(TOWNHALL)) {
				townHall = new Position(unit.getXPosition(), unit.getYPosition());
				townHallId = unit.getID();
			} else if (unit.getTemplateView().getName().equals(PEASANT)) {

				peasantTemplateId = unit.getTemplateView().getID();

				DummyUnit peasant = new DummyUnit(new Position(unit.getXPosition(), unit.getYPosition()), unit.getID());

				if(unit.getCargoAmount() != 0) {
					if(unit.getCargoType() == ResourceType.GOLD) {
						peasant.gold = unit.getCargoAmount();
					} else if (unit.getCargoType() == ResourceType.WOOD) {
						peasant.wood = unit.getCargoAmount();
					}
				}

				peasants.add(peasant);
			}
		}

		if (townHall == null) {
			System.err.println("No townhall found");
			System.exit(1);
		}

		for(ResourceNode.ResourceView node: state.getAllResourceNodes()) {
			if (node.getType() == ResourceNode.Type.GOLD_MINE) {
				goldmines.add(new DummyResourceSpot(node, townHall));
			} else if (node.getType() == ResourceNode.Type.TREE) {
				forests.add(new DummyResourceSpot(node, townHall));
			}
		}
	}

	public GameState(GameState parent, MultiStripsAction action) {
		collectedGold = parent.collectedGold;
		collectedWood = parent.collectedWood;

		this.peasants  = deepCopyList(parent.peasants);
		this.forests   = deepCopyList(parent.forests);
		this.goldmines = deepCopyList(parent.goldmines);

		this.parent = parent;
		this.action = action;

		cachedCost = parent.cachedCost + 1;
	}

	public GameState getParentState() {
		return parent;
	}

	public MultiStripsAction getAction() {
		return action;
	}

	private <T extends Copyable<T>> List<T> deepCopyList(List<T> list) {
		return list.stream().map(Copyable::copy).collect(Collectors.toList());
	}

	// Returns the fake id of the built peasant
	public void makePeasant(int id) {
		if (collectedGold < GameState.PEASANT_GOLD_COST) {
			throw new Error("Tried to create peasant with not enough gold!");
		}

		collectedGold -= PEASANT_GOLD_COST;

		DummyUnit newPeasant = new DummyUnit(new Position(townHall.x, townHall.y-1), id);
		peasants.add(newPeasant);

	}

	public void doHarvest(int unitId, int resourceId) {
		DummyResourceSpot spot = getResourceSpot(resourceId);

		int amountGathered = Math.min(spot.amountLeft, GameState.MAX_PEASANT_HOLD);
		spot.amountLeft -= amountGathered;
		if (spot.amountLeft <= 0) {
			//deleteResourceSpot(spot);
		}

		getUnit(unitId).give(spot.getType(), amountGathered);
	}

	public void deleteResourceSpot(DummyResourceSpot spot){
		for(DummyResourceSpot possibleSpot: forests) {
			if(spot.position.equals(possibleSpot.position)) {
				forests.remove(spot);
				return;
			}
		}

		for(DummyResourceSpot possibleSpot: goldmines) {
			if(spot.position.equals(possibleSpot.position)) {
				goldmines.remove(spot);
				return;
			}
		}
	}


	public void doDeposit(int unitId) {
		DummyUnit unit = getUnit(unitId);

		if (unit.wood > 0) {
			collectedWood += unit.wood;
			unit.wood = 0;
		} else {
			collectedGold += unit.gold;
			unit.gold = 0;
		}
	}

	public DummyUnit getUnit(int id) {
		for (DummyUnit unit : peasants) {
			if (unit.id == id) {
				return unit;
			}
		}

		return null;
	}

	public DummyResourceSpot getResourceSpot(int id) {
		for (DummyResourceSpot spot : goldmines) {
			if (spot.id == id) {
				return spot;
			}
		}

		for (DummyResourceSpot spot : forests) {
			if (spot.id == id) {
				return spot;
			}
		}

		return null;
	}

	/**
	 * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
	 * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
	 * this function to check if the goal conditions are met and return true if they are.
	 *
	 * @return true if the goal conditions are met in this instance of game state.
	 */
	public boolean isGoal() {
		return (!needMoreGold() && !needMoreWood());
	}

	/**
	 * Determines if the current GameState is valid or not.
	 * 
	 * @return validity of this instance
	 */
	public boolean isValid() {
		// TODO : implement me
		return true;
	}

	/**
	 * The branching factor of this search graph are much higher than the planning. Generate all of the possible
	 * successor states and their associated actions in this method.
	 *
	 * @return A list of the possible successor states
	 */
	public List<GameState> generateChildren() {
		List<List<StripsAction>> actionLists = new ArrayList<>();

		if (buildPeasants) {
			actionLists.add(getTownhallActions());
		}

		for (DummyUnit unit : peasants) {
			DummyResourceSpot spot = getAdjacentResource(unit.position);
			if (spot != null && !unit.hasSomething() && spot.amountLeft > 0) {
				actionLists.add(Collections.singletonList(new HarvestAction(unit.id, spot.id, unit.position.getDirection(spot.position))));
			} else if (unit.hasSomething()) {
				if (nextToTownhall(unit)) {
					actionLists.add(Collections.singletonList(new DepositAction(unit.id, unit.position.getDirection(townHall))));
				} else {
					actionLists.add(Collections.singletonList(new MoveAction(unit.id, unit.position, townHall)));
				}
			} else {
				actionLists.add(getMoveToResourceActions(unit));
			}
		}

		return CartesianProduct.stream(actionLists)
				.map(list -> new MultiStripsAction(list))
				.filter(multiaction -> multiaction.preconditionsMet(GameState.this))
				.map(multiaction -> multiaction.apply(this))
				.filter(GameState::isValid)
				.collect(Collectors.toList());
	}

	private List<StripsAction> getTownhallActions() {
		List<StripsAction> townhallActions = new ArrayList<>();
		townhallActions.add(new CreatePeasantAction(this));
		townhallActions.add(new NullAction());
		return townhallActions;
	}

	private List<StripsAction> getMoveToResourceActions(DummyUnit unit) {
		List<StripsAction> actions = new ArrayList<>();
		//if(needMoreGold()) {
			for (DummyResourceSpot spot : goldmines) {
				if (spot.amountLeft > 0) {
					actions.add(new MoveAction(unit.id, unit.position, spot.position));
				}
			}
		//}
		//if(needMoreWood()) {
			for (DummyResourceSpot spot : forests) {
				if (spot.amountLeft > 0) {
					actions.add(new MoveAction(unit.id, unit.position, spot.position));
				}
			}
		//}
		
		return actions;
	}

	private boolean nextToTownhall(DummyUnit unit) {
		return unit.position.isAdjacent(townHall);
	}

	/**
	 * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
	 * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
	 *
	 * Add a description here in your submission explaining your heuristic.
	 *
	 * @return The value estimated remaining cost to reach a goal state from this state.
	 */
	public double heuristic() {
		if (cachedHeuristic != null) {
			return cachedHeuristic;
		}

		cachedHeuristic = 100.0;
		int goldCollectionsNeeded = goldMineMovesLeft();
		int woodCollectionsNeeded = woodMineMovesLeft();

		for (DummyUnit peasant: peasants) {
			cachedHeuristic += 50;

			if (peasant.hasSomething()) {
				cachedHeuristic -= peasant.position.chebyshevDistance(townHall);

				if (peasant.gold > 0 ) {
					goldCollectionsNeeded--;
				} else {
					woodCollectionsNeeded--;
				}
			} else if (goldCollectionsNeeded > 0) {
				cachedCost -= getShortestRoundtrip(peasant.position, goldmines);
				goldCollectionsNeeded--;
			} else if (woodCollectionsNeeded > 0) {
				cachedCost -= getShortestRoundtrip(peasant.position, forests);
				woodCollectionsNeeded--;
			}
		}

		cachedHeuristic -= goldCollectionsNeeded * 15;
		cachedHeuristic -= woodCollectionsNeeded * 10;

		System.out.println(cachedHeuristic);

		return cachedHeuristic;
	}

	public double getShortestRoundtrip(Position pos, List<DummyResourceSpot> resourceSpots) {
		return resourceSpots.stream()
				.filter(resource -> resource.amountLeft > 0)
				.mapToInt(resource -> {
					return pos.chebyshevDistance(resource.position) + resource.distanceToTownHall;
				})
				.min().orElse(0);
	}

	public DummyResourceSpot getAdjacentResource(Position pos) {
		if (goldMineMovesLeft() > 0) {
			for (DummyResourceSpot mine : goldmines) {
				if (mine.amountLeft > 0 && pos.isAdjacent(mine.position)) {
					return mine;
				}
			}
		}

		if (woodMineMovesLeft() > 0) {
			for (DummyResourceSpot wood : forests) {
				if (wood.amountLeft > 0 && pos.isAdjacent(wood.position)) {
					return wood;
				}
			}
		}

		return null;
	}

	public int goldMineMovesLeft() {
		return Math.max(ceilDivide(requiredGold - collectedGold, MAX_PEASANT_HOLD), 0);
	}

	public int woodMineMovesLeft() {
		return Math.max(ceilDivide(requiredWood - collectedWood, MAX_PEASANT_HOLD), 0);
	}

	// might be a nicer way of doing this, but otherwise the two above methods underestimate by 1
	private int ceilDivide(double n, double d) {
		return (int) Math.ceil(n / d);
	}

	public boolean needMoreWood() {
		return collectedWood < requiredWood;
	}

	public boolean needMoreGold() {
		return collectedGold < requiredGold;
	}

	public int getGold() { return this.collectedGold; };

	/**
	 *
	 * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
	 * determine which actions/states are better to explore.
	 *
	 * @return The current cost to reach this goal
	 */
	public double getCost() {
		return cachedCost;
	}

	/**
	 * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
	 * interface documentation to learn how this function should work.
	 *
	 * @param o The other game state to compare
	 * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
	 */
	@Override
	public int compareTo(GameState o) {
		return Double.compare(o.heuristic(), heuristic());
	}


	/**
	 * This will be necessary to use the GameState as a key in a Set or Map.
	 *
	 * @param o The game state to compare
	 * @return True if this state equals the other state, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof GameState && hashCode() == o.hashCode();
	}

	/**
	 * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
	 * equal they should hash to the same value.
	 *
	 * @return An integer hashcode that is equal for equal states.
	 */
	@Override
	public int hashCode() {
		int hash = 0;

		hash ^= requiredGold  * 5527 + requiredWood  * 719; // magic primes
		hash ^= collectedGold * 6763 + collectedWood * 401;

		// using two primes, gives 2213 distributed integers in the cyclic group
		// of Z\2213Z since gcd(983, 2213) = 1
		int mult = 983;
		for (DummyResourceSpot mine : goldmines) {
			// might need to multiply the ID factor to further distinguish instances
			hash ^= mult * mine.amountLeft * (mine.id + 1);
			mult = (mult + 983) % 2213;
		}

		for (DummyResourceSpot forest : forests) {
			hash ^= mult * forest.amountLeft * (forest.id + 1);
			mult = (mult + 983) % 2213;
		}

		for (DummyUnit peasant : peasants) {
			hash ^= peasant.hashCode();
		}

		return hash;
	}

	public Position getTownHall() {
		return townHall;
	}

	public int getTownHallId() {
		return townHallId;
	}

	public int getPeasantTemplateId() {
		return peasantTemplateId;
	}

	@Override
	public String toString() {
		return "GameState{" +
				", peasants=" + peasants.get(0).getId() +
				", collectedGold=" + collectedGold +
				", collectedWood=" + collectedWood +
				", action=" + action +
				", cachedHeuristic=" + cachedHeuristic +
				", cachedCost=" + cachedCost +
				'}';
	}
}
