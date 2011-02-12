package net.diva.browser.settings;

import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
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
		TitleInfo title = m_adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("title_id", title.id);
		intent.putExtra("image_id", title.image_id);
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

	private static class TitleAdapter extends ArrayAdapter<TitleInfo> {
		public TitleAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}

		public void setTitles(List<TitleInfo> titles) {
			setNotifyOnChange(false);
			clear();

			for (TitleInfo title: titles)
				add(title);

			setNotifyOnChange(true);
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			TitleInfo title = getItem(position);
			if (title != null) {
				TextView tv = (TextView)view.findViewById(android.R.id.text1);
				tv.setText(title.name);
			}
			return view;
		}
	}

	private class TitleDownloader extends ServiceTask<Void, Void, Boolean> {
		TitleDownloader() {
			super(TitleListActivity.this, R.string.message_updating);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			List<TitleInfo> titles = service.getTitles(DdN.getTitles());
			m_store.updateTitles(titles);
			DdN.setTitles(m_store.getTitles());
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result)
				refresh();
		}
	}
}
