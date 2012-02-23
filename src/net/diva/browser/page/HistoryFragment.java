package net.diva.browser.page;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.diva.browser.R;
import net.diva.browser.db.HistoryStore;
import net.diva.browser.db.HistoryTable;
import net.diva.browser.history.HistoryDetailActivity;
import net.diva.browser.history.HistorySerializer;
import net.diva.browser.history.UpdateHistoryTask;
import net.diva.browser.model.History;
import net.diva.browser.util.DdNUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
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
			m_sortOrder = args.getString(KEY_ORDER, m_sortOrder);
			m_isReverseOrder = args.getBoolean(KEY_REVERSE, m_isReverseOrder);
			m_music_title = args.getString(KEY_MUSIC, m_music_title);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.history_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.history_delete_selected).setEnabled(m_music_title != null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history_update:
			new UpdateHistoryTask(this, R.string.message_downloading).execute();
			break;
		case R.id.history_sort:
			selectSortOrder();
			break;
		case R.id.history_delete_all:
			delete(true);
			break;
		case R.id.history_delete_selected:
			delete(false);
			break;
		case R.id.history_export:
			export();
			break;
		case R.id.history_import:
			csvImport();
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
		String sortOrder = HistoryTable.PLAY_DATE + " DESC";
		if (args != null) {
			selection = args.getString("selection", selection);
			selectionArgs = args.getStringArray("selectionArgs");
			sortOrder = args.getString("sortOrder", sortOrder);
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

	private void delete(final boolean isAllDel) {
		Resources res = getResources();

		String confirmMsg = String.format(
				"ロックされていないプレイ履歴を全削除します。よろしいですか？\n対象曲：%s\n難易度：%s",
				isAllDel ? res.getString(R.string.all_music) : m_music_title,
				isAllDel ? res.getString(R.string.all_difficulty) : DdNUtil.getDifficultyName(m_rank));
		Context context = getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(res.getString(R.string.hist_confirm_delete_dialog));
		builder.setMessage(confirmMsg);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				long sec = cal.getTimeInMillis() / 1000;
				m_store.deleteHistory(isAllDel ? null : m_music_title, m_rank, (int) sec);
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private void export(){
		Resources res = getResources();
		String confirmMsg = res.getString(R.string.hist_confirm_export_msg);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(res.getString(R.string.hist_confirm_export_dialog));
		builder.setMessage(confirmMsg);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(exportCsv()){
					Toast.makeText(getActivity(), "エクスポート成功", Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(getActivity(), "エクスポート失敗", Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private boolean exportCsv(){
		if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			return false;

		String outStragePath = Environment.getExternalStorageDirectory().getPath() + "/net.diva.browser";
		File dir = new File(outStragePath);
		if(!dir.exists())
			dir.mkdirs();

		String outputCsv = outStragePath + "/DdNB_history_"+ DdNUtil.now() + "_exported.csv";
		File csv = new File(outputCsv);
		try {
			if(!csv.createNewFile())
				return false;

			HistorySerializer serializer = new HistorySerializer(m_store);
			serializer.exportTo(new FileOutputStream(csv));
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void csvImport(){
		File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/net.diva.browser/");

		if(!dir.exists()){
			Toast.makeText(getActivity(), "ファイルがありません", Toast.LENGTH_LONG).show();
			return;
		}

		final File[] files = dir.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.matches("DdNB_history_\\d{10}_exported\\.csv$");
					}
				});

		if(files.length == 0){
			Toast.makeText(getActivity(), "ファイルがありません", Toast.LENGTH_LONG).show();
			return;
		}

		String[] items = new String[files.length];
		for(int i = 0; i < files.length; i++)
			items[i] = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date(1000L * Integer.valueOf(files[i].getName().substring(13, 23))));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.hist_menu_import);
		builder.setItems(items,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				importCsv(files[which]);
				dialog.dismiss();
			}
		});
		builder.show();
	}

	private void importCsv(File csv){
		HistorySerializer serializer = new HistorySerializer(m_store);
		try {
			serializer.importFrom(new FileInputStream(csv));
		}
		catch (IOException e) {
			e.printStackTrace();
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

		StringBuilder sb = new StringBuilder();
		List<String> args = new ArrayList<String>();
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

		if (!args.isEmpty()) {
			m_args.putString("selection", sb.substring(4));
			m_args.putStringArray("selectionArgs", args.toArray(new String[args.size()]));
		}
		else {
			m_args.putString("selection", null);
			m_args.putStringArray("selectionArgs", null);
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
