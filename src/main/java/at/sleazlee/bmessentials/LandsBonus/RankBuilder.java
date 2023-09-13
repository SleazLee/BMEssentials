package at.sleazlee.bmessentials.LandsBonus;

/**
 * The Rank class represents a rank in the game with the name, total chunks, bonus chunks and member slots.
 */
public class RankBuilder {
	// Private fields to store the rank name, total chunks, bonus chunks and member slots.
	private String rank = "";
	private int totalChunks = 0;
	private int bonusChunks = 0;
	private int memberSlots = 0;
	private int ranking = 0;

	/**
	 * Constructor for the Rank class that sets the rank name, total chunks, bonus chunks and member slots.
	 * @param rankIn the rank name
	 * @param totalChunksIn the total chunks
	 * @param bonusChunksIn the bonus chunks
	 * @param memberSlotsIn the member slots
	 * @param rankingIn the rank number
	 */
	public RankBuilder(String rankIn, int totalChunksIn, int bonusChunksIn, int memberSlotsIn, int rankingIn) {
		rank = rankIn;
		totalChunks = totalChunksIn;
		bonusChunks = bonusChunksIn;
		memberSlots = memberSlotsIn;
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
	 * Gets the total chunks for the rank.
	 * @return the total chunks for the rank
	 */
	public int getTotalChunks() {
		return totalChunks;
	}

	/**
	 * Gets the bonus chunks for the rank.
	 * @return the bonus chunks for the rank
	 */
	public int getBonusChunks() {
		return bonusChunks;
	}

	/**
	 * Gets the member slots for the rank.
	 * @return the member slots for the rank
	 */
	public int getMemberSlots() {
		return memberSlots;
	}

	/**
	 * Gets the member slots for the rank.
	 * @return the member slots for the rank
	 */
	public int getranking() {
		return ranking;
	}
}
