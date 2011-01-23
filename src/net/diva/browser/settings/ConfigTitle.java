package net.diva.browser.settings;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.PlayRecord;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigTitle extends ConfigItem {
	private Context m_context;

	public ConfigTitle(Context context) {
		m_context = context;
	}

	@Override
	public boolean isCategory() {
		return false;
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_context.getText(R.string.description_title));
		setText(view, R.id.summary, getCurrentTitle());
	}

	@Override
	public Intent dispatch() {
		Intent intent = new Intent(m_context, TitleListActivity.class);
		intent.putExtra("title_id", DdN.getPlayRecord().title_id);
		return intent;
	}

	@Override
	public boolean onResult(int result, Intent data) {
		return result == Activity.RESULT_OK;
	}

	private String getCurrentTitle() {
		PlayRecord record = DdN.getPlayRecord();
		return record == null ? null : DdN.getTitle(record.title_id);
	}
}
