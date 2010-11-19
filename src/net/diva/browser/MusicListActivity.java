package net.diva.browser;

import java.net.URI;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import net.diva.browser.db.LocalStore;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.NoLoginException;
import net.diva.browser.service.Service;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MusicListActivity extends ListActivity {
	private static final URI URL = URI.create("http://project-diva-ac.net/divanet/");

	private TextView m_player_name;
	private TextView m_level_rank;
	private View m_buttons[];
	private MusicAdapter m_adapter;

	private SharedPreferences m_preferences;
	private Service m_service;
	private LocalStore m_store;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_list);
		registerForContextMenu(getListView());

		m_preferences = PreferenceManager.getDefaultSharedPreferences(this);
		m_store = LocalStore.instance(this);

		final int difficulty = m_preferences.getInt("difficulty", 3);
		m_player_name = (TextView)findViewById(R.id.player_name);
		m_level_rank = (TextView)findViewById(R.id.level_rank);
		m_adapter = new MusicAdapter(this, difficulty);
		setListAdapter(m_adapter);

		m_player_name.setText("");
		m_level_rank.setText("");

		m_buttons = new View[] {
				findViewById(R.id.button_easy),
				findViewById(R.id.button_normal),
				findViewById(R.id.button_hard),
				findViewById(R.id.button_extreme),
		};
		for (int i = 0; i < m_buttons.length; ++i) {
			final int d = i;
			m_buttons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					setDifficulty(d);
				}
			});
			m_buttons[i].setEnabled(d != difficulty);

		}
		getListView().setOnTouchListener(new OnTouchListener());

		String access_code = m_preferences.getString("access_code", null);
		if (access_code == null) {
			inputAccountInformation();
			return;
		}
		m_service = new Service(URL, access_code, m_preferences.getString("password", ""));

		new PlayRecordLoader().execute(access_code);
	}

	private static final int MENU_UPDATE = Menu.FIRST;
	private static final int MENU_CANCEL = Menu.FIRST + 1;

	private static final int MENU_SORT_BY_DIFFICULTY = Menu.FIRST + 1;
	private static final int MENU_SORT_BY_SCORE = Menu.FIRST + 2;
	private static final int MENU_SORT_BY_ACHIEVEMENT = Menu.FIRST + 3;
	private static final int MENU_SORT_BY_CLEAR_STATUS = Menu.FIRST + 4;
	private static final int MENU_SORT_BY_TRIAL_STATUS = Menu.FIRST + 5;

	private static final int MENU_GROUP_SORT = Menu.FIRST;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_UPDATE, 0, R.string.item_update_all)
			.setIcon(R.drawable.ic_menu_refresh);

		SubMenu sort = menu.addSubMenu(R.string.item_sort)
			.setIcon(android.R.drawable.ic_menu_sort_by_size);
		sort.add(MENU_GROUP_SORT, MENU_SORT_BY_DIFFICULTY, 0, R.string.item_sort_by_difficulty);
		sort.add(MENU_GROUP_SORT, MENU_SORT_BY_SCORE, 0, R.string.item_sort_by_score);
		sort.add(MENU_GROUP_SORT, MENU_SORT_BY_ACHIEVEMENT, 0, R.string.item_sort_by_achievement);
		sort.add(MENU_GROUP_SORT, MENU_SORT_BY_CLEAR_STATUS, 0, R.string.item_sort_by_clear_status);
		sort.add(MENU_GROUP_SORT, MENU_SORT_BY_TRIAL_STATUS, 0, R.string.item_sort_by_trial_status);
		sort.setGroupCheckable(MENU_GROUP_SORT, true, true);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final long now = new Date().getTime();
		boolean enable = now - m_preferences.getLong("last_updated", 0) > 12*60*60*1000;
		menu.findItem(MENU_UPDATE).setEnabled(enable);
		menu.findItem(m_adapter.sortOrder()).setChecked(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getGroupId()) {
		case MENU_GROUP_SORT:
			m_adapter.sortBy(item.getItemId());
			return true;
		default:
			break;
		}

		switch (item.getItemId()) {
		case MENU_UPDATE:
			updateAll();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, MENU_UPDATE, 0, R.string.item_update_selction);
		menu.add(0, MENU_CANCEL, 0, R.string.cancel);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case MENU_UPDATE:
			new PlayRecordUpdater().execute(m_adapter.getItem(info.position));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void setPlayRecord(PlayRecord record) {
		setPlayRecord(record, record.musics);
	}

	public void setPlayRecord(PlayRecord record, List<MusicInfo> music) {
		if (music != null)
			m_adapter.setData(music);
		else
			m_adapter.notifyDataSetChanged();

		m_player_name.setText(record.player_name);
		m_level_rank.setText(arrangeRankText(record.level_rank));
	}

	public void setDifficulty(int difficulty) {
		m_adapter.setDifficulty(difficulty);
		m_preferences.edit().putInt("difficulty", difficulty).commit();
		for (int i = 0; i < m_buttons.length; ++i)
			m_buttons[i].setEnabled(i != difficulty);
	}

	private String arrangeRankText(String level_rank) {
		final int[] rank_points = getResources().getIntArray(R.array.rank_points);

		int point = rankPoint();
		int rank = 0;
		while (rank < rank_points.length && point >= rank_points[rank])
			point -= rank_points[rank++];
		rank += point/150;
		point %= 150;

		int next = point - (rank < rank_points.length ? rank_points[rank] : 150);
		String name = rankName(rank);
		if (level_rank.lastIndexOf(name) >= 0)
			return String.format("%s (%dpts)", level_rank, next);
		else
			return String.format("%s\n(%s / %dpts)", level_rank, name, next);
	}

	private int rankPoint() {
		int point = 0;
		for (MusicInfo m: m_adapter.getData())
			point += m.rankPoint();
		return point;
	}

	private String rankName(int rank) {
		final String[] rank_names = getResources().getStringArray(R.array.rank_names);
		int max_rank = rank_names.length - 1;
		if (rank <= max_rank)
			return rank_names[rank];
		else
			return String.format("%s+%d", rank_names[max_rank], rank-max_rank);
	}

	private void inputAccountInformation() {
		final View view = getLayoutInflater().inflate(R.layout.account_input, null);
		final TextView access_code = (TextView)view.findViewById(R.id.edit_access_code);
		final TextView password = (TextView)view.findViewById(R.id.edit_password);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(view)
		.setTitle(R.string.account_input_title)
		.setNegativeButton(R.string.cancel, null)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new PlayRecordDownloader().execute(
						access_code.getText().toString(),
						password.getText().toString());
			}
		})
		.show();
	}

	private void updateAll() {
		String access_code = m_preferences.getString("access_code", null);
		if (access_code == null) {
			inputAccountInformation();
			return;
		}

		String password = m_preferences.getString("password", "");
		new PlayRecordDownloader().execute(access_code, password);
	}

	private class OnTouchListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
		private GestureDetector m_detector;

		public OnTouchListener() {
			m_detector = new GestureDetector(MusicListActivity.this, this);
		}

		public boolean onTouch(View view, MotionEvent event) {
			return m_detector.onTouchEvent(event);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			int position = getListView().pointToPosition((int)e.getX(), (int)e.getY());
			if (position != AdapterView.INVALID_POSITION) {
				new PlayRecordUpdater().execute(m_adapter.getItem(position));
				return true;
			}
			return super.onDoubleTap(e);
		}
	}

	private class PlayRecordDownloader extends AsyncTask<String, Integer, PlayRecord> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(MusicListActivity.this);
			m_progress.setMessage(getString(R.string.message_downloading));
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			m_progress.setIndeterminate(false);
			m_progress.show();
		}

		@Override
		protected PlayRecord doInBackground(String... args) {
			final String access_code = args[0];
			final String password = args[1];
			Service service = new Service(URL, access_code, password);
			try {
				PlayRecord record = service.login();
				service.update(record);
				publishProgress(0, record.musics.size());

				for (MusicInfo music: record.musics) {
					service.update(music);
					m_store.cacheCoverart(music, service);
					publishProgress(1);
				}

				m_service = service;
				m_store.insert(record);
				m_preferences.edit()
					.putString("access_code", access_code)
					.putString("password", password)
					.putLong("last_updated", new Date().getTime())
					.commit();
				return record;
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			catch (NoLoginException e) {
				assert(false);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values.length > 1)
				m_progress.setMax(values[1]);

			m_progress.incrementProgressBy(values[0]);
		}

		@Override
		protected void onPostExecute(PlayRecord result) {
			if (result != null)
				setPlayRecord(result);
			m_progress.dismiss();
		}
	}

	private class PlayRecordUpdater extends AsyncTask<MusicInfo, Void, PlayRecord> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(MusicListActivity.this);
			m_progress.setMessage(getString(R.string.message_updating));
			m_progress.setIndeterminate(false);
			m_progress.show();
		}

		@Override
		protected PlayRecord doInBackground(MusicInfo... args) {
			final MusicInfo music = args[0];
			try {
				PlayRecord record = m_service.login();
				m_service.update(music);
				m_store.update(music);
				return record;
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			catch (NoLoginException e) {
				assert(false);
			}
			return null;
		}

		@Override
		protected void onPostExecute(PlayRecord result) {
			if (result != null)
				setPlayRecord(result, null);
			m_progress.dismiss();
		}
	}

	private class PlayRecordLoader extends AsyncTask<String, Void, PlayRecord> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(MusicListActivity.this);
			m_progress.setMessage(getString(R.string.message_loading));
			m_progress.setIndeterminate(false);
			m_progress.show();
		}

		@Override
		protected PlayRecord doInBackground(String... args) {
			return m_store.load(args[0]);
		}

		@Override
		protected void onPostExecute(PlayRecord result) {
			if (result != null)
				setPlayRecord(result);
			m_progress.dismiss();
		}
	}

	private class MusicAdapter extends ArrayAdapter<MusicInfo> {
		private static final int LIST_ITEM_ID = R.layout.music_item;

		private LayoutInflater m_inflater;
		private String[] m_trial_labels;
		private Drawable[] m_clear_icons;

		private List<MusicInfo> m_musics;
		private int m_difficulty;
		private int m_sortBy;

		public MusicAdapter(Context context, int difficulty) {
			super(context, LIST_ITEM_ID);
			m_inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			Resources resources = context.getResources();
			m_trial_labels = resources.getStringArray(R.array.trial_labels);
			m_clear_icons = new Drawable[] {
					resources.getDrawable(R.drawable.clear0),
					resources.getDrawable(R.drawable.clear1),
					resources.getDrawable(R.drawable.clear2),
					resources.getDrawable(R.drawable.clear3),
			};

			m_difficulty = difficulty;
			m_sortBy = 0;
		}

		public void setData(List<MusicInfo> music) {
			m_musics = music;
			setDifficulty(m_difficulty);
		}

		public List<MusicInfo> getData() {
			return m_musics;
		}

		public void setDifficulty(int difficulty) {
			m_difficulty = difficulty;
			setNotifyOnChange(false);
			clear();
			for (MusicInfo music: m_musics) {
				if (music.records[m_difficulty] != null)
					add(music);
			}
			sortBy(m_sortBy);
			setNotifyOnChange(true);
			notifyDataSetChanged();
		}

		private void setText(View view, int id, String text) {
			TextView tv = (TextView)view.findViewById(id);
			tv.setText(text);
		}

		private void setText(View view, int id, String format, Object... args) {
			setText(view, id, String.format(format, args));
		}

		private void setImage(View view, int id, Drawable image) {
			ImageView iv = (ImageView)view.findViewById(id);
			iv.setImageDrawable(image);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView != null ? convertView : m_inflater.inflate(LIST_ITEM_ID, null);
			MusicInfo music = getItem(position);
			if (music != null) {
				setText(view, R.id.music_title, music.title);
				setImage(view, R.id.cover_art, m_store.getCoverart(music));
				ScoreRecord score = music.records[m_difficulty];
				setText(view, R.id.difficulty, "★%d", score.difficulty);
				setImage(view, R.id.clear_status, m_clear_icons[score.clear_status]);
				setText(view, R.id.trial_status, m_trial_labels[score.trial_status]);
				setText(view, R.id.high_score, "スコア: %dpts", score.high_score);
				setText(view, R.id.achivement, "達成率: %d.%02d%%", score.achievement/100, score.achievement%100);
			}
			return view;
		}

		public int sortOrder() {
			return m_sortBy;
		}

		public void sortBy(int order) {
			switch (order) {
			case MENU_SORT_BY_DIFFICULTY:
				sortByDifficulty();
				break;
			case MENU_SORT_BY_SCORE:
				sortByScore();
				break;
			case MENU_SORT_BY_ACHIEVEMENT:
				sortByAchievement();
				break;
			case MENU_SORT_BY_CLEAR_STATUS:
				sortByClearStatus();
				break;
			case MENU_SORT_BY_TRIAL_STATUS:
				sortByTrialStatus();
				break;
			}

			m_sortBy = order;
		}

		private void sortByDifficulty() {
			sort(new Comparator<MusicInfo>() {
				public int compare(MusicInfo lhs, MusicInfo rhs) {
					return lhs.records[m_difficulty].difficulty - rhs.records[m_difficulty].difficulty;
				}
			});
		}

		private void sortByScore() {
			sort(new Comparator<MusicInfo>() {
				public int compare(MusicInfo lhs, MusicInfo rhs) {
					return lhs.records[m_difficulty].high_score - rhs.records[m_difficulty].high_score;
				}
			});
		}

		private void sortByAchievement() {
			sort(new Comparator<MusicInfo>() {
				public int compare(MusicInfo lhs, MusicInfo rhs) {
					return lhs.records[m_difficulty].achievement - rhs.records[m_difficulty].achievement;
				}
			});
		}

		private void sortByClearStatus() {
			sort(new Comparator<MusicInfo>() {
				public int compare(MusicInfo lhs, MusicInfo rhs) {
					int res = lhs.records[m_difficulty].clear_status
							- rhs.records[m_difficulty].clear_status;
					if (res != 0)
						return res;
					return lhs.records[m_difficulty].difficulty - rhs.records[m_difficulty].difficulty;
				}
			});
		}

		private void sortByTrialStatus() {
			sort(new Comparator<MusicInfo>() {
				public int compare(MusicInfo lhs, MusicInfo rhs) {
					final int lhs_clear = lhs.records[m_difficulty].clear_status;
					final int rhs_clear = rhs.records[m_difficulty].clear_status;
					final int lhs_trial = lhs.records[m_difficulty].trial_status;
					final int rhs_trial = rhs.records[m_difficulty].trial_status;

					int res = (rhs_clear - rhs_trial) - (lhs_clear - lhs_trial);
					if (res != 0)
						return res;
					res = lhs_trial - rhs_trial;
					if (res != 0)
						return res;
					return lhs.records[m_difficulty].difficulty - rhs.records[m_difficulty].difficulty;
				}
			});
		}
	}
}
