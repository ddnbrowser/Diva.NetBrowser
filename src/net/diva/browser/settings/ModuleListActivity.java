package net.diva.browser.settings;

import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class ModuleListActivity extends ExpandableListActivity {
	private ModuleAdapter m_adapter;
	private int m_request;
	private int m_part;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.module_list);
		Intent intent = getIntent();
		if (intent != null) {
			m_request = intent.getIntExtra("request", 0);
			m_part = intent.getIntExtra("part", 0);
			if (m_part > 1) {
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

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int group, int position, long id) {
		Module module = (Module)m_adapter.getChild(group, position);
		Intent data = new Intent(getIntent());
		data.putExtra(String.format("vocal%d", m_request), module.id);
		if (m_request < m_part) {
			data.putExtra("request", m_request+1);
			startActivityForResult(data, R.id.item_set_module);
		}
		else {
			setResult(RESULT_OK, data);
			finish();
		}
		return true;
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
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
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
			try {
				if (!service.isLogin())
					service.login();

				List<ModuleGroup> modules = service.getModules();
				DdN.setModules(modules);
				DdN.getLocalStore().updateModules(modules);
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

	private static class ModuleAdapter extends BaseExpandableListAdapter {
		Context m_context;
		List<ModuleGroup> m_modules;
		boolean m_showAll = false;

		ModuleAdapter(Context context, List<ModuleGroup> modules) {
			m_context = context;
			m_modules = modules;
		}

		void setModules(List<ModuleGroup> modules) {
			m_modules = modules;
			notifyDataSetChanged();
		}

		void toggleShowAll() {
			m_showAll = !m_showAll;
			notifyDataSetChanged();
		}

		public Object getChild(int group, int position) {
			List<Module> modules = m_modules.get(group).modules;
			if (m_showAll)
				return modules.get(position);

			int count = 0;
			for (Module module: m_modules.get(group).modules) {
				if (module.purchased && count++ == position)
					return module;
			}
			return null;
		}

		public long getChildId(int group, int position) {
			return position;
		}

		public View getChildView(int group, int position, boolean flag, View view, ViewGroup parent) {
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
			}
			TextView tv = (TextView)view.findViewById(android.R.id.text1);
			Module module = (Module)getChild(group, position);
			tv.setText(module.name);
			return view;
		}

		public int getChildrenCount(int group) {
			List<Module> modules = m_modules.get(group).modules;
			if (m_showAll)
				return modules.size();

			int count = 0;
			for (Module module: m_modules.get(group).modules) {
				if (module.purchased)
					++count;
			}
			return count;
		}

		public Object getGroup(int position) {
			return m_modules.get(position);
		}

		public int getGroupCount() {
			return m_modules.size();
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
			ModuleGroup group = (ModuleGroup)getGroup(position);
			tv.setText(group.name);
			return view;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isChildSelectable(int i, int j) {
			return true;
		}
	}
}
