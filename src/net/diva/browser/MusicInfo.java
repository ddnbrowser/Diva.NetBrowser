package net.diva.browser;


public class MusicInfo {
	private static final int[] STATUS_POINTS = new int[] { 0, 1, 4, 6 };
	private static final int[] DIFFICULTY_POINTS = new int[] { 1, 2, 5, 7 };

	public String id;
	public String title;
	public String coverart;
	public ScoreRecord[] records;

	public MusicInfo(String id_, String title_) {
		id = id_;
		title = title_;
		records = new ScoreRecord[4];
	}

	public int rankPoint() {
		int max = 0;
		for (int difficulty = 0; difficulty < records.length; ++difficulty) {
			ScoreRecord score = records[difficulty];
			if (score == null)
				continue;
			int point = DIFFICULTY_POINTS[difficulty] * STATUS_POINTS[score.clear_status];
			if (point > max)
				max = point;
		}
		return max;
	}
}
