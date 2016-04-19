package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.history.History;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RLAgent extends Agent {

	private static final long serialVersionUID = 1L;

	/**
     * Set in the constructor. Defines how many learning episodes your agent should run for.
     * When starting an episode. If the count is greater than this value print a message
     * and call sys.exit(0)
     */
    public final int numEpisodes;

    /**
     * List of your footmen and your enemies footmen
     */
    private List<Integer> myFootmen;
    private List<Integer> enemyFootmen;

    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;

    /**
     * Set this to whatever size your feature vector is.
     */
    public static final int NUM_FEATURES = 2;
    
    public static final int CLOSEST_ENEMY_FEATURE = 0;
    public static final int WEAKEST_ENEMY_FEATURE = 1;

    /** Use this random number generator for your epsilon exploration. When you submit we will
     * change this seed so make sure that your agent works for more than the default seed.
     */
    public final Random random = new Random(12345);

    /**
     * Your Q-function weights.
     */
    public double[] weights;

    /**
     * These variables are set for you according to the assignment definition. You can change them,
     * but it is not recommended. If you do change them please let us know and explain your reasoning for
     * changing them.
     */
    public final double gamma = 0.9;			// discount factor
    public final double learningRate = .0001;
    public final double epsilon = .02;
    
    private final double UNIT_BONUS = 100.0;
    private final double HP_BONUS = 1.0;
    private final double TURN_PENALTY = 0.1;
    
    private double sumReward = 0.0; // used to store the summed reward of the current/previous step

    public RLAgent(int playernum, String[] args) {
        super(playernum);

        if (args.length >= 1) {
            numEpisodes = Integer.parseInt(args[0]);
            System.out.println("Running " + numEpisodes + " episodes.");
        } else {
            numEpisodes = 10;
            System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
        }

        boolean loadWeights = false;
        if (args.length >= 2) {
            loadWeights = Boolean.parseBoolean(args[1]);
        } else {
            System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        }

        if (loadWeights) {
            weights = loadWeights();
        } else {
            // initialize weights to random values between -1 and 1
            weights = new double[NUM_FEATURES];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = random.nextDouble() * 2 - 1;
            }
        }
    }

    /**
     * We've implemented some setup code for your convenience. Change what you need to.
     */
    @Override
    public Map<Integer, Action> initialStep(StateView stateView, HistoryView historyView) {

        // You will need to add code to check if you are in a testing or learning episode

        // Find all of your unit IDs
        myFootmen = stateView.getUnits(playernum).stream()
        				.filter(unit -> unit.getTemplateView().getName().toLowerCase().equals("footman"))
        				.map(unit -> unit.getID())
        				.collect(Collectors.toList());

        // Find all of the enemy unit IDs
        enemyFootmen = stateView.getUnits(ENEMY_PLAYERNUM).stream()
				.filter(unit -> unit.getTemplateView().getName().toLowerCase().equals("footman"))
				.map(unit -> unit.getID())
				.collect(Collectors.toList());

        return middleStep(stateView, historyView);
    }

    /**
     * You will need to calculate the reward at each step and update your totals. You will also need to
     * check if an event has occurred. If it has then you will need to update your weights and select a new action.
     *
     * If you are using the footmen vectors you will also need to remove killed units. To do so use the historyView
     * to get a DeathLog. Each DeathLog tells you which player's unit died and the unit ID of the dead unit. To get
     * the deaths from the last turn do something similar to the following snippet. Please be aware that on the first
     * turn you should not call this as you will get nothing back.
     *
     * for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
     *     System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
     * }
     *
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an even whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     *
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     *
     * @return New actions to execute or nothing if an event has not occurred.
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        Map<Integer, Action> actionMap = new HashMap<>();
        if (stateView.getTurnNumber() == 1 || unitDied(historyView, stateView) || actionCompleted(historyView, stateView)) {
            // Do some fun reevaluation stuff

        }
        return actionMap;
    }


    private boolean unitDied(History.HistoryView historyView, State.StateView stateView) {
        return historyView.getDeathLogs(stateView.getTurnNumber() - 1).size() > 0;
    }

    private boolean actionCompleted(History.HistoryView historyView, State.StateView stateView) {
        for(Entry<Integer, ActionResult> result : historyView.getCommandFeedback(0, stateView.getTurnNumber() - 1).entrySet()) {
        	// TODO : do something with result
        }
        
        return false;
    }

    /**
     * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
     * finished a set of test episodes you will call out testEpisode.
     *
     * It is also a good idea to save your weights with the saveWeights function.
     */
    @Override
    public void terminalStep(StateView stateView, HistoryView historyView) {

        // MAKE SURE YOU CALL printTestData after you finish a test episode.

        // Save your weights
        saveWeights(weights);

    }

    /**
     * Calculate the updated weights for this agent. 
     * @param oldWeights Weights prior to update
     * @param oldFeatures Features from (s,a)
     * @param totalReward Cumulative discounted reward for this footman.
     * @param stateView Current state of the game.
     * @param historyView History of the game up until this point
     * @param footmanId The footman we are updating the weights for
     * @return The updated weight vector.
     */
    public double[] updateWeights(double[] oldWeights, double[] oldFeatures, double totalReward, StateView stateView, HistoryView historyView, int footmanId) {
        return null;
    }

    /**
     * Given a footman and the current state and history of the game select the enemy that this unit should
     * attack. This is where you would do the epsilon-greedy action selection.
     *
     * @param stateView Current state of the game
     * @param historyView The entire history of this episode
     * @param attackerId The footman that will be attacking
     * @return The enemy footman ID this unit should attack
     */
    public int selectAction(StateView stateView, HistoryView historyView, int attackerId) {
        return -1;
    }

    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
    public double calculateReward(StateView stateView, HistoryView historyView, int footmanId) {
    	double newSumReward = 0.0;
    	
    	// TODO : confirm that this is correct
    	
    	for (UnitView unit : getUnitViews(stateView, myFootmen)) {
    		newSumReward += UNIT_BONUS + HP_BONUS * unit.getHP();
    	}
    	
    	for (UnitView unit : getUnitViews(stateView, enemyFootmen)) {
    		newSumReward -= UNIT_BONUS + HP_BONUS * unit.getHP();
    	}
    	
    	double delta = newSumReward - sumReward;
    	sumReward = newSumReward;
    	
        return TURN_PENALTY + delta;
    }
    
    /**
     * Returns the respective UnitViews for a given side for a given stateView
     * @param stateView
     * @param ourUnits if true, returns player units, else enemy units
     * @return
     */
    private List<UnitView> getUnitViews(StateView stateView, List<Integer> unitIds) {
    	return unitIds.stream()
    			.map(id -> stateView.getUnit(id))
    			.filter(unit -> unit != null)
    			.collect(Collectors.toList());
    }
    
    /**
     * Calculates a normalized weight for a given enemy unit based on distance
     * Returns 1.0 for the closest unit and 0.0 for the farthest unit,
     * linearly decreasing as distance increases.
     * 
     * @param us	Our unit
     * @param them	Enemy unit
     * @param all	All enemy units
     * @return		Normalized weight for distance
     */
    private double getNormalizedDistance(UnitView us, UnitView them, List<UnitView> all) {
    	// TODO : does default of MAX_VALUE and 0 make sense here? only hit when units is empty, so should be ok?
    	double closest  = all.stream().mapToInt(u -> getDistance(us, u)).min().orElse(Integer.MAX_VALUE);
    	double farthest = all.stream().mapToInt(u -> getDistance(us, u)).max().orElse(0);
    	
    	double dist = getDistance(us, them);
    	
    	return 1.0 - ((dist - closest) / (farthest - closest));
    }
    
    /**
     * Calculates a normalized weight for a given unit based on HP. The weakest
     * enemy is assigned a weight of 1.0 and the strongest a weight of 0.0,
     * with all in between weighted linearly between them.
     * 
     * @param target	Target to generate weight for
     * @param units		All targets to consider for normalization
     * @return 			Normalized weight for HP
     */
    private double getNormalizedWeakness(UnitView target, List<UnitView> units) {
    	// TODO : does default of MAX_VALUE and 0 make sense here? only hit when units is empty, so should be ok?
    	double weakest   = units.stream().mapToInt(UnitView::getHP).min().orElse(Integer.MAX_VALUE);
    	double strongest = units.stream().mapToInt(UnitView::getHP).max().orElse(0);
    	
    	return 1.0 - ((target.getHP() - weakest) / (strongest - weakest));
    }
    
    private int getDistance(UnitView a, UnitView b) {
    	return Math.min(
    			Math.abs(a.getXPosition() - b.getXPosition()),
    			Math.abs(a.getYPosition() - b.getYPosition()));
    }

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
    public double calcQValue(StateView stateView,
                             HistoryView historyView,
                             int attackerId,
                             int defenderId) {
    	
    	double q = 0.0;
    	double[] features = calculateFeatureVector(stateView, historyView, attackerId, defenderId);
    	
    	for (int i = 0; i < features.length; i++) {
    		q += weights[i] * features[i];
    	}
    	
        return q;
    }

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
    public double[] calculateFeatureVector(StateView stateView,
                                           HistoryView historyView,
                                           int attackerId,
                                           int defenderId) {
    	double[] features = new double[NUM_FEATURES];
    	
    	UnitView attacker = stateView.getUnit(attackerId);
    	UnitView target = stateView.getUnit(defenderId);
    	
    	List<UnitView> targets = getUnitViews(stateView, enemyFootmen);
    	
    	// TODO : consider if linear functions are more or less appropriate here
    	features[CLOSEST_ENEMY_FEATURE] = getNormalizedDistance(attacker, target, targets);
    	features[WEAKEST_ENEMY_FEATURE] = getNormalizedWeakness(target, targets);
    	
    	// TODO : more of this
    	
        return features;
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include the output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, false))) {
            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        double[] weights = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            reader.lines().map(line -> Double.parseDouble(line)).collect(Collectors.toList());
            weights = reader.lines().mapToDouble(line -> Double.parseDouble(line)).toArray();
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        
        return weights;
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
