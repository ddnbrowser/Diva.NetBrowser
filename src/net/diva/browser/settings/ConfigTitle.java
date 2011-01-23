package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigTitle extends ConfigItem {
	private CharSequence m_title;
	private CharSequence m_applying;

	public ConfigTitle(Context context) {
		m_title = context.getText(R.string.description_title);
		m_applying = context.getText(R.string.summary_applying);
	}

	@Override
	public boolean isCategory() {
		return false;
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, getCurrentTitle());
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		Intent intent = new Intent(context, TitleListActivity.class);
		intent.putExtra("title_id", DdN.getPlayRecord().title_id);
		return intent;
	}

	private CharSequence getCurrentTitle() {
		if (inProgress())
			return m_applying;

		PlayRecord record = DdN.getPlayRecord();
		return record == null ? null : DdN.getTitle(record.title_id);
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		String title_id = data.getStringExtra("title_id");
		service.setTitle(title_id);
		PlayRecord record = DdN.getPlayRecord();
		record.title_id = title_id;
		store.update(record);
		return Boolean.TRUE;
	}
}
