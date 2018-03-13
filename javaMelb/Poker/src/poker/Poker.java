package poker;

//Include main method.
public class Poker {

	public static void main(String[] args) {
		for (int i=0; i<args.length; ++i) {
			if(args[i].length()!=2) {
				System.out.println("U should input just rank and suit of one card");
				return;
			}
		}
		//part 2 not attempted
		if(args.length > 5) { 
			
			System.out.println("NOT UNDERTAKEN");
		} 
		
		//if not 5
		else if(args.length != 5) { 
			
			System.out.println("Error: wrong number of arguments; must be a multiple of 5");
		} 
		
		else {
			
			//get cards
			Card[] cards = getCards(args);
			
			//if converted
			if(cards!=null) {
				//create poker hand
				PokerHand pokerHand = new PokerHand(cards);
				//display description
				System.out.printf("Player %d: %s%n",1, pokerHand.classifyHand());
			}
			
		}
			
	}
	
	
	//get cards from the string cards
	private static Card[] getCards(String[] cardsStr) {
		Card[] cards = new Card[cardsStr.length]; //create array
		
		//for each card
		for (int i = 0; i < cards.length; i++) {
			//get rank and suit
			Rank rank = getRank(cardsStr[i].toUpperCase().charAt(0));
			Suit suit = getSuit(cardsStr[i].toUpperCase().charAt(1));
			
			//if rank or suit not valid
			if(rank == null || suit  == null) {
				System.out.printf("Error: invalid card name ’%s’%n",cardsStr[i]);
				return null;
			}
			
			//add card
			cards[i] = new Card(rank, suit);
		}
		
		return cards;
	}
	
	//Get Rank. Return null if no match.
	private static Rank getRank(char r) {
		switch(r) {
		case('A'):
			return Rank.ACE;
		case('2'):
			return Rank.TWO;
		case('3'):
			return Rank.THREE;
		case('4'):
			return Rank.FOUR;
		case('5'):
			return Rank.FIVE;
		case('6'):
			return Rank.SIX;
		case('7'):
			return Rank.SEVEN;
		case('8'):
			return Rank.EIGHT;
		case('9'):
			return Rank.NINE;
		case('T'):
			return Rank.TEN;
		case('J'):
			return Rank.JACK;
		case('Q'):
			return Rank.QUEEN;
		case('K'):
			return Rank.KING;
		}	
		return null;
	}
	
	// Get Suit from char. Return null if no match.
	private static Suit getSuit(char s) {
		switch(s)
		{
			case('C'):
				return Suit.Clubs;
			case('S'):
				return Suit.Spades;
			case('D'):
				return Suit.Diamonds;
			case('H'):
				return Suit.Hearts;
		}
		return null;
	}
	
}
