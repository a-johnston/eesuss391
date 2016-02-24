package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {
	private static final long serialVersionUID = 3012228857344935814L;

	class MapLocation implements Comparable<MapLocation>
    {
        public final int x, y;
        public MapLocation cameFrom;
        public float f, g;

        public MapLocation(int x, int y, MapLocation cameFrom, float f, float g)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.f = f;
            this.g = g;
        }
        
        public MapLocation(MapLocation from, MapLocation cameFrom, float f, float g) {
        	this.x = from.x;
        	this.y = from.y;
        	this.cameFrom = cameFrom;
        	this.f = f;
            this.g = g;
        }
        
        /**
         * Checks to see if this map location and the other map location share
         * their coordinates
         * @param other
         * @return whether two maps locations share same position
         */
        public boolean samePosition(MapLocation other) {
        	return samePosition(other.x, other.y);
        }
        
        /**
         * Checks to see if this map location is at the given coordinates.
         * @param x
         * @param y
         * @return whether coordinates are equal in Z^2
         */
        public boolean samePosition(int x, int y) {
        	return this.x == x && this.y == y;
        }

        /**
         * Compares this map location to another map location by cost.
         * @param o other map location
         * @return comparison of float cost values
         */
		@Override
		public int compareTo(MapLocation o) {
			return Float.compare(this.f, o.f);
		}
		
		/**
		 * Generally bad practice but makes the code nicer. For our
		 * considerations, two map locations are the same if they share the
		 * same coordinates, not necessarily their origin or cost.
		 * @param obj object to compare to
		 * @return whether the objects are equal
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MapLocation) {
				return samePosition((MapLocation) obj);
			}
			return super.equals(obj);
		}
		
//		@Override
//		public int hashCode() {
//			return x * 100 + y;
//		}
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs
    
    private int intimidate;

    public AstarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	if(enemyFootmanID == -1) {
            return false;
        }
    	
    	Unit.UnitView enemy = state.getUnit(enemyFootmanID);
    	
    	for (MapLocation future : currentPath) {
    		if (future.x == enemy.getXPosition() && future.y == enemy.getYPosition()) {
    			if (intimidate > 0) {
    				/*
    				 * Added to help deter replanning when something moves over our path briefly
    				 */
    				System.out.println("Trying to intimidate enemy");
    				intimidate--;
    				return false;
    			} else {
    				return true;
    			}
    		}
    	}
    	
    	intimidate = 6; // reset intimidation factor immediately
    	// I am guilty of fine-tuning the magic number above -- a heuristic for the game, not for A* itself
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {	
    	// build map as an array for speed and consistent lookup
    	// (also because I'm using a hash set)
    	
    	MapLocation[][] map = new MapLocation[xExtent][yExtent];
    	
    	for (int i = 0; i < xExtent; i++) {
    		for (int j = 0; j < yExtent; j++) {
    			map[i][j] = new MapLocation(i, j, null, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    		}
    	}
    	
    	for (MapLocation l : resourceLocations) {
    		map[l.x][l.y] = null;
    	}
    	
    	if (enemyFootmanLoc != null) {
    		map[enemyFootmanLoc.x][enemyFootmanLoc.y] = null;
    	}
    	
    	// keep start and goal instances just for fun
    	
    	map[start.x][start.y] = start;
    	map[goal.x][goal.y] = goal;
    	
    	start.g = 0;
    	start.f = estimateCost(start, goal);
    	
        Queue<MapLocation> frontier = new PriorityQueue<>(); //sorts based on f score
        Set<MapLocation> explored = new HashSet<>();
        
        frontier.add(new MapLocation(start, null, estimateCost(start, goal), 0f));
        
        MapLocation current = null;
        while (!frontier.isEmpty()) { //fairly pure A*
        	current = frontier.remove();
        	System.out.println("Current: " + current.x + " : " + current.y);
        	explored.add(current);
        	
        	if (current == goal) {
        		return rebuildPath(current.cameFrom); //don't move into town hall
        	}
        	
        	for (MapLocation next : getNeighbors(current, map)) {
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
        		next.f = new_g + estimateCost(next, goal);
        	}
        	
        	System.out.println(frontier);
        }
        
        System.out.println("No available path.");
        
        System.exit(0);
        
        return new Stack<>(); //used for looking at maps and as a formality
    }
    
    /**
     * Returns the Chebyshev estimate of the distance. In terms of an open
     * movement grid, this becomes the minimum steps required to reach goal
     * from location.
     * @param location Location on the map to estimate cost from
     * @param goal Single goal to target from location
     * @return estimated cost. Lower is better.
     */
    private float estimateCost(MapLocation location, MapLocation goal) {
    	return Math.min(Math.abs(location.x - goal.x), Math.abs(location.y - goal.y));
    }
    
    /**
     * Rebuilds the A* path based on the "cameFrom" parameter of a node.
     * @param node Final node to build the path from
     * @return Reconstructed path as the program wants it
     */
    private Stack<MapLocation> rebuildPath(MapLocation node) {
    	Stack<MapLocation> path = new Stack<>();
    	
    	while (node != null) {
    		path.add(node);
    		node = node.cameFrom;
    	}
    	
    	return path;
    }
    
    /**
     * Gets the neighbors of the current location taking into account the size
     * of the map and any obstacles. The returned neighbors have their values
     * set according to current and the estimated cost. The location of the
     * enemy has a slight negative effect on nearby nodes.
     * @param current Current location to find neighbors of
     * @param width width of the map
     * @param height height of the map
     * @param goal location of goal on map
     * @param enemy location of the enemy on the map
     * @param obstacles all the obstacles on the map with cost infinity
     * @return list of valid locations to try out
     */
    private List<MapLocation> getNeighbors(MapLocation current, MapLocation[][] map) {
    	List<MapLocation> neighbors = new ArrayList<>(8);
    	
    	//enumerate possible positions
    	for (int i = -1; i < 2; i++) {
    		if (current.x + i < 0 || current.x + i >= map.length) {
    			continue;
    		}
    		
    		for (int j = -1; j < 2; j++) {
    			if (current.y + j < 0 || current.y + j >= map[0].length) {
        			continue;
        		}
    			
    			if (i == 0 && j == 0) { //skip over position equivalent to current
    				continue;
    			}
    			
    			if (map[current.x + i][current.y + j] != null) {
    				neighbors.add(map[current.x + i][current.y + j]);
    			}
    		}
    	}
    	
    	return neighbors;
    }

    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
