import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Collections;

/*
Travail Réalisé par Abdelouhab Yacine / Lemaitre Victor / Mohamed Amine /Mohamed Zouad
*/

/**
 * Class used to model the set of belief states already visited and to keep track of their values (in order to avoid visiting multiple times the same states)
 */
class ExploredSet{
	TreeMap<BeliefState, Float> exploredSet;

	/**
	 * construct an empty set
	 */
	public ExploredSet() {
		this.exploredSet = new TreeMap<BeliefState, Float>();
	}

	/**
	 * Search if a given state belongs to the explored set and returns its values if that is the case
	 * @param state the state for which the search takes place
	 * @return the value of the state if it belongs to the set, and null otherwise
	 */
	public Float get(BeliefState state) {
		Entry<BeliefState, Float> entry = this.exploredSet.ceilingEntry(state);
		if(entry == null || state.compareTo(entry.getKey()) != 0) {
			return null;
		}
		return entry.getValue() * state.probaSum() / entry.getKey().probaSum();
	}

	/**
	 * Put a belief state and its corresponding value into the set
	 * @param beliefState the belief state to be added
	 * @param value the
	 */
	public void put(BeliefState beliefState, float value) {
		this.exploredSet.put(beliefState, value);
	}
}

/**
 * Class used to store all possible results of performing an action at a given belief state
 */
class Results implements Iterable<BeliefState>{
	TreeMap<String, BeliefState> results;

	public Results(){
		this.results = new TreeMap<String, BeliefState>();
	}

	/**
	 * Return the belief state of the result that correspond to a given percept
	 * @param percept String that describe what is visible on the board for player 2
	 * @return belief state corresponding percept, or null if such a percept is not possible
	 */
	public BeliefState get(String percept) {
		return this.results.get(percept);
	}

	public void put(String s, BeliefState state) {
		this.results.put(s, state);
	}

	public Iterator<BeliefState> iterator(){
		return this.results.values().iterator();
	}
}

/**
 * Class used to represent a belief state i.e., a set of possible states the agent may be in
 */
class BeliefState implements Comparable<BeliefState>, Iterable<GameState>{
	private byte[] isVisible;

	private TreeSet<GameState> beliefState;

	private int played;

