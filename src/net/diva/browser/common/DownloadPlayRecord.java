/**
 *
 */
package net.diva.browser.common;

import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.DdNIndex;
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

		List<MusicInfo> musics = record.musics;
		final int size = newMusics.size();
		for (int i = 0; i < size; ++i) {
			MusicInfo music = newMusics.get(i);
			int index = musics.indexOf(music);
			if (index >= 0) {
				MusicInfo old = musics.get(index);
				old.title = music.title;
				newMusics.set(i, old);
			}
		}

		return newMusics;
	}

	private boolean hasNoPublishOrder(List<MusicInfo> musics) {
		for (MusicInfo m: musics) {
			if (m.publish_order < 0)
				return true;
		}
		return false;
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
			List<MusicInfo> musics = record.musics = mergeMusicList(service.getMusics());
			if (hasNoPublishOrder(musics))
				service.updatePublishOrder(musics, record.nextPublishOrder());
			publishProgress(0, musics.size());

			DdNIndex index = new DdNIndex(m_context);
			for (MusicInfo music: musics) {
				if (music.reading == null)
					music.reading = store.getReading(music.title);
				if (music.ordinal == null)
					music.ordinal = StringUtils.forLexicographical(music.reading);
				service.update(music);
				service.cacheContent(music.coverart, music.getCoverArtPath(m_context));
				if (music.role1 == null) {
					service.updateRoles(music, index);
					service.updateIndividualSE(music, index);
				}
				publishProgress(1);
			}

			store.insert(record);

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(m_context);
			SharedPreferences.Editor editor = preferences.edit();
			account.putTo(editor);
			DdN.setUpdateTime(editor, musics.size());
			editor.putLong("last_played", System.currentTimeMillis());
			editor.commit();
			return record;
		}
		catch (LoginFailedException e) {
			e.printStackTrace();
		}
		catch (NoLoginException e) {
			assert(false);
		}
		catch (IOException e) {
			e.printStackTrace();
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