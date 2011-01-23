package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

public class ConfigResetCommon extends ConfigItem {
	private SharedPreferences m_preferences;

	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	public ConfigResetCommon(Context context) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);

		m_title = context.getText(R.string.description_reset_common);
		m_summary = context.getText(R.string.summary_reset_common);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public boolean isCategory() {
		return false;
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_summary);
	}

	@Override
	public Intent dispatch(Context context, final Callback callback) {
		confirm(context, callback, R.string.description_reset_common, R.string.message_reset_common);
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.resetCommonModules();
		m_preferences.edit().remove("vocal1").remove("vocal2").commit();
		return Boolean.TRUE;
	}
}
