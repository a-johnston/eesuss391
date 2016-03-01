package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

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
	private class DummyUnit {
        int x;
		int y;
		int hp;
		int id;
		UnitView view;

		public DummyUnit(DummyUnit parent) {
			this.x  = parent.x;
			this.y  = parent.y;
			this.hp = parent.hp;
			this.id = parent.id;
			this.view = parent.view;
		}

		
		public DummyUnit(UnitView view) {
			this.x  = view.getXPosition();
			this.y  = view.getYPosition();
			this.hp = view.getHP();
			this.id = view.getID();
			this.view = view;
		}


		public DummyUnit(DummyUnit parent, Direction direction) {
			this.x  = parent.x + direction.xComponent();
			this.y  = parent.y + direction.yComponent();
			this.hp = parent.hp;
			this.id = parent.id;
			this.view = parent.view;
		}
		
		public DummyUnit(DummyUnit parent, int damage) {
			this.x  = parent.x;
			this.y  = parent.y;
			this.hp = parent.hp - damage;
			this.id = parent.id;
			this.view = parent.view;
		}
	}
	
	private static final double FOOTMAN_ARCHER_HEALTH_RATIO = 160.0/50.0;

    private static final double ARCHER_WIN_BONUS        = -1000.0;
    private static final double FOOTMEN_WIN_BONUS       = 1000.0;
    private static final double LIVING_ARCHER_BONUS     = -10.0;
    private static final double LIVING_FOOTMAN_BONUS    = 10.0;
	private static final double UTILITY_BASE			= 0;
	private static final double UTILITY_ATTACK_BONUS	= 5.0;
	private static final double UTILITY_NO_PATH         = -30.0;
    private static final double ROOK_CHECKMATE_BONUS    = 10.0;
    private static final double UNCHASED_ARCHER_BONUS   = -20.0; // It's really bad to leave an archer "unchased"

    private State.StateView game;
    private boolean maxAgent;
    private Double utility = null;
    
    private List<DummyUnit> footmen;
    private List<DummyUnit> archers;

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
        this.maxAgent = true;
        
        buildDummyUnits();
    }
    
    private GameState(GameState parent) {
    	this.game   = parent.game;
    	this.maxAgent = !parent.maxAgent; // swap sides
    }

    /**
     * gets a specific unit's health
     * @param id
     * @return
     */
    public int getUnitHealth(int id) {
        return this.game.getUnit(id).getHP();
    }
    
    public void buildDummyUnits() {
    	List<UnitView> footmenView = this.game.getUnits(0);
    	
    	footmen = new ArrayList<>();
        for (UnitView view : footmenView) {
        	footmen.add(new DummyUnit(view));
        }
        
        archers = new ArrayList<>();
        for (UnitView view: this.game.getUnits(1)) {
            archers.add(new DummyUnit(view));
        }
    }
    
    public void copyDummyUnits(GameState parent) {
    	
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
    	// Since we run a sort and then more checks, cache for performance
        if (this.utility != null) {
            return this.utility;
        }

        utility = UTILITY_BASE;
        
        // Handle end-game scenarios
        if (footmen.size() == 0) {
        	return ARCHER_WIN_BONUS;
        }
        
        if (archers.size() == 0) {
        	return FOOTMEN_WIN_BONUS;
        }

        utility += footmen.size()*LIVING_FOOTMAN_BONUS;
        utility += archers.size()*LIVING_ARCHER_BONUS;
        utility += rookCheckmatePositionUtility();

        // Prioritize being closer, having more footmen, and attacking
        int temp;
        for (DummyUnit footman : footmen) {
            temp = getShortestDistance(footman);

            if (temp == 1) {
                utility += UTILITY_ATTACK_BONUS;
            }

            utility -= temp;
        }
        utility += Math.random(); // Break ties randomly
        return utility;
    }

    /**
     * Determines if the footment are in a "rook checkmate" like position.
     * Such a position is nice because it allows for the archer to be trapped and subsequently killed.
     * @return
     */
    public double rookCheckmatePositionUtility() {
        double utility = 0.0;

        List<Pair<DummyUnit, Direction>> edgeArchers = new ArrayList<>();
        for (DummyUnit archer : archers) {
            // Not else if because of corner case...like a literal corner case.
            if(archer.x == 0) {
                edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.WEST));
            }

            if (archer.y == 0) {
                edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.NORTH));
            }
            if (archer.x == game.getXExtent() - 1) {
                edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.EAST));

            }
            if (archer.y == game.getYExtent() - 1) {
                edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.SOUTH));
            }
        }

        for(Pair<DummyUnit, Direction> edgeArcherPair: edgeArchers) {
            if(isRookCheckmatePosition( edgeArcherPair)) {
                utility += ROOK_CHECKMATE_BONUS;
            }
        }

        return utility;
    }

    public boolean isRookCheckmatePosition(Pair<DummyUnit, Direction> edgeArcherPair) {
        boolean firstRankCovered = false;
        boolean secondRankCovered = false;

        for (DummyUnit footman: footmen) {
            if(edgeArcherPair.b == Direction.EAST) {
                if (footman.x == game.getXExtent() - 1) {
                    firstRankCovered = true;
                } else if (footman.x == game.getXExtent() - 2) {
                    secondRankCovered = true;
                }
            } else if (edgeArcherPair.b == Direction.WEST) {
                if (footman.x == 0) {
                    firstRankCovered = true;
                } else if (footman.x == 1) {
                    secondRankCovered = true;
                }
            } else if (edgeArcherPair.b == Direction.NORTH) {
                if (footman.y == 0) {
                    firstRankCovered = true;
                } else if (footman.y == 1) {
                    secondRankCovered = true;
                }
            } else if (edgeArcherPair.b == Direction.SOUTH) {
                if (footman.y == game.getYExtent() - 1 ) {
                    firstRankCovered = true;
                } else if (footman.y == game.getYExtent() - 2) {
                    secondRankCovered = true;
                }
            }
        }
        return firstRankCovered && secondRankCovered;
    }

    /**
     * Logically, if the "moving towards" path is blocked, the utility is flipped.
     * @return
     */
    public boolean blockedPath() {

        return false;

    }

    /**
     *
     * @param footman
     * @return
     */
    public int getShortestDistance(DummyUnit footman) {
    	int best = Integer.MAX_VALUE;
    	
    	int temp;
    	for (DummyUnit archer : archers) {
    		temp = getDistance(footman, archer);
    		
    		if (temp < best) {
    			best = temp;
    		}
    	}
    	
    	return best;
    }
    
    /**
     * Computes the taxicab norm dx + dy. In this assignment, the minimum
     * number of moves to reach a given destination. Unlike Chebychev, accounts
     * for not being able to move diagonally.
     * 
     * @param from
     * @param to
     * @return
     */
    public int getDistance(DummyUnit from, DummyUnit to) {
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
    }

    /**
     * Generates all possible children of the current state. Makes moves for both
     * the footmen and the archers depending on which turn it is.
     * @return list of game states that are one step from this step
     */
    public List<GameStateChild> getChildren() {
        if (maxAgent) {
            return getPossibleFutures(footmen, archers);
        } else {
        	return getPossibleFutures(archers, footmen);
        }
    }

    /**
     * Given controlled units and their possible targets, returns possible moves
     * with a computed heuristic.
     * @param controlled units that are controlled on the current turn
     * @param targets units that are targetted on the current turn
     * @return list if this state's children 
     */
    private List<GameStateChild> getPossibleFutures(List<DummyUnit> controlled, List<DummyUnit> targets) {
        List<Pair<DummyUnit, List<Action>>> controlledActions = generateUnitActions(controlled, targets);
        List<List<Pair<DummyUnit, Action>>> gameStateActions = generateActionCombinations(controlledActions);
        return buildGameStateChildren(gameStateActions, targets);
    }
    
    /**
     * Generates all possible actions for all controlled units. Does not return invalid moves.
     * @param controlled
     * @param targets
     * @return
     */
    private List<Pair<DummyUnit, List<Action>>> generateUnitActions(List<DummyUnit> controlled, List<DummyUnit> targets) {
    	List<Pair<DummyUnit, List<Action>>> controlledActions = new ArrayList<>();
    	
    	for (DummyUnit unit : controlled) {
            List<Action> unitActions = new ArrayList<>();

        	for(DummyUnit enemy: targets) {
        		if (getDistance(unit, enemy) <= unit.view.getTemplateView().getRange()) {
                    unitActions.add(this.createAttackAction(unit, enemy));
        		}
        	}

        	for (Direction direction: Direction.values()) {
                // Ignore diagonal directional movement
                if (direction == Direction.SOUTHEAST ||
                        direction == Direction.SOUTHWEST ||
                        direction == Direction.NORTHEAST ||
                        direction == Direction.NORTHWEST){
                    continue;
                }
        		int newX = unit.x + direction.xComponent();
        		int newY = unit.y + direction.yComponent();
        		if (validMove(newX, newY, targets)) {
                    unitActions.add(this.createMoveAction(unit, direction));
        		}
        	}

            controlledActions.add(new Pair<>(unit, unitActions));
        }
    	
    	return controlledActions;
    }
    
    /**
     * Given all possible actions, generates all unique combinations of moves
     * on a given turn.
     * 
     * @param controlledActions
     * @return
     */
    private List<List<Pair<DummyUnit, Action>>> generateActionCombinations(List<Pair<DummyUnit, List<Action>>> controlledActions) {
    	List<List<Pair<DummyUnit, Action>>> gameStateActions = new ArrayList<>();
    	
    	for (Pair<DummyUnit, List<Action>> actions : controlledActions) {
        	if (gameStateActions.size() == 0) {
                for (Action action : actions.b) {
                    List<Pair<DummyUnit, Action>> added = new ArrayList<>();
                    added.add(new Pair<>(actions.a, action));
        			gameStateActions.add(added);
        		}
        	} else {
                List<List<Pair<DummyUnit, Action>>> temp = new ArrayList<>();
        		for (List<Pair<DummyUnit, Action>> state : gameStateActions) {
        			for (Action action : actions.b) {
                        List<Pair<DummyUnit, Action>> tempState = new ArrayList<Pair<DummyUnit, Action>>(state);
                        tempState.add(new Pair<>(actions.a, action));
                        temp.add(tempState);
                    }
        		}
                gameStateActions = temp;
        	}
        }
    	return gameStateActions;
    }
    
    /**
     * Given all possible combinations of moves for controlled units, creates
     * a child state where those moves have been taken. Accounts for location
     * and health.
     * 
     * @param gameStateActions
     * @param targets
     * @return
     */
    private List<GameStateChild> buildGameStateChildren(List<List<Pair<DummyUnit, Action>>> gameStateActions, List<DummyUnit> targets) {
    	List<GameStateChild> next = new ArrayList<>();
    	
    	for (List<Pair<DummyUnit, Action>> gameStateAction: gameStateActions) {
            GameState state = new GameState(this);
            List<DummyUnit> newControlledUnits	= new ArrayList<>();
            List<DummyUnit> newTargetUnits		= deepCopyDummies(targets);
            
            state.footmen = newControlledUnits; // TODO : look at this more closely
            state.archers = newTargetUnits;

            Map<Integer, Action> map = new HashMap<>();
            
            for (Pair<DummyUnit, Action> pair : gameStateAction) {
                Action action = pair.b;
                if (action instanceof TargetedAction) {
                    for(DummyUnit attackTarget: targets) {
                        if(attackTarget.id == ((TargetedAction) action).getTargetId()) {
                        	attackTarget.hp -= pair.a.view.getTemplateView().getBasicAttack();
                        	break;
                        }
                    }
                    
                    newControlledUnits.add(new DummyUnit(pair.a));
                } else if (action instanceof DirectedAction){
                	newControlledUnits.add(new DummyUnit(pair.a, ((DirectedAction) action).getDirection()));
                }
                
                map.put(pair.a.id, pair.b);
            }
            
            GameStateChild newChild = new GameStateChild(map, state);

            next.add(newChild);
        }
    	
    	return next;
    }

    /**
     * Given a location (newX, newY) and a list of enemies, returns if the move
     * to that location is valid.
     * 
     * @param newX
     * @param newY
     * @param enemies
     * @return
     */
    private boolean validMove(int newX, int newY, List<DummyUnit> enemies) {
        if (!this.game.inBounds(newX, newY)) {
            return false;
        } else if (this.game.isResourceAt(newX, newY)) {
            return false;
        }

        // Enemies don't move on our turn...right...right guys?!?!?!?
        for (DummyUnit enemy: enemies) {
            if(enemy.x == newX && enemy.y == newY) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Helper method for creating attacks
     * @param attackingUnit
     * @param victimUnit
     * @return
     */
    private Action createAttackAction(DummyUnit attackingUnit, DummyUnit victimUnit) {
        return Action.createPrimitiveAttack(attackingUnit.view.getID(), victimUnit.view.getID());
    }

    /**
     * Helper method for creating movements
     * @param movingUnit
     * @param direction
     * @return
     */
    private Action createMoveAction(DummyUnit movingUnit,Direction direction) {
        return Action.createPrimitiveMove(movingUnit.view.getID(), direction);
    }
    
    /**
     * Helper for generating new dummies for mutating health, location etc
     * @param dummies
     * @return
     */
    private List<DummyUnit> deepCopyDummies(List<DummyUnit> dummies) {
    	List<DummyUnit> newList = new ArrayList<>();
    	
    	for (DummyUnit dummy : dummies) {
    		newList.add(new DummyUnit(dummy));
    	}
    	
    	return newList;
    }
}
