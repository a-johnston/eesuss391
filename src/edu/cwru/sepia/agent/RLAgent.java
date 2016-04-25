package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.history.History;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class RLAgent extends Agent {

	private static final long serialVersionUID = 1L;

	/**
	 * These variables and constants are generally useful for house keeping
	 */
	private static final int TURNS_BETWEEN_TESTING = 10;
	private static final int NUMBER_OF_TEST_RUNS = 5;
	private int episodeNumber = 0;
	private int totalEpisodes = 0;
	private int testsCompleted = 0;
	private boolean testingEpisode = false;
	private List<Double> averageRewards = new ArrayList<>();
	private double cumulativeReward = 0;

	/**
	 * Cached values from the last execution to use this turn when updating the weights function
	 */
	private Map<Integer, Double> lastReward = new HashMap<>();
	private Map<Integer, double[]> lastFeatureVector = new HashMap<>();

	/**
	 * Set in the constructor. Defines how many learning episodes your agent should run for.
	 * When starting an episode. If the count is greater than this value print a message
	 * and call sys.exit(0)
	 */
	public final int numEpisodes;
	private int wins = 0;
	private int losses = 0;

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
	 * Current features:
	 * Attacking the closest unit
	 * Attacking the lowest health unit
	 */
	public static final int NUM_FEATURES = 6;

	public static final int CLOSEST_ENEMY_FEATURE = 0;
	public static final int WEAKEST_ENEMY_FEATURE = 1;
	public static final int FRIENDLY_UNIT_HEALTH_FEATURE = 2;
	public static final int NUMBER_OF_ENEMIES_FEATURE = 3;
	public static final int NUMBER_OF_FRIENDS_FEATURE = 4;
	public static final int FIRST_ENEMY_FEATURE = 5;


	public final Random random = new Random();

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
	private final double TURN_PENALTY = 0.1; // TODO: Why is this not used?

	public RLAgent(int playernum, String[] args) {
		super(playernum);

		if (args.length >= 1) {
			numEpisodes = Integer.parseInt(args[0]);
			System.out.println("Running " + numEpisodes + " episodes.");
		} else {
			numEpisodes = 10;
			System.err.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
		}

		boolean loadWeights = false;
		if (args.length >= 2) {
			loadWeights = Boolean.parseBoolean(args[1]);
		} else {
			System.err.println("Warning! Load weights argument not specified. Defaulting to not loading.");
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

		cumulativeReward = 0.0;

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
		double stateReward = 0.0;
		boolean unitDidDie = updateUnitLists(historyView, stateView); // Important to check this after we calculate the state reward.
		Map<Integer, Double> nextLastReward = new HashMap<>();

		// Calculate the reward of this state.
		for (int friendlyUnit : myFootmen) {
			stateReward += calculateReward(stateView, historyView, friendlyUnit);
			nextLastReward.put(friendlyUnit, calculateReward(stateView, historyView, friendlyUnit));
		}

		// Update the weights for this turn if the units received feedback and we're not testing.
		double[] oldWeights = weights.clone();
		for(int friendlyUnit: myFootmen) {
			if (stateView.getTurnNumber() != 0 && !testingEpisode && actionCompleted(historyView, stateView)) {
				weights = updateWeights(oldWeights, lastFeatureVector.get(friendlyUnit), lastReward.get(friendlyUnit), stateView, historyView, friendlyUnit);
			}
		}

		lastReward = nextLastReward;

		// Issue new commands if a significant event occurred
		// TODO: Probably add more feature vectors here.
		if(stateView.getTurnNumber() == 0 || unitDidDie || actionCompleted(historyView, stateView) || friendlyDamageTaken(historyView, stateView)) {
			// Update the weights of our feature vectors
			for (int friendlyUnit : myFootmen) {
				int enemyTarget = selectAction(stateView, historyView, friendlyUnit);
				lastFeatureVector.put(friendlyUnit, calculateFeatureVector(stateView, historyView, friendlyUnit, enemyTarget));
				actionMap.put(friendlyUnit, Action.createCompoundAttack(friendlyUnit, enemyTarget));
			}
		}

		cumulativeReward += stateReward;

		return actionMap;
	}


	/**
	 *
	 * @param historyView
	 * @param stateView
	 * @return true if a unit died, else false.
	 */
	private boolean updateUnitLists(History.HistoryView historyView, State.StateView stateView) {
		myFootmen = myFootmen.stream().filter(unit -> stateView.getUnit(unit) != null).collect(Collectors.toList());
		enemyFootmen = enemyFootmen.stream().filter(unit -> stateView.getUnit(unit) != null).collect(Collectors.toList());


		return historyView.getDeathLogs(stateView.getTurnNumber() - 1).size() > 0;
	}

	private boolean friendlyDamageTaken(History.HistoryView historyView, State.StateView stateView) {
		// Check the damage logs to figure out if anyone died/was injured
		for (DamageLog log : historyView.getDamageLogs(stateView.getTurnNumber() - 1)) {
			if (myFootmen.contains(log.getDefenderID())) {
				return true;
			}
		}

		return false;
	}

	private boolean actionCompleted(History.HistoryView historyView, State.StateView stateView) {
		return historyView.getCommandFeedback(this.getPlayerNumber(), stateView.getTurnNumber() -1).size() > 0;
	}

	/**
	 * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
	 * finished a set of test episodes you will call out testEpisode.
	 *
	 * It is also a good idea to save your weights with the saveWeights function.
	 */
	@Override
	public void terminalStep(StateView stateView, HistoryView historyView) {
		totalEpisodes++;

		if(episodeNumber % TURNS_BETWEEN_TESTING == 0 && NUMBER_OF_TEST_RUNS > testsCompleted) {
			// Do testing episode
			testsCompleted ++;
			testingEpisode = true;
			int arrayIndex = episodeNumber/TURNS_BETWEEN_TESTING;
			double currentValue;
			
			try {
				currentValue = averageRewards.get(arrayIndex);
			} catch(Exception e) {
				currentValue = 0.0;
				averageRewards.add(arrayIndex, currentValue);
			}
			averageRewards.set(arrayIndex, currentValue + cumulativeReward * .2);
		} else {
			testingEpisode = false;
			// Do learning episode
			testsCompleted = 0;
			episodeNumber++;
		}

		if(stateView.getUnits(ENEMY_PLAYERNUM).size() == 0) {
			wins ++;
		} else {
			losses ++;
		}

		// Save your weights
		saveWeights(weights);
		if (totalEpisodes >= 1000) {
			System.out.println("Wins: ");
			System.out.println(wins);
			System.out.println("Losses: ");
			System.out.println(losses);
			outputCSV(averageRewards);
			printTestData(averageRewards);
			System.exit(0);
		}
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
		// TODO : Why is there a totalRewards input to this function?

		double[] newWeights = new double[oldWeights.length];
		for (int i = 0; i < oldWeights.length; i++) {
			newWeights[i] = oldWeights[i];
			newWeights[i] += learningRate * oldFeatures[i] * totalReward; // Reward addition
			newWeights[i] += learningRate * oldFeatures[i] * gamma * getBestQValue(stateView, historyView, footmanId); // Best next reward addition
			newWeights[i] -= learningRate * oldFeatures[i] * dotProduct(oldFeatures, oldWeights); // Current value
		}

		return newWeights;
	}

	public double dotProduct(double[] array1, double[] array2) {
		double sum = 0.0;
		for(int i = 0; i < array1.length; i ++) {
			sum += array1[i] * array2[i];
		}
		return sum;
	}

	public double getBestQValue(StateView stateView, HistoryView historyView, int footmanId) {
		int bestEnemy = selectBestEnemy(stateView, historyView, footmanId);
		return calcQValue(stateView, historyView, footmanId, bestEnemy);
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
		// Returns a random enemy to attack with probability epsilon
		if (random.nextDouble() < epsilon) {
			return enemyFootmen.get((int) random.nextDouble() * enemyFootmen.size());
		}

		// Otherwise returns the enemy that maximizes the Q value
		return selectBestEnemy(stateView, historyView, attackerId);
	}

	public int selectBestEnemy(StateView stateView, HistoryView historyView, int attackerId) {
		int bestEnemy = -1;
		double bestQ = Double.NEGATIVE_INFINITY;

		for (int enemy : enemyFootmen) {
			double temp = calcQValue(stateView, historyView, attackerId, enemy);
			if (temp > bestQ) {
				bestQ = temp;
				bestEnemy = enemy;
			}
		}
		return bestEnemy;
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
	 * The reward function should be defined as follows:
	 * each action costs -.1
	 * +d for each damage the unit dealt
	 * -d for each damage the unit took
	 * +100 if an enemy dies
	 * -100 if a friendly dies
	 * @param stateView The current state of the game.
	 * @param historyView History of the episode up until this turn.
	 * @param footmanId The footman ID you are looking for the reward from.
	 * @return The current reward
	 */
	public double calculateReward(StateView stateView, HistoryView historyView, int footmanId) {

		double reward = TURN_PENALTY;
		if (stateView.getUnit(footmanId) == null) {
			reward -= UNIT_BONUS;
		}

		// Check the damage logs to figure out if anyone died/was injured
		for (DamageLog log : historyView.getDamageLogs(stateView.getTurnNumber() - 1)) {
			if (log.getAttackerID() == footmanId) {
				reward += log.getDamage() * HP_BONUS;

				if (stateView.getUnit(log.getDefenderID()) == null) {
					reward += UNIT_BONUS;
				}
			} else if (log.getDefenderID() == footmanId) {
				reward -= log.getDamage() * HP_BONUS;
			}
		}
		return reward;
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
		double[] features = calculateFeatureVector(stateView, historyView, attackerId, defenderId);
		return dotProduct(weights, features);
	}

	/**
	 * Given a state and action calculate your features here. Please include a comment explaining what features
	 * you chose and why you chose them.
	 *
	 * Features Included:
	 * 1. Attacking the closest enemy - generally speaking the best unit to attack is probably the closest one
	 * 2.
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
		if (defenderId == getClosestEnemy(stateView, attackerId)) {
			features[CLOSEST_ENEMY_FEATURE] = 1;
		} else {
			features[CLOSEST_ENEMY_FEATURE] = 0;
		}

		if (defenderId == getWeakestEnemy(stateView)) {
			features[WEAKEST_ENEMY_FEATURE] = 1;
		} else {
			features[WEAKEST_ENEMY_FEATURE] = 0;
		}

		features[FRIENDLY_UNIT_HEALTH_FEATURE] = stateView.getUnit(attackerId).getHP();
		features[NUMBER_OF_ENEMIES_FEATURE] = enemyFootmen.size();
		features[NUMBER_OF_FRIENDS_FEATURE] = myFootmen.size();
		features[FIRST_ENEMY_FEATURE] = (enemyFootmen == null || defenderId == enemyFootmen.get(0)) ? 1.0 : 0.0;
		return features;
	}

	public int getClosestEnemy(StateView stateView, int attackerId) {
		int id = enemyFootmen.get(0);
		int distance = Integer.MAX_VALUE;
		UnitView attacker = stateView.getUnit(attackerId);
		for (int enemy: enemyFootmen) {
			if (distance > getDistance(attacker, stateView.getUnit(enemy))) {
				id = enemy;
				distance = getDistance(attacker, stateView.getUnit(enemy));
			}
		}
		return id;
	}

	public int getWeakestEnemy(StateView stateView) {
		int id = enemyFootmen.get(0);
		int health = Integer.MAX_VALUE;
		for (int enemy: enemyFootmen) {
			if (health > stateView.getUnit(enemy).getHP()) {
				id = enemy;
				health = stateView.getUnit(enemy).getHP();
			}
		}
		return id;
	}

	public void outputCSV(List<Double> averageRewards) {
		File path = new File("agent_weights/data.txt");
		// create the directories if they do not already exist
		path.getAbsoluteFile().getParentFile().mkdirs();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, false))) {
			for (int i = 0; i < averageRewards.size(); i++) {
				writer.write(String.format("%d, %f\n", i*10, averageRewards.get(i)));
			}
			writer.flush();
		} catch(IOException ex) {
			System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
		}
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
