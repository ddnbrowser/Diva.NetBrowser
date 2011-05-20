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
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, getCurrentTitle());
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		return new Intent(context, TitleListActivity.class);
	}

	private CharSequence getCurrentTitle() {
		if (inProgress())
			return m_applying;

		PlayRecord record = DdN.getPlayRecord();
		return record == null ? null : record.title;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.setTitle(data.getStringExtra("title_id"));
		PlayRecord record = DdN.getPlayRecord();
		record.title = DdN.getTitle(data.getStringExtra("image_id"));
		store.update(record);
		return Boolean.TRUE;
	}
}
