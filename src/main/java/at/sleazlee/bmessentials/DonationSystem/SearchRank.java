package at.sleazlee.bmessentials.DonationSystem;

public class SearchRank {

	static RankBuilder[] listOfRanks = new RankBuilder[67];

	/**
	 * Searches the list of Rank objects for a rank with the desired name and returns the value of the specified attribute.
	 * @param desiredRank The name of the rank to search for.
	 * @param lookForWhat The attribute to return the value of. Must be "TotalChunks", "BonusChunks", or "memberSlots".
	 * @return The value of the specified attribute for the rank with the desired name, or -1 if the rank was not found.
	 */
	public static int searchRank(String desiredRank, String lookForWhat) {

		for (RankBuilder listOfRank : listOfRanks) {
			if (listOfRank.getRank().equals(desiredRank)) {
				switch (lookForWhat) {
					case "Ranking":
						return listOfRank.getranking();
				}
			}
		}
		// If the desired rank was not found, return -1
		System.out.println("§c§lBMLands §cDesired rank not found: " + desiredRank); // Debugging statement
		return -1;
	}

	/** Loads a list of Ranks in an Array called listOfRanks
	 *
	 * This list is in order to be more efficient for linear search,
	 * I did however add comments on each rank so that I'll be able to
	 * order them back in an Excel/Sheet doc.
	 */
	private void loadRanks() {
		listOfRanks[0] = new RankBuilder("default", 0); //0
		listOfRanks[1] = new RankBuilder("[1]", 1); //1
		listOfRanks[2] = new RankBuilder("[2]", 2); //2
		listOfRanks[3] = new RankBuilder("[3]", 3); //3
		listOfRanks[4] = new RankBuilder("[4]", 4); //4
		listOfRanks[5] = new RankBuilder("[5]", 5); //5
		listOfRanks[6] = new RankBuilder("[3][plus]", 3); //14
		listOfRanks[7] = new RankBuilder("[4][plus]", 4); //15
		listOfRanks[8] = new RankBuilder("[5][plus]", 5); //16
		listOfRanks[9] = new RankBuilder("[3][premium]", 3); //25
		listOfRanks[10] = new RankBuilder("[4][premium]", 4); //26
		listOfRanks[11] = new RankBuilder("[5][premium]", 5); //27
		listOfRanks[12] = new RankBuilder("[4][ultra]", 4); //37
		listOfRanks[13] = new RankBuilder("[5][ultra]", 5); //38
		listOfRanks[14] = new RankBuilder("[6][ultra]", 6); //39
		listOfRanks[15] = new RankBuilder("[4][super]", 4); //48
		listOfRanks[16] = new RankBuilder("[5][super]", 5); //49
		listOfRanks[17] = new RankBuilder("[6][super]", 6); //50
		listOfRanks[18] = new RankBuilder("[6]", 6); //6
		listOfRanks[19] = new RankBuilder("[7]", 7); //7
		listOfRanks[20] = new RankBuilder("[8]", 8); //8
		listOfRanks[21] = new RankBuilder("[9]", 9); //9
		listOfRanks[22] = new RankBuilder("[10]", 10); //10
		listOfRanks[23] = new RankBuilder("[6][plus]", 6); //17
		listOfRanks[24] = new RankBuilder("[7][plus]", 7); //18
		listOfRanks[25] = new RankBuilder("[6][premium]", 6); //28
		listOfRanks[26] = new RankBuilder("[7][premium]", 7); //29
		listOfRanks[27] = new RankBuilder("[7][ultra]", 7); //40
		listOfRanks[28] = new RankBuilder("[8][ultra]", 8); //41
		listOfRanks[29] = new RankBuilder("[7][super]", 7); //51
		listOfRanks[30] = new RankBuilder("[8][super]", 8); //52
		listOfRanks[31] = new RankBuilder("[0][plus]", 0); //11
		listOfRanks[32] = new RankBuilder("[1][plus]", 1); //12
		listOfRanks[33] = new RankBuilder("[2][plus]", 2); //13
		listOfRanks[34] = new RankBuilder("[0][premium]", 0); //22
		listOfRanks[35] = new RankBuilder("[1][premium]", 1); //23
		listOfRanks[36] = new RankBuilder("[2][premium]", 2); //24
		listOfRanks[37] = new RankBuilder("[0][ultra]", 0); //33
		listOfRanks[38] = new RankBuilder("[1][ultra]", 1); //34
		listOfRanks[39] = new RankBuilder("[2][ultra]", 2); //35
		listOfRanks[40] = new RankBuilder("[3][ultra]", 3); //36
		listOfRanks[41] = new RankBuilder("[0][super]", 0); //44
		listOfRanks[42] = new RankBuilder("[1][super]", 1); //45
		listOfRanks[43] = new RankBuilder("[2][super]", 2); //46
		listOfRanks[44] = new RankBuilder("[3][super]", 3); //47
		listOfRanks[45] = new RankBuilder("[8][plus]", 8); //19
		listOfRanks[46] = new RankBuilder("[8][premium]", 8); //30
		listOfRanks[47] = new RankBuilder("[9][ultra]", 9); //42
		listOfRanks[48] = new RankBuilder("[9][super]", 9); //53
		listOfRanks[49] = new RankBuilder("[9][plus]", 9); //20
		listOfRanks[50] = new RankBuilder("[10][plus]", 10); //21
		listOfRanks[51] = new RankBuilder("[9][premium]", 9); //31
		listOfRanks[52] = new RankBuilder("[10][premium]", 10); //32
		listOfRanks[53] = new RankBuilder("[10][ultra]", 10); //43
		listOfRanks[54] = new RankBuilder("[10][super]", 10); //54
		listOfRanks[55] = new RankBuilder("[4][blockminer]", 4); //59
		listOfRanks[56] = new RankBuilder("[5][blockminer]", 5); //60
		listOfRanks[57] = new RankBuilder("[6][blockminer]", 6); //61
		listOfRanks[58] = new RankBuilder("[7][blockminer]", 7); //62
		listOfRanks[59] = new RankBuilder("[0][blockminer]", 0); //55
		listOfRanks[60] = new RankBuilder("[1][blockminer]", 1); //56
		listOfRanks[61] = new RankBuilder("[2][blockminer]", 2); //57
		listOfRanks[62] = new RankBuilder("[3][blockminer]", 3); //58
		listOfRanks[63] = new RankBuilder("[8][blockminer]", 8); //63
		listOfRanks[64] = new RankBuilder("[9][blockminer]", 9); //64
		listOfRanks[65] = new RankBuilder("[10][blockminer]", 10); //65
		listOfRanks[66] = new RankBuilder("admin", 10); //66
	}

}
