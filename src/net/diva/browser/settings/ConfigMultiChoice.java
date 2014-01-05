package net.diva.browser.settings;

import net.diva.browser.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

public class ConfigMultiChoice extends ConfigItem {
	private SharedPreferences m_preferences;
	private int m_names;
	protected String[] m_keys;
	protected boolean[] m_values;

	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	public ConfigMultiChoice(Context context, int keys, int names, boolean fallback, int title, int summary) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		m_keys = context.getResources().getStringArray(keys);
		m_names = names;
		m_values = new boolean[m_keys.length];
		for (int i = 0; i < m_keys.length; ++i)
			m_values[i] = m_preferences.getBoolean(m_keys[i], fallback);

		m_title = context.getText(title);
		m_summary = context.getText(summary);
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
		b.setMultiChoiceItems(m_names, m_values, new DialogInterface.OnMultiChoiceClickListener() {
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

	protected void saveToLocal() {
		SharedPreferences.Editor editor = m_preferences.edit();
		for (int i = 0; i < m_keys.length; ++i)
			editor.putBoolean(m_keys[i], m_values[i]);
		editor.commit();
	}
}
