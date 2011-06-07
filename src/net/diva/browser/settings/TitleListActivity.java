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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TitleListActivity extends ListActivity {
	private TitleAdapter m_adapter;
	private LocalStore m_store;

	private TextView m_decorView;
	private String m_decorId;

	private List<TitleInfo> m_titles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.title_list);
		m_adapter = new TitleAdapter(this);
		m_store = LocalStore.instance(this);
		m_decorView = (TextView)findViewById(R.id.decor_title);
		m_titles = m_store.getTitles();

		refresh();
		setListAdapter(m_adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		TitleInfo title = m_adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("title_id", title.id);
		intent.putExtra("decor_id", m_decorId);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_select_decor_title:
			if (resultCode == RESULT_OK)
				setDecorTitle(data);
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void selectDecor(View sender) {
		Intent intent = new Intent(getApplicationContext(), DecorTitlesActivity.class);
		intent.putExtra("pre", true);
		startActivityForResult(intent, R.id.item_select_decor_title);
	}

	public void setDecorTitle(Intent data) {
		m_decorId = data.getStringExtra("id");
		m_decorView.setText(data.getStringExtra("name"));
	}

	private void refresh() {
		m_adapter.setTitles(m_titles);

		String current = DdN.getPlayRecord().title;
		if (current == null)
			return;

		for (TitleInfo title: m_titles) {
			int index = current.indexOf(title.name);
			if (index > 0) {
				m_decorView.setText(current.subSequence(0, index));
				break;
			}
		}
	}

	private static class TitleAdapter extends BaseAdapter {
		LayoutInflater m_inflater;
		List<TitleInfo> m_titles;

		public TitleAdapter(Context context) {
			m_inflater = LayoutInflater.from(context);
		}

		public void setTitles(List<TitleInfo> titles) {
			m_titles = titles;
			if (m_titles.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		public int getCount() {
			return m_titles.size();
		}

		public TitleInfo getItem(int position) {
			return m_titles.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View view, ViewGroup parent) {
			TextView text;
			if (view != null)
				text = (TextView)view.getTag();
			else {
				view = m_inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
				text = (TextView)view.findViewById(android.R.id.text1);
				view.setTag(text);
			}

			TitleInfo title = getItem(position);
			if (title != null && text != null)
				text.setText(title.name);

			return view;
		}
	}

	private class TitleDownloader extends ServiceTask<Void, Void, Boolean> {
		TitleDownloader() {
			super(TitleListActivity.this, R.string.message_updating);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			m_titles = service.getTitles(m_titles);
			m_store.updateTitles(m_titles);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result)
				refresh();
		}
	}
}
