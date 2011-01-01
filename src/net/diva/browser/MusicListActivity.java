package net.diva.browser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN.Account;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.NoLoginException;
import net.diva.browser.service.ServiceClient;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MusicListActivity extends ListActivity {
	private TextView m_player_name;
	private TextView m_level_rank;
	private View m_buttons[];
	private MusicAdapter m_adapter;

	private SharedPreferences m_preferences;
	private ServiceClient m_service;
	private LocalStore m_store;

	private PlayRecord m_record;

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

		DdN.Account account = DdN.Account.load(m_preferences);
		if (account == null) {
			DdN.Account.input(this, new PlayRecordDownloader());
			return;
		}
		m_service = DdN.getServiceClient(account);

		new PlayRecordLoader().execute(account);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.list_options, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final long now = System.currentTimeMillis();
		boolean enable = now - m_preferences.getLong("last_updated", 0) > 12*60*60*1000;
		menu.findItem(R.id.item_update).setEnabled(enable);
		MenuItem sort = menu.findItem(m_adapter.sortOrder());
		if (sort != null)
			sort.setChecked(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (item.getGroupId()) {
		case R.id.group_sort:
			m_adapter.sortBy(id);
			return true;
		default:
			break;
		}

		switch (id) {
		case R.id.item_update:
			updateAll();
			break;
		case R.id.item_news:
			openPage("/divanet/menu/news/");
			break;
		case R.id.item_settings: {
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, R.id.item_settings);
		}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.list_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.item_update:
			new PlayRecordUpdater().execute(m_adapter.getItem(info.position));
			return true;
		case R.id.item_ranking:
			openPage(String.format("/divanet/ranking/summary/%s/0", m_adapter.getItem(info.position).id));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_settings:
			if (m_preferences.getBoolean("download_rankin", false))
				DownloadRankingService.reserve(this);
			else
				DownloadRankingService.cancel(this);
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	public void setPlayRecord(PlayRecord record) {
		setPlayRecord(record, record.musics);
	}

	public void setPlayRecord(PlayRecord record, List<MusicInfo> music) {
		m_record = record;

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
		for (MusicInfo m: m_record.musics)
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

	private void updateAll() {
		PlayRecordDownloader task = new PlayRecordDownloader();

		DdN.Account account = DdN.Account.load(m_preferences);
		if (account == null) {
			DdN.Account.input(this, task);
			return;
		}

		task.execute(account);
	}

	private void openPage(String relative) {
		Intent intent = new Intent(
				Intent.ACTION_VIEW, Uri.parse(DdN.url(relative)),
				getApplicationContext(), WebBrowseActivity.class);
		intent.putExtra("cookies", m_service.cookies());
		startActivity(intent);
	}

	private class OnTouchListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
		private GestureDetector m_detector;

		public OnTouchListener() {
			m_detector = new GestureDetector(MusicListActivity.this, this);
		}

		public boolean onTouch(View view, MotionEvent event) {
			return m_detector.onTouchEvent(event);
		}

		private MusicInfo getItem(MotionEvent e) {
			int position = getListView().pointToPosition((int)e.getX(), (int)e.getY());
			if (position != AdapterView.INVALID_POSITION)
				return m_adapter.getItem(position);
			else
				return null;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			MusicInfo music = getItem(e);
			if (music != null) {
				Intent i = new Intent(getApplicationContext(), MusicDetailActivity.class);
				i.putExtra("id", music.id);
				i.putExtra("title", music.title);
				i.putExtra("coverart", music.getCoverArtPath(getApplicationContext()).getAbsolutePath());
				startActivity(i);
				return true;
			}
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			MusicInfo music = getItem(e);
			if (music != null) {
				new PlayRecordUpdater().execute(music);
				return true;
			}
			return super.onDoubleTap(e);
		}
	}

	private class PlayRecordDownloader extends AsyncTask<DdN.Account, Integer, PlayRecord> {
		private ProgressDialog m_progress;

		public void cacheCoverart(MusicInfo music, ServiceClient service) {
			File cache = music.getCoverArtPath(getApplicationContext());
			if (cache.exists())
				return;

			try {
				FileOutputStream out = new FileOutputStream(cache);
				service.download(music.coverart, out);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(MusicListActivity.this);
			m_progress.setMessage(getString(R.string.message_downloading));
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			m_progress.setIndeterminate(false);
			m_progress.show();
		}

		@Override
		protected PlayRecord doInBackground(DdN.Account... args) {
			final Account account = args[0];
			ServiceClient service = DdN.getServiceClient(account);
			try {
				PlayRecord record = service.login();
				service.update(record);
				publishProgress(0, record.musics.size());

				for (MusicInfo music: record.musics) {
					service.update(music);
					cacheCoverart(music, service);
					publishProgress(1);
				}

				m_service = service;
				m_store.insert(record);
				SharedPreferences.Editor editor = m_preferences.edit();
				account.putTo(editor);
				editor.putLong("last_updated", System.currentTimeMillis());
				editor.commit();
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
				PlayRecord record = null;
				if (!m_service.isLogin())
					record = m_service.login();
				try {
					m_service.update(music);
				}
				catch (NoLoginException e) {
					record = m_service.login();
					m_service.update(music);
				}
				m_store.update(music);
				if (record == null)
					return m_record;
				else {
					m_store.update(record);
					record.musics = m_record.musics;
					return record;
				}
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

	private class PlayRecordLoader extends AsyncTask<DdN.Account, Void, PlayRecord> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(MusicListActivity.this);
			m_progress.setMessage(getString(R.string.message_loading));
			m_progress.setIndeterminate(false);
			m_progress.show();
		}

		@Override
		protected PlayRecord doInBackground(DdN.Account... args) {
			return m_store.load(args[0].access_code);
		}

		@Override
		protected void onPostExecute(PlayRecord result) {
			if (result != null)
				setPlayRecord(result);
			m_progress.dismiss();
		}
	}
}
