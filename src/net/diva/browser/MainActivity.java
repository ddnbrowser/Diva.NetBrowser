package net.diva.browser;

import net.diva.browser.common.DownloadPlayRecord;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TabHost;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Context context = getApplicationContext();
		final Resources res = getResources();

		String[] tags = res.getStringArray(R.array.tab_tags);
		String[] names = res.getStringArray(R.array.tab_names);
		TypedArray icons = res.obtainTypedArray(R.array.tab_icons);
		Intent[] intents = {
				new Intent(context, MusicListActivity.class),
				new Intent(context, MusicListActivity.class).putExtra("is_favorite", true),
		};

		TabHost host = getTabHost();
		for (int i = 0; i < tags.length; ++i) {
			TabHost.TabSpec tab = host.newTabSpec(tags[i]);
			tab.setIndicator(names[i], icons.getDrawable(i));
			tab.setContent(intents[i]);
			host.addTab(tab);
		}
		icons.recycle();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		host.setCurrentTabByTag(prefs.getString("default_tab", tags[0]));

		DdN.Account account = DdN.Account.load(prefs);
		if (account == null)
			DdN.Account.input(this, new DownloadPlayRecord(this));
	}

	@Override
	protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
		super.onChildTitleChanged(childActivity, title);
		setTitle(title);
	}

	public void onTabChanged(String tagId) {
		CharSequence title = getCurrentActivity().getTitle();
		if (title != null)
			setTitle(title);
	}
}
