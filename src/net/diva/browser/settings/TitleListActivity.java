package net.diva.browser.settings;

import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.DecorTitle;
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

	private TextView[] m_decorViews = new TextView[2];
	private String[] m_decorIds = new String[2];

	private List<TitleInfo> m_titles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.title_list);
		getListView().setFastScrollEnabled(DdN.Settings.enableFastScroll);

		m_adapter = new TitleAdapter(this);
		m_store = LocalStore.instance(this);
		m_decorViews[0] = (TextView)findViewById(R.id.pre_decor);
		m_decorViews[1] = (TextView)findViewById(R.id.post_decor);
		m_titles = m_store.getTitles();

		m_adapter.setTitles(m_titles);
		setListAdapter(m_adapter);
		setDecorTitles();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		TitleInfo title = m_adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("title_id", title.id);
		intent.putExtra("decor_pre", m_decorIds[0]);
		intent.putExtra("decor_post", m_decorIds[1]);
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
		intent.putExtra("pre", sender == m_decorViews[0]);
		startActivityForResult(intent, R.id.item_select_decor_title);
	}

	public void setDecorTitle(Intent data) {
		final int index = data.getBooleanExtra("pre", true) ? 0 : 1;
		m_decorIds[index] = data.getStringExtra("id");
		m_decorViews[index].setText(data.getStringExtra("name"));
	}

	private void setDecorTitles() {
		String current = DdN.getPlayRecord().title;
		if (current == null)
			return;

		boolean hasPre = false;
		for (DecorTitle deco: m_store.getDecorTitles(true)) {
			if (current.startsWith(deco.name)) {
				current = current.substring(deco.name.length());
				m_decorViews[0].setText(deco.name);
				hasPre = true;
				break;
			}
		}

		boolean hasPost = false;
		for (DecorTitle deco: m_store.getDecorTitles(false)) {
			if (current.endsWith(deco.name)) {
				int length = current.length();
				current = current.substring(length-deco.name.length(), length);
				m_decorViews[1].setText(deco.name);
				hasPost = true;
				break;
			}
		}

		if (hasPre && hasPost)
			return;

		if (hasPre) {
			for (TitleInfo title: m_titles) {
				if (current.startsWith(title.name)) {
					final int start = title.name.length();
					final int end = current.length();
					if (start < end)
						m_decorViews[1].setText(current.subSequence(start, end));
					break;
				}
			}
			return;
		}

		if (hasPost) {
			for (TitleInfo title: m_titles) {
				if (current.endsWith(title.name)) {
					final int length = current.length() - title.name.length();
					if (length > 0)
						m_decorViews[0].setText(current.subSequence(0, length));
					break;
				}
			}
			return;
		}

		for (TitleInfo title: m_titles) {
			int index = current.indexOf(title.name);
			if (index >= 0) {
				if (index > 0)
					m_decorViews[0].setText(current.subSequence(0, index));
				index += title.name.length();
				if (index < current.length())
					m_decorViews[1].setText(current.subSequence(index, current.length()));
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

	private class TitleDownloader extends ServiceTask<Void, Void, List<TitleInfo>> {
		TitleDownloader() {
			super(TitleListActivity.this, R.string.message_updating);
		}

		@Override
		protected List<TitleInfo> doTask(ServiceClient service, Void... params) throws Exception {
			List<TitleInfo> titles = service.getTitles(m_titles);
			m_store.updateTitles(titles);
			return titles;
		}

		@Override
		protected void onResult(List<TitleInfo> result) {
			if (result != null)
				m_adapter.setTitles(m_titles = result);
		}
	}
}
