package net.diva.browser.model;

/**
 *
 * @author silvia
 *
 */
public class History {
	public String music_title;
	public int rank;
	public long play_date;
	public String play_place;
	public int clear_status;
	public int achievement;
	public int score;
	public int cool;
	public int cool_rate;
	public int fine;
	public int fine_rate;
	public int safe;
	public int safe_rate;
	public int sad;
	public int sad_rate;
	public int worst;
	public int worst_rate;
	public int combo;
	public int challange_time;
	public int hold;
	public int slide;
	public int trial;
	public int trial_result;
	public int pv_fork;
	public Module module1;
	public Module module2;
	public Module module3;
	public String se_button;
	public String se_slide;
	public String se_chain;
	public String se_touch;
	public String skin;
	public int lock;

	public boolean isLocked(){
		return lock == 1;
	}

	public void setLocked(boolean on) {
		lock = on ? 1 : 0;
	}

	public static class Module {
		public String base;
		public String head;
		public String face;
		public String front;
		public String back;
	}
}
