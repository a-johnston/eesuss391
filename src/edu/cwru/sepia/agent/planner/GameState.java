package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.CreatePeasantAction;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Collector;

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
    public class DummyUnit {
        public Position getPosition() {
            return position;
        }

        private Position position;
        private int wood;
        private int gold;

        public double getRandomId() {
            return randomId;
        }

        private double randomId;

        public DummyUnit(Position p) {
            this.position = p;
            this.randomId = Math.random();
        }
        
        public DummyUnit(DummyUnit parent) {
        	this.position = parent.position;
        	this.wood	  = parent.wood;
        	this.gold	  = parent.gold;
        	this.randomId = parent.randomId;
        }
        
        public boolean hasSomething() {
        	return wood + gold != 0;
        }
        
        @Override
        public int hashCode() {
        	int hash = position.hashCode();
        	
        	hash ^= 307 * wood;
        	hash ^= 569	* gold;
        	
        	return hash;
        }
        
        /*
         * TODO: current use case makes it seem like dummies should be equal
         * not when their parameters are equal but when they represent the same
         * actual unit. Might need to change this as we move forward.
         */
        @Override
        public boolean equals(Object obj) {
        	if (obj instanceof DummyUnit) {
        		return this.randomId == ((DummyUnit) obj).randomId;
        	}
        	return false;
        }
    }

    public class DummyResourceSpot {
        public Position getPosition() {
            return position;
        }
        private Position position;
        private int amountLeft;
        private ResourceNode.Type type;
        private int distanceToTownHall;

        public int getId() {
            return id;
        }

        private int id;

        public DummyResourceSpot(Position p, int id, ResourceNode.Type t, int amountLeft, Position townHall) {
            this.position = p;
            this.amountLeft = amountLeft;
            this.type = t;
            this.distanceToTownHall = p.chebyshevDistance(townHall);
            this.id = id;
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
    private State.StateView state;

    public List<DummyUnit> getPeasants() {
        return peasants;
    }

    private List<DummyUnit> peasants;
    private int peasantTemplateId;

    private int collectedGold;
    private int collectedWood;
    
    private GameState parent;
    // represents the action taken to get to this state from the parent
    private StripsAction action;
    
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

        // Init townhall and peasants
        peasants = new ArrayList<DummyUnit>();
        for(UnitView unit: state.getUnits(playerNum)) {
            if (unit.getTemplateView().getName().equals(TOWNHALL)) {
                townHall = new Position(unit.getXPosition(), unit.getYPosition());
                townHallId = unit.getID();
            } else if (unit.getTemplateView().equals(PEASANT)) {

            	peasantTemplateId = unit.getTemplateView().getID();
            	
                DummyUnit peasant = new DummyUnit(
                        new Position(unit.getXPosition(), unit.getYPosition())
                );

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
        }

        for(ResourceNode.ResourceView node: state.getAllResourceNodes()) {
            if (node.getType() == ResourceNode.Type.GOLD_MINE) {
                goldmines.add(new DummyResourceSpot(
                        new Position(node.getXPosition(), node.getYPosition()),
                        node.getID(),
                        node.getType(),
                        node.getAmountRemaining(),
                        townHall
                ));
            } else if (node.getType() == ResourceNode.Type.TREE) {
                forests.add(new DummyResourceSpot(
                        new Position(node.getXPosition(), node.getYPosition()),
                        node.getID(),
                        node.getType(),
                        node.getAmountRemaining(),
                        townHall
                ));
            }
        }
    }

    public GameState(GameState parent) {
    	collectedGold = parent.collectedGold;
    	collectedWood = parent.collectedWood;
    	
    	cachedCost = parent.cachedCost + 1;
    }
    
    public void makePeasant() {
    	if (collectedGold < GameState.PEASANT_GOLD_COST) {
    		throw new Error("Tried to create peasant with not enough gold!");
    	}
    	
    	collectedGold -= PEASANT_GOLD_COST;
    	
    	// CREATE PEASANT HERE
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
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        List<StripsAction> actions = new ArrayList<>();

        if (buildPeasants) {
        	actions.add(new CreatePeasantAction());
        }
        
        // TODO: Implement me! Basic actions here
        
        return actions.stream()
        		.filter(action -> action.preconditionsMet(GameState.this))
        		.map(action -> action.apply(GameState.this))
        		.collect(Collectors.toList());
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

    	cachedHeuristic = 0.0;

        int goldCollectionsNeeded = goldMineMovesLeft();
        int woodCollectionsNeeded = woodMineMovesLeft();
        cachedHeuristic += (goldCollectionsNeeded + woodCollectionsNeeded)/peasants.size(); // Not sure if i can divide this value by the number of units left and still have it admissible

        int temp;
        int max = 0;
        /*
         * TODO:
         * This loop figures out what the most expensive peasant is going to be.
         * Potentially overestimates as we don't check if resource spot runs out?
         * i.e. one very close, one very far, first peasant depletes close one,
         * heuristic still treats as if second peasant can go there
         */
        for (DummyUnit peasant: peasants) {
        	temp = 0;
            if (peasant.hasSomething()) {
                temp += peasant.position.chebyshevDistance(townHall);
            } else if (goldCollectionsNeeded > 0) {
                temp += getShortestRoundtrip(peasant.position, goldmines);
                goldCollectionsNeeded--;
            } else if (woodCollectionsNeeded > 0) {
                temp += getShortestRoundtrip(peasant.position, forests);
                woodCollectionsNeeded--;
            }

            if (temp > max) {
                max = temp;
            }
        }

        cachedHeuristic += max;
        cachedHeuristic += getShortestRoundtrip(townHall, goldmines) * ceilDivide(goldMineMovesLeft(), peasants.size());
        cachedHeuristic += getShortestRoundtrip(townHall, forests) * ceilDivide(woodMineMovesLeft(), peasants.size());

        return cachedHeuristic;
    }

    public double getShortestRoundtrip(Position pos, List<DummyResourceSpot> resourceSpots) {
        return resourceSpots.stream()
        		.mapToInt(resource -> {
        			return resource.amountLeft > 0 
        					? pos.chebyshevDistance(resource.position) + resource.distanceToTownHall
        					: Integer.MAX_VALUE;
        		}).min().getAsInt();
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
        return Double.compare(getCost(), o.getCost()); // TODO: this is just a placeholder
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode(); // TODO: might want to additionally enforce type checking
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
        
        // TODO: More XOR once we have more member variables
        
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
}
