package net.diva.browser.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
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
	private ModuleAdapter m_adapter;
	private int m_request;
	private int m_part;
	private String m_key;

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
		m_adapter = new ModuleAdapter(this, DdN.getModules());
		setListAdapter(m_adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.module_list_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateTask().execute();
			return true;
		case R.id.item_toggle_show_all:
			m_adapter.toggleShowAll();
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
			intent.setData(Uri.parse(DdN.url("/divanet/module/detailShop/%s/0/0", module.id).toString()));
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

	private class UpdateTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(ModuleListActivity.this);
			m_progress.setMessage(getString(R.string.message_module_updating));
			m_progress.setIndeterminate(true);
			m_progress.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			ServiceClient service = DdN.getServiceClient();
			LocalStore store = DdN.getLocalStore();
			try {
				if (!service.isLogin())
					service.login();

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
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			m_progress.dismiss();
			if (result)
				m_adapter.setModules(DdN.getModules());
		}
	}

	private class BuyTask extends AsyncTask<Module, Void, Boolean> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(ModuleListActivity.this);
			m_progress.setMessage(getString(R.string.buying));
			m_progress.setIndeterminate(true);
			m_progress.show();
		}

		@Override
		protected Boolean doInBackground(Module... params) {
			Module module = params[0];
			ServiceClient service = DdN.getServiceClient();
			LocalStore store = DdN.getLocalStore();
			try {
				if (!service.isLogin())
					service.login();

				service.buyModule(module.id);
				module.purchased = true;
				store.updateModule(module);
				return Boolean.TRUE;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			m_progress.dismiss();
			if (result)
				m_adapter.notifyDataSetChanged();
			else
				Toast.makeText(ModuleListActivity.this, R.string.faile_buying, Toast.LENGTH_SHORT).show();
		}
	}

	private static class ModuleAdapter extends BaseExpandableListAdapter {
		Context m_context;
		List<ModuleGroup> m_modules;
		List<ModuleGroup> m_groups;
		List<List<Module>> m_purchased;
		boolean m_showAll = false;

		ModuleAdapter(Context context, List<ModuleGroup> modules) {
			m_context = context;
			setModules(modules);
		}

		void setModules(List<ModuleGroup> modules) {
			m_modules = modules;

			List<ModuleGroup> groups = new ArrayList<ModuleGroup>();;
			List<List<Module>> purchased = new ArrayList<List<Module>>();
			for (ModuleGroup group: modules) {
				List<Module> list = new ArrayList<Module>();
				for (Module module: group.modules) {
					if (module.purchased)
						list.add(module);
				}
				if (!list.isEmpty()) {
					groups.add(group);
					purchased.add(list);
				}
			}
			m_groups = groups;
			m_purchased = purchased;

			notifyDataSetChanged();
		}

		void toggleShowAll() {
			m_showAll = !m_showAll;
			notifyDataSetChanged();
		}

		public Module getChild(int group, int position) {
			if (m_showAll)
				return m_modules.get(group).modules.get(position);
			else
				return m_purchased.get(group).get(position);
		}

		public long getChildId(int group, int position) {
			return position;
		}

		public View getChildView(int group, int position, boolean flag, View view, ViewGroup parent) {
			return getModuleView(m_context, getChild(group, position), view, parent);
		}

		public int getChildrenCount(int group) {
			if (m_showAll)
				return m_modules.get(group).modules.size();
			else
				return m_purchased.get(group).size();
		}

		public ModuleGroup getGroup(int position) {
			return m_showAll ? m_modules.get(position) : m_groups.get(position);
		}

		public int getGroupCount() {
			return m_showAll ? m_modules.size() : m_groups.size();
		}

		public long getGroupId(int position) {
			return position;
		}

		public View getGroupView(int position, boolean flag, View view, ViewGroup parent) {
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
			}
			TextView tv = (TextView)view.findViewById(android.R.id.text1);
			ModuleGroup group = getGroup(position);
			tv.setText(group.name);
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
