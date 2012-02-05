package net.diva.browser.page;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.HistoryTable;
import net.diva.browser.history.HistoryDetailActivity;
import net.diva.browser.history.UpdateHistoryTask;
import net.diva.browser.model.History;
import net.diva.browser.util.DdNUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/** @author silvia */
public class HistoryFragment extends ListFragment implements PageAdapter {
	private HistoryAdapter m_adapter;

	private static int VIEW_COUNT = 20;

	private static String m_hist_back;

	private static String m_music_id;
	private static int m_rank;
	private static int m_date;
	private static boolean m_lock;
	private static String sort;
	private static boolean isReverseOrder;
	static { initOrder(); }

	private View mFooter;
	private int addCount;
	private List<String> dateList;
	private String where;
	private List<String> params;
	private String orderBy;

	private int selectedItemPosition;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		m_adapter = new HistoryAdapter(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.basic_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		refresh();

		ListView listView = getListView();
		listView.setFocusable(true);
		listView.setTextFilterEnabled(true);

		setListAdapter(m_adapter);
		registerForContextMenu(listView);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.history_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.history_delete_selected).setEnabled(m_music_id != null);
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
			initOrder();
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
		History history = m_adapter.getItem(info.position);
		menu.setHeaderTitle(DdNUtil.getMusicTitle(history.music_id));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (getListView().getPositionForView(info.targetView) != info.position)
			return false;

