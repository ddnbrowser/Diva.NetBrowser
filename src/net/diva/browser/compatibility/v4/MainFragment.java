package net.diva.browser.compatibility.v4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.MainActivity;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.page.PageAdapter;
import android.app.Activity;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

public class MainFragment extends Fragment
		implements DdN.Observer, MainActivity.Content, OnGlobalLayoutListener {
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
	private List<TabHolder> m_myListTabs = new ArrayList<TabHolder>();

	private TabHost getTabHost(View view) {
		return (TabHost)view.findViewById(android.R.id.tabhost);
	}

	private TabWidget getTabWidget(View view) {
		return (TabWidget)view.findViewById(android.R.id.tabs);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.main_content, container, false);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		TabHost host = getTabHost(view);
		host.setup();
		TabWidget widget = getTabWidget(view);
		widget.getViewTreeObserver().addOnGlobalLayoutListener(this);

		m_adapter = new TabsAdapter(getActivity(), host, (ViewPager)view.findViewById(R.id.pager),
				widget, view.findViewById(R.id.tabbase));
		addTabs(host, widget, m_adapter, preferences);

		final int width = getResources().getDimensionPixelSize(R.dimen.tab_width);
		for (int i = 0; i < widget.getTabCount(); ++i)
			widget.getChildTabViewAt(i).getLayoutParams().width = width;

		final String tag = getTag(preferences);
		if (tag != null)
			host.setCurrentTabByTag(tag);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		m_adapter.updateTitle();
		DdN.registerObserver(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		DdN.unregisterObserver(this);
	}

	public void onGlobalLayout() {
		m_adapter.updateTitle();
		m_adapter.adjustTabPosition();

		getTabWidget(getView()).getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	@Override
	public boolean onSearchRequested() {
		TabsAdapter.Page p = m_adapter.getCurrentPage();
		if (p.adapter != null)
			return p.adapter.onSearchRequested();
		return true;
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		for (TabsAdapter.Page p: m_adapter.m_pages) {
			if (p.observer != null)
				p.observer.onUpdate(record, noMusic);
		}

		m_adapter.updateTitle();
	}

	public void onUpdate(MyList myList, boolean noMusic) {
		for (TabsAdapter.Page p: m_adapter.m_pages) {
			if (p.observer != null)
				p.observer.onUpdate(myList, noMusic);
		}

		for (TabHolder holder: m_myListTabs) {
			if (holder.myList.id == myList.id) {
				holder.myList = myList;
				holder.title.setText(myList.name);
			}
			int active = DdN.getLocalStore().getActiveMyList(holder.myList.id);
			holder.icon.setImageDrawable(getMyListIcon(active));
		}
		m_adapter.updateTitle();
	}

	private void addTabs(TabHost host, TabWidget widget, TabsAdapter adapter, SharedPreferences preferences) {
		final Resources resources = getResources();

		String[] tags = resources.getStringArray(R.array.tab_tags);
		String[] names = resources.getStringArray(R.array.tab_names);
		TypedArray icons = resources.obtainTypedArray(R.array.tab_icons);
		String[] classes = resources.getStringArray(R.array.tab_classes);

		Map<String, Integer> tagNumbering = new HashMap<String, Integer>();
		for(int i = 0; i < tags.length; i++)
			tagNumbering.put(tags[i], i);

		String customTabList = preferences.getString("customTabList", "");
		if (!"".equals(customTabList)) {
			String[] customTags = customTabList.split(",");
			if (customTags.length < 6) {
				String strTags = "";
				for (String tag : tags)
					strTags += tag + ",";
				strTags = strTags.substring(0, strTags.length() - 1);
				preferences.edit().putString("customTabList", strTags).commit();
			} else {
				tags = customTags;
			}
		}

		for (int i = 0; i < tags.length; ++i) {
			final String tag = tags[i];
			final int tagNumber = tagNumbering.get(tag);
			if ("mylist".equals(tag)) {
				addMyListTabs(host, widget, adapter, classes[tagNumber]);
				continue;
			}
			Bundle args = new Bundle();
			args.putString("tag", tag);
			TabHost.TabSpec tab = host.newTabSpec(tag);
			tab.setIndicator(names[tagNumber], icons.getDrawable(tagNumber));
			adapter.addTab(tab, classes[tagNumber], args);
		}
		icons.recycle();
	}

	private void addMyListTabs(TabHost host, TabWidget widget, TabsAdapter adapter, String klass) {
		final LocalStore store = DdN.getLocalStore();
		for (MyList mylist: store.loadMyLists()) {
			Bundle args = new Bundle(3);
			args.putInt("id", mylist.id);
			args.putString("name", mylist.name);
			args.putInt("max", mylist.max);
			args.putString("tag", mylist.tag);

			TabHost.TabSpec tab = host.newTabSpec(mylist.tag);
			tab.setIndicator(mylist.name, getMyListIcon(store.getActiveMyList(mylist.id)));
			adapter.addTab(tab, klass, args);

			m_myListTabs.add(new TabHolder(mylist, widget.getChildTabViewAt(widget.getTabCount()-1)));
		}
	}

	private Drawable getMyListIcon(int target) {
		Drawable icon;
		switch(target){
		case 0:
			icon = getResources().getDrawable(R.drawable.ic_tab_mylist_selected_a);
			break;
		case 1:
			icon = getResources().getDrawable(R.drawable.ic_tab_mylist_selected_b);
			break;
		case 2:
			icon = getResources().getDrawable(R.drawable.ic_tab_mylist_selected_c);
			break;
		default:
			icon = getResources().getDrawable(R.drawable.ic_tab_mylist);
		}
		return icon;
	}

	private String getTag(SharedPreferences preferences) {
		final String tag = preferences.getString("default_tab", null);
		if (tag == null || tag.matches("mylist[0-9]+"))
			return tag;

		Matcher m = Pattern.compile("mylist(.)").matcher(tag);
		if(!m.find())
			return null;
		int target = m.group(1).charAt(0) - 'a';
		for (TabHolder holder: m_myListTabs) {
			if (DdN.getLocalStore().getActiveMyList(holder.myList.id) == target)
				return holder.myList.tag;
		}

		return null;
	}

	private static class TabsAdapter extends FragmentPagerAdapter
			implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
		static class Page {
			String klass;
			Bundle args;
			PageAdapter adapter;
			DdN.Observer observer;

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

		Activity m_activity;
		TabHost m_host;
		TabWidget m_tabs;
		View m_base;
		ViewPager m_pager;
		List<Page> m_pages = new ArrayList<Page>();

		TabsAdapter(FragmentActivity activity, TabHost host, ViewPager pager,
				TabWidget tabs, View base) {
			super(activity.getSupportFragmentManager());

			m_activity = activity;
			m_host = host;
			m_host.setOnTabChangedListener(this);
			m_tabs = tabs;
			m_base = base;
			m_pager = pager;
			m_pager.setAdapter(this);
			m_pager.setOnPageChangeListener(this);
		}

		void addTab(TabHost.TabSpec spec, String klass, Bundle args) {
			spec.setContent(new DummyTabFactory(m_activity));
			m_pages.add(new Page(klass, args));
			m_host.addTab(spec);
			notifyDataSetChanged();
		}

		Page getCurrentPage() {
			return m_pages.get(m_pager.getCurrentItem());
		}

		void updateTitle() {
			updateTitle(m_pager.getCurrentItem());
		}

		void updateTitle(int position) {
			Page page = m_pages.get(position);
			if (page.adapter != null)
				m_activity.setTitle(page.adapter.getTitle());
			else
				m_activity.setTitle(R.string.app_name);
		}

		void adjustTabPosition() {
			View tab = m_tabs.getChildTabViewAt(m_host.getCurrentTab());
			if (tab == null)
				return;

			final int o = m_base.getScrollX();
			final int l = tab.getLeft();
			final int r = tab.getRight();
			final int w = m_base.getWidth();
			if (l < o)
				m_base.scrollTo(l, 0);
			else if (r > w + o)
				m_base.scrollTo(r - w, 0);
		}

		@Override
		public int getCount() {
			return m_pages.size();
		}

		@Override
		public Fragment getItem(int position) {
			Page page = m_pages.get(position);
			Fragment f = page.instantiate(m_activity);
			if (f instanceof PageAdapter)
				page.adapter = (PageAdapter)f;
			if (f instanceof DdN.Observer)
				page.observer = (DdN.Observer)f;
			return f;
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
			updateTitle(position);
			adjustTabPosition();
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	}
}
