package net.diva.browser.settings;

import net.diva.browser.R;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public abstract class ConfigActivity extends ListActivity {
	private ConfigAdapter m_adapter;

	protected abstract ConfigItem[] createItems();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);

		m_adapter = new ConfigAdapter(this, createItems());
		setListAdapter(m_adapter);
	}

	private ConfigItem.Callback m_callback = new ConfigItem.Callback() {
		public void onUpdated() {
			m_adapter.notifyDataSetChanged();
			setResult(RESULT_OK);
		}
	};

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ConfigItem item = (ConfigItem)m_adapter.getItem(position);
		Intent intent = item.dispatch(this, m_callback);
		if (intent != null)
			startActivityForResult(intent, position);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ConfigItem item = (ConfigItem)m_adapter.getItem(requestCode);
		item.onResult(resultCode, data, m_callback);
	}

	private static class ConfigAdapter extends BaseAdapter {
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
			return m_items.length;
		}

		@Override
		public int getItemViewType(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ConfigItem item = m_items[position];
			View view = convertView;
			if (view == null)
				view = item.onCreateView(m_context, parent);
			item.setContent(view);
			return view;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return m_items[position].isEnabled();
		}

	}
}
