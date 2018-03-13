package poker;

import java.util.Arrays;

//Represent a poker hand
public class PokerHand {

	// cards
	private Card[] cards;
	private int[] classification = new int[9];
	private int label;
	

	// creates a new poker hand
	public PokerHand(Card[] cards) {
		this.cards = Arrays.copyOf(cards, cards.length);
		Arrays.sort(this.cards); // sort cards
	}


	// classify hand
	String classifyHand() {

		for(int i=0; i<9;++i) {
			classification[i]=0;
		}
		
		//flush
		label=1;
		for (int i=0; i<cards.length-1;++i) {
			
			if (cards[i].getSuit().equals(cards[i+1].getSuit())==false){
				
				label=0;
				break;
			}
		}
		if (label==1) {
			
			classification[4]=1;
			System.out.println('1');
		}
		
		//straight
		label=1;
		for (int i=0; i<cards.length-1;++i) {
			
			if (Math.abs(cards[i].getRank().get_rank()-cards[i+1].getRank().get_rank())!=1){
				
				label=0;
				break;
			}
			}
		if (label==1) {
			
			classification[3]=1;
			System.out.println('2');
		}
		
		if (classification[3]==1 && classification[4]==1) {
			
			classification[0]=1;
		}
		
		// four of kind 1
		if (n_aKind(4,null)!=null) {
			classification[1] = 1;
		}
		
		// full house 2
		// three of kind 5
		if(n_aKind(3,null)!=null) {
			classification[5]=1;
			if(n_aKind(2,n_aKind(3,null))!=null) {
				classification[2]=1;
				
			}
		}
		// two pairs 6
		// one pair 7
		if(n_aKind(2,null)!=null) {
			classification[7]=1;
			if(n_aKind(2,n_aKind(2,null))!=null) {
				classification[6]=1;
			}
		}
		// high card 8
		classification[8]=1;
	
		if (classification[0]==1) {
			return String.format("%s-high straight flush",cards[0].getRank().get_fullname());
		}
		if (classification[1]==1) {
			return String.format("Four %ss",n_aKind(4,null).getRank().get_fullname());
		}
		if (classification[2]==1) {
			return String.format("%ss full of %ss",n_aKind(3,null).getRank().get_fullname(),
					n_aKind(2,n_aKind(3,null)).getRank().get_fullname());
		}
		if (classification[3]==1) {
			return String.format("%s-high flush",cards[0].getRank().get_fullname());
		}
		if (classification[4]==1) {
			return String.format("%s-high straight",cards[0].getRank().get_fullname());
		}
		if (classification[5]==1) {
			return String.format("Three %ss",n_aKind(3,null).getRank().get_fullname());
		}
		if (classification[6]==1) {
			return String.format("%ss over %ss",n_aKind(2,null).getRank().get_fullname(),
					n_aKind(2,n_aKind(2,null)).getRank().get_fullname());
		}
		if (classification[7]==1) {
			return String.format("Pair of %ss",n_aKind(2,null).getRank().get_fullname());
		}	
		
		return String.format("%s-high", cards[0].getRank().get_fullname());
	}
	
	private Card n_aKind(int n, Card cardToIgnore) {
		for (int i = 0; i < cards.length; i++) {

			// if this card is to be ignored
			if (cardToIgnore != null
					&& cardToIgnore.getRank() == cards[i].getRank())
				continue;

			int count = 0;
			for (int j = 0; j < cards.length; j++) {
				if (cards[j].getRank() == cards[i].getRank())
					count++;
			}

			if (count == n) // if found
				return cards[i];
		}

		return null;
	}
}