package net.diva.browser.page;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.ticket.DecorPrizeActivity;
import net.diva.browser.ticket.SkinPrizeActivity;
import net.diva.browser.util.ComplexAdapter;
import net.diva.browser.util.ComplexAdapter.Item;
import net.diva.browser.util.ComplexAdapter.MultiTextItem;
import net.diva.browser.util.ComplexAdapter.TextItem;
import net.diva.browser.util.ProgressTask;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class InformationFragment extends ListFragment implements DdN.Observer {
	private ComplexAdapter m_adapter;

	private TextItem m_name = new TextItem(R.layout.info_item, R.id.text1);
	private TextItem m_title = new TextItem(R.layout.info_item, R.id.text1);
	private TextItem m_level = new TextItem(R.layout.info_item, R.id.text1);
	private ProgressItem m_experience = new ProgressItem(DdN.EXPERIENCE_UNIT);
	private MultiTextItem m_total = new MultiTextItem(R.layout.info_right_with_label, R.id.text1, R.id.text2);
	private MultiTextItem m_toNextLevel = new MultiTextItem(R.layout.info_right_with_label, R.id.text1, R.id.text2);
	private MultiTextItem m_toNextRank = new MultiTextItem(R.layout.info_right_with_label, R.id.text1, R.id.text2);
	private TextItem m_vp = new TextItem(R.layout.info_right, R.id.text1);
	private TextItem m_ticket = new TextItem(R.layout.info_right, R.id.text1);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.basic_list, container, false);
		TextView empty = (TextView)v.findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_record);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		m_total.setText(R.id.text1, getText(R.string.total_exp));
		m_toNextLevel.setText(R.id.text1, getText(R.string.next_level));
		m_toNextRank.setText(R.id.text1, getText(R.string.next_rank));

		m_adapter = new ComplexAdapter(getActivity());
		m_adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.player_name)));
		m_adapter.add(m_name);
		m_adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.level_rank)));
		m_adapter.add(m_title);
		m_adapter.add(m_level);
		m_adapter.add(m_experience);
		m_adapter.add(m_total);
		m_adapter.add(m_toNextLevel);
		m_adapter.add(m_toNextRank);
		m_adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.vocaloid_point)));
		m_adapter.add(m_vp);
		m_adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.diva_ticket)));
		m_adapter.add(m_ticket);
		setListAdapter(m_adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		final PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.information_options, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateTask(getActivity()).execute();
			break;
		case R.id.item_exchange_skin:
			startActivity(new Intent(getActivity(), SkinPrizeActivity.class));
			break;
		case R.id.item_exchange_title:
			startActivity(new Intent(getActivity(), DecorPrizeActivity.class));
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onUpdate(MyList myList, boolean noMusic) {
		// do nothing.
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		long total = record.experience();
		int experience = (int)(total % DdN.EXPERIENCE_UNIT);
		int nextLevel = DdN.EXPERIENCE_UNIT - experience;

		int[] nextRank = new int[1];
		record.rank(nextRank);

		m_name.setText(record.player_name);
		m_title.setText(record.title);
		m_level.setText(record.level);
		m_experience.value = experience;
		m_total.setText(R.id.text2, String.format("%d.%02d %%", total/100, total%100));
		m_toNextLevel.setText(R.id.text2, String.format("%d.%02d %%", nextLevel/100, nextLevel%100));
		m_toNextRank.setText(R.id.text2, String.format("%d pts", nextRank[0]));
		m_vp.setText(String.format("%d VP", record.vocaloid_point));
		m_ticket.setText(String.format("%d 枚", record.ticket));

		m_adapter.notifyDataSetChanged();
	}

	private static class ProgressItem extends Item {
		int max;
		int value;

		ProgressItem(int max_) {
			super(R.layout.info_bar, null);
			max = max_;
		}

		@Override
		public void prepare(View view) {
			view.setTag(view.findViewById(R.id.progress));
		}

		@Override
		public void attach(View view) {
			ProgressBar progress = (ProgressBar)view.getTag();
			progress.setMax(max);
			progress.setProgress(value);
		}
	}

	private static class UpdateTask extends ProgressTask<Void, Void, Boolean> {
		public UpdateTask(Context context) {
			super(context, R.string.message_updating);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			ServiceClient service = DdN.getServiceClient();
			try {
				PlayRecord record = service.login();
				DdN.getLocalStore().update(record);
				DdN.setPlayRecord(record);
				return Boolean.TRUE;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}
	}
}
