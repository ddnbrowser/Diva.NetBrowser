package net.diva.browser.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.DecorTitle;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DecorTitlesActivity extends ListActivity {
	private LocalStore m_store;
	private DecorAdapter m_adapter;

	private boolean m_pre;
	private int m_mode;
	private List<DecorTitle> m_titles;
	private List<DecorTitle> m_purchased;
	private List<DecorTitle> m_notPurchased;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);
		getListView().setFastScrollEnabled(DdN.Settings.enableFastScroll);

		m_store = DdN.getLocalStore();
		m_pre = getIntent().getBooleanExtra("pre", true);
		m_titles = m_store.getDecorTitles(m_pre);
		m_adapter = new DecorAdapter(this);
		refresh(R.id.item_show_purchased);
		setListAdapter(m_adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DecorTitle title = m_adapter.getItem(position);
		if (title.purchased) {
			Intent intent = getIntent();
			intent.putExtra("id", title.id);
			intent.putExtra("name", title.name);
			setResult(RESULT_OK, intent);
			finish();
		}
		else {
			Intent intent = new Intent(getApplicationContext(), ShopActivity.class);
			intent.setData(Uri.parse(DdN.url("/divanet/title/decorDetail/%s/0/0", title.id)));
			intent.putExtra("id", title.id);
			startActivityForResult(intent, R.id.item_confirm_buying);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_confirm_buying:
			if (resultCode == RESULT_OK)
				new BuyTask().execute(findTitle(data.getStringExtra("id")));
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.skin_list_options, menu);
		return super.onCreateOptionsMenu(menu);
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
			new DecorDownloader().execute();
			break;
		case R.id.item_show_all:
		case R.id.item_show_purchased:
		case R.id.item_show_not_purchased:
			refresh(id);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private DecorTitle findTitle(String id) {
		for (DecorTitle title: m_titles) {
			if (title.id.equals(id))
				return title;
		}
		return null;
	}

	private void refresh(int mode) {
		switch (mode) {
		case R.id.item_show_all:
			m_adapter.setData(m_titles);
			break;
		case R.id.item_show_purchased:
			if (m_purchased == null)
				m_purchased = filter(m_titles, true);
			m_adapter.setData(m_purchased);
			break;
		case R.id.item_show_not_purchased:
			if (m_notPurchased == null)
				m_notPurchased = filter(m_titles, false);
			m_adapter.setData(m_notPurchased);
			break;
		}
		m_mode = mode;
	}

	private List<DecorTitle> filter(Collection<DecorTitle> titles, boolean purchased) {
		List<DecorTitle> filtered = new ArrayList<DecorTitle>();
		for (DecorTitle title: titles) {
			if (title.purchased == purchased)
				filtered.add(title);
		}
		return filtered;
	}

	private static class DecorAdapter extends BaseAdapter {
		LayoutInflater m_inflater;
		List<DecorTitle> m_titles;

		DecorAdapter(Context context) {
			m_inflater = LayoutInflater.from(context);
		}

		void setData(List<DecorTitle> titles) {
			m_titles = titles;
			if (m_titles.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		public int getCount() {
			return m_titles.size();
		}

		public DecorTitle getItem(int position) {
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

			DecorTitle title = getItem(position);
			if (title != null && text != null)
				text.setText(title.purchased ? title.name : String.format("%s (未購入)", title.name));

			return view;
		}
	}

	private class DecorDownloader extends ServiceTask<Void, Void, List<DecorTitle>> {
		public DecorDownloader() {
			super(DecorTitlesActivity.this, R.string.message_updating);
		}

		@Override
		protected List<DecorTitle> doTask(ServiceClient service, Void... params) throws Exception {
			List<DecorTitle> titles = service.getDecorTitles(m_pre);
			m_store.updateDecorTitles(titles);
			return titles;
		}

		@Override
		protected void onResult(List<DecorTitle> result) {
			if (result == null)
				return;

			m_titles = result;
			m_purchased = m_notPurchased = null;
			refresh(m_mode);
		}
	}

	private class BuyTask extends ServiceTask<DecorTitle, Void, Boolean> {
		BuyTask() {
			super(DecorTitlesActivity.this, R.string.buying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, DecorTitle... params) throws Exception {
			DecorTitle title = params[0];

			int vp = service.buyDecorTitle(title.id);
			if (vp >= 0)
				DdN.setVocaloidPoint(vp);
			title.purchased = true;
			m_store.updateDecorTitle(title);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result) {
				m_purchased = m_notPurchased = null;
				refresh(m_mode);
			}
			else
				Toast.makeText(DecorTitlesActivity.this, R.string.faile_buying, Toast.LENGTH_SHORT).show();
		}
	}
}
