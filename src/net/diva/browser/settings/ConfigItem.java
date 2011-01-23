package net.diva.browser.settings;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

public abstract class ConfigItem {
	public abstract boolean isCategory();
	public abstract void setContent(View view);
	public abstract Intent dispatch();

	public boolean onResult(int result, Intent data) {
		return false;
	}

	protected void setText(View view, int resId, CharSequence text) {
		TextView tv = (TextView)view.findViewById(resId);
		if (tv != null)
			tv.setText(text);
	}
}
