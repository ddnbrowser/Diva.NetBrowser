package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigSetSkin extends ConfigItem {
	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	public ConfigSetSkin(Context context) {
		m_title = context.getText(R.string.description_set_skin);
		m_summary = context.getText(R.string.summary_set_skin);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_summary);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		return new Intent(context, SkinListActivity.class);
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.setSkin(data.getStringExtra("group_id"), data.getStringExtra("id"));
		return Boolean.TRUE;
	}
}
