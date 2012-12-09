package net.diva.browser.settings;

import java.util.HashMap;
import java.util.Map;

import net.diva.browser.MainActivity;
import net.diva.browser.R;
import net.diva.browser.util.SortableListView;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TabSortActivity extends ListActivity {
	private String[] tabs;
	private Map<String, String> tabMap;

	int mDraggingPosition = -1;
	TabListAdapter mAdapter;
	SortableListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Resources res = getResources();
		String[] tags = res.getStringArray(R.array.tab_tags);
		String[] tagNames = res.getStringArray(R.array.tab_sort_name);
		tabMap = new HashMap<String, String>();
		for(int i = 0; i < tags.length; i++)
			tabMap.put(tags[i], tagNames[i]);

		String strTags = "";
		for(String tag : tags)
			strTags += tag + ",";
		strTags = strTags.substring(0, strTags.length() - 1);
		String customTabList = preferences.getString("customTabList", strTags);
		tabs = customTabList.split(",");

		setContentView(R.layout.tab_sort);
		mAdapter = new TabListAdapter();
		mListView = (SortableListView) findViewById(android.R.id.list);
		mListView.setDragListener(new DragListener());
		mListView.setSortable(true);
		mListView.setAdapter(mAdapter);
	}

	public void commit(View view){
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

		String tabList = "";
		for(String tab : tabs)
			tabList += tab + ",";
		tabList = tabList.substring(0, tabList.length() - 1);

		editor.putString("customTabList", tabList);
		editor.commit();
		Intent i = new Intent(this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
	}

	class TabListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return tabs.length;
		}

		@Override
		public String getItem(int position) {
			return tabs[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(
						android.R.layout.simple_list_item_1, null);
			}
			final TextView view = (TextView) convertView;
			view.setText(tabMap.get(tabs[position]));
			view.setVisibility(position == mDraggingPosition ? View.INVISIBLE
					: View.VISIBLE);
			return convertView;
		}
	}

	class DragListener extends SortableListView.SimpleDragListener {
		@Override
		public int onStartDrag(int position) {
			mDraggingPosition = position;
			mListView.invalidateViews();
			return position;
		}

		@Override
		public int onDuringDrag(int positionFrom, int positionTo) {
			if (positionFrom < 0 || positionTo < 0
					|| positionFrom == positionTo) {
				return positionFrom;
			}
			int i;
			if (positionFrom < positionTo) {
				final int min = positionFrom;
				final int max = positionTo;
				final String data = tabs[min];
				i = min;
				while (i < max) {
					tabs[i] = tabs[++i];
				}
				tabs[max] = data;
			} else if (positionFrom > positionTo) {
				final int min = positionTo;
				final int max = positionFrom;
				final String data = tabs[max];
				i = max;
				while (i > min) {
					tabs[i] = tabs[--i];
				}
				tabs[min] = data;
			}
			mDraggingPosition = positionTo;
			mListView.invalidateViews();
			return positionTo;
		}

		@Override
		public boolean onStopDrag(int positionFrom, int positionTo) {
			mDraggingPosition = -1;
			mListView.invalidateViews();
			return super.onStopDrag(positionFrom, positionTo);
		}
	}
}
