package at.sleazlee.bmessentials.DonationSystem;

/**
 * The Rank class represents a rank in the game with the name, total chunks, bonus chunks and member slots.
 */
public class RankBuilder {
	// Private fields to store the rank name, total chunks, bonus chunks and member slots.
	private String rank = "";
	private int ranking = 0;

	/**
	 * Constructor for the Rank class that sets the rank name, total chunks, bonus chunks and member slots.
	 * @param rankIn the rank name
	 * @param rankingIn the rank number
	 */
	public RankBuilder(String rankIn, int rankingIn) {
		rank = rankIn;
		ranking = rankingIn;
	}

	/**
	 * Gets the rank name.
	 * @return the rank name
	 */
	public String getRank() {
		return rank;
	}

	/**
	 * Gets the member slots for the rank.
	 * @return the member slots for the rank
	 */
	public int getranking() {
		return ranking;
	}
}
