package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.util.CodeMap;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigSetButtonSE extends ConfigItem {
	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;
	private int m_type;

	public ConfigSetButtonSE(Context context, int type) {
		m_type = type;
		CodeMap names = new CodeMap(context, R.array.sound_effects);
		m_title = context.getString(R.string.description_set_buttonse, names.name(type));
		m_summary = context.getString(R.string.summary_set_buttonse, names.name(type));
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_summary);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		Intent intent = new Intent(context, SEListActivity.class);
		intent.putExtra("type", m_type);
		intent.putExtra("enable_unset", true);
		return intent;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		final String id = data.getStringExtra("id");
		if (id !=null)
			service.setButtonSE("COMMON", m_type, id);
		else
			service.resetButtonSE("COMMON", m_type);
		return Boolean.TRUE;
	}
}
