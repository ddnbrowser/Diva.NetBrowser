package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

public class ConfigInterimRanking extends ConfigItem {
	private static final String KEY = "dispInterimRanking";

	private SharedPreferences m_preferences;
	private boolean m_value;

	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	public ConfigInterimRanking(Context context) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		m_value = m_preferences.getBoolean(KEY, false);

		m_title = context.getText(R.string.description_interim_ranking);
		m_summary = context.getText(R.string.summary_interim_ranking);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public View onCreateView(Context context, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.setting_item, parent, false);
		ViewGroup widget = (ViewGroup)view.findViewById(R.id.widget_frame);
		inflater.inflate(R.layout.widget_checkbox, widget);
		return view;
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_summary);

		CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox);
		if (cb != null)
			cb.setChecked(m_value);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		onResult(Activity.RESULT_OK, null, callback);
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		boolean value = !m_value;
		service.setInterimRanking(value);
		m_preferences.edit().putBoolean(KEY, value).commit();
		m_value = value;
		return Boolean.TRUE;
	}
}
