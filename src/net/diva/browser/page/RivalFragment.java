package net.diva.browser.page;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.SortOrder;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.RivalInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class RivalFragment extends MusicListFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		getLayout();
		m_list.setFastScrollEnabled(DdN.Settings.enableFastScroll);

		setDifficulty(m_localPrefs.getInt("difficulty_rival_tab", 3), false);
		m_adapter.setSortOrder(
				SortOrder.fromOrdinal(m_localPrefs.getInt("sort_order_rival_tab", 0)),
				m_localPrefs.getBoolean("reverse_order_rival_tab", false));
		PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
	}

	@Override
	public void onPause() {
		super.onPause();
		final Editor editor = m_localPrefs.edit();
		editor.putInt("difficulty_rival_tab", m_adapter.getDifficulty());
		editor.putInt("sort_order_rival_tab", m_adapter.sortOrder().ordinal());
		editor.putBoolean("reverse_order_rival_tab", m_adapter.isReverseOrder());
		editor.commit();
	}

	@Override
	protected void getLayout() {
		int id = getResources().getIdentifier("music_item4r", "layout", getActivity().getPackageName());
		if (id != 0 && m_adapter.setLayout(id))
			setListAdapter(m_adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.rival_options, menu);
		inflater.inflate(R.menu.main_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.item_rival_sync).setEnabled(DdN.isAllowSyncRivalData(m_preferences));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_sort:
			m_adapter.selectSortOrder(true);
			break;
		case R.id.item_rival_input:
			rivalInput();
			break;
		case R.id.item_rival_change:
			rivalChange();
			break;
		case R.id.item_rival_remove:
			rivalRemoveSelect();
			break;
		case R.id.item_rival_update:
			rivalUpdate();
			break;
		case R.id.item_rival_sync:
			rivalSync();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		getActivity().getMenuInflater().inflate(R.menu.rival_context, menu);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
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
		case R.id.rival_cxt_update:
			updateMusic(music);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		MusicInfo music = m_adapter.getItem(position);
		if (music == null)
			return;
	}

	@Override
	public CharSequence getTitle() {
		String name = m_preferences.getString("rival_name", "");
		if (!"".equals(name)) {
			StringBuilder sb = new StringBuilder();
			sb.append("vs ").append(name);
			return sb.toString();
		}

		return "ライバル未設定";
	}

	@Override
	protected void updateMusic(MusicInfo music){

	}

	private void rivalSync() {
		new RivalSyncTask().execute();
		Editor editor = m_preferences.edit();
		DdN.setUpdateSyncRivalTime(editor);
		editor.commit();
	}

	private void rivalInput() {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.input_rival_code, null);
		final TextView edit = (TextView) view.findViewById(R.id.rival_code);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.rival_input_title);
		builder.setView(view);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				final RivalInfo rival = new RivalInfo();
				rival.rival_code = edit.getText().toString().toUpperCase();
				(new ServiceTask<Void, Void, Integer>(getActivity(), R.string.rival_regist_msg){
					@Override
					protected Integer doTask(ServiceClient service, Void... params) {
						try {
							if (rival.regist(service)) {
								if(rival.getData(service)){
									m_store.update(rival);
									rivalChanged(rival);
								}else{
									return R.string.rival_get_info_error_msg;
								}
							} else {
								return R.string.rival_regist_error_msg;
							}
						} catch (Exception e) {
							return  R.string.rival_regist_error_msg;
						}
						return  R.string.rival_regist_success_msg;
					}
					@Override
					protected void onResult(Integer resultMsgId){
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(resultMsgId);
						AlertDialog dialog = builder.create();
						dialog.setCancelable(true);
						dialog.setCanceledOnTouchOutside(true);
						dialog.show();
					}
				}).execute();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private void rivalChange() {
		final List<RivalInfo> rivalList = m_store.getRivalList();
		if(rivalList.size() == 0){
			Toast.makeText(getActivity(), "データが登録されていません", Toast.LENGTH_LONG).show();
			return;
		}
		final String[] nameList = new String[rivalList.size()];
		for (int i = 0; i < rivalList.size(); i++) {
			RivalInfo rival = rivalList.get(i);
			nameList[i] = rival.rival_name;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.rival_change_title);
		builder.setItems(nameList, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, final int which) {
				dialog.dismiss();
				(new ServiceTask<Void, Void, Integer>(getActivity(), R.string.rival_change_msg){
					@Override
					protected Integer doTask(ServiceClient service, Void... params) {
						try{
							RivalInfo rival = rivalList.get(which);
							if(rival.regist(service))
								rivalChanged(rival);
							else
								return R.string.rival_change_error_msg;
						}catch(Exception e){
							return R.string.rival_change_error_msg;
						}
						return R.string.rival_change_success_msg;
					}
					@Override
					protected void onResult(Integer resultMsgId){
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(resultMsgId);
						AlertDialog dialog = builder.create();
						dialog.setCancelable(true);
						dialog.setCanceledOnTouchOutside(true);
						dialog.show();
					}
				}).execute();
			}
		});
		builder.show();
	}

	private void rivalRemoveSelect(){
		(new ServiceTask<Void, Void, AlertDialog.Builder>(getActivity(), R.string.rival_get_info_msg){
			@Override
			protected AlertDialog.Builder doTask(final ServiceClient service, Void... params) {
				try {
					final List<RivalInfo> rivalList = service.getAllRivalInfo();
					if(rivalList.size() == 0){
						Toast.makeText(getActivity(), "ライバルが登録されていません", Toast.LENGTH_LONG).show();
						return null;
					}
					final String[] nameList = new String[rivalList.size()];
					for (int i = 0; i < rivalList.size(); i++) {
						RivalInfo r = rivalList.get(i);
						nameList[i] = r.rival_name;
					}
					final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(R.string.rival_remove_title);
					builder.setItems(nameList, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							final RivalInfo rival = rivalList.get(which);
							(new ServiceTask<Void, Void, Integer>(getActivity(), R.string.rival_remove_msg) {
								protected Integer doTask(ServiceClient service, Void... params) {
									try {
										if (!rival.remove(service))
											return R.string.rival_remove_error_msg;
									} catch (Exception e) {
										return R.string.rival_remove_error_msg;
									}
									return R.string.rival_remove_success_msg;
								}
								@Override
								protected void onResult(Integer resultMsgId){
									AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
									builder.setMessage(resultMsgId);
									AlertDialog dialog = builder.create();
									dialog.setCancelable(true);
									dialog.setCanceledOnTouchOutside(true);
									dialog.show();

									if(resultMsgId == R.string.rival_remove_success_msg){
										rivalChanged(null);
									}
								}
							}).execute();
						}
					});
					return builder;
				} catch (Exception e) {
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.rival_get_info_error_msg);
				return builder;
			}
			@Override
			protected void onResult(AlertDialog.Builder builder) {
				if(builder != null){
					AlertDialog dialog = builder.create();
					dialog.setCancelable(true);
					dialog.setCanceledOnTouchOutside(true);
					dialog.show();
				}
			}
		}).execute();
	}

	private void rivalUpdate() {
		final List<RivalInfo> rivalList = m_store.getRivalList();
		if(rivalList.size() == 0){
			Toast.makeText(getActivity(), "データが登録されていません", Toast.LENGTH_LONG).show();
			return;
		}
		final String[] nameList = new String[rivalList.size()];
		for (int i = 0; i < rivalList.size(); i++) {
			RivalInfo rival = rivalList.get(i);
			nameList[i] = rival.rival_name;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.rival_change_title);
		builder.setItems(nameList, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, final int which) {
				dialog.dismiss();
				(new ServiceTask<Void, Void, Integer>(getActivity(), R.string.rival_update_msg){
					@Override
					protected Integer doTask(ServiceClient service, Void... params) {
						RivalInfo rival = rivalList.get(which);
						try{
							rival.getData(service);
							m_store.update(rival);
							m_store.loadRivalScore(DdN.getPlayRecord().musics);
							DdN.notifyPlayRecordChanged();
						}catch(Exception e){
							return R.string.rival_update_error_msg;
						}
						return R.string.rival_update_success_msg;
					}
					@Override
					protected void onResult(Integer resultMsgId){
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(resultMsgId);
						AlertDialog dialog = builder.create();
						dialog.setCancelable(true);
						dialog.setCanceledOnTouchOutside(true);
						dialog.show();
					}
				}).execute();
			}
		});
		builder.show();
	}

	private void rivalChanged(RivalInfo rival){
		final Editor editor = m_preferences.edit();
		if (rival != null) {
			editor.putString("rival_code", rival.rival_code);
			editor.putString("rival_name", rival.rival_name);
		}else{
			editor.putString("rival_code", "");
			editor.putString("rival_name", "");
		}
		editor.commit();
		m_store.loadRivalScore(DdN.getPlayRecord().musics);
		DdN.notifyPlayRecordChanged();
	}

	protected List<MusicInfo> getMusics() {
		return DdN.getPlayRecord().musics;
	}

	private class RivalSyncTask extends ServiceTask<Void, Integer, Boolean> {
		public RivalSyncTask() {
			super(getActivity(), R.string.message_updating);
		}

		@Override
		protected void onPreExecute() {
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			super.onPreExecute();
		}

		@Override
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			List<RivalInfo> rivalList = service.getAllRivalInfo();
			m_progress.setMax(rivalList.size());
			publishProgress(0, rivalList.size());
			for(int i = 0; i < rivalList.size(); i++){
				RivalInfo rival = rivalList.get(i);
				rival.getData(service);
				m_store.update(rival);
				publishProgress(1);
			}
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

}
