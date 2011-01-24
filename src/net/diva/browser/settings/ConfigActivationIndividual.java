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
	private static final String KEY = "activation_individual";

	private SharedPreferences m_preferences;

	private CharSequence m_title;
	private CharSequence m_summaryOn;
	private CharSequence m_summaryOff;
	private CharSequence m_applying;

	public ConfigActivationIndividual(Context context) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);

		m_title = context.getText(R.string.description_activation_individual);
		m_summaryOn = context.getText(R.string.summary_activation_individual_on);
		m_summaryOff = context.getText(R.string.summary_activation_individual_off);
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
		boolean isEnable = m_preferences.getBoolean(KEY, true);

		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : isEnable ? m_summaryOn : m_summaryOff);

		CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox);
		if (cb != null)
			cb.setChecked(isEnable);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		onResult(Activity.RESULT_OK, null, callback);
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		boolean on = !m_preferences.getBoolean(KEY, true);
		service.activateIndividualModules(on);
		m_preferences.edit().putBoolean(KEY, on).commit();
		return Boolean.TRUE;
	}

}
