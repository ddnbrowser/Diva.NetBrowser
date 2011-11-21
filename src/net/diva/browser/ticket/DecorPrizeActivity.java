package net.diva.browser.ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.DecorTitle;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.settings.ShopActivity;

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

public class DecorPrizeActivity extends ListActivity {
	private LocalStore m_store;
	private DecorAdapter m_adapter;

	private List<DecorTitle> m_titles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);
		ListView list = getListView();
		list.setFastScrollEnabled(DdN.Settings.enableFastScroll);
		TextView empty = (TextView)findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_prize);

		m_store = DdN.getLocalStore();
		m_adapter = new DecorAdapter(this);
		m_adapter.setTitles(m_titles = m_store.getDecorPrize());
		setListAdapter(m_adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DecorTitle title = m_adapter.getItem(position);
		Intent intent = new Intent(getApplicationContext(), ShopActivity.class);
		intent.setData(Uri.parse(DdN.url("/divanet/divaTicket/confirmExchangeTitle/%s", title.id)));
		intent.putExtra("id", title.id);
		intent.putExtra("label", R.string.do_exchange);
		startActivityForResult(intent, R.id.item_exchange_title);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_exchange_title:
			if (resultCode == RESULT_OK)
				new ExchangeTask().execute(findTitle(data.getStringExtra("id")));
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.prize_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateTask().execute();
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

	private static class DecorAdapter extends BaseAdapter {
		Context m_context;
		List<DecorTitle> m_titles = Collections.emptyList();

		DecorAdapter(Context context) {
			m_context = context;
		}

		void setTitles(List<DecorTitle> titles) {
			m_titles = titles;
			if (m_titles.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return m_titles.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public DecorTitle getItem(int position) {
			return m_titles.get(position);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			TextView text;
			if (view != null)
				text = (TextView)view.getTag();
			else {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
				text = (TextView)view.findViewById(android.R.id.text1);
				view.setTag(text);
			}

			DecorTitle title = getItem(position);
			if (title != null && text != null)
				text.setText(String.format(title.pre ? "%1$s %2$s" : "%2$s %1$s", title.name, "－"));

			return view;
		}
	}

	private class UpdateTask extends ServiceTask<Void, Void, List<DecorTitle>> {
		UpdateTask() {
			super(DecorPrizeActivity.this, R.string.message_updating);
		}

		@Override
		protected List<DecorTitle> doTask(ServiceClient service, Void... params)
				throws Exception {
			List<DecorTitle> titles = service.getDecorPrize();
			m_store.updateDecorTitles(titles);

			List<DecorTitle> missing = new ArrayList<DecorTitle>(m_titles);
			missing.removeAll(titles);
			if (!missing.isEmpty()) {
				for (DecorTitle t: missing) {
					t.prize = false;
					t.purchased = true;
				}
				m_store.updateDecorTitles(missing);
			}

			return titles;
		}

		@Override
		protected void onResult(List<DecorTitle> result) {
			if (result != null)
				m_adapter.setTitles(m_titles = result);
		}
	}

	private class ExchangeTask extends ServiceTask<DecorTitle, Void, DecorTitle> {
		ExchangeTask() {
			super(DecorPrizeActivity.this, R.string.exchanging);
		}

		@Override
		protected DecorTitle doTask(ServiceClient service, DecorTitle... params)
				throws Exception {
			DecorTitle title = params[0];
			int ticket = service.exchangeDecorTitle(title.id);
			if (ticket >= 0)
				DdN.setTicketCount(ticket);
			title.prize = false;
			title.purchased = true;
			m_store.updateDecorTitle(title);
			return title;
		}

		@Override
		protected void onResult(DecorTitle result) {
			if (result == null)
				Toast.makeText(DecorPrizeActivity.this, R.string.exchange_failure, Toast.LENGTH_SHORT).show();
			else if (m_titles.remove(result))
				m_adapter.setTitles(m_titles);
		}
	}
}
