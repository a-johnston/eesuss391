package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.Pair;

import java.util.*;

/**
 * Stores relevant information for a state in the game tree and generates
 * child states upon request.
 * 
 * Handles finding all combinations of actions and applying actions to child
 * states, including movement, attacking, and the death of units. Generalized
 * for arbitrary numbers of footmen and archers.
 * 
 * Holds onto an A* implementation to implement a planned movement heuristic.
 * 
 * @author Adam Johnston	amj69
 * @author Eric Luan		efl11
 */
public class GameState {

	private static class XY implements Comparable<XY> {
		final int x;
		final int y;

		XY cameFrom;
		float f;
		float g; 

		public XY(int x, int y) {
			this.x = x;
			this.y = y;

			cameFrom = null;
			f = Float.POSITIVE_INFINITY;
			g = Float.POSITIVE_INFINITY;
		}

		@Override
		public int compareTo(XY o) {
			return Float.compare(this.f, o.f);
		}

		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}
	}

	private static XY[][] map;

	public static XY[][] buildMap(StateView view) {
		map = new XY[view.getXExtent()][view.getYExtent()];

		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[0].length; y++) {
				map[x][y] = new XY(x, y);
			}
		}

		for (ResourceView tree : view.getAllResourceNodes()) {
			map[tree.getXPosition()][tree.getYPosition()] = null;
		}

		return map;
	}

	public List<XY> astar(XY from, XY to) {
		XY[][] map = buildMap(this.game);

		map[from.x][from.y] = from;
		map[to.x][to.y] = to;

		from.g = 0;
		from.f = getDistance(from, to);

		Queue<XY> frontier = new PriorityQueue<>(); //sorts based on f score
		Set<XY> explored = new HashSet<>();

		frontier.add(from);

		XY current;
		while (!frontier.isEmpty()) {
			current = frontier.remove();
			explored.add(current);

			if (current == to) {
				return rebuildPath(current);
			}

			for (XY next : getNeighbors(current, map)) {
				if (explored.contains(next)) {
					continue; //explored neighbor isn't worth checking
				}

				float new_g = current.g + 1;

				if (!frontier.contains(next)) {
					frontier.add(next);
				} else if (new_g >= next.g) {
					continue; //already had a better path to get here
				}

				next.cameFrom = current;
				next.g = new_g;
				next.f = new_g + getDistance(next, to);
			}
		}

		return null;
	}

	public List<XY> getNeighbors(XY current, XY[][] map) {
		List<XY> neighbors = new ArrayList<>();

		for (int i = -1; i < 2; i++) {
			if (current.x + i < 0 || current.x + i >= map.length) {
				continue;
			}

			for (int j = -1; j < 2; j++) {
				if (current.y + j < 0 || current.y + j >= map[0].length) {
					continue;
				}

				if ((Math.abs(i) == 1) ^ (Math.abs(j) == 1)) {
					if (map[current.x + i][current.y + j] != null) {
						neighbors.add(map[current.x + i][current.y + j]);
					}
				}
			}
		}

		return neighbors;
	}

	public List<XY> rebuildPath(XY node) {
		List<XY> path = new ArrayList<>();

		while (node != null) {
			path.add(0, node);
			node = node.cameFrom;
		}

		return path;
	}

	private class DummyUnit {
		DummyUnit parent;
		DummyUnit target;
		List<XY> inherited;
		XY xy;
		int hp;
		int id;
		UnitView view;

		public DummyUnit(DummyUnit parent) {
			this.xy = parent.xy;
			this.hp = parent.hp;
			this.id = parent.id;
			this.view = parent.view;
			this.parent = parent;

			if (parent != null) {
				target = parent.target;
				if (parent.inherited != null) {
					this.inherited = new ArrayList<>(parent.inherited);
					this.inherited.remove(0);
				}
			}
		}


		public DummyUnit(UnitView view) {
			this.xy  = new XY(view.getXPosition(), view.getYPosition());
			this.hp = view.getHP();
			this.id = view.getID();
			this.view = view;
		}


		public DummyUnit(DummyUnit parent, Direction direction) {
			this.xy   = new XY(parent.xy.x + direction.xComponent(), parent.xy.y + direction.yComponent());
			this.hp   = parent.hp;
			this.id	  = parent.id;
			this.view = parent.view;
			this.parent = parent;

			if (parent != null) {
				target = parent.target;
				if (parent.inherited != null) {
					this.inherited = new ArrayList<>(parent.inherited);
					this.inherited.remove(0);
				}
			}
		}
	}

	// Weights for the utility function
	private static final double ARCHER_WIN_BONUS        = -100000.0; // Winning is trivially valuable
	private static final double FOOTMEN_WIN_BONUS       = 100000.0;
	private static final double CORRECT_MOVE_BONUS      = 30.0; // "reward" the agent for moving in the correct direction.
	private static final double UTILITY_BASE			= 0;
	private static final double LIVING_ARCHER_BONUS     = -1500.0;
	private static final double LIVING_FOOTMAN_BONUS    = 1500.0;
	private static final double UTILITY_ATTACK_BONUS	= 200.0;
	private static final double ROOK_CHECKMATE_BONUS    = 500.0;
	//	private static final double UNCHASED_ARCHER_BONUS   = -1.0; // It's really bad to leave an archer "unchased"
	private static final double CORNERED_ARCHER_BONUS   = 1000.0;

	private State.StateView game;
	private boolean maxAgent;
	public Double utility = null;

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

		getUtility();
	}

	private GameState(GameState parent) {
		this.game	  = parent.game;
		this.maxAgent = !parent.maxAgent; // swap sides
	}

	public void buildDummyUnits() {
		footmen = new ArrayList<>();
		for (UnitView view : this.game.getUnits(0)) {
			footmen.add(new DummyUnit(view));
		}

		archers = new ArrayList<>();
		for (UnitView view: this.game.getUnits(1)) {
			archers.add(new DummyUnit(view));
		}
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
		// Handle end-game scenarios
		if (footmen.size() == 0) {
			return ARCHER_WIN_BONUS;
		}

		if (archers.size() == 0) {
			System.out.println("!!!!!!!!!!!!!!!!!!!GAME WIN BONUS");
			return FOOTMEN_WIN_BONUS;
		}

		// Since we run a sort and then more checks, cache for performance
		if (this.utility != null) {
			return this.utility;
		}

		utility = UTILITY_BASE;

		utility += rookCheckmatePositionUtility();
		utility += archers.size() * LIVING_ARCHER_BONUS;
		utility += footmen.size() * LIVING_FOOTMAN_BONUS;
		for (DummyUnit footman: footmen) {

			if (footman.target == null) {
				footman.target = getBestTarget(footman);
			}

			if (footman.inherited == null || footman.inherited.size() == 0) {
				if (footman.parent == null) {
					footman.inherited = astar(footman.xy, footman.target.xy);;
				} else {
					footman.inherited = astar(footman.parent.xy, footman.target.xy);
					footman.inherited.remove(0);
				}
			}

			XY xy = footman.inherited.get(0);

			if (footman.xy.x == xy.x && footman.xy.y == xy.y) {
				utility += CORRECT_MOVE_BONUS;
			} else {
				footman.inherited = astar(footman.xy, footman.target.xy);
			}

			int temp = getDistance(footman, footman.target);

			System.out.println("Temp" + temp);

			if (temp == 1) {
				System.out.println("GET FUCKED UP");
				utility += UTILITY_ATTACK_BONUS;
			}

			//			utility += UNCHASED_ARCHER_BONUS * temp;
		}

		// Prioritize being closer, having more footmen, and attacking
		for (DummyUnit archer : archers) {
			if(this.archerTrapped(archer)) {
				utility += CORNERED_ARCHER_BONUS;
			}
		}

		//		for (DummyUnit footman : footmen) {
		//			temp = getShortestDistanceFootman(footman);
		//			utility -= temp;
		//			if (footman.x == 0 || footman.x == game.getXExtent() -1) {
		//				utility -= 10;
		//			} else if (footman.y == 0 || footman.y == game.getYExtent() - 1) {
		//				utility -= 10;
		//			}
		//		}
		//		utility += Math.random(); // Break ties randomly to decrease chance of infinite games.
		System.out.println(utility);
		return utility;
	}


	/**
	 * Determines if an archer can move. Archer's which cannot move should increase the overall utility.
	 * @param archer
	 * @return
	 */
	public boolean archerTrapped(DummyUnit archer) {
		for (Direction d: Direction.values()) {
			if (d == Direction.SOUTHEAST ||
					d == Direction.SOUTHWEST ||
					d == Direction.NORTHEAST ||
					d == Direction.NORTHWEST){
				continue;
			}

			int newX = archer.xy.x + d.xComponent();
			int newY = archer.xy.y + d.yComponent();
			if(!game.inBounds(newX, newY)) {
				continue;
			}

			if(game.isResourceAt(newX, newY)) {
				continue;
			}

			for(DummyUnit footman: footmen) {
				if(footman.xy.x == newX && footman.xy.y == newY) {
					continue;
				}
			}

			return false;

		}

		return true;
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
			if(archer.xy.x == 0) {
				edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.WEST));
			}

			if (archer.xy.y == 0) {
				edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.NORTH));
			}
			if (archer.xy.x == game.getXExtent() - 1) {
				edgeArchers.add(new Pair<DummyUnit, Direction>(archer, Direction.EAST));

			}
			if (archer.xy.y == game.getYExtent() - 1) {
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
				if (footman.xy.x == game.getXExtent() - 1) {
					firstRankCovered = true;
				} else if (footman.xy.x == game.getXExtent() - 2) {
					secondRankCovered = true;
				}
			} else if (edgeArcherPair.b == Direction.WEST) {
				if (footman.xy.x == 0) {
					firstRankCovered = true;
				} else if (footman.xy.x == 1) {
					secondRankCovered = true;
				}
			} else if (edgeArcherPair.b == Direction.NORTH) {
				if (footman.xy.y == 0) {
					firstRankCovered = true;
				} else if (footman.xy.y == 1) {
					secondRankCovered = true;
				}
			} else if (edgeArcherPair.b == Direction.SOUTH) {
				if (footman.xy.y == game.getYExtent() - 1 ) {
					firstRankCovered = true;
				} else if (footman.xy.y == game.getYExtent() - 2) {
					secondRankCovered = true;
				}
			}
		}
		return firstRankCovered && secondRankCovered;
	}

	public DummyUnit getBestTarget(DummyUnit footman) {
		int bestDistance = Integer.MAX_VALUE;
		DummyUnit best = null;
		DummyUnit option = null;

		for (DummyUnit archer : archers) {
			int temp = getDistance(footman, archer);

			if (temp < bestDistance) {
				//				if (getOtherFootman(footman).target == archer) {
				//					option = archer;
				//				} else {
				bestDistance = temp;
				best = archer;
				//				}
			}
		}

		return best == null ? option : best;
	}

	public DummyUnit getOtherFootman(DummyUnit footman) {
		for (DummyUnit other : footmen) {
			if (other != footman) {
				return other;
			}
		}
		return footman;
	}

	/**
	 *
	 * @param archer
	 * @return
	 */
	public int getShortestDistanceArcher(DummyUnit archer) {
		int best = Integer.MAX_VALUE;

		int temp;
		for (DummyUnit footman : footmen) {
			temp = getDistance(archer, footman);

			if (Math.abs(temp) < Math.abs(best)) {
				best = temp;
			}
		}
		return best;
	}

	/**
	 *
	 * @param footman
	 * @return
	 */
	//	public boolean isShortestDistanceFootman(DummyUnit footman) {
	//		int best = Integer.MAX_VALUE;
	//
	//		int temp;
	//		for (DummyUnit archer : archers) {
	//			XY xy = getBestMove(game., footman.y, archer.x, archer.y);
	//            if(footman.x == xy.x && footman.y == xy.y) {
	//                return true;
	//            }
	//		}
	//		return false;
	//	}
	/**
	 * Computes the taxicab norm dx + dy. In this assignment, the minimum
	 * number of moves to reach a given destination. Unlike Chebychev, accounts
	 * for not being able to move diagonally.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public static int getDistance(DummyUnit from, DummyUnit to) {
		return getDistance(from.xy, to.xy);
	}

	public static int getDistance(XY from, XY to) {
		return Math.abs(to.x - from.x) + Math.abs(to.y - from.y);
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

			for (DummyUnit enemy : targets) {
				if (getDistance(unit, enemy) <= unit.view.getTemplateView().getRange()) {
					unitActions.add(this.createAttackAction(unit, enemy));
				}
			}

			for (Direction direction : Direction.values()) {
				// Ignore diagonal directional movement
				if (direction == Direction.SOUTHEAST ||
						direction == Direction.SOUTHWEST ||
						direction == Direction.NORTHEAST ||
						direction == Direction.NORTHWEST) {
					continue;
				}
				int newX = unit.xy.x + direction.xComponent();
				int newY = unit.xy.y + direction.yComponent();
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

			if (maxAgent) {
				state.footmen = newControlledUnits;
				state.archers = newTargetUnits;
			} else {
				state.footmen = newTargetUnits;
				state.archers = newControlledUnits;
			}

			Map<Integer, Action> map = new HashMap<>();

			for (Pair<DummyUnit, Action> pair : gameStateAction) {
				Action action = pair.b;
				if (action instanceof TargetedAction) {
					Iterator<DummyUnit> targetIter = newTargetUnits.iterator();
					while (targetIter.hasNext()) {
						DummyUnit attackTarget = targetIter.next();
						if(attackTarget.id == ((TargetedAction) action).getTargetId()) {
							attackTarget.hp -= pair.a.view.getTemplateView().getBasicAttack();
							if (attackTarget.hp < 0) {
								System.out.println("Applying attack!");
								pair.a.target = null;
								targetIter.remove();
							}
							break;
						}
					}

					newControlledUnits.add(new DummyUnit(pair.a));
				} else if (action instanceof DirectedAction){
					newControlledUnits.add(new DummyUnit(pair.a, ((DirectedAction) action).getDirection()));
				}

				map.put(pair.a.id, pair.b);
			}

			state.getUtility();

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
			if(enemy.xy.x == newX && enemy.xy.y == newY) {
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

	public int[] getResources() {
		int[] trees = new int[game.getAllResourceIds().size()*2];
		int i = 0;
		for (ResourceNode.ResourceView view: game.getAllResourceNodes()) {
			trees[i] = view.getXPosition();
			trees[i+1] = view.getYPosition();
			i += 2;
		}
		return trees;
	}

	public int getMapX() {
		return game.getXExtent();
	}

	public int getMapY() {
		return game.getYExtent();
	}
}
