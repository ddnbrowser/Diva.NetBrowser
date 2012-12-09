package net.diva.browser.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.History;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.service.ParseException;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
/**
 *
 * @author silvia
 *
 */
public class UpdateHistory {
	private Context m_context;
	private SharedPreferences m_preferences;
	private UpdateHistoryTask task;
	private boolean manual = false;

	public UpdateHistory(Context context){
		m_context = context;
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public void setTask (UpdateHistoryTask task){
		this.task = task;
		manual = true;
	}

	public boolean update(ServiceClient service) throws IOException, ParseException {
		List<String> newHistorys = new ArrayList<String>();
		List<String> ids = new ArrayList<String>();
		long lastPlayedTime = m_preferences.getLong("last_played_use_history", 0);
		long lastPlayedScore = m_preferences.getLong("last_played_use_history_score", 0);
		long[] lastPlayed = service.getHistory(newHistorys, ids, lastPlayedTime, lastPlayedScore);
		boolean hasItem = newHistorys.size() > 0;

		if(hasItem){
			if(manual)
				task.myPublishProgress(0, newHistorys.size());
			for(String historyId : newHistorys){
				History h = null;
				try {
					h = service.getHistoryDetail(historyId);
				} catch(Exception e){
				}

				DdN.getLocalStore().insert(h);
				if(manual)
					task.myPublishProgress(1);
			}

			for(String musicId: ids){
				MusicInfo m = DdN.getPlayRecord().getMusic(musicId);
				try{
					service.update(m);
					LocalStore.instance(m_context).update(m);
				}catch(Exception e){
				}
			}

			m_preferences.edit().putLong("last_played_use_history", lastPlayed[0]).commit();
			m_preferences.edit().putLong("last_played_use_history_score", lastPlayed[1]).commit();
		}
		m_preferences.edit().putLong("history_last_download_time", System.currentTimeMillis()).commit();

		if(m_preferences.getBoolean("download_history", false)){
			if(hasItem || manual){
				DownloadHistoryService.forceReserve(m_context);
			}else{
				DownloadHistoryService.autoReserve(m_context);
			}
		}

		return hasItem;
	}

}
