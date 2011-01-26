package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

public class ConfigCommonModule extends ConfigItem {
	private SharedPreferences m_preferences;
	private String m_title;
	private String m_key;
	private CharSequence m_noModule;
	private CharSequence m_applying;

	public ConfigCommonModule(Context context, int number) {
		m_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		m_title = context.getString(R.string.description_module_common, number);
		m_key = String.format("vocal%d", number);
		m_noModule = context.getText(R.string.summary_no_module);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		CharSequence summary = null;
		if (inProgress()) {
			summary = m_applying;
		}
		else {
			Module module = DdN.getModule(m_preferences.getString(m_key, null));
			if (module != null) {
				summary = module.name;
			}
			else {
				summary = m_noModule;
			}
		}
		setText(view, R.id.summary, summary);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		Intent intent = new Intent(context, ModuleListActivity.class);
		intent.putExtra("request", 0);
		intent.putExtra("part", 0);
		return intent;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		String module_id = data.getStringExtra("vocal0");
		service.setCommonModule(m_key, module_id);
		m_preferences.edit().putString(m_key, module_id).commit();
		return Boolean.TRUE;
	}
}
