package net.diva.browser;

import java.util.List;

import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ListActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class RecordActivity extends ListActivity {
	ArrayAdapter<CharSequence> m_adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);
		TextView empty = (TextView)findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_records);

		m_adapter = new ArrayAdapter<CharSequence>(this, R.layout.record_item, R.id.text1);
		refresh(DdN.getLocalStore().getDIVARecords());
		setListAdapter(m_adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.record_options, menu);
		return super.onCreateOptionsMenu(menu);
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

		setTitle(getString(R.string.title_record_tab, cleared, records.size()));
	}

	private class UpdateRecord extends ServiceTask<Void, Void, List<String>> {
		UpdateRecord() {
			this(R.string.message_updating);
		}

		UpdateRecord(int message) {
			super(RecordActivity.this, message);
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
				Toast.makeText(RecordActivity.this, R.string.no_records_updated, Toast.LENGTH_SHORT).show();
			super.onResult(records);
		}
	}
}
