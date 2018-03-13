package poker;

//Record all cards' ranks and fullnames.
public enum Rank {
	
	TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5), 
		SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9),
			TEN("10", 10), JACK("Jack",11), QUEEN("Queen", 12), 
				KING("King", 13), ACE("Ace", 14);
	
	private final String fullname;
	private final int rank;
	
	private Rank(String fullname,int rank) {
		
		this.fullname = fullname;
		this.rank = rank;
	}
	
	public String get_fullname() {
		
		return this.fullname;
	}
	
	public int get_rank() {
		
		return rank;
	}
}