package poker;

//Define Card ADL, implements Comparable to sort Card.
public class Card implements Comparable<Card>{

	//Declare card's rank and suit.
	private Rank rank;
	private Suit suit;
	
	//Card's rank and suit.
	public Card(Rank rank, Suit suit) {
		this.rank = rank;
		this.suit = suit;
	}
	
	//Get rank.

	public Rank getRank() {
		return rank;
	}
	
	//Get suit.
	public Suit getSuit() {
		return suit;
	}

	//Compare two cards based on rank, higher rank comes first
	@Override
	public int compareTo(Card anotherCard) {
		return anotherCard.rank.get_rank() - this.rank.get_rank();
	}
	
	@Override
	public String toString() {
		return rank + " of " + suit;
	}
	
	
	
}
