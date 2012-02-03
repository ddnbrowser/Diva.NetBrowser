package net.diva.browser.model;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
/**
 *
 * @author silvia
 *
 */
public class History implements Serializable {

	private static final long serialVersionUID = -7599523144951319032L;

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy/MM/dd HH:mm");

	public String music_id;
	public int rank;
	public int play_date;
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

	public void setPlayDate(String play_date){
		Long date = 0L;
		try{
			date = DATE_FORMAT.parse(play_date).getTime() / 1000;
		} catch(ParseException e){
			e.printStackTrace();
		}

		this.play_date = date.intValue();
	}

	public String getPlayDateStr(){
		if(play_date == 0)
			return "日付不明";

		Calendar cal = Calendar.getInstance();

		cal.setTimeInMillis(((long)play_date) * 1000);
		return DATE_FORMAT.format(cal.getTime());
	}

	public boolean isLocked(){
		return lock == 1;
	}

}
