package net.diva.browser.compatibility.v11;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.MainActivity;
import net.diva.browser.R;
import net.diva.browser.compatibility.ActivitySupport;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.page.PageAdapter;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainFragment extends Fragment
		implements DdN.Observer, MainActivity.Content, ActivitySupport {

	private ActionBar m_bar;
	private TabsAdapter m_adapter;
	private SparseArray<ActionBar.Tab> m_myListTabs = new SparseArray<ActionBar.Tab>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewPager view = new ViewPager(getActivity());
		view.setId(R.id.pager);

		final FragmentActivity activity = getActivity();
		m_bar = activity.getActionBar();
		m_bar.setDisplayShowHomeEnabled(false);
		m_bar.setDisplayShowTitleEnabled(false);
		m_adapter = new TabsAdapter(activity, view);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		addTabs(m_bar, preferences.getString("default_tab", null));

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		DdN.registerObserver(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		DdN.unregisterObserver(this);
	}

	@Override
	public void onUpdate(PlayRecord record, boolean noMusic) {
		for (TabsAdapter.Page p: m_adapter.m_pages) {
			if (p.observer != null)
				p.observer.onUpdate(record, noMusic);
		}

		m_adapter.updateTitle();
	}

	@Override
	public void onUpdate(MyList myList, boolean noMusic) {
		for (TabsAdapter.Page p: m_adapter.m_pages) {
			if (p.observer != null)
				p.observer.onUpdate(myList, noMusic);
		}

		final int active = DdN.getLocalStore().getActiveMyList();
		for (int i = 0; i < m_myListTabs.size(); ++i) {
			final int id = m_myListTabs.keyAt(i);
			final ActionBar.Tab tab = m_myListTabs.valueAt(i);
			if (myList.id == id)
				tab.setText(myList.name);
			if (id == active)
				tab.setIcon(R.drawable.ic_tab_check);
			else
				tab.setIcon(null);
		}

		m_adapter.updateTitle();
	}

	@Override
	public boolean onSearchRequested() {
		return true;
	}

	@Override
	public void invalidateOptionsMenu() {
		getActivity().invalidateOptionsMenu();
	}

	private void addTabs(ActionBar bar, String selected) {
		final Resources resources = getResources();

		String[] tags = resources.getStringArray(R.array.tab_tags);
		String[] names = resources.getStringArray(R.array.tab_names);
		String[] classes = resources.getStringArray(R.array.tab_classes);

		for (int i = 0; i < tags.length; ++i) {
			final String tag = tags[i];
			if ("mylist".equals(tag)) {
				addMyListTabs(bar, classes[i], tag.equals(selected));
				continue;
			}
			Bundle args = new Bundle();
			args.putString("tag", tag);
			ActionBar.Tab tab = bar.newTab().setText(names[i]);
			m_adapter.addTab(tab, classes[i], args, tag.equals(selected));
		}
	}

	private void addMyListTabs(ActionBar bar, String klass, boolean selectActive) {
		final LocalStore store = DdN.getLocalStore();
		final int active = store.getActiveMyList();

		for (MyList mylist: store.loadMyLists()) {
			Bundle args = new Bundle(3);
			args.putInt("id", mylist.id);
			args.putString("name", mylist.name);
			args.putString("tag", mylist.tag);

			ActionBar.Tab tab = bar.newTab().setText(mylist.name);
			if (mylist.id == active)
				tab.setIcon(R.drawable.ic_tab_check);
			m_adapter.addTab(tab, klass, args, selectActive && mylist.id == active);
			m_myListTabs.put(mylist.id, tab);
		}
	}

	private static class TabsAdapter extends FragmentPagerAdapter
		implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
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

		Activity m_activity;
		ActionBar m_bar;
		ViewPager m_pager;

		List<Page> m_pages = new ArrayList<Page>();

		TabsAdapter(FragmentActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			m_activity = activity;
			m_bar = activity.getActionBar();
			m_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			m_pager = pager;
			m_pager.setAdapter(this);
			m_pager.setOnPageChangeListener(this);
		}

		void addTab(ActionBar.Tab tab, String klass, Bundle args, boolean selected) {
			Page page = new Page(klass, args);
			tab.setTag(page);
			tab.setTabListener(this);
			m_pages.add(page);
			m_bar.addTab(tab, selected);
			notifyDataSetChanged();
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
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			m_bar.setSelectedNavigationItem(position);
			updateTitle(position);
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Object tag = tab.getTag();
			for (int i = 0; i < m_pages.size(); ++i) {
				if (m_pages.get(i) == tag) {
					m_pager.setCurrentItem(i);
					break;
				}
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
}
