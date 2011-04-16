package net.diva.browser;

import java.util.List;

import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.settings.ModuleListActivity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MusicListActivity extends ListActivity implements DdN.Observer {
	private View m_buttons[];
	private MusicAdapter m_adapter;
	private Handler m_handler = new Handler();

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

		final boolean favorite = getIntent().getBooleanExtra("is_favorite", false);
		m_adapter = new MusicAdapter(this, favorite);
		setListAdapter(m_adapter);

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
					setDifficulty(d, true);
				}
			});
		}
		ListView list = getListView();
		list.setFocusable(true);
		list.setTextFilterEnabled(true);
		list.setOnTouchListener(new OnTouchListener());
	}

	@Override
	protected void onResume() {
		super.onResume();
		setDifficulty(m_preferences.getInt("difficulty", 3), false);
		PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
		DdN.registerObserver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		DdN.unregisterObserver(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.list_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final long now = System.currentTimeMillis();
		boolean enable_all = now - m_preferences.getLong("last_updated", 0) > 12*60*60*1000;
		menu.findItem(R.id.item_update).setVisible(enable_all);
		menu.findItem(R.id.item_update_new).setVisible(!enable_all);
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
		case R.id.item_update_new:
			new UpdateNewTask().execute();
			break;
		case R.id.item_search:
			activateTextFilter();
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
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		MusicInfo music = m_adapter.getItem(info.position);
		menu.setHeaderTitle(music.title);
		if (m_adapter.isFavorite()) {
			menu.findItem(R.id.item_add_favorite).setVisible(false);
			menu.findItem(R.id.item_remove_favorite).setEnabled(music.favorite);
		}
		else {
			menu.findItem(R.id.item_add_favorite).setEnabled(!music.favorite);
			menu.findItem(R.id.item_remove_favorite).setVisible(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		MusicInfo music = m_adapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.item_update:
			new MusicUpdateTask().execute(music);
			return true;
		case R.id.item_add_favorite:
			updateFavorite(music, true);
			return true;
		case R.id.item_remove_favorite:
			updateFavorite(music, false);
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
		case R.id.item_edit_reading:
			editTitleReading(music);
			return true;
		case R.id.item_ranking:
			WebBrowseActivity.open(this, String.format("/divanet/ranking/summary/%s/0", music.id));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_set_module:
			if (resultCode == RESULT_OK)
				setModule(data);
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		setTitle(rankText(record, DdN.getTitle(record.title_id)));
		if (!noMusic)
			m_adapter.setData(record.musics);
	}

	public void refreshList(boolean reload) {
		PlayRecord record = DdN.getPlayRecord();
		if (reload)
			m_adapter.setData(record.musics);
		else
			m_adapter.notifyDataSetChanged();
	}

	public void setDifficulty(int difficulty, boolean update) {
		m_adapter.setDifficulty(difficulty, update);
		if (update)
			m_preferences.edit().putInt("difficulty", difficulty).commit();
		for (int i = 0; i < m_buttons.length; ++i)
			m_buttons[i].setEnabled(i != difficulty);
	}

	private String rankText(PlayRecord record, String title) {
		int[] next = new int[1];
		record.rank(next);
		return String.format("%s %s (-%dpts)", record.level, title, next[0]);
	}

	private void updateAll() {
		DownloadPlayRecord task = new DownloadPlayRecord(this);

		DdN.Account account = DdN.Account.load(m_preferences);
		if (account == null) {
			DdN.Account.input(this, task);
			return;
		}

		task.execute(account);
	}

	private void activateTextFilter() {
		getListView().requestFocus();
		m_handler.postDelayed(new Runnable() {
			public void run() {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(getListView(), InputMethodManager.SHOW_IMPLICIT);
			}
		}, 100);
	}

	private void updateFavorite(MusicInfo music, boolean register) {
		music.favorite = register;
		m_store.updateFavorite(music);
		if (m_adapter.isFavorite())
			refreshList(true);
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

	private void editTitleReading(final MusicInfo music) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.input_reading, null);
		final EditText edit = (EditText)view.findViewById(R.id.reading);
		edit.setText(music.reading);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.input_reading);
		builder.setMessage(music.title);
		builder.setView(view);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				music.reading = edit.getText().toString();
				m_store.update(music);
				dialog.dismiss();
				refreshList(false);
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

	private class UpdateNewTask extends ServiceTask<Void, Integer, PlayRecord> {
		private Context m_context;

		public UpdateNewTask() {
			super(MusicListActivity.this, R.string.message_downloading);
			m_context = m_progress.getContext();
		}

		@Override
		protected void onPreExecute() {
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			super.onPreExecute();
		}

		@Override
		protected PlayRecord doTask(ServiceClient service, Void... params) throws Exception {
			PlayRecord record = DdN.getPlayRecord();

			List<MusicInfo> musics = service.getMusics();
			musics.removeAll(record.musics);
			if (musics.isEmpty())
				return null;

			publishProgress(0, musics.size());
			for (MusicInfo music: musics) {
				service.update(music);
				service.cacheContent(music.coverart, music.getCoverArtPath(m_context));
				publishProgress(1);
			}

			m_store.insert(musics);
			musics.addAll(0, record.musics);
			record.musics = musics;
			return record;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values.length > 1) {
				m_progress.setIndeterminate(false);
				m_progress.setMax(values[1]);
			}
			m_progress.incrementProgressBy(values[0]);
		}

		@Override
		protected void onResult(PlayRecord result) {
			if (result != null)
				DdN.setPlayRecord(result);
		}
	}

	private class MusicUpdateTask extends ServiceTask<MusicInfo, Void, Boolean> {
		public MusicUpdateTask() {
			super(MusicListActivity.this, R.string.message_updating);
		}

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params) throws Exception {
			final MusicInfo music = params[0];
			service.update(music);
			m_store.update(music);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null && result)
				refreshList(false);
		}
	}

	private class SetModuleTask extends ServiceTask<Module, Void, Boolean> {
		private MusicInfo m_music;

		public SetModuleTask(MusicInfo music) {
			super(MusicListActivity.this, R.string.summary_applying);
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
			return Boolean.TRUE;
		}
	}

	private class ResetModuleTask extends ServiceTask<MusicInfo, Void, Boolean> {
		public ResetModuleTask() {
			super(MusicListActivity.this, R.string.summary_applying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params) throws Exception {
			MusicInfo music = params[0];
			service.resetIndividualModule(music.id);
			music.vocal1 = music.vocal2 = null;
			m_store.updateModule(music);
			return Boolean.TRUE;
		}
	}
}
