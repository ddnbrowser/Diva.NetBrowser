package net.diva.browser;

import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.model.MyList;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabWidget;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
	private CharSequence m_defaultTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		m_defaultTitle = getTitle();

		final Context context = getApplicationContext();
		final Resources res = getResources();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences.getBoolean("fix_sort_order", false)) {
			String order = preferences.getString("initial_sort_order", null);
			if (order != null) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("sort_order", SortOrder.valueOf(order).ordinal());
				editor.putBoolean("reverse_order", preferences.getBoolean("initial_reverse_order", false));
				editor.commit();
			}
		}

		String[] tags = res.getStringArray(R.array.tab_tags);
		String[] names = res.getStringArray(R.array.tab_names);
		TypedArray icons = res.obtainTypedArray(R.array.tab_icons);
		Intent[] intents = {
				new Intent(context, InformationActivity.class),
				new Intent(context, MusicListActivity.class),
		};

		TabHost host = getTabHost();
		host.setOnTabChangedListener(this);
		for (int i = 0; i < tags.length; ++i) {
			TabHost.TabSpec tab = host.newTabSpec(tags[i]);
			tab.setIndicator(names[i], icons.getDrawable(i));
			tab.setContent(intents[i]);
			host.addTab(tab);
		}
		icons.recycle();

		for (MyList mylist: DdN.getLocalStore().loadMyLists()) {
			TabHost.TabSpec tab = host.newTabSpec(mylist.tag);
			tab.setIndicator(mylist.name, res.getDrawable(R.drawable.ic_menu_star));
			tab.setContent(new Intent(context, MyListActivity.class).putExtra("id", mylist.id));
			host.addTab(tab);
		}

		final int minWidth = 140;
		TabWidget widget = getTabWidget();
		for (int i = 0; i < widget.getTabCount(); ++i)
			widget.getChildTabViewAt(i).setMinimumWidth(minWidth);

		host.setCurrentTabByTag(preferences.getString("default_tab", tags[0]));

		DdN.Account account = DdN.Account.load(preferences);
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
		setTitle(title != null ? title : m_defaultTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.item_news:
			WebBrowseActivity.open(this, "/divanet/menu/news/");
			break;
		case R.id.item_contest:
			WebBrowseActivity.open(this, "/divanet/contest/info/");
			break;
		case R.id.item_statistics:
			WebBrowseActivity.open(this, "/divanet/pv/statistics/");
			break;
		case R.id.item_game_settings: {
			Intent intent = new Intent(getApplicationContext(), CommonConfigActivity.class);
			startActivity(intent);
		}
			break;
		case R.id.item_tool_settings: {
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, R.id.item_tool_settings);
		}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_tool_settings:
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			if (preferences.getBoolean("download_rankin", false))
				DownloadRankingService.reserve(this);
			else
				DownloadRankingService.cancel(this);
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
