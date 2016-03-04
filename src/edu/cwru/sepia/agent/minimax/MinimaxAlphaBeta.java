package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent that prunes its game tree via an alpha-beta search.
 * 
 * @author Adam Johnston	amj69
 * @author Eric Luan		efl11
 */
public class MinimaxAlphaBeta extends Agent {
	private static final long serialVersionUID = 3799985942000030012L;
	
	private static Agent myAgent;
	
	private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
        
        myAgent = this;
    }
    
    public static Agent getPlayerAgent() {
    	return myAgent;
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        
        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {}

    @Override
    public void savePlayerData(OutputStream os) {}

    @Override
    public void loadPlayerData(InputStream is) {}

    /**
     * Performs an alpha-beta search on the game tree for a given state and
     * depth. Very straightforward implementation that returns the eventual
     * leaf utility and the first action needed to reach that leaf.
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {
    	if (depth == 0) {
    		return node;
    	}
    	
    	GameStateChild temp, best = null;
    	
    	List<GameStateChild> sortedChildren;
    	
    	if ((depth + numPlys) % 2 == 0) {
    		//maximizing player
    		
    		sortedChildren = orderChildrenWithHeuristics(node.state.getChildren(), false);
    		
    		double v = Double.NEGATIVE_INFINITY;
    		
    		for (GameStateChild child : sortedChildren) {
    			temp = alphaBetaSearch(child, depth - 1, alpha, beta);
    			
    			if (v < temp.state.getUtility()) {
    				v = temp.state.getUtility();
    				best = temp;
    			}
    			alpha = Math.max(v, alpha);
    			
    			if (beta <= alpha) {
    				break;
    			}
    		}
    	} else {
    		//minimizing player
    		
    		double v = Double.POSITIVE_INFINITY;
    		
    		sortedChildren = orderChildrenWithHeuristics(node.state.getChildren(), true);
    		
    		for (GameStateChild child : sortedChildren) {
    			temp = alphaBetaSearch(child, depth - 1, alpha, beta);
    			
    			if (v > temp.state.getUtility()) {
    				v = temp.state.getUtility();
    				best = temp;
    			}
    			beta = Math.min(v, alpha);
    			
    			if (beta <= alpha) {
    				break;
    			}
    		}
    	}
    	
    	//inherit utility, but make sure we keep the correct next action
    	if (best == null) { //handles no-child case
    		return node;
    	}
    	
    	node.state.utility = best.state.utility; //inherit utility to not lose action map
    	if (node.action != null) {
    		return node;
    	} else {
    		return best; //for the first call to this wherein we have no action :(
    	}
    }

    /**
     * Sorts a list of child states according to their precomputed utility.
     * Takes an additional parameter to sort in ascending or descending order
     * to avoid a reverse list call in the minimizing player.
     * 
     * Does not take advantage of additional heuristics, i.e. the killer
     * heuristic for a-b search.
     * 
     * Stable assuming the underlying sort implementation on streams is stable.
     * 
     * @param children
     * @param ascending
     * @return
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children, boolean ascending) {
        return children.stream().sorted((a, b) -> {
        	return (ascending ? 1 : -1) * Double.compare(a.state.getUtility(), b.state.getUtility());
        }).collect(Collectors.toList()); // Fancy Java 8
    }


}
