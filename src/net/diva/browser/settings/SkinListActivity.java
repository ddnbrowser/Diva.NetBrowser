package net.diva.browser.settings;

import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SkinListActivity extends ListActivity {
	private SkinAdapter m_adapter;
	private LocalStore m_store;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skin_list);
		m_store = LocalStore.instance(this);

		setListAdapter(m_adapter = new SkinAdapter(this));
		refresh();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SkinInfo skin = m_adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("id", skin.id);
		intent.putExtra("group_id", skin.group_id);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.skin_list_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new SkinDownloader().execute();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private void refresh() {
		m_adapter.setSkins(m_store.loadSkins());
	}

	private static class SkinAdapter extends ArrayAdapter<SkinInfo> {
		public SkinAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}

		public void setSkins(List<SkinInfo> skins) {
			setNotifyOnChange(false);
			clear();

			for (SkinInfo skin: skins)
				add(skin);

			setNotifyOnChange(true);
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			SkinInfo skin = getItem(position);
			if (skin != null) {
				TextView tv = (TextView)view.findViewById(android.R.id.text1);
				tv.setText(skin.name);
			}
			return view;
		}
	}

	private class SkinDownloader extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(SkinListActivity.this);
			m_progress.setMessage(getString(R.string.message_skin_updating));
			m_progress.setIndeterminate(true);
			m_progress.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				ServiceClient service = DdN.getServiceClient();
				PlayRecord record = DdN.getPlayRecord();
				if (!service.isLogin()) {
					record = DdN.setPlayRecord(service.login());
					m_store.update(record);
				}

				m_store.updateSkins(service.getSkins());
				return Boolean.TRUE;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			m_progress.dismiss();
			if (result)
				refresh();
		}
	}
}
