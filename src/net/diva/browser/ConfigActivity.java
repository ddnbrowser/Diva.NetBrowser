package net.diva.browser;

import net.diva.browser.settings.ConfigCategory;
import net.diva.browser.settings.ConfigItem;
import net.diva.browser.settings.ConfigTitle;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class ConfigActivity extends ListActivity {
	private ConfigAdapter m_adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config);

		m_adapter = new ConfigAdapter(this,
				new ConfigCategory(getText(R.string.category_title)),
				new ConfigTitle(this)
		);
		setListAdapter(m_adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ConfigItem item = (ConfigItem)m_adapter.getItem(position);
		Intent intent = item.dispatch();
		if (intent != null)
			startActivityForResult(intent, position);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ConfigItem item = (ConfigItem)m_adapter.getItem(requestCode);
		if (item.onResult(resultCode, data)) {
			m_adapter.notifyDataSetChanged();
			setResult(RESULT_OK);
		}
	}

	private static class ConfigAdapter extends BaseAdapter {
		final static int TYPE_CATEGORY = 0;
		final static int TYPE_ITEM = 1;

		Context m_context;
		ConfigItem[] m_items;

		public ConfigAdapter(Context context, ConfigItem... items) {
			m_context = context;
			m_items = items;
		}

		public int getCount() {
			return m_items.length;
		}

		public Object getItem(int position) {
			return m_items[position];
		}

		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return m_items[position].isCategory() ? TYPE_CATEGORY : TYPE_ITEM;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ConfigItem item = m_items[position];
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				switch (getItemViewType(position)) {
				case TYPE_CATEGORY:
					view = inflater.inflate(android.R.layout.preference_category, null);
					break;
				case TYPE_ITEM:
					view = inflater.inflate(R.layout.setting_item, null);
					break;
				}
			}
			item.setContent(view);
			return view;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return !m_items[position].isCategory();
		}

	}
}
