package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;

import javax.annotation.Resource;
import java.util.*;

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

    private static boolean buildPeasants;
    private static int playerNum;
    private static int requiredGold;
    private static int requiredWood;
    private static List<ResourceNode.ResourceView> goldmines = new ArrayList<>(); // Not sure we want to actually make these lists static. doing it this way for now...
    private static List<ResourceNode.ResourceView> forests = new ArrayList<>();

    private int collectedGold;
    private int collectedWood;
    
    private Double cachedHeuristic, cachedCost;

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
        this.buildPeasants = buildPeasants;
        this.playerNum = playernum;
        this.requiredGold = requiredGold;
        this.requiredWood = requiredWood;

        for(ResourceNode.ResourceView node: state.getAllResourceNodes()) {
            if (node.getType() == ResourceNode.Type.GOLD_MINE) {
                 goldmines.add(node);
            } else if (node.getType() == ResourceNode.Type.TREE) {
                forests.add(node);
            }
        }

        // Presumably these numbers are 0 here...but you never know i guess
        collectedGold = state.getResourceAmount(playerNum, ResourceType.GOLD);
        collectedWood = state.getResourceAmount(playerNum, ResourceType.WOOD);
        // TODO: Figure out how to construct the state
    }

    public GameState() {
        // TODO: The constructor for every other case. Probably dependent on what generateGameStateChildren needs to pass along. 
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        if (buildPeasants) {
            return false; // Implement this for part two.
        } else {
            return this.isGoalNoBuildPeasants();
        }
    }

    /**
     * Determines if a state is a goal state in the non peasant building game
     * @return
     */
    public boolean isGoalNoBuildPeasants() {
        return (collectedGold == requiredGold && collectedWood == requiredWood);
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        // TODO: Implement me!
        return null;
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
        // TODO: Implement me!
    	
        return cachedHeuristic;
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
    	if (cachedCost != null) {
    		return cachedCost;
    	}
    	
    	cachedCost = 0.0;
        // TODO: Implement me!
    	
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
        return Double.compare(heuristic(), o.heuristic()); // TODO: this is just a placeholder
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
        // TODO: Implement me!
        return 0;
    }
}
