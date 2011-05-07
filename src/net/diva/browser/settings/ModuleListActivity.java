package net.diva.browser.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ModuleListActivity extends ExpandableListActivity implements AdapterView.OnItemClickListener {
	private static class Group {
		ModuleGroup group;
		List<Module> modules;
		int purchased;
	}

	private ModuleAdapter m_adapter;
	private int m_request;
	private int m_part;
	private String m_key;

	private int m_mode = R.id.item_show_purchased;
	private List<Group> m_modules;
	private List<Group> m_purchased;
	private List<Group> m_notPurchased;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.module_list);
		Intent intent = getIntent();
		if (intent != null) {
			m_request = intent.getIntExtra("request", 0);
			m_part = intent.getIntExtra("part", 0);
			m_key = String.format("vocal%d", m_request);
			Module module = DdN.getModule(intent.getStringExtra(m_key));
			if (module != null) {
				ExpandableListView listView = getExpandableListView();
				listView.addHeaderView(getModuleView(this, module, null, listView), module, true);
				listView.setOnItemClickListener(this);
			}
		}
		m_modules = reconstruct(DdN.getModules());
		m_purchased = filter(m_modules, true);

		m_adapter = new ModuleAdapter(this);
		refresh(savedInstanceState != null ? savedInstanceState.getInt("displayMode") : m_mode);
		setListAdapter(m_adapter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("displayMode", m_mode);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.module_list_options, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(m_mode);
		if (item != null)
			item.setChecked(true);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int id = item.getItemId();
		switch (id) {
		case R.id.item_update:
			new UpdateTask().execute();
			return true;
		case R.id.item_show_all:
		case R.id.item_show_purchased:
		case R.id.item_show_not_purchased:
			refresh(id);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		onSelectModule((Module)parent.getItemAtPosition(position));
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int group, int position, long id) {
		onSelectModule(m_adapter.getChild(group, position));
		return true;
	}

	private void onSelectModule(Module module) {
		if (module.purchased) {
			Intent data = new Intent(getIntent());
			data.putExtra(m_key, module.id);
			if (m_request < m_part) {
				data.putExtra("request", m_request+1);
				startActivityForResult(data, R.id.item_set_module);
			}
			else {
				setResult(RESULT_OK, data);
				finish();
			}
		}
		else {
			Intent intent = new Intent(getApplicationContext(), ShopActivity.class);
			intent.setData(Uri.parse(DdN.url("/divanet/module/detailShop/%s/0/0", module.id)));
			intent.putExtra(m_key, module.id);
			startActivityForResult(intent, R.id.item_confirm_buying);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_set_module:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
				finish();
			}
			break;
		case R.id.item_confirm_buying:
			if (resultCode == RESULT_OK) {
				Module module = DdN.getModule(data.getStringExtra(m_key));
				new BuyTask().execute(module);
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	private void refresh(int mode) {
		switch (mode) {
		case R.id.item_show_all:
			m_adapter.setModules(m_modules);
			break;
		case R.id.item_show_purchased:
			m_adapter.setModules(m_purchased);
			break;
		case R.id.item_show_not_purchased:
			if (m_notPurchased == null)
				m_notPurchased = filter(m_modules, false);
			m_adapter.setModules(m_notPurchased);
			break;
		}
		m_mode = mode;
	}

	private List<Group> reconstruct(List<ModuleGroup> groups) {
		List<Group> result = new ArrayList<Group>(groups.size());
		for (ModuleGroup group: groups) {
			Group g = new Group();
			g.group = group;
			g.modules = group.modules;
			for (Module m: group.modules) {
				if (m.purchased)
					++g.purchased;
			}
			result.add(g);
		}
		return result;
	}

	private List<Group> filter(List<Group> groups, boolean purchased) {
		List<Group> filtered = new ArrayList<Group>(groups.size());
		for (Group g: groups) {
			if (g.purchased == (purchased ? 0 : g.modules.size()))
				continue;

			Group group = new Group();
			group.group = g.group;
			group.modules = new ArrayList<Module>();
			for (Module m: g.modules) {
				if (m.purchased == purchased)
					group.modules.add(m);
			}
			filtered.add(group);
		}
		return filtered;
	}

	private static View getModuleView(Context context, Module module, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			view = inflater.inflate(R.layout.module_item, parent, false);
		}

		TextView text1 = (TextView)view.findViewById(android.R.id.text1);
		text1.setText(module.name);

		TextView text2 = (TextView)view.findViewById(android.R.id.text2);
		if (module.purchased) {
			text2.setVisibility(View.GONE);
		}
		else {
			text2.setVisibility(View.VISIBLE);
			text2.setText(R.string.not_purchased);
		}

		Drawable thumbnail = module.getThumbnail(context);
		if (thumbnail != null) {
			ImageView iv = (ImageView)view.findViewById(R.id.thumbnail);
			iv.setImageDrawable(thumbnail);
		}

		return view;
	}

	private class UpdateTask extends ServiceTask<Void, Void, Boolean> {
		UpdateTask() {
			super(ModuleListActivity.this, R.string.message_updating);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			LocalStore store = DdN.getLocalStore();

			store.updateModules(service.getModules());
			List<ModuleGroup> modules = store.loadModules();
			for (ModuleGroup group: modules) {
				for (Module module: group.modules) {
					if (module.image != null)
						continue;

					service.getModuleDetail(module);
					File file = module.getThumbnailPath(getApplicationContext());
					if (file != null) {
						FileOutputStream out = new FileOutputStream(file);
						service.download(module.thumbnail, out);
						out.close();
						store.updateModule(module);
					}
				}
			}
			DdN.setModules(modules);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null && result) {
				m_modules = reconstruct(DdN.getModules());
				m_purchased = filter(m_modules, true);
				m_notPurchased = null;
				refresh(m_mode);
			}
		}
	}

	private class BuyTask extends ServiceTask<Module, Void, Boolean> {
		BuyTask() {
			super(ModuleListActivity.this, R.string.buying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Module... params) throws Exception {
			Module module = params[0];
			LocalStore store = DdN.getLocalStore();

			service.buyModule(module.id);
			module.purchased = true;
			store.updateModule(module);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null && result) {
				m_purchased = filter(m_modules, true);
				m_notPurchased = null;
				refresh(m_mode);
			}
			else
				Toast.makeText(ModuleListActivity.this, R.string.faile_buying, Toast.LENGTH_SHORT).show();
		}
	}

	private static class ModuleAdapter extends BaseExpandableListAdapter {
		Context m_context;
		List<Group> m_modules;

		ModuleAdapter(Context context) {
			m_context = context;
		}

		void setModules(List<Group> modules) {
			m_modules = modules;
			if (m_modules.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		public Module getChild(int group, int position) {
			return m_modules.get(group).modules.get(position);
		}

		public long getChildId(int group, int position) {
			return position;
		}

		public View getChildView(int group, int position, boolean flag, View view, ViewGroup parent) {
			return getModuleView(m_context, getChild(group, position), view, parent);
		}

		public int getChildrenCount(int group) {
			return m_modules.get(group).modules.size();
		}

		public Group getGroup(int position) {
			return m_modules.get(position);
		}

		public int getGroupCount() {
			return m_modules.size();
		}

		public long getGroupId(int position) {
			return getGroup(position).group.id;
		}

		public View getGroupView(int position, boolean flag, View view, ViewGroup parent) {
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
			}
			TextView tv = (TextView)view.findViewById(android.R.id.text1);
			Group group = getGroup(position);
			tv.setText(group.group.name);
			return view;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isChildSelectable(int group, int position) {
			return true;
		}
	}
}
