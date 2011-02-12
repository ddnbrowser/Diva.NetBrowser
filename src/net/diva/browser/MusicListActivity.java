package net.diva.browser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN.Account;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.NoLoginException;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.settings.ModuleListActivity;
import net.diva.browser.util.ProgressTask;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

		DdN.Account account = DdN.Account.load(m_preferences);
		if (account == null) {
			DdN.Account.input(this, new PlayRecordDownloader());
			return;
		}

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
		case R.id.item_contest:
			openPage("/divanet/contest/info/");
			break;
		case R.id.item_statistics:
			openPage("/divanet/pv/statistics/");
			break;
		case R.id.item_game_settings: {
			Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
			startActivityForResult(intent, R.id.item_game_settings);
		}
			break;
		case R.id.item_tool_settings: {
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, R.id.item_tool_settings);
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
		MusicInfo music = m_adapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.item_update:
			new MusicUpdateTask().execute(music);
			return true;
		case R.id.item_set_module: {
			Intent intent = new Intent(getApplicationContext(), ModuleListActivity.class);
			intent.putExtra("request", 1);
			intent.putExtra("id", music.id);
			intent.putExtra("part", music.part);
			intent.putExtra("vocal1", music.vocal1);
			intent.putExtra("vocal2", music.vocal2);
			startActivityForResult(intent, R.id.item_set_module);
		}
			return true;
		case R.id.item_reset_module:
			resetModule(music);
			return true;
		case R.id.item_ranking:
			openPage(String.format("/divanet/ranking/summary/%s/0", music.id));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_game_settings:
			if (resultCode == RESULT_OK)
				refresh();
			break;
		case R.id.item_tool_settings:
			if (m_preferences.getBoolean("download_rankin", false))
				DownloadRankingService.reserve(this);
			else
				DownloadRankingService.cancel(this);
			break;
		case R.id.item_set_module:
			if (resultCode == RESULT_OK)
				setModule(data);
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	public void refresh() {
		PlayRecord record = DdN.getPlayRecord();

		String title = DdN.getTitle(record.title_id);
		if (title == null) {
			title = "取得中...";
			new TitleDownloader().execute(record);
		}
		m_player_name.setText(record.player_name);
		m_level_rank.setText(rankText(record, title));

		m_adapter.setData(record.musics);
	}

	public void setDifficulty(int difficulty) {
		m_adapter.setDifficulty(difficulty);
		m_preferences.edit().putInt("difficulty", difficulty).commit();
		for (int i = 0; i < m_buttons.length; ++i)
			m_buttons[i].setEnabled(i != difficulty);
	}

	private String rankText(PlayRecord record, String title) {
		final int[] rank_points = getResources().getIntArray(R.array.rank_points);

		int point = rankPoint(record);
		int rank = 0;
		while (rank < rank_points.length && point >= rank_points[rank])
			point -= rank_points[rank++];
		rank += point/150;
		point %= 150;

		int next = point - (rank < rank_points.length ? rank_points[rank] : 150);
		return String.format("%s %s (%dpts)", record.level, title, next);
	}

	private int rankPoint(PlayRecord record) {
		int point = 0;
		for (MusicInfo m: record.musics)
			point += m.rankPoint();
		return point;
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
		startActivity(intent);
	}

	private void setModule(Intent data) {
		final MusicInfo music = DdN.getPlayRecord().getMusic(data.getStringExtra("id"));
		final Module vocal1 = DdN.getModule(data.getStringExtra("vocal1"));
		final Module vocal2 = DdN.getModule(data.getStringExtra("vocal2"));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(music.title);
		builder.setMessage(R.string.message_set_module);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new SetModuleTask(music).execute(vocal1, vocal2);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private void resetModule(final MusicInfo music) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(music.title);
		builder.setMessage(R.string.message_reset_module);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new ResetModuleTask().execute(music);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
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
				new MusicUpdateTask().execute(music);
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

				m_store.insert(record);
				SharedPreferences.Editor editor = m_preferences.edit();
				account.putTo(editor);
				editor.putLong("last_updated", System.currentTimeMillis());
				editor.commit();
				return DdN.setPlayRecord(record);
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
				refresh();
			m_progress.dismiss();
		}
	}

	private class MusicUpdateTask extends BasicTask<MusicInfo> {
		public MusicUpdateTask() {
			super(R.string.message_music_updating);
		}

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params) throws Exception {
			final MusicInfo music = params[0];
			service.update(music);
			m_store.update(music);
			return Boolean.TRUE;
		}
	}

	private class PlayRecordLoader extends ProgressTask<DdN.Account, Void, Boolean> {
		PlayRecordLoader() {
			super(MusicListActivity.this, R.string.message_loading);
		}

		@Override
		protected Boolean doInBackground(DdN.Account... args) {
			DdN.setTitles(m_store.getTitles());
			DdN.setPlayRecord(m_store.load(args[0].access_code));
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null)
				refresh();
		}
	}

	private class TitleDownloader extends AsyncTask<PlayRecord, Void, String> {
		@Override
		protected String doInBackground(PlayRecord... params) {
			PlayRecord record = params[0];
			try {
				ServiceClient service = DdN.getServiceClient();
				if (!service.isLogin()) {
					record = DdN.setPlayRecord(service.login());
					m_store.update(record);
				}

				List<TitleInfo> titles = service.getTitles(DdN.getTitles());
				m_store.updateTitles(titles);
				DdN.setTitles(titles);
				String title = DdN.getTitle(record.title_id);
				if (title != null)
					return rankText(record, title);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			return rankText(record, "取得失敗");
		}

		@Override
		protected void onPostExecute(String result) {
			m_level_rank.setText(result);
		}
	}

	private class SetModuleTask extends BasicTask<Module> {
		private MusicInfo m_music;

		public SetModuleTask(MusicInfo music) {
			super(R.string.summary_applying);
			m_music = music;
		}

		@Override
		protected Boolean doTask(ServiceClient service, Module... params) throws Exception {
			Module vocal1 = params[0];
			Module vocal2 = params[1];

			if (vocal2 == null) {
				service.setIndividualModule(m_music.id, vocal1.id);
				m_music.vocal1 = vocal1.id;
				m_music.vocal2 = null;
			}
			else {
				service.setIndividualModule(m_music.id, vocal1.id, vocal2.id);
				m_music.vocal1 = vocal1.id;
				m_music.vocal2 = vocal2.id;
			}
			m_store.updateModule(m_music);
			return isRequiredRefresh();
		}
	}

	private class ResetModuleTask extends BasicTask<MusicInfo> {
		public ResetModuleTask() {
			super(R.string.summary_applying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params) throws Exception {
			MusicInfo music = params[0];
			service.resetIndividualModule(music.id);
			music.vocal1 = music.vocal2 = null;
			m_store.updateModule(music);
			return isRequiredRefresh();
		}
	}

	private abstract class BasicTask<Param> extends ServiceTask<Param, Void, Boolean> {
		PlayRecord m_record;

		BasicTask(int message) {
			super(MusicListActivity.this, message);
			m_record = DdN.getPlayRecord();
		}

		boolean isRequiredRefresh() {
			return m_record != DdN.getPlayRecord();
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null && result)
				refresh();
		}
	}
}
