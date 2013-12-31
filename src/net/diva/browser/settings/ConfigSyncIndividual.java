package net.diva.browser.settings;

import net.diva.browser.DdN;
import net.diva.browser.DdNIndex;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigSyncIndividual extends ConfigItem {
	private Context m_context;

	private CharSequence m_title;
	private CharSequence m_summary;

	public ConfigSyncIndividual(Context context) {
		m_context = context;
		m_title = context.getText(R.string.description_sync_individual);
		m_summary = context.getText(R.string.summary_sync_individual);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, m_summary);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		confirm(context, callback, R.string.description_sync_individual, R.string.message_sync_individual);
		return null;
	}

	@Override
	public void onResult(int result, Intent data, Callback callback) {
		if (result == Activity.RESULT_OK)
			new SyncTask(m_context, callback).execute();
	}

	private class SyncTask extends ServiceTask<Void, Void, Boolean> {
		Callback m_callback;

		public SyncTask(Context context, Callback callback) {
			super(context, R.string.message_updating);
			m_callback = callback;
		}

		@Override
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			LocalStore store = DdN.getLocalStore();

			PlayRecord record = DdN.getPlayRecord();
			service.updateIndividualSettings(record.musics, new DdNIndex(m_context));
			store.updateIndividual(record.musics);
			DdN.setPlayRecord(record);

			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			m_callback.onUpdated();
		}
	}
}
