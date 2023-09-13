package at.sleazlee.bmessentials.LandsBonus;

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
					case "TotalChunks":
						return listOfRank.getTotalChunks();
					case "BonusChunks":
						return listOfRank.getBonusChunks();
					case "MemberSlots":
						return listOfRank.getMemberSlots();
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
		listOfRanks[0] = new RankBuilder("default", 0, 0, 0, 0); //0
		listOfRanks[1] = new RankBuilder("[1]", 5, 2, 1, 1); //1
		listOfRanks[2] = new RankBuilder("[2]", 20, 5, 2, 2); //2
		listOfRanks[3] = new RankBuilder("[3]", 40, 10, 3, 3); //3
		listOfRanks[4] = new RankBuilder("[4]", 70, 15, 4, 4); //4
		listOfRanks[5] = new RankBuilder("[5]", 105, 25, 5, 5); //5
		listOfRanks[6] = new RankBuilder("[3][plus]", 85, 20, 6, 3); //14
		listOfRanks[7] = new RankBuilder("[4][plus]", 145, 35, 8, 4); //15
		listOfRanks[8] = new RankBuilder("[5][plus]", 225, 50, 10, 5); //16
		listOfRanks[9] = new RankBuilder("[3][premium]", 315, 55, 9, 3); //25
		listOfRanks[10] = new RankBuilder("[4][premium]", 505, 100, 11, 4); //26
		listOfRanks[11] = new RankBuilder("[5][premium]", 745, 150, 14, 5); //27
		listOfRanks[12] = new RankBuilder("[4][ultra]", 1745, 175, 23, 4); //37
		listOfRanks[13] = new RankBuilder("[5][ultra]", 2645, 260, 28, 5); //38
		listOfRanks[14] = new RankBuilder("[6][ultra]", 3645, 360, 33, 6); //39
		listOfRanks[15] = new RankBuilder("[4][super]", 6145, 600, 48, 4); //48
		listOfRanks[16] = new RankBuilder("[5][super]", 8895, 900, 58, 5); //49
		listOfRanks[17] = new RankBuilder("[6][super]", 12245, 1225, 68, 6); //50
		listOfRanks[18] = new RankBuilder("[6]", 145, 35, 6, 6); //6
		listOfRanks[19] = new RankBuilder("[7]", 195, 45, 7, 7); //7
		listOfRanks[20] = new RankBuilder("[8]", 270, 60, 8, 8); //8
		listOfRanks[21] = new RankBuilder("[9]", 370, 75, 9, 9); //9
		listOfRanks[22] = new RankBuilder("[10]", 495, 100, 10, 10); //10
		listOfRanks[23] = new RankBuilder("[6][plus]", 335, 70, 12, 6); //17
		listOfRanks[24] = new RankBuilder("[7][plus]", 470, 90, 14, 7); //18
		listOfRanks[25] = new RankBuilder("[6][premium]", 1025, 200, 16, 6); //28
		listOfRanks[26] = new RankBuilder("[7][premium]", 1355, 270, 19, 7); //29
		listOfRanks[27] = new RankBuilder("[7][ultra]", 4745, 475, 38, 7); //40
		listOfRanks[28] = new RankBuilder("[8][ultra]", 5995, 600, 43, 8); //41
		listOfRanks[29] = new RankBuilder("[7][super]", 15995, 1600, 78, 7); //51
		listOfRanks[30] = new RankBuilder("[8][super]", 20495, 2050, 88, 8); //52
		listOfRanks[31] = new RankBuilder("[0][plus]", 5, 2, 0, 0); //11
		listOfRanks[32] = new RankBuilder("[1][plus]", 15, 4, 2, 1); //12
		listOfRanks[33] = new RankBuilder("[2][plus]", 40, 10, 4, 2); //13
		listOfRanks[34] = new RankBuilder("[0][premium]", 10, 3, 1, 0); //22
		listOfRanks[35] = new RankBuilder("[1][premium]", 55, 10, 4, 1); //23
		listOfRanks[36] = new RankBuilder("[2][premium]", 175, 25, 6, 2); //24
		listOfRanks[37] = new RankBuilder("[0][ultra]", 45, 5, 3, 0); //33
		listOfRanks[38] = new RankBuilder("[1][ultra]", 245, 25, 8, 1); //34
		listOfRanks[39] = new RankBuilder("[2][ultra]", 595, 60, 13, 2); //35
		listOfRanks[40] = new RankBuilder("[3][ultra]", 1145, 115, 18, 3); //36
		listOfRanks[41] = new RankBuilder("[0][super]", 145, 15, 8, 0); //44
		listOfRanks[42] = new RankBuilder("[1][super]", 795, 80, 18, 1); //45
		listOfRanks[43] = new RankBuilder("[2][super]", 2095, 200, 28, 2); //46
		listOfRanks[44] = new RankBuilder("[3][super]", 3745, 375, 38, 3); //47
		listOfRanks[45] = new RankBuilder("[8][plus]", 595, 115, 16, 8); //19
		listOfRanks[46] = new RankBuilder("[8][premium]", 1725, 345, 21, 8); //30
		listOfRanks[47] = new RankBuilder("[9][ultra]", 7495, 750, 48, 9); //42
		listOfRanks[48] = new RankBuilder("[9][super]", 25495, 2550, 98, 9); //53
		listOfRanks[49] = new RankBuilder("[9][plus]", 745, 145, 18, 9); //20
		listOfRanks[50] = new RankBuilder("[10][plus]", 945, 200, 20, 10); //21
		listOfRanks[51] = new RankBuilder("[9][premium]", 2145, 425, 24, 9); //31
		listOfRanks[52] = new RankBuilder("[10][premium]", 2595, 550, 26, 10); //32
		listOfRanks[53] = new RankBuilder("[10][ultra]", 9195, 900, 53, 10); //43
		listOfRanks[54] = new RankBuilder("[10][super]", 30745, 3000, 108, 10); //54
		listOfRanks[55] = new RankBuilder("[4][blockminer]", 15995, 950, 98, 4); //59
		listOfRanks[56] = new RankBuilder("[5][blockminer]", 23495, 1400, 118, 5); //60
		listOfRanks[57] = new RankBuilder("[6][blockminer]", 32495, 1950, 138, 6); //61
		listOfRanks[58] = new RankBuilder("[7][blockminer]", 42064, 2525, 158, 7); //62
		listOfRanks[59] = new RankBuilder("[0][blockminer]", 495, 30, 18, 0); //55
		listOfRanks[60] = new RankBuilder("[1][blockminer]", 2245, 140, 38, 1); //56
		listOfRanks[61] = new RankBuilder("[2][blockminer]", 5495, 325, 58, 2); //57
		listOfRanks[62] = new RankBuilder("[3][blockminer]", 9995, 600, 78, 3); //58
		listOfRanks[63] = new RankBuilder("[8][blockminer]", 54495, 3200, 178, 8); //63
		listOfRanks[64] = new RankBuilder("[9][blockminer]", 64995, 3900, 198, 9); //64
		listOfRanks[65] = new RankBuilder("[10][blockminer]", 79995, 4800, 218, 10); //65
		listOfRanks[66] = new RankBuilder("admin", 79995, 4800, 218, 10); //66
	}

}