	public BeliefState() {
		this.beliefState = new TreeSet<GameState>();
		this.isVisible = new byte[6];
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = Byte.MIN_VALUE;
		}
		this.played = 0;
	}

	public BeliefState(byte[] isVisible, int played) {
		this();
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = isVisible[i];
		}
		this.played = played;
	}

	public void setStates(BeliefState beliefState) {
		this.beliefState = beliefState.beliefState;
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = beliefState.isVisible[i];
		}
		this.played = beliefState.played;
	}

	public boolean contains(GameState state) {
		return this.beliefState.contains(state);
	}

	/**
	 * returns the number of states in the belief state
	 * @return number of state
	 */
	public int size() {
		return this.beliefState.size();
	}

	public void add(GameState state) {
		if(!this.beliefState.contains(state)) {
			this.beliefState.add(state);
		}
		else {
			GameState copy = this.beliefState.floor(state);
			copy.addProba(state.proba());
		}
	}
	public void removeGameState(GameState state) {
		this.beliefState.remove(state);
	}

	/**
	 * Compute the possible results from a given believe state, after the opponent perform an action. This function souhd be used only when this is the turn of the opponent.
	 * @return an objet of class result containing all possible result of an action performed by the opponent if this is the turn of the opponent, and null otherwise.
	 */
	public Results predict(){
		if(this.turn()) {
			Results tmstates = new Results();
			for(GameState state: this.beliefState) {
				RandomSelector rs = new RandomSelector();
				ArrayList<Integer> listColumn = new ArrayList<Integer>();
				ArrayList<Integer> listGameOver = new ArrayList<Integer>();
				int minGameOver = Integer.MAX_VALUE;
				for(int column = 0; column < 7; column++) {
					if(!state.isFull(column)) {
						GameState copy = state.copy();
						copy.putPiece(column);
						if(copy.isGameOver()) {
							listColumn.clear();
							listColumn.add(column);
							rs = new RandomSelector();
							rs.add(1);
							break;
						}
						int nbrGameOver = 0;
						for(int i = 0; i < 7; i++) {
							if(!copy.isFull(i)) {
								GameState copycopy = copy.copy();
								copycopy.putPiece(i);
								if(copycopy.isGameOver()) {
									nbrGameOver++;
								}
							}
						}
						if(nbrGameOver == 0) {
							rs.add(ProbabilisticOpponentAI.heuristicValue(state, column));
							listColumn.add(column);
						}
						else {
							if(minGameOver > nbrGameOver) {
								minGameOver = nbrGameOver;
								listGameOver.clear();
								listGameOver.add(column);
							}
							else {
								if(minGameOver == nbrGameOver) {
									listGameOver.add(column);
								}
							}
						}
					}
				}
				int index = 0;
				if(listColumn.isEmpty()) {
					for(int column: listGameOver) {
						listColumn.add(column);
						rs.add(1);
					}
				}
				for(int column: listColumn) {
					GameState copy = state.copy();
					if(!copy.isFull(column)) {
						byte[] tab = new byte[6];
						for(int i = 0; i < 6; i++) {
							tab[i] = this.isVisible[i];
						}
						copy.putPiece(column);
						if(copy.isGameOver()) {
							for(int i = 0; i < 6; i++) {
								for(int j = 0; j < 7; j++) {
									BeliefState.setVisible(i, j, true, tab);
								}
							}
						}
						else {
							boolean isVisible = copy.isGameOver() || copy.isFull(column);
							BeliefState.setVisible(5, column, isVisible, tab);
							for(int row = 4; row > -1; row--) {
								isVisible = isVisible || copy.content(row, column) == 2;
								BeliefState.setVisible(row, column, isVisible, tab);
							}
						}
						String s = "";
						char c = 0;
						for(int i = 0; i < 6; i++) {
							int val = tab[i] + 128;
							s += ((char)(val % 128));
							c += (val / 128) << i;
						}
						s += c;
						copy.multProba(rs.probability(index++));
						BeliefState bs = tmstates.get(s);
						if(bs!= null) {
							bs.add(copy);
						}
						else {
							bs = new BeliefState(tab, this.played + 1);
							bs.add(copy);
							tmstates.put(s, bs);
						}
					}
				}
			}
			return tmstates;
		}
		else {
			return null;
		}
	}

	/**
	 * Perform the action corresponding for the player to play a given column, and return the result of this action for each state of the belief state as a Results
	 * @param column index of the column played
	 * @return object of type Results representing all states resulting from playing the column if this is the turn of the player, and null otherwise
	 */
	public Results putPiecePlayer(int column){
		if(!this.turn()) {
			Results tmstates = new Results();
			for(GameState state: this.beliefState) {
				GameState copy = state.copy();
				byte[] tab = new byte[6];
				for(int i = 0; i < 6; i++) {
					tab[i] = this.isVisible[i];
				}
				copy.putPiece(column);
				if(copy.isGameOver()) {
					for(int i = 0; i < 6; i++) {
						for(int j = 0; j < 7; j++) {
							BeliefState.setVisible(i, j, true, tab);
						}
					}
				}
				else {
					boolean isVisible = copy.isFull(column);
					BeliefState.setVisible(5, column, isVisible, tab);
					for(int row = 4; row > -1; row--) {
						isVisible = isVisible || copy.content(row, column) == 2;
						BeliefState.setVisible(row, column, isVisible, tab);
					}
				}
				String s = "";
				char c = 0;
				for(int i = 0; i < 6; i++) {
					int val = tab[i] + 128;
					s += ((char)(val % 128));
					c += (val / 128) << i;
				}
				s += c;
				BeliefState bs = tmstates.get(s);
				if(bs!= null) {
					bs.add(copy);
				}
				else {
					bs = new BeliefState(tab, this.played + 1);
					bs.add(copy);
					tmstates.put(s, bs);
				}
			}
			return tmstates;
		}
		else {
			return null;
		}

	}

	public static BeliefState filter(Results beliefStates, GameState state) {
		byte tab[] = new byte[6];
		for(int i = 0; i < 6; i++) {
			tab[i] = Byte.MIN_VALUE;
		}
		for(int column = 0; column < 7; column++) {
			boolean isVisible = state.isGameOver() || state.isFull(column);
			BeliefState.setVisible(5, column, isVisible, tab);
			for(int row = 4; row > -1; row--) {
				isVisible = isVisible || (state.content(row, column) == 2);
				BeliefState.setVisible(row, column, isVisible, tab);
			}
		}
		String s = "";
		char c = 0;
		for(int i = 0; i < 6; i++) {
			int val = tab[i] + 128;
			s += ((char)(val % 128));
			c += (val / 128) << i;
		}
		s += c;
		BeliefState beliefState = beliefStates.get(s);
		RandomSelector rs = new RandomSelector();
		for(GameState st: beliefState.beliefState) {
			rs.add(st.proba());
		}
		int i = 0;
		for(GameState st: beliefState.beliefState) {
			st.setProba(rs.probability(i++));
		}
		return beliefState;
	}

	/**
	 * Make a copy of the belief state containing the same states
	 * @return copy of the belief state
	 */
	public BeliefState copy() {
		BeliefState bs = new BeliefState();
		for(GameState state: this.beliefState) {
			bs.add(state.copy());
		}
		for(int i = 0; i < 6; i++) {
			bs.isVisible[i] = this.isVisible[i];
		}
		bs.played = this.played;
		return bs;
	}

	public Iterator<GameState> iterator(){
		return this.beliefState.iterator();
	}

	/**
	 * Return the list of the column where a piece can be played (columns which are not full)
	 * @return
	 */
	public ArrayList<Integer> getMoves(){
		if(!this.isGameOver()) {
			ArrayList<Integer> moves = new ArrayList<Integer>();
			GameState state = this.beliefState.first();
			for(int i = 0; i < 7; i++) {
				if(!state.isFull(i))
					moves.add(i);
			}
			return moves;
		}
		else {
			return new ArrayList<Integer>();
		}
	}

	/**
	 * Provide information about the next player to play
	 * @return true if the next to play is the opponent, and false otherwise
	 */
	public boolean turn() {
		return this.beliefState.first().turn();
	}

	public boolean isVisible(int row, int column) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		return ((this.isVisible[index] + 128) >> pos) % 2 == 1;
	}

	public void setVisible(int row, int column, boolean val) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		int delta = ((val? 1: 0) - (this.isVisible(row, column)? 1: 0)) << pos;
		this.isVisible[index] = (byte) (this.isVisible[index] + delta);
	}

	public static void setVisible(int row, int column, boolean val, byte[] tab) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		int posValue = tab[index] + 128;
		int delta = ((val? 1: 0) - ((posValue >> pos) % 2)) << pos;
		tab[index] = (byte) (posValue + delta - 128);
	}

	/**
	 * Check if the game is over in all state of the belief state. Note that when the game is over, the board is revealed and the environment becomes observable.
	 * @return true if the game is over, and false otherwise
	 */
	public boolean isGameOver() {
		for(GameState state: this.beliefState) {
			if(!state.isGameOver()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if all the games in the belief state are full
	 * @return
	 */
	public boolean isFull() {
		return this.beliefState.first().isFull();
	}


	public void restart() {
		this.beliefState = new TreeSet<GameState>();
		this.isVisible = new byte[6];
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = Byte.MIN_VALUE;
		}
		this.played = 0;
	}

	public String toString() {
		String s = "BeliefState: size = " + this.beliefState.size() + " played = " + this.played + "\n";
		for(int row = 5; row > -1; row--) {
			for(int column = 0; column < 7; column++) {
				s += this.isVisible(row, column)? "1": "0";
			}
			s += "\n";
		}
		for(GameState state:this.beliefState) {
			s += state.toString() + "\n";
		}
		return s;
	}

	public int compareTo(BeliefState bs) {
		if(this.played != bs.played)
			return this.played > bs.played? 1: -1;
		for(int i = 0; i < 6; i++) {
			if(this.isVisible[i] != bs.isVisible[i])
				return this.isVisible[i] > bs.isVisible[i]? 1: -1;
		}
		if(this.beliefState.size() != bs.beliefState.size()) {
			return this.beliefState.size() > bs.beliefState.size()? 1: -1;
		}
		Iterator<GameState> iter = bs.beliefState.iterator();
		for(GameState next: this.beliefState) {
			GameState otherNext = iter.next();
			int comp = next.compareTo(otherNext);
			if(comp != 0)
				return comp;
		}
		iter = bs.beliefState.iterator();
		float sum1 = this.probaSum(), sum2 = bs.probaSum();
		for(GameState next: this.beliefState) {
			GameState otherNext = iter.next();
			if(Math.abs(next.proba() * sum1 - otherNext.proba() * sum2) > 0.001) {
				return next.proba() > otherNext.proba()? 1: -1;
			}
		}
		return 0;
	}

	public float probaSum() {
		float sum = 0;
		for(GameState state: this.beliefState) {
			sum += state.proba();
		}
		return sum;
	}
}

class ExploredSet_2{
	TreeMap<GameState, Double> exploredSet_2;

	/**
	 * construct an empty set
	 */
	public ExploredSet_2() {
		this.exploredSet_2 = new TreeMap<GameState, Double>();
	}

	/**
	 * Search if a given state belongs to the explored set and returns its values if that is the case
 * @param state the state for which the search takes place
 * @return the value of the state if it belongs to the set, and null otherwise
 */
public Double get(GameState state) {
	Entry<GameState, Double> entry = this.exploredSet_2.ceilingEntry(state);
	if(entry == null || state.compareTo(entry.getKey()) != 0) {
		return null;
	}
	return entry.getValue();
}

/**
 * Put a belief state and its corresponding value into the set
 * @param beliefState the belief state to be added
 * @param value the
 */
public void put(GameState g, double value) {
	this.exploredSet_2.put(g, value);
}
}


public class AI{
	/*
 * Global variables used during the execution of our AI algorithm.
 * These variables help track the number of games played, decide the appropriate search depth,
 * and manage the set of previously explored game states.
 *
 * exploredSet -> A collection of previously explored game states. This set allows the AI to avoid redundant calculations
 * by storing and reusing previously evaluated states during the search. The set grows as the AI explores more  belief states
 * at each search depth.
 *
 * nbr_partie -> Tracks the number of games played. This variable helps to determine which depth to use for the AI's search.
 * Initially, we will use depth_max = 2, so the exploredSet will store belief states evaluated with a lookahead of 2 moves.
 * This depth provides the best results for the AI's decision-making algorithm.
 *
 * depth_max -> The maximum search depth used by the AI's algorithm. Initially set to 2, this depth allows the AI to evaluate
 * the beliefstate up to two moves ahead. This choice offers a good balance between decision quality and computational performance.
 *
 * depth_max_bis -> A slightly deeper search with a maximum depth of 3. The algorithm is first run with depth=2,
 * which is the most precise. At this depth, the exploredSet contains belief states evaluated with depth=2.
 * This provides a sufficiently accurate search, so when we increase the depth to 3, the performance is not significantly
 * compromised, and we can still achieve a good level of precision. The increased depth allows the AI to explore
 * further, helping it anticipate more complex game scenarios while maintaining overall decision quality.
 *
 *
 * ExploredSet_2 is essentially the same class as ExploredSet but for GameState
 */


	public static ExploredSet_2 exploredSet_game = new ExploredSet_2();
	public static ExploredSet exploredSet = new ExploredSet();
	private static int nbr_partie = 0;
	private static int depth_max = 2;
	private static int depth_max_bis = 3;




	public AI() {
	}

	/**
     * Determines if there is a winning alignment in the game state.
     * Checks vertical, horizontal, and diagonal (both directions) alignments for 4 consecutive pieces.
     *
     * @param state The current game state.
     * @return -1 if yellow wins, 1 if red wins, 0 if no winner is found.
     */
	public static int fourInARow(GameState state) {

		     int rows = 6;  // The number of rows in the board (6 rows)
		     int cols = 7;  // The number of columns in the board (7 columns)

		     // Checking vertical alignment (column by column)
		     for (int col = 0; col < cols; col++) {
		         int countYellow = 0;  // Counter for yellow pieces
		         int countRed = 0;     // Counter for red pieces

		         // Iterate through each row in the column
		         for (int row = 0; row < rows; row++) {
		             int content = state.content(row, col);  // Get the content of the current cell

		             // If the current cell contains a yellow piece
		             if (content == 1) {
		                 countYellow++;
		                 countRed = 0;  // Reset red counter if yellow is encountered
		             }
		             // If the current cell contains a red piece
		             else if (content == 2) {
		                 countRed++;
		                 countYellow = 0;  // Reset yellow counter if red is encountered
		             }
		             // If the current cell is empty
		             else {
		                 countYellow = 0;
		                 countRed = 0;  // Reset both counters if the cell is empty
		             }

		             // If four yellow pieces are found in a vertical line, yellow wins
		             if (countYellow == 4) return -1;
		             // If four red pieces are found in a vertical line, red wins
		             if (countRed == 4) return 1;
		         }
		     }

		     // Checking horizontal alignment (row by row)
		     for (int row = 0; row < rows; row++) {
		         int countYellow = 0;  // Counter for yellow pieces
		         int countRed = 0;     // Counter for red pieces

		         // Iterate through each column in the row
		         for (int col = 0; col < cols; col++) {
		             int content = state.content(row, col);  // Get the content of the current cell

		             // If the current cell contains a yellow piece
		             if (content == 1) {
		                 countYellow++;
		                 countRed = 0;  // Reset red counter if yellow is encountered
		             }
		             // If the current cell contains a red piece
		             else if (content == 2) {
		                 countRed++;
		                 countYellow = 0;  // Reset yellow counter if red is encountered
		             }
		             // If the current cell is empty
		             else {
		                 countYellow = 0;
		                 countRed = 0;  // Reset both counters if the cell is empty
		             }

		             // If four yellow pieces are found in a horizontal line, yellow wins
		             if (countYellow == 4) return -1;
		             // If four red pieces are found in a horizontal line, red wins
		             if (countRed == 4) return 1;
		         }
		     }

		     // Checking diagonal alignment (\ direction)
		     for (int row = 0; row <= rows - 4; row++) {  // Ensure there's enough space for 4 pieces vertically
		         for (int col = 0; col <= cols - 4; col++) {  // Ensure there's enough space for 4 pieces horizontally
		             int countYellow = 0;  // Counter for yellow pieces
		             int countRed = 0;     // Counter for red pieces

		             // Check diagonal from (row, col) to (row+3, col+3)
		             for (int i = 0; i < 4; i++) {
		                 int content = state.content(row + i, col + i);  // Get the content of the current diagonal cell

		                 // If the current diagonal cell contains a yellow piece
		                 if (content == 1) {
		                     countYellow++;
		                     countRed = 0;  // Reset red counter if yellow is encountered
		                 }
		                 // If the current diagonal cell contains a red piece
		                 else if (content == 2) {
		                     countRed++;
		                     countYellow = 0;  // Reset yellow counter if red is encountered
		                 }
		                 // If the current diagonal cell is empty
		                 else {
		                     countYellow = 0;
		                     countRed = 0;  // Reset both counters if the cell is empty
		                 }

		                 // If four yellow pieces are found in the diagonal, yellow wins
		                 if (countYellow == 4) return -1;
		                 // If four red pieces are found in the diagonal, red wins
		                 if (countRed == 4) return 1;
		             }
		         }
		     }

		     // Checking diagonal alignment (/ direction)
		     for (int row = 0; row <= rows - 4; row++) {  // Ensure there's enough space for 4 pieces vertically
		         for (int col = 3; col < cols; col++) {  // Ensure there's enough space for 4 pieces horizontally (starting from column 3)
		             int countYellow = 0;  // Counter for yellow pieces
		             int countRed = 0;     // Counter for red pieces

		             // Check diagonal from (row, col) to (row+3, col-3)
		             for (int i = 0; i < 4; i++) {
		                 int content = state.content(row + i, col - i);  // Get the content of the current diagonal cell

		                 // If the current diagonal cell contains a yellow piece
		                 if (content == 1) {
		                     countYellow++;
		                     countRed = 0;  // Reset red counter if yellow is encountered
		                 }
		                 // If the current diagonal cell contains a red piece
		                 else if (content == 2) {
		                     countRed++;
		                     countYellow = 0;  // Reset yellow counter if red is encountered
		                 }
		                 // If the current diagonal cell is empty
		                 else {
		                     countYellow = 0;
		                     countRed = 0;  // Reset both counters if the cell is empty
		                 }

		                 // If four yellow pieces are found in the diagonal, yellow wins
		                 if (countYellow == 4) return -1;
		                 // If four red pieces are found in the diagonal, red wins
		                 if (countRed == 4) return 1;
		             }
		         }
		     }

		     // If no winner is found after checking all directions, return 0 (no winner)
		     return 0;
		 }

	/**
 * Calculates the heuristic value of a cell in the game state.
 * Each cell has a weight corresponding to the number of possible "four-in-a-row" configurations
 * (vertical, horizontal, and diagonal) that include this cell.
 * Central positions are generally more valuable as they participate in more configurations.
 *
 * @param row The row index of the cell.
 * @param column The column index of the cell.
 * @return The heuristic value of the cell as a double.
 */
	public static double value_box(int row, int column) {

	    int[][] grid = {
	        {3, 4, 5, 7, 5, 4, 3},
	        {4, 6, 8, 10, 8, 6, 4},
	        {5, 8, 11, 13, 11, 8, 5},
	        {5, 8, 11, 13, 11, 8, 5},
	        {4, 6, 8, 10, 8, 6, 4},
	        {3, 4, 5, 7, 5, 4, 3}
	    };

	    return (double) grid[row][column];

	}



/**
 * Check if there are combinations with 3 red pieces and one blank spot that is playable.
 * This function allows us to win one depth in our tree-search. If there is a win combination for red,
 * we will have the answer directly.
 *
 * @param g The current GameSate
 * @return 1 if there is a way to win, 0 otherwise
*/
public static int checkPossibleWin(GameState g) {

		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 7; col++) {
				if (g.content(row, col) == 0) {

					if (row>0 && g.content(row-1, col)==0) {
						continue;
					}

					//check south
					if (row > 2) {
						if (g.content(row-1, col) == 2 && g.content(row-2, col) == 2 && g.content(row-3, col) == 2) {
							return 1;
						}
					}

					//check east/west
					int i = 1;
					int countRed = 0;
					while (col-i >= 0) {
						if (g.content(row, col-i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (countRed >= 3 && col-i >= 0 && doable(g,row,col-i)) {
						return 1;
					}
					i = 1;
					while (col+i < 7) {
						if (g.content(row, col+i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (countRed >= 3 && col+i < 7 && doable(g,row,col+i)) {
						return 1;
					}


					//check NorthWest to SouthEast
					i = 1;
					countRed = 0;
					while (col-i >= 0 && row+i < 6) {
						if (g.content(row+i, col-i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (countRed >= 3 && col-i >= 0 && row+i < 6 && doable(g,row+i,col-i)) {
						return 1;
					}
					i = 1;
					while (col+i < 7 && row-i >= 0) {
						if (g.content(row-i, col+i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (countRed >= 3 && col+i < 7 && row-i >= 0 && doable(g,row-i,col+i)) {
						return 1;
					}


					//check NorthEast to SouthWest
					i = 1;
					countRed = 0;
					while (col-i >= 0 && row-i >= 0) {
						if (g.content(row-i, col-i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (countRed >= 3 && col-i >= 0 && row-i >= 0 && doable(g,row-i,col-i)) {
						return 1;
					}
					i = 1;
					while (col+i < 7 && row+i < 6) {
						if (g.content(row+i, col+i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (countRed >= 3 && col+i < 7 && row+i < 6 && doable(g,row+i,col+i)) {
						return 1;
					}


				}
			}
		}
		return 0;

	}



public static boolean doable(GameState g, int row, int col) {
	if (row > 0) {
		if (g.content(row, col) == 0 && g.content(row-1, col) != 0) {
			return true;
		}

	}
	else if (row == 0 && g.content(row, col)==0) {
		return true;
	}
	return false;
}


/**
 *   xx_
 *     x
 *     x
 *
 *  Check if there is a way to win watever the oponnent play.
 *  @param g The current GameSate
 *  @return 1 if there is a "smart" way to win
 */

public static int checkPossibleWin2(GameState g) {

		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 7; col++) {
				if (g.content(row, col) == 0) {

					if (row>0 && g.content(row-1, col)==0) {
						continue;
					}



					int south = 0;
					int east = 0;
					int west = 0;
					int SW = 0;
					int NE = 0;
					int SE = 0;
					int NW = 0;



					//check south
					if (row > 1) {
						if (g.content(row-1, col) == 2 && g.content(row-2, col) == 2) {
							south = 1;
						}
					}

					//check east
					int i = 1;
					int countRed = 0;
					while (col-i >= 0) {
						if (g.content(row, col-i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (col < 5 && countRed >= 2 && doable(g,row, col+1)) {
						east =1;
					}

					//check west
					i = 1;
					while (col+i < 7) {
						if (g.content(row, col+i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (col>=1 && countRed >= 2 && doable(g,row, col-1)) {
						west =1;
					}

					//check NorthWest
					i = 1;
					countRed = 0;
					while (col-i >= 0 && row+i < 6) {
						if (g.content(row+i, col-i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (col < 6 && row>=1 && countRed >= 2 && doable(g,row-1, col+1)) {
						NW =1;
					}
					i = 1;
					while (col+i < 7 && row-i >= 0) {
						if (g.content(row-i, col+i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (col > 0 && row < 5 && countRed >= 2 && doable(g,row+1, col-1)) {
						SE =1;
					}


					//check NorthEast to SouthWest
					i = 1;
					countRed = 0;
					while (col-i >= 0 && row-i >= 0) {
						if (g.content(row-i, col-i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (col < 6 && row < 5 && countRed >= 2 && doable(g,row+1, col+1)) {
						NE =1;
					}
					i = 1;
					while (col+i < 7 && row+i < 6) {
						if (g.content(row+i, col+i) == 2) {
							countRed++;
						}
						else {
							break;
						}
						i++;
					}
					if (col > 0 && row > 0 && countRed >= 2 && doable(g,row-1, col-1)) {
						SW =1;
					}

					if (south+west+east+NW+NE+SW+SE >= 2) {
						return 1;
					}



				}
			}
		}
		return 0;

	}





	/**
 * Analyzes the neighboring cells around a specific cell in the game state.
 * Counts the number of empty, yellow, and red neighboring cells.
 * This method allows us to dynamically adjust the heuristic value_box, based on the
 * concentration of yellow,red pieces around it, which can indicate potential threats or opportunities.
 *
 * @param state The current game state
 * @param row The row index of the target cell.
 * @param column The column index of the target cell.
 * @return An array of integers:
 *         [number of empty neighbors, number of yellow neighbors, number of red neighbors].
 */
	public static int[] analyzeNeighbors(GameState state, int row, int column) {
	    // Define constants for the piece types
			// Heuristic plus dynamic. Compte autour combien de Yellow ou red
	    final int YELLOW = 1;
	    final int RED = 2;


	    // Initialize counters for red, yellow, and empty squares
	    int redCount = 0;
	    int yellowCount = 0;
	    int emptyCount = 0;


	    // Directions array to check 8 neighbors (rowDelta, colDelta)
	    int[][] directions = {
	        {-1, -1}, {-1, 0}, {-1, 1}, // Top-left, Top, Top-right
	        {0, -1},          {0, 1},  // Left,        Right
	        {1, -1}, {1, 0}, {1, 1}    // Bottom-left, Bottom, Bottom-right
	    };

	    // Iterate through all directions
	    for (int[] dir : directions) {
	        int newRow = row + dir[0];
	        int newCol = column + dir[1];

	        // Check if the neighbor is within the board boundaries
	        if (newRow >= 0 && newRow < 6 && newCol >= 0 && newCol < 7) {
	            int content = state.content(newRow, newCol);
	            if (content == RED) {
	                redCount++;
	            } else if (content == YELLOW) {
	                yellowCount++;
	            }
	            else {
	            	emptyCount++;
	            }
	        }
	    }

	    // Return the counts in an array: [emptyCount, yellowCount, redCount]
	    return new int[] {emptyCount,yellowCount, redCount};
	}


	/**
     * Evaluates the heuristic value of the current game state based on the position of pieces.
     * Considers the value of cells and neighboring pieces to calculate a score.
     *
     * @param g The current game state.
     * @return The heuristic value of the game state.
     */
	public static Double heuristic_game(GameState g){
		Double value = 0.0;
		Double alpha = 0.5d;

		Double retrievedValue = exploredSet_game.get(g);
		if (retrievedValue != null) {
			value = retrievedValue;
		}
		else {

			if (checkPossibleWin(g) == 1) {
				value += 50d;
			}
			if (checkPossibleWin2(g) == 1) {
				value += 25d;
			}
			for(int i = 0;i<=5;i++){
				for(int j = 0;j<=6;j++){
					int[] counts = analyzeNeighbors(g,i,j);

					switch(g.content(i,j)){
					case(0):
						value += counts[2] - counts[1];
						break;
					case(1):
						value = value - (value_box(i,j) + 2*counts[1] + (1/alpha)*counts[0]);
						break;
					case(2):
						value = value + value_box(i,j) + 2*counts[2]+(1/alpha)*counts[0] ;
						break;


					}
				}
			}





			exploredSet_game.put(g, value);
		}
		return value;
		}


	/**
	 * Evaluates the overall heuristic value of a belief state by combining probabilities of all possible game states.
	 *
	 * @param belief The belief state to evaluate.
	 * @return The combined heuristic value of the belief state.
	 */
	public static Double heuristic_belief(BeliefState belief){
     Double proba_limite = Math.pow(10, -6); // States with very low probabilities are ignored.
     Double points = 0.0; // Initialize the points accumulator to store the heuristic score.

     // Loop through each GameState in the belief
     for(GameState g : belief){

         // If the game is over, we check the result (win, loss, or draw) and adjust points accordingly
         if (g.isGameOver()) {

             // If the game is full (no more moves), we check the outcome of the game
             if (g.isFull()) {
                 switch(fourInARow(g)){
                     case(-1): // Opponent wins
                         if(g.proba() >= proba_limite){
                             points -= g.proba() * 100d; // Deduct points for a loss with a high probability
                         }
                         break;
                     case(0): // Draw
                         if(g.proba() >= proba_limite){
                             points += g.proba() * 100d; // Add points for a draw with a high probability
                         }
                         break;
                     case(1): // Current player wins
                         if(g.proba() >= proba_limite){
                             points += g.proba() * 100d; // Add points for a win with a high probability
                         }
                         break;
                 }
             }

             // If it's the opponent's turn, this means they won and we penalize the state since it indicates a possible threat
             else if(g.turn()) {
                 if(g.proba() >= proba_limite){
                     points -= g.proba() * 100d; // Deduct points for states where it's the opponent's turn
                 }
             }
             // If it's the current player's turn, this means we won and we reward the state
             else {
                 if(g.proba() >= proba_limite){
                     points += g.proba() * 100d; // Add points for favorable states where it's the current player's turn
                 }
             }
         }
         // If the game is not over, evaluate the current game state using our heuristic
         else {
             if(g.proba() >= proba_limite){
                 points += g.proba() * heuristic_game(g); // Add points based on the heuristic evaluation of the current game state
             }
         }
     }

     // Return the accumulated heuristic value
     return points;
 }


	/**
   * Aggregates belief states after simulating a player's move and the opponent's predicted moves.
   * Used to explore all possible outcomes after a specific move.
   *
   * @param belief The current belief state.
   * @param move The move to simulate.
   * @return A list of belief states resulting from the move and predictions.
   */
	public static ArrayList<BeliefState> agregation(BeliefState belief, int move){
    // Initialize the final list of belief states after aggregation
    ArrayList<BeliefState> array_final_belief = new ArrayList<>();

    // Apply the player's move to the current belief state and get the resulting game states
    Results res = belief.putPiecePlayer(move);

    // Loop through each belief state after the player's move
    for (BeliefState b : res) {
        // Predict the opponent's move for each belief state
        Results help = b.predict();

        // If no prediction can be made (indicating a terminal state), add the belief state to the final list
        if (help == null) {
            array_final_belief.add(b); // The Belief state is terminal or cannot be further predicted and we will evalute it later
            continue;
        }



      // We do not consider the GameState with low probability
      // Add all the predicted belief states to the final list
      for(BeliefState b2 : help){
			BeliefState b3 = b2.copy();
			for (GameState g : b2) {
				if (g.proba() < 1e-2 ) {
					b3.removeGameState(g);
				}
			}
			if (b3.size()>0) {
				array_final_belief.add(b3);
			}

		}
    }

    // Return the aggregated list of belief states after the player's move and the opponent's prediction
    return array_final_belief;
}



	/**
	 * Performs an AND search on a set of belief states to calculate the combined value.
	 * If the maximum search depth is reached or the game ends, evaluates the belief state heuristically.
	 * Otherwise, calls `or_search` to continue the search.
	 *
	 * @param array_belief The list of belief states to evaluate.
	 * @param depth The current depth of the search.
	 * @param max_depth The maximum depth allowed for the search.
	 * @return The heuristic value of the belief states.
	 */
	public static Double and_search(ArrayList<BeliefState> array_belief, int depth, int max_depth) {
	     // Base case: if the maximum depth is reached, evaluate the belief states
	     if (depth == max_depth) {
	         Double points = 0.0; // Total points to accumulate the heuristic values of belief states
	         double value_belief = 0.0; // Value of each individual belief state
	         Float retrievedValue = null; // Retrieved heuristic value from the explored set

	         // Iterate through each belief state in the provided array
	         for (BeliefState b : array_belief) {
	             // Check if the heuristic value for the belief state has already been calculated
	             retrievedValue = exploredSet.get(b);

							 //Before we make some sanity check...
	             if (retrievedValue != null && !Double.isNaN(retrievedValue) && !Double.isInfinite(retrievedValue)) {
	                 // If the value is already in the explored set, add it to the total points
	                 points += retrievedValue;
	             } else {
	                 // If the value is not found, calculate it using our heuristic
	                 value_belief = heuristic_belief(b);
	                 points += value_belief;

	                 // Store the computed value in the explored set for future reference
	                 exploredSet.put(b, (float) value_belief);
	             }
	         }
	         return points;
	     }
			 else {
	         // Recursive case: explore deeper if the maximum depth is not reached
	         Double points = 0.0; // Total points to accumulate the heuristic values of belief states
	         for (BeliefState b : array_belief) {
	             // Retrieve the previously calculated heuristic value, if available
	             Float retrievedValue = exploredSet.get(b);
	             if (retrievedValue != null && !Double.isNaN(retrievedValue) && !Double.isInfinite(retrievedValue)) {
	                 // If the value is already computed, add it to the total points
	                 points += retrievedValue;
	             }
							 else {
	                 double value_belief = 0;

	                 // If the game is over or full, directly apply the heuristic to evaluate the belief state
	                 if (b.isGameOver() || b.isFull()) {
	                     value_belief = heuristic_belief(b);
	                 } else {
	                     // Otherwise, perform an OR search on the belief state to explore further
	                     ArrayList<Double> array = or_search(b, depth, max_depth);
	                     // Get the maximum value from the OR search results
	                     value_belief = Collections.max(array);
	                 }

	                 // Add the computed value to the total points
	                 points += value_belief;

	                 // Store the computed value in the explored set for future reference
	                 if (!Double.isNaN(value_belief) && !Double.isInfinite(value_belief)) {
		                 exploredSet.put(b,  (float) value_belief);
		             }
	             }
	         }
	         return points;
	     }
	 }


	/**
	* Performs an OR search on a single belief state, evaluating possible moves and their outcomes.
	* Calculates the heuristic value for each move and stores them in an array.
	*
	* @param belief The current belief state.
	* @param depth The current depth of the search.
	* @param max_depth The maximum depth allowed for the search.
	* @return An array of heuristic values for each possible move.
	*/
	public static ArrayList<Double> or_search(BeliefState belief, int depth, int max_depth) {
	    // Initialize an array to hold the heuristic values for each possible move
	    // Start with very low values, as we will update these with better scores
	    ArrayList<Double> array_mark = new ArrayList<>();
	    for (int i = 0; i <= 6; i++) {
	        array_mark.add(-100000d); // Use a very low value initially
	    }

	    // Get the list of possible moves for the current belief state
	    ArrayList<Integer> moves = belief.getMoves();

	    // Evaluate each possible move by calling the AND search for each resulting belief state
	    for (int i : moves) {
	        // For each move, aggregate the belief states and evaluate their heuristic values
	        Double d = and_search(agregation(belief, i), depth + 1, max_depth);

	        // Set the heuristic value for the corresponding move in the array
	        array_mark.set(i, d);
	    }

	    // Return the array of heuristic values corresponding to each possible move
	    return array_mark;
	}


	/**
     * Finds the index of the maximum value in a list of doubles.
     * (Used to determine the best move based on heuristic, after the or_search call)
     *
     * @param list The list of values to analyze.
     * @return The index of the maximum value.
     */
	public static int maxIndex(ArrayList<Double> list) {
		   // Check if the list is null or empty and throw an exception if it is
	    if (list == null || list.isEmpty()) {
			     throw new IllegalArgumentException("La liste ne doit pas être vide ou nulle");
	    }

		  // Initialize the maximum value and its index
		  double maxVal = list.get(0);
		  int maxIndex = 0;

		  // Traverse the list to find the maximum value and its index
		  for (int i = 1; i < list.size(); i++) {
		     if (list.get(i) > maxVal) {
		         maxVal = list.get(i);  // Update the max value
		         maxIndex = i;          // Update the index of the max value
		     }
		   }

		  // Return the index of the maximum value
		  return maxIndex;
 }

	/**
	* Determines the next move to play based on the current belief state.
	* Uses different maximum depths depending on the number of games played to balance performance and precision.
	*
	* @param game The current belief state of the game.
	* @return The index of the column for the next move.
	*/
	public static int findNextMove(BeliefState game) {

		ArrayList<Double> a = null;
	    // If the number of games played is less than or equal to 600, use depth_max for searching
			//Why this value ? After many test, it was the value, that gives us the best ratio win/loss on average
	    if (nbr_partie <= 600) {
	        a = or_search(game, 0, depth_max);
	    } else {
	        a = or_search(game, 0, depth_max_bis);
	    }

	    // Increment the game counter to track how many games have been played
	    nbr_partie += 1;

	    // Return the index of the move with the highest evaluation score
	    return maxIndex(a);
	}
}
