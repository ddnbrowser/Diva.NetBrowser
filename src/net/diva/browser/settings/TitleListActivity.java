package net.diva.browser.settings;

import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;

import org.apache.http.NameValuePair;

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

public class TitleListActivity extends ListActivity {
	private TitleAdapter m_adapter;
	private LocalStore m_store;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.title_list);
		m_store = LocalStore.instance(this);

		setListAdapter(m_adapter = new TitleAdapter(this));
		refresh();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		NameValuePair pair = m_adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("title_id", pair.getName());
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.title_list_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new TitleDownloader().execute();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private void refresh() {
		m_adapter.setTitles(DdN.getTitles());
	}

	private static class TitleAdapter extends ArrayAdapter<NameValuePair> {
		public TitleAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}

		public void setTitles(List<NameValuePair> titles) {
			setNotifyOnChange(false);
			clear();

			for (NameValuePair title: titles)
				add(title);

			setNotifyOnChange(true);
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			NameValuePair pair = getItem(position);
			if (pair != null) {
				TextView tv = (TextView)view.findViewById(android.R.id.text1);
				tv.setText(pair.getValue());
			}
			return view;
		}
	}

	private class TitleDownloader extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(TitleListActivity.this);
			m_progress.setMessage(getString(R.string.message_title_updating));
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

				List<NameValuePair> titles = service.getTitles();
				m_store.updateTitles(titles);
				DdN.setTitles(titles);
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
