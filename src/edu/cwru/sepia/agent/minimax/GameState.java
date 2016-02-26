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

	private static final double UTILITY_BASE			= 30.0;
	private static final double UTILITY_ATTACK_BONUS	= 100.0;
	

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
        for (UnitView view: this.game.getAllUnits()) {
            if (!footmenView.contains(view)) {
                archers.add(new DummyUnit(view));
            }
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
        	return Double.NEGATIVE_INFINITY;
        }
        
        if (archers.size() == 0) {
        	return Double.POSITIVE_INFINITY;
        }
        
        // Prioritize being closer, having more footmen, and attacking
        int temp;
        for (DummyUnit footman : footmen) {
        	temp = getShortestDistance(footman);
        	
        	if (temp == 1) {
        		utility += UTILITY_ATTACK_BONUS;
        	}
        	
        	utility -= temp;
        }
        
        return utility;
    }
    
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
    
    /*
     * Taxicab norm dx + dy
     * Minimum number of moves to reach (to.x, to.y) from (from.x, from.y)
     * Assuming as per instructions we cannot move diagonally
     */
    public int getDistance(DummyUnit from, DummyUnit to) {
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
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
        if (maxAgent) {
            return getPossibleFutures(footmen, archers, footmen.get(0).view.getTemplateView().getRange());
        } else {
        	return getPossibleFutures(archers, footmen, archers.get(0).view.getTemplateView().getRange());
        }
    }

    public List<GameStateChild> getPossibleFutures(List<DummyUnit> controlled, List<DummyUnit> targets, int range) {
        List<GameStateChild> next = new ArrayList<>();
        List<Pair<DummyUnit, List<Action>>> controlledActions = new ArrayList<>();

        for (DummyUnit unit : controlled) {
            List<Action> unitActions = new ArrayList<>();

        	for(DummyUnit enemy: targets) {
        		if (getDistance(unit, enemy) <= range) {
                    unitActions.add(this.createAttackAction(unit.view, enemy.view));
        		}
        	}

        	for (Direction direction: Direction.values()) {
        		int newX = unit.x + direction.xComponent();
        		int newY = unit.y + direction.yComponent();
        		if (validMove(newX, newY, targets)) {
                    unitActions.add(this.createMoveAction(unit.view, direction));
        		}
        	}

            controlledActions.add(new Pair<>(unit, unitActions));
        }

        List<List<Pair<DummyUnit, Action>>> gameStateActions = new ArrayList<>();

        for (Pair<DummyUnit, List<Action>> actions : controlledActions) {
        	if (gameStateActions.size() == 0) {
        		for (Action action : actions.b) {
        			List<Pair<DummyUnit, Action>> added = new ArrayList<>();
        			added.add(new Pair<>(actions.a, action));
        			gameStateActions.add(added);
        		}
        	} else {
        		for (List<Pair<DummyUnit, Action>> state : gameStateActions) {
        			for (Action action : actions.b) {
        				state.add(new Pair<>(actions.a, action));
        			}
        		}
        	}
        }
        
        for (List<Pair<DummyUnit, Action>> gameStateAction: gameStateActions) {
            GameState state = new GameState(this);
            List<DummyUnit> newControlledUnits	= new ArrayList<>();
            List<DummyUnit> newTargetUnits		= deepCopyDummies(targets);
            
            state.footmen = newControlledUnits;
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

    public boolean validMove(int newX, int newY, List<DummyUnit> enemies) {
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
    
    public Action createAttackAction(UnitView attackingUnit, UnitView victimUnit) {
        return Action.createPrimitiveAttack(attackingUnit.getID(), victimUnit.getID());
    }

    public Action createMoveAction(UnitView movingUnit,Direction direction) {
        return Action.createPrimitiveMove(movingUnit.getID(), direction);
    }
    
    public List<DummyUnit> deepCopyDummies(List<DummyUnit> dummies) {
    	List<DummyUnit> newList = new ArrayList<>();
    	
    	for (DummyUnit dummy : dummies) {
    		newList.add(new DummyUnit(dummy));
    	}
    	
    	return newList;
    }
}
