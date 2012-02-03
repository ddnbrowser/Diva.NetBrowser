package net.diva.browser.util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.MusicInfo;
import android.content.res.Resources;
/**
 *
 * @author silvia
 *
 */
public class DdNUtil {

	private static Map<String, Integer> m_difficulty;
	private static Map<String, Integer> m_clear_status;
	private static Map<String, Integer> m_trials;
	private static Map<String, Integer> m_trial_results;

	public static void init(Resources res){
		String[] diffNames = res.getStringArray(R.array.difficulty_names);
		int[] diffIds = res.getIntArray(R.array.difficulty_ids);
		m_difficulty = new HashMap<String, Integer>(diffNames.length, 1.0f);
		for(int i = 0; i < diffNames.length; i++){
			m_difficulty.put(diffNames[i], diffIds[i]);
		}
		String[] cleStaNames = res.getStringArray(R.array.clear_status_names);
		int[] cleStaIds = res.getIntArray(R.array.clear_status_ids);
		m_clear_status = new HashMap<String, Integer>(cleStaNames.length, 1.0f);
		for(int i = 0; i < cleStaNames.length; i++){
			m_clear_status.put(cleStaNames[i], cleStaIds[i]);
		}
		String[] triNames = res.getStringArray(R.array.trial_names);
		int[] triIds = res.getIntArray(R.array.trial_ids);
		m_trials = new HashMap<String, Integer>(triNames.length, 1.0f);
		for(int i = 0; i < triNames.length; i++){
			m_trials.put(triNames[i], triIds[i]);
		}
		String[] triResNames = res.getStringArray(R.array.trial_result_names);
		int[] triResIds = res.getIntArray(R.array.trial_result_ids);
		m_trial_results = new HashMap<String, Integer>(triResNames.length, 1.0f);
		for(int i = 0; i < triResNames.length; i++){
			m_trial_results.put(triResNames[i], triResIds[i]);
		}
	}

	public static String now(){
		return String.valueOf(Calendar.getInstance().getTimeInMillis() / 1000);
	}

	public static int getDifficultyCord(String name){
		return getCord(m_difficulty, name);
	}

	public static String getDifficultyName(int cord){
		return getName(m_difficulty, cord);
	}

	public static int getClearStatusCord(String name){
		return getCord(m_clear_status, name);
	}

	public static String getClearStatusName(int cord){
		return getName(m_clear_status, cord);
	}

	public static int getTrialsCord(String name){
		return getCord(m_trials, name);
	}

	public static String getTrialsName(int cord){
		return getName(m_trials, cord);
	}

	public static int getTrialResultsCord(String name){
		return getCord(m_trial_results, name);
	}

	public static String getTrialResultsName(int cord){
		return getName(m_trial_results, cord);
	}

	public static String getMusicTitle(String music_id){
		for(MusicInfo m : DdN.getPlayRecord().musics){
			if(m.id.equals(music_id))
				return m.title;
		}
		return "";
	}

	private static int getCord(Map<String, Integer> target, String name){
		if(target.containsKey(name))
			return target.get(name);
		else
			return 0;
	}

	private static String getName(Map<String, Integer> target, int cord){
		Iterator<String> ite = target.keySet().iterator();
		while(ite.hasNext()){
			String key = ite.next();
			if(target.get(key) == cord)
				return key;
		}
		return "";
	}

}
