package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
	private static final double FOOTMAN_ARCHER_HEALTH_RATIO = 160.0/50.0;
	private static final double MAGIC_UNKNOWN_UTILITY = Double.NEGATIVE_INFINITY + Math.PI;
	
    private State.StateView game;
    private int player;
    private double utility = MAGIC_UNKNOWN_UTILITY;
    
    private List<UnitView> footmen;
    private List<UnitView> archers;

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        this.game = state;
        this.player = MinimaxAlphaBeta.getPlayerAgent().getPlayerNumber();
        
        getUnitLists();
    }

    /**
     * gets a specific unit's health
     * @param id
     * @return
     */
    public int getUnitHealth(int id) {
        return this.game.getUnit(id).getHP();
    }

    /**
     * Returns all of the healths of the units in the game
     * @return
     */
    public Map<Integer, Integer> getAllUnitsAndHealth() {
        Map<Integer, Integer> unitHealthMap = new HashMap<Integer, Integer>();
        for (UnitView unit : this.game.getAllUnits()) {
            unitHealthMap.put(unit.getID(), unit.getHP());
        }

        return unitHealthMap;
    }
    
    public List<UnitView> getUnitLists() {
        footmen = this.game.getUnits(this.player);
        archers = new ArrayList<UnitView>();
        for (UnitView unit: this.game.getAllUnits()) {
            if (!footmen.contains(unit)) {
                archers.add(unit);
            }
        }

        return archers;
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
        // Cache poke.
        if (this.utility != MAGIC_UNKNOWN_UTILITY) {
            return this.utility;
        }

        utility  = 0;
        utility += getArcherHealthUtility();
        utility += getFootmenHealthUtility();
        utility += getDistanceUtility();
        
        return utility;
    }

    public double getArcherHealthUtility() {
        double archerHealthUtility = 0;
        int unitCount = 0;
        for (UnitView archer: archers) {
            archerHealthUtility -= archer.getHP();
            unitCount++;
        }

        // Archers are ded.
        if (archerHealthUtility == 0) {
            return Double.POSITIVE_INFINITY;
        } else if (unitCount == 1) {
            archerHealthUtility += Double.MAX_VALUE/4;
        }

        return archerHealthUtility;
    }

    public double getFootmenHealthUtility() {
        double footmenHealthUtility = 0;
        int unitCount = 0;
        for (UnitView footman: this.game.getUnits(this.player)) {
            footmenHealthUtility += footman.getHP();
            unitCount++;
        }

        // Footmen are ded
        if (unitCount == 0) {
            return Double.NEGATIVE_INFINITY;
        } else if (unitCount == 1) {
            footmenHealthUtility -= Double.MIN_VALUE/4;
        }

        return footmenHealthUtility;
    }

    public double getDistanceUtility() {
        double distanceUtility = 0;
        for(UnitView footman: this.game.getUnits(this.player)) {
            for(UnitView archer: archers) {
                distanceUtility -= calculateDistance(footman, archer);
            }
        }
        return distanceUtility;
    }

    /*
     * Minimum number of moves to reach (to.x, to.y) from (from.x, from.y)
     */
    public double calculateDistance(UnitView from, UnitView to) {
        return Math.max(
        		Math.abs(from.getXPosition() - to.getXPosition()),
        		Math.abs(from.getYPosition() - to.getYPosition()));
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
        List<GameStateChild> children = new ArrayList<GameStateChild>();

        if (this.game.getTurnNumber() % 2 == 0) {
            children = doPlayerTurn();
        } else {
            children = doArcherTurn();
        }

        return children;
    }

    public List<GameStateChild> doPlayerTurn() {
        List<GameStateChild> children = new ArrayList<GameStateChild>();
        List<UnitView> footmen = this.game.getUnits(this.player);
        if(footmen.size() == 2) {

        } else {

        }
        
        return children;
    }

    public List<Action> playerUnitsActions(UnitView unit) {
        List<Action> actions = new ArrayList<Action>();
        boolean addedAttack = false;
        for (Direction direction: Direction.values()) {
            int newX = unit.getXPosition() + direction.xComponent();
            int newY = unit.getYPosition() + direction.yComponent();
            if (this.game.inBounds(newX, newY)) {
                for(UnitView archer: archers) {
                    if(newX==archer.getXPosition() && newY==archer.getYPosition()){
                        actions.add(Action.createPrimitiveAttack(unit.getID(), archer.getID()));
                        addedAttack = true;
                        break;
                    }
                }
                if(!addedAttack) {
                    actions.add(Action.createPrimitiveMove(unit.getID(), direction));
                }

                addedAttack = false;
            }
        }

        return actions;
    }

    public List<GameStateChild> doArcherTurn() {
    	return null;
    }
}
