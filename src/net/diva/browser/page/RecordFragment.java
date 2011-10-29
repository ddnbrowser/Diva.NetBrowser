package net.diva.browser.page;

import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class RecordFragment extends ListFragment implements PageAdapter {
	ArrayAdapter<CharSequence> m_adapter;
	private String m_title;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.no_records));

		m_adapter = new ArrayAdapter<CharSequence>(getActivity(), R.layout.record_item, R.id.text1);
		refresh(DdN.getLocalStore().getDIVARecords());
		setListAdapter(m_adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.record_options, menu);
		inflater.inflate(R.menu.main_options, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateRecord().execute();
			break;
		case R.id.item_check_record:
			new CheckRecord().execute();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public CharSequence getTitle() {
		return m_title;
	}

	private void refresh(List<String> records) {
		int cleared = 0;
		m_adapter.setNotifyOnChange(false);
		m_adapter.clear();
		for (String record: records) {
			m_adapter.add(Html.fromHtml(record));
			if (record.length() > 5)
				++cleared;
		}
		m_adapter.notifyDataSetChanged();

		m_title = getString(R.string.title_record_tab, cleared, records.size());
	}

	private class UpdateRecord extends ServiceTask<Void, Void, List<String>> {
		UpdateRecord() {
			this(R.string.message_updating);
		}

		UpdateRecord(int message) {
			super(getActivity(), message);
		}

		@Override
		protected List<String> doTask(ServiceClient service, Void... params) throws Exception {
			List<String> records = service.getDIVARecords();
			DdN.getLocalStore().updateDIVARecords(records);
			return records;
		}

		@Override
		protected void onResult(List<String> records) {
			if (records != null)
				refresh(records);
		}
	}

	private class CheckRecord extends UpdateRecord {
		CheckRecord() {
			super(R.string.message_checking_record);
		}

		@Override
		protected List<String> doTask(ServiceClient service, Void... params) throws Exception {
			if (!service.checkDIVARecord())
				return null;

			return super.doTask(service, params);
		}

		@Override
		protected void onResult(List<String> records) {
			if (records == null)
				Toast.makeText(getActivity(), R.string.no_records_updated, Toast.LENGTH_SHORT).show();
			super.onResult(records);
		}
	}
}
