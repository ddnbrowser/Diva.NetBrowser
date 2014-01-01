package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

public class ConfigActivationIndividual extends ConfigItem {
	private SharedPreferences m_preferences;
	private String[] m_keys;
	private boolean[] m_values;

	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	public ConfigActivationIndividual(Context context) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		m_keys = context.getResources().getStringArray(R.array.individual_setting_keys);
		m_values = new boolean[m_keys.length];
		for (int i = 0; i < m_keys.length; ++i)
			m_values[i] = m_preferences.getBoolean(m_keys[i], true);

		m_title = context.getText(R.string.category_activation_individual);
		m_summary = context.getText(R.string.summary_activation_individual);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_summary);
	}

	@Override
	public Intent dispatch(Context context, final Callback callback) {
		AlertDialog.Builder b = new AlertDialog.Builder(context);
		b.setTitle(m_title);
		b.setMultiChoiceItems(R.array.individual_setting_names, m_values, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				m_values[which] = isChecked;
			}
		});
		b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onResult(Activity.RESULT_OK, null, callback);
				dialog.dismiss();
			}
		});
		b.setNegativeButton(R.string.cancel, null);
		b.show();
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.activateIndividual(m_keys, m_values);
		SharedPreferences.Editor editor = m_preferences.edit();
		for (int i = 0; i < m_keys.length; ++i)
			editor.putBoolean(m_keys[i], m_values[i]);
		editor.commit();
		return Boolean.TRUE;
	}
}
