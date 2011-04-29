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

public class ConfigActivationIndividual extends ConfigItem {
	private static final String[] KEYS = { "usePvModuleEquip", "usePvSkinEquip", "usePvButtonSeEquip" };

	private SharedPreferences m_preferences;

	private int m_key;
	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	public ConfigActivationIndividual(Context context, int key, int titleId, int summaryId) {
		m_key = key;
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);

		m_title = context.getText(titleId);
		m_summary = context.getText(summaryId);
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
			cb.setChecked(m_preferences.getBoolean(KEYS[m_key], true));
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		onResult(Activity.RESULT_OK, null, callback);
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		boolean[] values = new boolean[KEYS.length];
		for (int i = 0; i < KEYS.length; ++i)
			values[i] = m_preferences.getBoolean(KEYS[i], true);
		values[m_key] = !values[m_key];
		service.activateIndividual(KEYS, values);
		m_preferences.edit().putBoolean(KEYS[m_key], values[m_key]).commit();
		return Boolean.TRUE;
	}
}
