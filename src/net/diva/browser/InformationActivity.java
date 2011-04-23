package net.diva.browser;

import net.diva.browser.model.PlayRecord;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class InformationActivity extends ListActivity implements DdN.Observer {
	private InformationAdapter m_adapter;
	private PlayRecord m_record;

	private int m_experience;
	private int m_rankPoints;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);
		TextView empty = (TextView)findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_record);

		m_adapter = new InformationAdapter();
		setListAdapter(m_adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		final PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
		DdN.registerObserver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		DdN.unregisterObserver(this);
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		m_record = record;

		m_experience = (int)(m_record.experience() % DdN.EXPERIENCE_UNIT);

		int[] next = new int[1];
		m_record.rank(next);
		m_rankPoints = next[0];

		m_adapter.notifyDataSetChanged();
	}

	static final private int[] LAYOUTS = {
		android.R.layout.preference_category,
		R.layout.info_item,
		android.R.layout.preference_category,
		R.layout.info_item,
		R.layout.info_bar,
		R.layout.info_right,
		R.layout.info_right,
		android.R.layout.preference_category,
		R.layout.info_right,
	};

	private class InformationAdapter extends BaseAdapter {
		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public int getViewTypeCount() {
			return LAYOUTS.length;
		}

		@Override
		public int getItemViewType(int position) {
			return position;
		}

		public int getCount() {
			return m_record == null ? 0 : LAYOUTS.length;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				LayoutInflater inflater = getLayoutInflater();
				view = inflater.inflate(LAYOUTS[position], parent, false);
			}

			switch (position) {
			case 0:
				setText(view, android.R.id.title, R.string.player_name);
				break;
			case 1:
				setText(view, R.id.text1, m_record.player_name);
				break;
			case 2:
				setText(view, android.R.id.title, R.string.level_rank);
				break;
			case 3:
				setText(view, R.id.text1, m_record.level);
				setText(view, R.id.text2, DdN.getTitle(m_record.title_id));
				break;
			case 4:
				ProgressBar progress = (ProgressBar)view.findViewById(R.id.progress);
				progress.setMax(DdN.EXPERIENCE_UNIT);
				progress.setProgress(m_experience);
				break;
			case 5:
				int shortage = DdN.EXPERIENCE_UNIT - m_experience;
				setText(view, R.id.text1, R.string.next_level);
				setText(view, R.id.text2, String.format("%d.%02d %%", shortage/100, shortage%100));
				break;
			case 6:
				setText(view, R.id.text1, R.string.next_rank);
				setText(view, R.id.text2, String.format("%d pts", m_rankPoints));
				break;
			case 7:
				setText(view, android.R.id.title, R.string.vocaloid_point);
				break;
			case 8:
				setText(view, R.id.text2, String.format("%d VP", m_record.vocaloid_point));
				break;
			}

			return view;
		}
	}

	private void setText(View view, int targetId, CharSequence content) {
		TextView text = (TextView)view.findViewById(targetId);
		if (text != null)
			text.setText(content);
	}

	private void setText(View view, int targetId, int resId) {
		setText(view, targetId, getText(resId));
	}
}
