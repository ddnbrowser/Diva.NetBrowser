package net.diva.browser;

import net.diva.browser.settings.ConfigActivationIndividual;
import net.diva.browser.settings.ConfigCategory;
import net.diva.browser.settings.ConfigCommonModule;
import net.diva.browser.settings.ConfigItem;
import net.diva.browser.settings.ConfigRename;
import net.diva.browser.settings.ConfigResetCommon;
import net.diva.browser.settings.ConfigResetIndividual;
import net.diva.browser.settings.ConfigSetSkin;
import net.diva.browser.settings.ConfigTitle;
import net.diva.browser.settings.ConfigUnsetSkin;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class ConfigActivity extends ListActivity {
	private ConfigAdapter m_adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);

		m_adapter = new ConfigAdapter(this,
				new ConfigCategory(getText(R.string.category_player)),
				new ConfigRename(this),
				new ConfigTitle(this),
				new ConfigCategory(getText(R.string.category_module_common)),
				new ConfigCommonModule(this, 1),
				new ConfigCommonModule(this, 2),
				new ConfigResetCommon(this),
				new ConfigCategory(getText(R.string.category_module_individual)),
				new ConfigResetIndividual(this),
				new ConfigActivationIndividual(this),
				new ConfigCategory(getText(R.string.category_skin)),
				new ConfigSetSkin(this),
				new ConfigUnsetSkin(this)
		);
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
