package net.diva.browser.settings;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ConfigCategory extends ConfigItem {
	private CharSequence m_title;

	public ConfigCategory(CharSequence title) {
		m_title = title;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	public View onCreateView(Context context, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		return inflater.inflate(android.R.layout.preference_category, parent, false);
	}

	@Override
	public void setContent(View view) {
		setText(view, android.R.id.title, m_title);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		return null;
	}
}
