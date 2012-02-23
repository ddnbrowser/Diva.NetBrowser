package net.diva.browser.model;

/**
 *
 * @author silvia
 *
 */
public class History {
	public String music_id;
	public int rank;
	public long play_date;
	public String play_place;
	public int clear_status;
	public int achievement;
	public int score;
	public int cool;
	public int fine;
	public int safe;
	public int sad;
	public int worst;
	public int combo;
	public int challange_time;
	public int hold;
	public int trial;
	public int trial_result;
	public String module1;
	public String module2;
	public String button_se;
	public String skin;
	public int lock;

	public boolean isLocked(){
		return lock == 1;
	}

	public int totalNotes() {
		return cool + fine + safe + sad + worst;
	}
}
