package net.diva.browser.model;

public class ScoreRecord {
	public int difficulty;
	public int clear_status;
	public int trial_status;
	public int high_score;
	public int achievement;
	public int ranking;

	public boolean isRankIn() {
		return ranking > 0;
	}
}
