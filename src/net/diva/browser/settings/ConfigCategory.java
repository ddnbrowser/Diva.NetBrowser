package net.diva.browser.settings;

import android.content.Intent;
import android.view.View;

public class ConfigCategory extends ConfigItem {
	private CharSequence m_title;

	public ConfigCategory(CharSequence title) {
		m_title = title;
	}

	@Override
	public boolean isCategory() {
		return true;
	}

	@Override
	public void setContent(View view) {
		setText(view, android.R.id.title, m_title);
	}

	@Override
	public Intent dispatch() {
		return null;
	}
}
