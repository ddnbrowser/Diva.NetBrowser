package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener, DdN.Observer {
	private class TabHolder {
		int myListId;
		TextView title;
		ImageView icon;

		TabHolder(int id, View view) {
			myListId = id;
			title = (TextView)view.findViewById(android.R.id.title);
			icon = (ImageView)view.findViewById(android.R.id.icon);
		}
	}

	private CharSequence m_defaultTitle;
	private List<TabHolder> m_myListTabs = new ArrayList<TabHolder>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		m_defaultTitle = getTitle();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("fix_sort_order", false)) {
			String order = preferences.getString("initial_sort_order", null);
			if (order != null) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("sort_order", SortOrder.valueOf(order).ordinal());
				editor.putBoolean("reverse_order", preferences.getBoolean("initial_reverse_order", false));
				editor.commit();
			}
		}

		TabHost host = getTabHost();
		host.setOnTabChangedListener(this);
		TabWidget widget = getTabWidget();

		addFixedTabs(host);
		addMyListTabs(host, widget);

		final int width = getResources().getDimensionPixelSize(R.dimen.tab_width);
		for (int i = 0; i < widget.getTabCount(); ++i)
			widget.getChildTabViewAt(i).getLayoutParams().width = width;

		final String tag = preferences.getString("default_tab", null);
		if (tag != null)
			host.setCurrentTabByTag(tag);

		DdN.Account account = DdN.Account.load(preferences);
		if (account == null)
			DdN.Account.input(this, new DownloadPlayRecord(this));
	}

	@Override
	protected void onResume() {
		super.onResume();
		DdN.registerObserver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		DdN.unregisterObserver(this);
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

	public void onUpdate(PlayRecord record, boolean noMusic) {
		// do nothing.
	}

	public void onUpdate(MyList myList, boolean noMusic) {
		final int active = DdN.getLocalStore().getActiveMyList();
		for (TabHolder holder: m_myListTabs) {
			if (holder.myListId == myList.id)
				holder.title.setText(myList.name);
			holder.icon.setImageDrawable(getMyListIcon(holder.myListId == active));
		}
	}

	private void addFixedTabs(TabHost host) {
		final Context context = getApplicationContext();
		final Resources resources = getResources();

		String[] tags = resources.getStringArray(R.array.tab_tags);
		String[] names = resources.getStringArray(R.array.tab_names);
		TypedArray icons = resources.obtainTypedArray(R.array.tab_icons);
		Intent[] intents = {
				new Intent(context, InformationActivity.class),
				new Intent(context, AllMusicActivity.class),
		};

		for (int i = 0; i < tags.length; ++i) {
			TabHost.TabSpec tab = host.newTabSpec(tags[i]);
			tab.setIndicator(names[i], icons.getDrawable(i));
			tab.setContent(intents[i]);
			host.addTab(tab);
		}
		icons.recycle();
	}

	private void addMyListTabs(TabHost host, TabWidget widget) {
		final LocalStore store = DdN.getLocalStore();
		final int active = store.getActiveMyList();
		for (MyList mylist: store.loadMyLists()) {
			final Intent intent = new Intent(getApplicationContext(), MyListActivity.class);
			intent.putExtra("id", mylist.id);
			intent.putExtra("name", mylist.name);

			TabHost.TabSpec tab = host.newTabSpec(mylist.tag);
			tab.setIndicator(mylist.name, getMyListIcon(mylist.id == active));
			tab.setContent(intent);
			host.addTab(tab);

			m_myListTabs.add(new TabHolder(mylist.id, widget.getChildTabViewAt(widget.getTabCount()-1)));
		}
	}

	private Drawable getMyListIcon(boolean active) {
		return getResources().getDrawable(active ? R.drawable.checked_star : R.drawable.ic_menu_star);
	}
}
