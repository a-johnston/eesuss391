package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.event.ListSelectionEvent;

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
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
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
    	sortedChildren = orderChildrenWithHeuristics(node.state.getChildren());
    	
    	if ((depth + numPlys) % 2 == 0) {
    		//maximizing player
    		
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
    		
    		Collections.reverse(sortedChildren);
    		
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
    	
        return best;
    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
        return children.stream().sorted((a, b) -> {
        	return Double.compare(b.state.getUtility(), a.state.getUtility());
        }).collect(Collectors.toList());
    }
}