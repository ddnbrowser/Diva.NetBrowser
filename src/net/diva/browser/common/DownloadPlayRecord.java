/**
 *
 */
package net.diva.browser.common;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.DdN.Account;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.NoLoginException;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.util.StringUtils;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class DownloadPlayRecord extends AsyncTask<DdN.Account, Integer, PlayRecord> {
	private Context m_context;
	private ProgressDialog m_progress;

	public DownloadPlayRecord(Context context) {
		m_context = context;
	}

	private List<MusicInfo> mergeMusicList(List<MusicInfo> newMusics) {
		PlayRecord record = DdN.getPlayRecord();
		if (record == null)
			return newMusics;

		List<MusicInfo> musics = new ArrayList<MusicInfo>(record.musics);
		for (MusicInfo music: newMusics) {
			int index = musics.indexOf(music);
			if (index < 0)
				musics.add(music);
			else
				musics.get(index).title = music.title;
		}

		return musics;
	}

	@Override
	protected void onPreExecute() {
		m_progress = new ProgressDialog(m_context);
		m_progress.setMessage(m_context.getString(R.string.message_downloading));
		m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		m_progress.setIndeterminate(true);
		m_progress.show();
	}

	@Override
	protected PlayRecord doInBackground(DdN.Account... args) {
		final Account account = args[0];
		ServiceClient service = DdN.getServiceClient(account);
		LocalStore store = DdN.getLocalStore();
		try {
			PlayRecord record = service.login();
			record.musics = mergeMusicList(service.getMusics());
			publishProgress(0, record.musics.size());

			for (MusicInfo music: record.musics) {
				if (music.reading == null)
					music.reading = store.getReading(music.title);
				if (music.ordinal == null)
					music.ordinal = StringUtils.forLexicographical(music.reading);
				service.update(music);
				service.cacheContent(music.coverart, music.getCoverArtPath(m_context));
				publishProgress(1);
			}

			store.insert(record);

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(m_context);
			SharedPreferences.Editor editor = preferences.edit();
			account.putTo(editor);
			DdN.setUpdateTime(editor, record.musics.size());
			editor.commit();
			return record;
		}
		catch (LoginFailedException e) {
			e.printStackTrace();
		}
		catch (NoLoginException e) {
			assert(false);
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (values.length > 1) {
			m_progress.setIndeterminate(false);
			m_progress.setMax(values[1]);
		}
		m_progress.incrementProgressBy(values[0]);
	}

	@Override
	protected void onPostExecute(PlayRecord result) {
		if (result != null)
			DdN.setPlayRecord(result);
		m_progress.dismiss();
	}
}