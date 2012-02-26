package net.diva.browser.page;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.diva.browser.R;
import net.diva.browser.db.HistoryStore;
import net.diva.browser.db.HistoryTable;
import net.diva.browser.history.DownloadHistoryService;
import net.diva.browser.history.HistoryDetailActivity;
import net.diva.browser.history.HistorySerializer;
import net.diva.browser.model.History;
import net.diva.browser.util.DdNUtil;
import net.diva.browser.util.ProgressTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/** @author silvia */
public class HistoryFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final String KEY_MUSIC = HistoryFragment.class.getName() + ":music";
	public static final String KEY_RANK = HistoryFragment.class.getName() + ":rank";
	public static final String KEY_DATE = HistoryFragment.class.getName() + ":date";
	public static final String KEY_ORDER = HistoryFragment.class.getName() + ":order";
	public static final String KEY_REVERSE = HistoryFragment.class.getName() + ":reverse";

	private HistoryStore m_store;
	private HistoryAdapter m_adapter;

	private DownloadHistoryService m_service;
	private ServiceConnection m_connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			DownloadHistoryService.LocalBinder binder = (DownloadHistoryService.LocalBinder)service;
			m_service = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			m_service = null;
		}
	};

	private Bundle m_args = new Bundle();

	private String m_music_title = null;
	private int m_rank = -1;
	private long m_date = -1;
	private String m_sortOrder = HistoryTable.PLAY_DATE;
	private boolean m_isReverseOrder = true;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		m_store = HistoryStore.getInstance(activity);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.no_record));
		setHasOptionsMenu(true);

		Bundle args = savedInstanceState;
		if (savedInstanceState == null)
			args = getArguments();
		if (args != null) {
			if (args.containsKey(KEY_ORDER))
				m_sortOrder = args.getString(KEY_ORDER);
			m_isReverseOrder = args.getBoolean(KEY_REVERSE, m_isReverseOrder);
			if (args.containsKey(KEY_MUSIC))
				m_music_title = args.getString(KEY_MUSIC);
			m_rank = args.getInt(KEY_RANK, m_rank);
			m_date = args.getLong(KEY_DATE, m_date);
		}
		setSortOrder(m_sortOrder, m_isReverseOrder);
		setFilterCondition(m_music_title, m_rank, m_date);

		ListView listView = getListView();
		listView.setFocusable(true);
		listView.setTextFilterEnabled(true);
		registerForContextMenu(listView);

		m_adapter = new HistoryAdapter(getActivity());
		setListAdapter(m_adapter);
		setListShown(false);

		getLoaderManager().initLoader(0, m_args, this);
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putString(KEY_MUSIC, m_music_title);
		bundle.putInt(KEY_RANK, m_rank);
		bundle.putLong(KEY_DATE, m_date);
		bundle.putString(KEY_ORDER, m_sortOrder);
		bundle.putBoolean(KEY_REVERSE, m_isReverseOrder);
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(getActivity(), DownloadHistoryService.class);
		getActivity().bindService(intent, m_connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (m_service != null) {
			getActivity().unbindService(m_connection);
			m_service = null;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.history_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.setGroupEnabled(R.id.group_require_records, m_adapter != null && !m_adapter.isEmpty());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history_update:
			new UpdateTask(getActivity(), R.string.message_downloading).execute(m_service);
			break;
		case R.id.history_sort:
			selectSortOrder();
			break;
		case R.id.history_delete:
			deleteHistories();
			break;
		case R.id.history_export:
			exportHistories();
			break;
		case R.id.history_import:
			importHistories();
			break;
		case R.id.history_awake:
			setFilterCondition(null, -1, -1);
			refresh();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.history_context, menu);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		History history = m_adapter.getHistory(info.position);
		menu.setHeaderTitle(history.music_title);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (getListView().getPositionForView(info.targetView) != info.position)
			return false;

		History history = m_adapter.getHistory(info.position);
		switch (item.getItemId()) {
		case R.id.hist_search_music_and_rank:
			setFilterCondition(history.music_title, history.rank, -1);
			refresh();
			return true;
		case R.id.hist_search_music:
			setFilterCondition(history.music_title, -1, -1);
			refresh();
			return true;
		case R.id.hist_search_date:
			setFilterCondition(null, -1, history.play_date);
			refresh();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		Intent i = new Intent(getActivity(), HistoryDetailActivity.class);
		i.putExtra("history_id", id);
		startActivity(i);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = null;
		if (args != null) {
			selection = args.getString("selection");
			selectionArgs = args.getStringArray("selectionArgs");
			sortOrder = args.getString("sortOrder");
		}
		return new CursorLoader(getActivity(),
				HistoryStore.URI_HISTORIES,
				HistoryAdapter.COLUMNS,
				selection,
				selectionArgs,
				sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		m_adapter.swapCursor(data);
		if (isResumed())
			setListShown(true);
		else
			setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		m_adapter.swapCursor(null);
	}

	private void deleteHistories() {
		final CharSequence all = getText(R.string.all);
		final String message = getString(R.string.hist_confirm_delete_message,
				(m_music_title == null ? all : m_music_title),
				(m_rank == -1 ? all : DdNUtil.getDifficultyName(m_rank)),
				(m_date == -1 ? all : android.text.format.DateFormat.format("yyyy/MM/dd", m_date)));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.hist_confirm_delete_dialog));
		builder.setMessage(message);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				List<String> args = new ArrayList<String>();
				String selection = buildSelection(m_music_title, m_rank, m_date, true, args);
				m_store.deleteHistories(selection, args.toArray(new String[args.size()]));
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private static File getBackupDirectory() {
		return new File(Environment.getExternalStorageDirectory(), "net.diva.browser");
	}

	private static DateFormat getExportFileFormat() {
		return new SimpleDateFormat("'history_'yyyyMMdd'T'HHmmss'_exported.csv'");
	}

	private void exportHistories(){
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.hist_confirm_export_dialog));
		builder.setMessage(getString(R.string.hist_confirm_export_msg));
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new Exporter(getActivity(), m_store).execute();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private static class Exporter extends ProgressTask<Void, Void, CharSequence> {
		HistoryStore m_store;

		Exporter(Context context, HistoryStore store) {
			super(context, R.string.histories_exporting);
			m_store = store;
		}

		@Override
		protected CharSequence doInBackground(Void... params) {
			if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
				return m_context.getText(R.string.unmounted_sdcard);

			File dir = getBackupDirectory();
			if (!dir.exists())
				dir.mkdirs();

			final DateFormat fmt = getExportFileFormat();
			File file = new File(dir, fmt.format(new Date()));
			HistorySerializer serializer = new HistorySerializer(m_store);
			try {
				final int exported = serializer.exportTo(new FileOutputStream(file));
				if (exported > 0)
					return m_context.getString(R.string.history_exported, exported);
				else
					return m_context.getText(R.string.no_history_exported);
			}
			catch (IOException e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}

		@Override
		protected void onResult(CharSequence message) {
			if (message != null)
				Toast.makeText(m_context, message, Toast.LENGTH_SHORT).show();
		}
	}

	private void importHistories(){
		File dir = getBackupDirectory();
		if (!dir.exists()) {
			Toast.makeText(getActivity(), R.string.no_exported_files, Toast.LENGTH_SHORT).show();
			return;
		}

		final File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches("history_\\d{8}T\\d{6}_exported\\.csv$");
			}
		});
		if (files.length == 0) {
			Toast.makeText(getActivity(), R.string.no_exported_files, Toast.LENGTH_SHORT).show();
			return;
		}
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				return rhs.compareTo(lhs);
			}
		});

		final DateFormat ifmt = getExportFileFormat();
		final DateFormat ofmt = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");

		String[] items = new String[files.length];
		for (int i = 0; i < files.length; ++i) {
			try {
				items[i] = ofmt.format(ifmt.parse(files[i].getName()));
			}
			catch (ParseException e) {
				e.printStackTrace();
			}
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_import_history);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new Importer(getActivity(), m_store).execute(files[which]);
			}
		});
		builder.show();
	}

	private static class Importer extends ProgressTask<File, Void, CharSequence> {
		HistoryStore m_store;

		Importer(Context context, HistoryStore store) {
			super(context, R.string.histories_importing);
			m_store = store;
		}

		@Override
		protected CharSequence doInBackground(File... params) {
			HistorySerializer serializer = new HistorySerializer(m_store);
			try {
				final int imported = serializer.importFrom(new FileInputStream(params[0]));
				if (imported > 0)
					return m_context.getString(R.string.history_imported, imported);
				else
					return m_context.getText(R.string.no_history_imported);
			}
			catch (IOException e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}

		@Override
		protected void onResult(CharSequence message) {
			if (message != null)
				Toast.makeText(m_context, message, Toast.LENGTH_SHORT).show();
		}
	}

	private void selectSortOrder() {
		Context context = getActivity();
		final String[] orderBys = context.getResources().getStringArray(R.array.hist_sort_order_values);

		View custom = LayoutInflater.from(context).inflate(R.layout.descending_order, null);
		final CheckBox descending = (CheckBox)custom.findViewById(R.id.descending);
		descending.setChecked(m_isReverseOrder);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.item_sort);
		builder.setItems(R.array.hist_sort_order_names,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setSortOrder(orderBys[which], descending.isChecked());
				refresh();
				dialog.dismiss();
			}
		});
		builder.setView(custom);
		builder.show();
	}

	private void refresh() {
		getLoaderManager().restartLoader(0, m_args, this);
	}

	private void setSortOrder(String sortOrder, boolean reverse) {
		m_sortOrder = sortOrder;
		m_isReverseOrder = reverse;

		StringBuilder sb = new StringBuilder(sortOrder);
		if (reverse)
			sb.append(" DESC");
		if (HistoryTable.LOCK.equals(sortOrder))
			sb.append(',').append(HistoryTable.PLAY_DATE).append(" DESC");

		m_args.putString("sortOrder", sb.toString());
	}

	private void setFilterCondition(String music_title, int rank, long date) {
		m_music_title = music_title;
		m_rank = rank;
		m_date = date;

		List<String> args = new ArrayList<String>();
		String selection = buildSelection(music_title, rank, date, false, args);
		m_args.putString("selection", selection);
		if (!args.isEmpty())
			m_args.putStringArray("selectionArgs", args.toArray(new String[args.size()]));
		else
			m_args.putStringArray("selectionArgs", null);
	}

	private String buildSelection(String music_title, int rank, long date, boolean ignoreLocked,
			List<String> args) {
		StringBuilder sb = new StringBuilder();
		if (music_title != null) {
			sb.append(" AND ").append(HistoryTable.MUSIC_TITLE).append("=?");
			args.add(music_title);
		}
		if (rank >=0 ) {
			sb.append(" AND ").append(HistoryTable.RANK).append("=?");
			args.add(String.valueOf(rank));
		}
		if (date >= 0) {
			sb.append(" AND ?<=")
			.append(HistoryTable.PLAY_DATE).append(" AND ")
			.append(HistoryTable.PLAY_DATE).append("<?");
			Calendar t = Calendar.getInstance();
			t.setTimeInMillis(date);
			Calendar d = new GregorianCalendar(t.get(Calendar.YEAR), t.get(Calendar.MONTH), t.get(Calendar.DAY_OF_MONTH));
			args.add(String.valueOf(d.getTimeInMillis()));
			d.add(Calendar.DATE, 1);
			args.add(String.valueOf(d.getTimeInMillis()));
		}
		if (ignoreLocked) {
			sb.append(" AND ").append(HistoryTable.LOCK).append("<>?");
			args.add(String.valueOf(HistoryTable.LOCKED));
		}

		return sb.length() > 5 ? sb.substring(5) : null;
	}

	private static class UpdateTask extends ProgressTask<DownloadHistoryService, Integer, String> {
		public UpdateTask(Context context, int message) {
			super(context, message);
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}

		@Override
		protected String doInBackground(DownloadHistoryService... args) {
			try {
				if (!args[0].downloadHistory(
						new DownloadHistoryService.ProgressListener() {
							@Override
							public void onProgress(int value, int max) {
								publishProgress(value, max);
							}
						}))
					return m_context.getString(R.string.no_got_histories);
			}
			catch (Exception e) {
				e.printStackTrace();
				return m_context.getString(R.string.get_histories_failed);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values.length > 1) {
				m_progress.setMax(values[1]);
				m_progress.setIndeterminate(false);
			}
			m_progress.setProgress(values[0]);
		}

		@Override
		protected void onResult(String error) {
			if (error != null)
				Toast.makeText(m_context, error, Toast.LENGTH_LONG).show();
		}
	}

	private static class HistoryAdapter extends ResourceCursorAdapter {
		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("[yyyy/MM/dd]");
		private static final int[] RANK_COLORS = { R.color.easy, R.color.normal, R.color.hard, R.color.extreme };
		private static final String[] COLUMNS = new String[] {
			HistoryTable.PLAY_DATE,
			HistoryTable.RANK,
			HistoryTable.CLEAR_STATUS,
			HistoryTable.MUSIC_TITLE,
			HistoryTable.SCORE,
			HistoryTable.ACHIEVEMENT,
			HistoryTable.LOCK,
			HistoryTable._ID,
		};

		private class Holder {
			TextView date;
			TextView rank;
			TextView status;
			TextView music;
			TextView score;
			TextView achievement;
			ImageView lock;
		}

		private int[] m_from;

		public HistoryAdapter(Context context) {
			super(context, R.layout.history_item, null, true);
		}

		public History getHistory(int position) {
			Cursor c = (Cursor)getItem(position);
			if (c == null)
				return null;

			return getHistory(c);
		}

		public History getHistory(Cursor c) {
			History h = new History();
			h.play_date = c.getLong(m_from[0]);
			h.rank = c.getInt(m_from[1]);
			h.clear_status = c.getInt(m_from[2]);
			h.music_title = c.getString(m_from[3]);
			h.score = c.getInt(m_from[4]);
			h.achievement = c.getInt(m_from[5]);
			h.lock = c.getInt(m_from[6]);
			return h;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = super.newView(context, cursor, parent);
			Holder h = new Holder();
			h.date = (TextView)view.findViewById(R.id.play_date);
			h.rank = (TextView)view.findViewById(R.id.rank);
			h.status = (TextView)view.findViewById(R.id.clear_status);
			h.music = (TextView)view.findViewById(R.id.music_title);
			h.score = (TextView)view.findViewById(R.id.score);
			h.achievement = (TextView)view.findViewById(R.id.achievement);
			h.lock = (ImageView)view.findViewById(R.id.lock);
			view.setTag(h);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor c) {
			final History d = getHistory(c);
			final Holder h = (Holder)view.getTag();
			h.date.setText(DATE_FORMAT.format(new Date(d.play_date)));
			h.rank.setText(DdNUtil.getDifficultyName(d.rank));
			h.rank.setTextColor(context.getResources().getColor(RANK_COLORS[d.rank]));
			h.status.setText(context.getResources().getStringArray(R.array.clear_status_names)[d.clear_status]);
			h.music.setText(d.music_title);
			h.score.setText(String.format("%dpts", d.score));
			h.achievement.setText(String.format("%d.%02d%%", d.achievement/100, d.achievement%100));
			h.lock.setVisibility(d.isLocked() ? View.VISIBLE : View.INVISIBLE);
		}

		private void findColumns(String[] from) {
			if (mCursor != null) {
				int i;
				int count = from.length;
				if (m_from == null || m_from.length != count) {
					m_from = new int[count];
				}
				for (i = 0; i < count; i++) {
					m_from[i] = mCursor.getColumnIndexOrThrow(from[i]);
				}
			}
			else {
				m_from = null;
			}
		}

		@Override
		public Cursor swapCursor(Cursor c) {
			if (m_from == null)
				findColumns(COLUMNS);

			Cursor res = super.swapCursor(c);
			findColumns(COLUMNS);
			return res;
		}
	}
}
