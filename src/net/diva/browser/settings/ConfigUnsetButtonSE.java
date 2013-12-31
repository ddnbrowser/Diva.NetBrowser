package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.util.CodeMap;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigUnsetButtonSE extends ConfigItem {
	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;
	private int m_type;

	public ConfigUnsetButtonSE(Context context, int type) {
		m_type = type;
		CodeMap names = new CodeMap(context, R.array.sound_effects);
		m_title = context.getString(R.string.description_unset_buttonse, names.name(type));
		m_summary = context.getString(R.string.summary_unset_buttonse, names.name(type));
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_summary);
	}

	@Override
	public Intent dispatch(Context context, final Callback callback) {
		CodeMap names = new CodeMap(context, R.array.sound_effects);
		confirm(context, callback, m_title, context.getString(R.string.message_unset_buttonse, names.name(m_type)));
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.resetButtonSE("COMMON", m_type);
		return Boolean.TRUE;
	}
}