		History history = m_adapter.getItem(info.position);
		switch (item.getItemId()) {
		case R.id.hist_search_music_and_rank:
			initOrder();
			m_music_id = history.music_id;
			m_rank = history.rank;
			refresh();
			return true;
		case R.id.hist_search_music:
			initOrder();
			m_music_id = history.music_id;
			refresh();
			return true;
		case R.id.hist_search_date:
			initOrder();
			m_date = history.play_date;
			refresh();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private View getFooter() {
		if (mFooter == null) {
			mFooter = getActivity().getLayoutInflater().inflate(R.layout.listview_footer, null);
		}
		return mFooter;
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {

		if(v.getId() == R.id.history_footer){
			int next = addCount * VIEW_COUNT;
			if(next >= dateList.size()){
				try{
					getListView().removeFooterView(getFooter());
				}catch(ClassCastException e){
					e.printStackTrace();
				}
				return;
			}

			addCount++;
			int limit = dateList.size() < addCount * VIEW_COUNT ? dateList.size() : addCount * VIEW_COUNT;
			m_adapter.addData(DdN.getLocalStore().getPlayHistoryList(dateList.subList(next, limit), orderBy));
			getListView().invalidateViews();

			return;
		}

		selectedItemPosition = position;
		History history = m_adapter.getItem(position);
		if (history == null)
			return;

		Intent i = new Intent(getActivity(), HistoryDetailActivity.class);
		i.putExtra("history", history);
		startActivityForResult(i, 0);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			refresh();
		} else if (resultCode == Activity.RESULT_FIRST_USER) {
			History history = (History) data.getSerializableExtra("history");
			m_adapter.setItem(selectedItemPosition, history);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public CharSequence getTitle() {
		return getActivity().getResources().getString(R.string.hist_tab_title);
	}

	@Override
	public boolean onSearchRequested() {
		return false;
	}

	private void delete(final boolean isAllDel) {
		Resources res = getResources();

		String confirmMsg = String.format(
				"ロックされていないプレイ履歴を全削除します。よろしいですか？\n対象曲：%s\n難易度：%s",
				isAllDel ? res.getString(R.string.all_music) : (DdNUtil.getMusicTitle(m_music_id)),
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
				String music_id = isAllDel ? null : m_music_id;
				DdN.getLocalStore().deleteHistory(music_id, m_rank, (int) sec);
				refresh();
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
		FileOutputStream fos = null;

		try {
			if(!csv.exists())
				if(!csv.createNewFile())
					return false;

			List<byte[]> data = DdN.getLocalStore().csvExport();

			fos = new FileOutputStream(csv);
			for(byte[] b : data)
				fos.write(b);

		} catch(Exception e) {
			return false;
		} finally {
			if(fos != null){
				try{
					fos.close();
				}catch(IOException e){
				}
			}
		}

		return true;
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
		BufferedReader br = null;
		try{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(csv)));

			String line;
			while((line = br.readLine()) != null) {
				String[] data = line.split(",", -1);
				History h = new History();
				h.music_id = data[0];
				h.rank = Integer.valueOf(data[1]);
				h.play_date = Integer.valueOf(data[2]);
				h.play_place = data[3];
				h.clear_status = Integer.valueOf(data[4]);
				h.achievement = Integer.valueOf(data[5]);
				h.score = Integer.valueOf(data[6]);
				h.cool = Integer.valueOf(data[7]);
				h.cool_per = Integer.valueOf(data[8]);
				h.fine = Integer.valueOf(data[9]);
				h.fine_per = Integer.valueOf(data[10]);
				h.safe = Integer.valueOf(data[11]);
				h.safe_per = Integer.valueOf(data[12]);
				h.sad = Integer.valueOf(data[13]);
				h.sad_per = Integer.valueOf(data[14]);
				h.worst = Integer.valueOf(data[15]);
				h.worst_per = Integer.valueOf(data[16]);
				h.combo = Integer.valueOf(data[17]);
				h.challange_time = Integer.valueOf(data[18]);
				h.hold = Integer.valueOf(data[19]);
				h.trial = Integer.valueOf(data[20]);
				h.trial_result = Integer.valueOf(data[21]);
				h.module1_id = data[22];
				h.module2_id = data[23];
				h.se_id = data[24];
				h.skin_id = data[25];
				h.lock = Integer.valueOf(data[26]);
				DdN.getLocalStore().insert(h);
			}

		}catch(Exception e){
			//mouyada-
		}finally{
			try{
				if(br !=null)
					br.close();
			}catch(Exception e){
				// mouyada-
			}
		}
	}

	private void selectSortOrder() {
		Context context = getActivity();
		final List<String> orderBys = Arrays.asList(context.getResources().getStringArray(R.array.hist_sort_order_values));

		View custom = LayoutInflater.from(context).inflate(R.layout.descending_order, null);
		final CheckBox descending = (CheckBox)custom.findViewById(R.id.descending);
		descending.setChecked(isReverseOrder);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.item_sort);
		builder.setItems(R.array.hist_sort_order_names,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				HistoryFragment.isReverseOrder = descending.isChecked();
				HistoryFragment.sort = orderBys.get(which);
				refresh();
				dialog.dismiss();
			}
		});
		builder.setView(custom);
		builder.show();
	}

	private static void initOrder(){
		m_music_id = null;
		m_rank = -1;
		m_date = -1;
		m_lock = false;
		sort = HistoryTable.PLAY_DATE;
		isReverseOrder = true;
	}

	public void refresh(){
		ListView v = getListView();
		if(v != null && v.getFooterViewsCount() == 0)
			v.addFooterView(getFooter());
		createOrder();
		dateList = DdN.getLocalStore().getPlayHistoryList(where, params, orderBy);
		int limit = dateList.size() < VIEW_COUNT ? dateList.size() : VIEW_COUNT;
		addCount = 1;
		m_adapter.setData(DdN.getLocalStore().getPlayHistoryList(dateList.subList(0, limit), orderBy));
		m_adapter.notifyDataSetChanged();
	}

	private void createOrder(){
		List<String> wl = new ArrayList<String>();
		params = new ArrayList<String>();
		String hatena = " = ? ";
		String and = " and ";

		if(m_music_id != null){
			wl.add(HistoryTable.MUSIC_ID + hatena);
			params.add(m_music_id);
		}

		if(-1 < m_rank && m_rank < 4){
			wl.add(HistoryTable.RANK + hatena);
			params.add(String.valueOf(m_rank));
		}

		if(-1 < m_date){
			wl.add(" ? <= " + HistoryTable.PLAY_DATE + and + HistoryTable.PLAY_DATE + " < ? ");
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(m_date * 1000L);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			params.add(String.valueOf(cal.getTimeInMillis() / 1000));
			cal.add(Calendar.DATE, 1);
			params.add(String.valueOf((cal.getTimeInMillis() / 1000) - 1));
		}

		if(m_lock){
			wl.add(HistoryTable.LOCK + hatena);
			params.add(String.valueOf(1));
		}

		StringBuffer sb = new StringBuffer();

		for(int i = 0; i < wl.size(); i++){
			if(!sb.toString().isEmpty())
				sb.append(and);
			sb.append(wl.get(i));
		}
		where = sb.toString();

		if("lock".equals(sort)){
			orderBy = sort + (isReverseOrder ? " DESC" : "") + ", play_date DESC";
		}else{
			orderBy = sort + (isReverseOrder ? " DESC" : "");
		}
	}

	public static void setMusicId(String music_id){
		m_music_id = music_id;
	}
	public static void setRank(int rank){
		m_rank = rank;
	}
	public static void setHistBack(String hist_back){
		m_hist_back = hist_back;
	}

	public static String getHistBack(){
		return m_hist_back;
	}

	private static class HistoryAdapter extends BaseAdapter {
		Context m_context;
		List<History> m_historys = Collections.emptyList();

		int[] rankColor = {R.color.easy, R.color.normal, R.color.hard, R.color.extreme};

		public HistoryAdapter(Context context) {
			m_context = context;
		}

		void setData(List<History> historys) {
			m_historys = historys;
			if (m_historys.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		void addData(List<History> historys){
			m_historys.addAll(historys);
		}

		public int getCount() {
			return m_historys.size();
		}

		public History getItem(int position) {
			return m_historys.get(position);
		}

		public void setItem(int position, History history){
			m_historys.set(position, history);
			notifyDataSetChanged();
		}

		public long getItemId(int position) {
			return position;
		}

		private class Holder {
			TextView play_date;
			TextView rank;
			TextView clear_status;
			TextView music_title;
			TextView score;
			ImageView lock;
			TextView achievement;

			private Holder(View view) {
				play_date = (TextView)view.findViewById(R.id.play_date);
				rank = (TextView)view.findViewById(R.id.rank);
				clear_status = (TextView)view.findViewById(R.id.clear_status);
				music_title = (TextView)view.findViewById(R.id.music_title);
				lock = (ImageView)view.findViewById(R.id.lock);
				score = (TextView)view.findViewById(R.id.score);
				achievement = (TextView)view.findViewById(R.id.achievement);
			}

			void attach(History history) {
				play_date.setText(String.format("[%s]", history.getPlayDateStr()));
				rank.setText(DdNUtil.getDifficultyName(history.rank));
				rank.setTextColor(m_context.getResources().getColor(rankColor[history.rank]));
				clear_status.setText(m_context.getResources().getStringArray(R.array.clear_status_names)[history.clear_status]);
				music_title.setText(DdNUtil.getMusicTitle(history.music_id));
				lock.setVisibility(history.isLocked() ? View.VISIBLE : View.INVISIBLE);
				score.setText(String.format("%dpts", history.score));
				achievement.setText(String.format("%d.%02d%%", history.achievement/100, history.achievement%100));
			}

		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Holder holder;

			if (view != null)
				holder = (Holder)view.getTag();
			else {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(R.layout.history_item, parent, false);
				holder = new Holder(view);
				view.setTag(holder);
			}
			History history = getItem(position);
			if(history != null)
				holder.attach(history);

			return view;
		}
	}
}