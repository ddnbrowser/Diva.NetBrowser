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
	public int cool_per;
	public int fine;
	public int fine_per;
	public int safe;
	public int safe_per;
	public int sad;
	public int sad_per;
	public int worst;
	public int worst_per;
	public int combo;
	public int challange_time;
	public int hold;
	public int trial;
	public int trial_result;
	public String module1_id;
	public String module2_id;
	public String se_id;
	public String skin_id;
	public int lock;

	public boolean isLocked(){
		return lock == 1;
	}

}
