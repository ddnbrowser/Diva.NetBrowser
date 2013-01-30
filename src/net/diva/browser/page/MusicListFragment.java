package net.diva.browser.page;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.MusicAdapter;
import net.diva.browser.MusicDetailActivity;
import net.diva.browser.R;
import net.diva.browser.SortOrder;
import net.diva.browser.WebBrowseActivity;
import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.compatibility.Compatibility;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.util.CheckedFrameLayout;
import net.diva.browser.util.StringUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;

public abstract class MusicListFragment extends ListFragment
		implements DdN.Observer, PageAdapter {
	protected View m_buttons[];
	protected ListView m_list;
	protected MusicAdapter m_adapter;
	protected Handler m_handler = new Handler();

	protected SharedPreferences m_preferences;
	protected SharedPreferences m_localPrefs;
	protected LocalStore m_store;

	protected List<MusicInfo> m_selections = new ArrayList<MusicInfo>();

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		m_preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		m_localPrefs = activity.getSharedPreferences(getArguments().getString("tag"), Context.MODE_PRIVATE);
		m_store = LocalStore.instance(activity);
		m_adapter = new MyAdapter(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.music_list, container, false);
		m_buttons = new View[] {
				v.findViewById(R.id.button_easy),
				v.findViewById(R.id.button_normal),
				v.findViewById(R.id.button_hard),
				v.findViewById(R.id.button_extreme),
		};
		for (int i = 0; i < m_buttons.length; ++i) {
			final int d = i;
			m_buttons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					setDifficulty(d, true);
				}
			});
		}
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setListAdapter(m_adapter);

		m_list = getListView();
		m_list.setFocusable(true);
		m_list.setTextFilterEnabled(true);
		m_list.setOnTouchListener(new OnTouchListener());
		registerForContextMenu(m_list);
	}

	@Override
	public void onResume() {
		super.onResume();
		getLayout();
		m_list.setFastScrollEnabled(DdN.Settings.enableFastScroll);

		setDifficulty(m_localPrefs.getInt("difficulty", 3), false);
		m_adapter.setSortOrder(
				SortOrder.fromOrdinal(m_localPrefs.getInt("sort_order", 0)),
				m_localPrefs.getBoolean("reverse_order", false));
		PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
	}

	protected void getLayout(){
		String name = m_preferences.getString("music_layout", null);
		if (name != null) {
			int id = getResources().getIdentifier(name, "layout", getActivity().getPackageName());
			if (id != 0 && m_adapter.setLayout(id))
				setListAdapter(m_adapter);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(m_list.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		final Editor editor = m_localPrefs.edit();
		editor.putInt("difficulty", m_adapter.getDifficulty());
		editor.putInt("sort_order", m_adapter.sortOrder().ordinal());
		editor.putBoolean("reverse_order", m_adapter.isReverseOrder());
		editor.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update_all:
			updateAll();
			break;
		case R.id.item_update_bulk:
			if (isSelectionMode()) {
				updateMusics(m_selections);
				setSelectionMode(false);
			}
			else {
				updateMusics(getMusics());
			}
			break;
		case R.id.item_update_new:
			new UpdateNewTask().execute();
			break;
		case R.id.item_update_in_history:
			new UpdateInHistory().execute();
			break;
		case R.id.item_search:
			activateTextFilter();
			break;
		case R.id.item_sort:
			m_adapter.selectSortOrder();
			break;
		case R.id.item_enable_selection:
			setSelectionMode(true);
			break;
		case R.id.item_cancel_selection:
			setSelectionMode(false);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.list_context, menu);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		MusicInfo music = m_adapter.getItem(info.position);
		menu.setHeaderTitle(music.title);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (getListView().getPositionForView(info.targetView) != info.position)
			return false;

		MusicInfo music = m_adapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.item_update:
			updateMusic(music);
			return true;
		case R.id.item_select:
			m_selections.add(music);
			if (!isSelectionMode())
				setSelectionMode(true);
			return true;
		case R.id.item_edit_reading:
			editTitleReading(music);
			return true;
		case R.id.item_ranking:
			WebBrowseActivity.open(getActivity(), String.format("/divanet/ranking/summary/%s/0", music.id));
			return true;
		case R.id.item_copy_title:
			Compatibility.copyText(getActivity(), music.title);
			return true;
		case R.id.item_play_record:
			try{
				TabHost host = (TabHost)getActivity().findViewById(android.R.id.tabhost);
				HistoryFragment.setMusicId(music.id);
				HistoryFragment.setRank(m_adapter.getDifficulty());
				HistoryFragment.setHistBack(host.getCurrentTabTag());
				host.setCurrentTabByTag("history");
			}catch(Exception e){
				return false;
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onSearchRequested() {
		activateTextFilter();
		return false;
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		if (noMusic)
			m_adapter.notifyDataSetChanged();
		else {
			m_adapter.setData(getMusics());
			m_adapter.update();
		}
	}

	public void onUpdate(MyList myList, boolean noMusic) {
		// do nothing.
	}

	public void setDifficulty(int difficulty, boolean update) {
		m_adapter.setDifficulty(difficulty);
		if (update)
			m_adapter.update();
		for (int i = 0; i < m_buttons.length; ++i)
			m_buttons[i].setEnabled(i != difficulty);
	}

	@Override
	public CharSequence getTitle() {
		StringBuilder sb = new StringBuilder();
		makeTitle(sb, DdN.getPlayRecord());
		return sb.toString();
	}

	private String rankText(PlayRecord record, String title) {
		int[] next = new int[1];
		record.rank(next);
		return String.format("%s %s (%dpts)", record.level, title, -next[0]);
	}

	protected abstract List<MusicInfo> getMusics();

	protected void makeTitle(StringBuilder title, PlayRecord record) {
		title.append(rankText(record, record.title));
	}

	protected void updateAll() {
		DownloadPlayRecord task = new DownloadPlayRecord(getActivity());

		DdN.Account account = DdN.Account.load(m_preferences);
		if (account == null) {
			DdN.Account.input(getActivity(), task);
			return;
		}

		task.execute(account);
	}

	protected void updateMusic(MusicInfo music) {
		new UpdateSingleMusic().execute(music);
	}

	protected void updateMusics(List<MusicInfo> musics) {
		new UpdateMultiMusic().execute(musics.toArray(new MusicInfo[musics.size()]));
	}

	private void activateTextFilter() {
		m_list.requestFocus();
		m_handler.postDelayed(new Runnable() {
			public void run() {
				InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(m_list, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 100);
	}

	private void editTitleReading(final MusicInfo music) {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.input_reading, null);
		final EditText edit = (EditText)view.findViewById(R.id.reading);
		final boolean isReadingEmpty = "".equals(music.reading);
		if(isReadingEmpty){
			edit.setText(LocalStore.instance(getActivity()).getReading(music.title));
		}else{
			edit.setText(music.reading);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
		if(!isReadingEmpty)
			builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		MusicInfo music = m_adapter.getItem(position);
		if (music == null)
			return;

		if (isSelectionMode()) {
			if (list.isItemChecked(position))
				m_selections.add(music);
			else
				m_selections.remove(music);
		}
		else {
			Intent i = new Intent(getActivity(), MusicDetailActivity.class);
			i.putExtra("id", music.id);
			startActivity(i);
		}
	}

	private class OnTouchListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
		private GestureDetector m_detector;

		public OnTouchListener() {
			m_detector = new GestureDetector(getActivity(), this);
		}

		public boolean onTouch(View view, MotionEvent event) {
			return m_detector.onTouchEvent(event);
		}

		private MusicInfo getItem(MotionEvent e) {
			int position = m_list.pointToPosition((int)e.getX(), (int)e.getY());
			if (position != AdapterView.INVALID_POSITION)
				return m_adapter.getItem(position);
			else
				return null;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			MusicInfo music = getItem(e);
			if (music != null && !isSelectionMode()) {
				new UpdateSingleMusic().execute(music);
				return true;
			}
			return super.onDoubleTap(e);
		}
	}

	private class UpdateNewTask extends ServiceTask<Void, Integer, PlayRecord> {
		private Context m_context;

		public UpdateNewTask() {
			super(getActivity(), R.string.message_downloading);
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

			service.updatePublishOrder(musics, record.nextPublishOrder());
			publishProgress(0, musics.size());
			for (MusicInfo music: musics) {
				music.reading = m_store.getReading(music.title);
				music.ordinal = StringUtils.forLexicographical(music.reading);
				service.update(music);
				service.cacheContent(music.coverart, music.getCoverArtPath(m_context));
				if (music.voice1 < 0) {
					String[] voices = service.getVoice(music.id);
					music.voice1 = DdN.getVoice(voices[0]);
					music.voice2 = DdN.getVoice(voices[1]);
				}
				publishProgress(1);
			}
			m_store.insert(musics);

			final Editor editor = m_preferences.edit();
			DdN.setUpdateTime(editor, musics.size());
			editor.commit();

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

	private class UpdateSingleMusic extends ServiceTask<MusicInfo, Void, Boolean> {
		public UpdateSingleMusic() {
			super(getActivity(), R.string.message_updating);
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

	private class UpdateMultiMusic extends ServiceTask<MusicInfo, Integer, Boolean> {
		public UpdateMultiMusic() {
			super(getActivity(), R.string.message_updating);
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params) throws Exception {
			publishProgress(0, params.length);
			for (MusicInfo music: params) {
				service.update(music);
				m_store.update(music);
				publishProgress(1);
			}
			final Editor editor = m_preferences.edit();
			DdN.setUpdateTime(editor, params.length);
			editor.commit();
			return Boolean.TRUE;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values.length > 1) {
				m_progress.setMax(values[1]);
				m_progress.setIndeterminate(false);
			}
			m_progress.incrementProgressBy(values[0]);
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null && result)
				DdN.notifyPlayRecordChanged();
		}
	}

	private class UpdateInHistory extends UpdateMultiMusic {
		boolean m_hasUnknown;

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params)
				throws Exception {
			List<String> ids = new ArrayList<String>();
			long lastPlayed = m_preferences.getLong("last_played", 0);
			lastPlayed = service.getMusicsInHistory(ids, lastPlayed);
			if (ids.isEmpty())
				return Boolean.FALSE;

			List<MusicInfo> musics = new ArrayList<MusicInfo>(ids.size());
			PlayRecord record = DdN.getPlayRecord();
			for (String id: ids) {
				MusicInfo m = record.getMusic(id);
				if (m != null)
					musics.add(m);
				else
					m_hasUnknown = true;
			}

			super.doTask(service, musics.toArray(new MusicInfo[musics.size()]));
			m_preferences.edit().putLong("last_played", lastPlayed).commit();
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			super.onResult(result);
			if (!m_hasUnknown)
				return;

			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(R.string.found_unknown_musics);
			b.setNegativeButton(android.R.string.no, null);
			b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new UpdateNewTask().execute();
				}
			});
			b.show();
		}
	}

	protected boolean isSelectionMode() {
		return getView() != null && getListView().getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE;
	}

	protected void setSelectionMode(boolean on) {
		if (on) {
			m_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		}
		else {
			m_list.setChoiceMode(ListView.CHOICE_MODE_NONE);
			m_selections.clear();
		}
		m_adapter.notifyDataSetChanged();
		Compatibility.invalidateOptionsMenu(this);
	}

	private class MyAdapter extends MusicAdapter {
		int checkMarkId;

		MyAdapter(Context context) {
			super(context);

			TypedValue value = new TypedValue();
			context.getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, value, true);
			checkMarkId = value.resourceId;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			view = super.getView(position, view, parent);
			if (view instanceof CheckedFrameLayout) {
				if (isSelectionMode()) {
					((CheckedFrameLayout) view).setCheckMarkDrawable(checkMarkId);
					MusicInfo music = getItem(position);
					m_list.setItemChecked(position, m_selections.contains(music));
				}
				else {
					((CheckedFrameLayout) view).setCheckMarkDrawable(0);
				}
			}
			return view;
		}
	}
}
