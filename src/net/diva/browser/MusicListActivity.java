package net.diva.browser;

import java.util.Arrays;
import java.util.List;

import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.util.StringUtils;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MusicListActivity extends ListActivity implements DdN.Observer {
	private View m_buttons[];
	private MusicAdapter m_adapter;
	private Handler m_handler = new Handler();

	protected SharedPreferences m_preferences;
	protected LocalStore m_store;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_list);
		registerForContextMenu(getListView());

		m_preferences = PreferenceManager.getDefaultSharedPreferences(this);
		m_store = LocalStore.instance(this);

		m_adapter = new MusicAdapter(this);
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
		String name = m_preferences.getString("music_layout", null);
		if (name != null) {
			int id = getResources().getIdentifier(name, "layout", getPackageName());
			if (id != 0 && m_adapter.setLayout(id))
				setListAdapter(m_adapter);
		}

		setDifficulty(m_preferences.getInt("difficulty", 3), false);
		m_adapter.setSortOrder(
				SortOrder.fromOrdinal(m_preferences.getInt("sort_order", 0)),
				m_preferences.getBoolean("reverse_order", false));
		PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
		DdN.registerObserver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		DdN.unregisterObserver(this);
		final Editor editor = m_preferences.edit();
		editor.putInt("difficulty", m_adapter.getDifficulty());
		editor.putInt("sort_order", m_adapter.sortOrder().ordinal());
		editor.putBoolean("reverse_order", m_adapter.isReverseOrder());
		editor.commit();
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
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			updateAll();
			break;
		case R.id.item_update_new:
			new UpdateNewTask().execute();
			break;
		case R.id.item_search:
			activateTextFilter();
			break;
		case R.id.item_sort:
			selectSortOrder();
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
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		MusicInfo music = m_adapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.item_update:
			new MusicUpdateTask().execute(music);
			return true;
		case R.id.item_edit_mylist:
			editMyList(music);
			return true;
		case R.id.item_edit_reading:
			editTitleReading(music);
			return true;
		case R.id.item_ranking:
			WebBrowseActivity.open(this, String.format("/divanet/ranking/summary/%s/0", music.id));
			return true;
		case R.id.item_copy_title:
			ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			if (clipboard != null)
				clipboard.setText(music.title);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		setTitle(rankText(record, DdN.getTitle(record.title_id)));
		if (noMusic)
			m_adapter.notifyDataSetChanged();
		else {
			m_adapter.setData(getMusics(record));
			m_adapter.update();
		}
	}

	public void setDifficulty(int difficulty, boolean update) {
		m_adapter.setDifficulty(difficulty);
		if (update)
			m_adapter.update();
		for (int i = 0; i < m_buttons.length; ++i)
			m_buttons[i].setEnabled(i != difficulty);
	}

	private String rankText(PlayRecord record, String title) {
		int[] next = new int[1];
		record.rank(next);
		return String.format("%s %s (-%dpts)", record.level, title, next[0]);
	}

	protected List<MusicInfo> getMusics(PlayRecord record) {
		return record.musics;
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

	private void selectSortOrder() {
		final List<String> values = Arrays.asList(getResources().getStringArray(R.array.sort_order_values));
		final int checked = values.indexOf(m_adapter.sortOrder().name());

		View custom = getLayoutInflater().inflate(R.layout.descending_order, null);
		final CheckBox descending = (CheckBox)custom.findViewById(R.id.descending);
		descending.setChecked(m_adapter.isReverseOrder());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.item_sort);
		builder.setSingleChoiceItems(R.array.sort_order_names, checked,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				m_adapter.sortBy(SortOrder.valueOf(values.get(which)), descending.isChecked());
				dialog.dismiss();
			}
		});
		builder.setView(custom);
		builder.show();
	}

	private void editMyList(final MusicInfo music) {
		final List<MyList> myLists = m_store.loadMyLists();
		final int size = myLists.size();
		CharSequence[] items = new CharSequence[size];
		for (int i = 0; i < size; ++i)
			items[i] = myLists.get(i).name;
		final boolean[] values = m_store.containedInMyLists(myLists, music.id);
		final boolean[] before = values.clone();

		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(music.title);
		b.setMultiChoiceItems(items, values, new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
			}
		});
		b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new UploadMyListTask(myLists, music).execute(before, values);
			}
		});
		b.show();
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
				music.ordinal = StringUtils.forLexicographical(music.reading);
				m_store.update(music);
				dialog.dismiss();
				DdN.notifyPlayRecordChanged();
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
				music.reading = m_store.getReading(music.title);
				music.ordinal = StringUtils.forLexicographical(music.reading);
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
				DdN.notifyPlayRecordChanged();
		}
	}

	private class UploadMyListTask extends ServiceTask<boolean[], Void, Boolean> {
		List<MyList> m_myLists;
		MusicInfo m_music;

		public UploadMyListTask(List<MyList> myLists, MusicInfo music) {
			super(MusicListActivity.this, R.string.message_updating);
			m_myLists = myLists;
			m_music = music;
		}

		@Override
		protected Boolean doTask(ServiceClient service, boolean[]... params) throws Exception {
			boolean[] oldValues = params[0];
			boolean[] newValues = params[1];
			for (int i = 0; i < oldValues.length; ++i) {
				if (oldValues[i] == newValues[i])
					continue;
				MyList myList = m_myLists.get(i);
				if (newValues[i])
					service.addToMyList(myList.id, m_music.id);
				else
					service.removeFromMyList(myList.id, m_music.id);

				m_store.updateMyList(myList.id, service.getMyList(myList.id));
			}
			return Boolean.TRUE;
		}
	}
}
