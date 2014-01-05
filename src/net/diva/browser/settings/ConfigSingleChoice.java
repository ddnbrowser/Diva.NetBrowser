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

public class ConfigSingleChoice extends ConfigItem {
	private SharedPreferences m_preferences;
	private CharSequence[] m_names;
	protected String m_key;
	protected int m_value;

	private CharSequence m_title;
	private CharSequence m_applying;

	public ConfigSingleChoice(Context context, String key, int names, int fallback, int title) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		m_key = key;
		m_names = context.getResources().getTextArray(names);
		m_value = m_preferences.getInt(m_key, fallback);

		m_title = context.getText(title);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_names[m_value]);
	}

	@Override
	public Intent dispatch(Context context, final Callback callback) {
		AlertDialog.Builder b = new AlertDialog.Builder(context);
		b.setTitle(m_title);
		b.setSingleChoiceItems(m_names, m_value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				m_value = which;
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
		editor.putInt(m_key, m_value);
		editor.commit();
	}
}
