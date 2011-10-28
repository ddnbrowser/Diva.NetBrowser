package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

public class MainActivity extends FragmentActivity
		implements TabHost.OnTabChangeListener, DdN.Observer, OnGlobalLayoutListener {
	private static final int TOOL_SETTINGS = 1;

	private class TabHolder {
		MyList myList;
		TextView title;
		ImageView icon;

		TabHolder(MyList myList_, View view) {
			myList = myList_;
			title = (TextView)view.findViewById(android.R.id.title);
			icon = (ImageView)view.findViewById(android.R.id.icon);
		}
	}

	private TabsAdapter m_adapter;
//	private CharSequence m_defaultTitle;
	private List<TabHolder> m_myListTabs = new ArrayList<TabHolder>();

	private TabHost getTabHost() {
		return (TabHost)findViewById(android.R.id.tabhost);
	}

	private TabWidget getTabWidget() {
		return (TabWidget)findViewById(android.R.id.tabs);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
//		m_defaultTitle = getTitle();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		TabHost host = getTabHost();
		host.setup();
		host.setOnTabChangedListener(this);
		TabWidget widget = getTabWidget();
		widget.getViewTreeObserver().addOnGlobalLayoutListener(this);

		m_adapter = new TabsAdapter(this, host, (ViewPager)findViewById(R.id.pager));
		addTabs(host, m_adapter);

		final int width = getResources().getDimensionPixelSize(R.dimen.tab_width);
		for (int i = 0; i < widget.getTabCount(); ++i)
			widget.getChildTabViewAt(i).getLayoutParams().width = width;

		final String tag = getTag(preferences);
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

	public void onGlobalLayout() {
		TabWidget widget = getTabWidget();
		View tab = widget.getChildTabViewAt(getTabHost().getCurrentTab());
		if (tab == null)
			return;
		View base = findViewById(R.id.tabbase);
		base.scrollTo(tab.getRight() - base.getWidth(), 0);

		widget.getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	@Override
	protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
		super.onChildTitleChanged(childActivity, title);
		setTitle(title);
	}

	public void onTabChanged(String tagId) {
//		CharSequence title = getCurrentActivity().getTitle();
//		setTitle(title != null ? title : m_defaultTitle);
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
		case R.id.item_vp_history:
			WebBrowseActivity.open(this, "/divanet/personal/vpHistory/");
			break;
		case R.id.item_play_history:
			WebBrowseActivity.open(this, "/divanet/personal/playHistory/0");
			break;
		case R.id.item_contest:
			WebBrowseActivity.open(this, "/divanet/contest/info/");
			break;
		case R.id.item_statistics:
			WebBrowseActivity.open(this, "/divanet/pv/statistics/");
			break;
		case R.id.item_ranking_list:
			WebBrowseActivity.open(this, "/divanet/ranking/list/0");
			break;
		case R.id.item_game_settings: {
			Intent intent = new Intent(getApplicationContext(), CommonConfigActivity.class);
			startActivity(intent);
		}
			break;
		case R.id.item_tool_settings: {
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, TOOL_SETTINGS);
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
		case TOOL_SETTINGS:
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
			if (holder.myList.id == myList.id) {
				holder.myList = myList;
				holder.title.setText(myList.name);
			}
			holder.icon.setImageDrawable(getMyListIcon(holder.myList.id == active));
		}
	}

	private void addTabs(TabHost host, TabsAdapter adapter) {
		final Resources resources = getResources();

		String[] tags = resources.getStringArray(R.array.tab_tags);
		String[] names = resources.getStringArray(R.array.tab_names);
		TypedArray icons = resources.obtainTypedArray(R.array.tab_icons);
		String[] classes = resources.getStringArray(R.array.tab_classes);

		for (int i = 0; i < tags.length; ++i) {
			final String tag = tags[i];
			if ("mylist".equals(tag)) {
				addMyListTabs(host, adapter, classes[i]);
				continue;
			}
			Bundle args = new Bundle();
			args.putString("tag", tag);
			TabHost.TabSpec tab = host.newTabSpec(tag);
			tab.setIndicator(names[i], icons.getDrawable(i));
			adapter.addTab(tab, classes[i], args);
		}
		icons.recycle();
	}

	private void addMyListTabs(TabHost host, TabsAdapter adapter, String klass) {
		TabWidget widget = getTabWidget();
		final LocalStore store = DdN.getLocalStore();
		final int active = store.getActiveMyList();
		for (MyList mylist: store.loadMyLists()) {
			Bundle args = new Bundle(3);
			args.putInt("id", mylist.id);
			args.putString("name", mylist.name);
			args.putString("tag", mylist.tag);

			TabHost.TabSpec tab = host.newTabSpec(mylist.tag);
			tab.setIndicator(mylist.name, getMyListIcon(mylist.id == active));
			adapter.addTab(tab, klass, args);

			m_myListTabs.add(new TabHolder(mylist, widget.getChildTabViewAt(widget.getTabCount()-1)));
		}
	}

	private Drawable getMyListIcon(boolean active) {
		return getResources().getDrawable(active ? R.drawable.ic_tab_mylist_checked : R.drawable.ic_tab_mylist);
	}

	private String getTag(SharedPreferences preferences) {
		final String tag = preferences.getString("default_tab", null);
		if (tag == null || !tag.equals("mylist"))
			return tag;

		final int active = DdN.getLocalStore().getActiveMyList();
		for (TabHolder holder: m_myListTabs) {
			if (holder.myList.id == active)
				return holder.myList.tag;
		}

		return null;
	}

	private static class TabsAdapter extends FragmentPagerAdapter
			implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
		static class Page {
			String klass;
			Bundle args;

			Page(String klass_, Bundle args_) {
				klass = klass_;
				args = args_;
			}

			Fragment instantiate(Context context) {
				return Fragment.instantiate(context, klass, args);
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			Context m_context;

			DummyTabFactory(Context context) {
				m_context = context;
			}

			@Override
			public View createTabContent(String tag) {
				View view = new View(m_context);
				view.setMinimumWidth(0);
				view.setMinimumHeight(0);
				return view;
			}
		}

		Context m_context;
		TabHost m_host;
		ViewPager m_pager;
		List<Page> m_pages = new ArrayList<Page>();

		TabsAdapter(FragmentActivity activity, TabHost host, ViewPager pager) {
			super(activity.getSupportFragmentManager());

			m_context = activity;
			m_host = host;
			m_host.setOnTabChangedListener(this);
			m_pager = pager;
			m_pager.setAdapter(this);
			m_pager.setOnPageChangeListener(this);
		}

		void addTab(TabHost.TabSpec spec, String klass, Bundle args) {
			spec.setContent(new DummyTabFactory(m_context));
			m_pages.add(new Page(klass, args));
			m_host.addTab(spec);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return m_pages.size();
		}

		@Override
		public Fragment getItem(int position) {
			return m_pages.get(position).instantiate(m_context);
		}

		@Override
		public void onTabChanged(String tabId) {
			m_pager.setCurrentItem(m_host.getCurrentTab(), false);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			m_host.setCurrentTab(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	}
}
