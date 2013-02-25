package net.diva.browser.page;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.RivalInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
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
		menu.findItem(R.id.item_rival_update).setEnabled(DdN.isAllowUpdateRivalData(m_preferences));
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
			rivalSelect();
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
				rival.rival_code = edit.getText().toString();
				(new AsyncTask<Void, Void, Void>(){
					@Override
					protected Void doInBackground(Void... params) {
						try {
							if (rival.regist()) {
								rival.getData();
								m_store.update(rival);
								rivalChanged(rival);
							} else {
								Toast.makeText(getActivity(), R.string.rival_regist_error_msg, Toast.LENGTH_LONG).show();
							}
						} catch (Exception e) {
							Toast.makeText(getActivity(), "何かおかしいです", Toast.LENGTH_SHORT).show();
						}
						return null;
					}
				}).execute();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private void rivalSelect() {
		final List<RivalInfo> rivalList = m_store.getRivalList();
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
				(new AsyncTask<Void, Void, Void>(){
					@Override
					protected Void doInBackground(Void... params) {
						try{
							if(rivalList.get(which).regist())
								rivalChanged(rivalList.get(which));
							else
								Toast.makeText(getActivity(), R.string.rival_regist_error_msg, Toast.LENGTH_LONG).show();
						}catch(Exception e){
							Toast.makeText(getActivity(), "何かおかしいです", Toast.LENGTH_SHORT).show();
						}
						return null;
					}
				}).execute();
			}
		});
		builder.show();
	}

	private void rivalRemoveSelect(){
		(new AsyncTask<Void, Void, Void>(){
			AlertDialog.Builder builder;
			@Override
			protected Void doInBackground(Void... params) {

				try {
					ServiceClient service = DdN.getServiceClient();
					if(!service.isLogin())
						service.login();
					final List<RivalInfo> rivalList = service.getAllRivalInfo();
					final String[] nameList = new String[rivalList.size()];
					for (int i = 0; i < rivalList.size(); i++) {
						RivalInfo r = rivalList.get(i);
						nameList[i] = r.rival_name;
					}
					builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(R.string.rival_remove_title);
					builder.setItems(nameList, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							try {
								if(!rivalList.get(which).remove())
									Toast.makeText(getActivity(), R.string.rival_remove_error_msg, Toast.LENGTH_LONG).show();
							} catch (Exception e) {
								Toast.makeText(getActivity(), "何かおかしいです", Toast.LENGTH_SHORT).show();
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
			@Override
			protected void onPostExecute(Void result) {
				if(builder != null)
					builder.show();
			}

		}).execute();
	}

	private void rivalUpdate() {
		final List<RivalInfo> rivalList = m_store.getRivalList();
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
				(new AsyncTask<Void, Void, Void>(){

					@Override
					protected Void doInBackground(Void... params) {
						RivalInfo rival = rivalList.get(which);
						try{
							rival.getData();
						}catch(Exception e){
							Toast.makeText(getActivity(), "何かおかしいです", Toast.LENGTH_SHORT).show();
							return null;
						}
						m_store.update(rival);
						m_store.loadRivalScore(DdN.getPlayRecord().musics);
						DdN.notifyPlayRecordChanged();
						Editor editor = m_preferences.edit();
						DdN.setUpdateRivalTime(editor);
						editor.commit();

						return null;
					}

				}).execute();
			}
		});
		builder.show();
	}

	private void rivalChanged(RivalInfo rival){
		final Editor editor = m_preferences.edit();
		editor.putString("rival_code", rival.rival_code);
		editor.putString("rival_name", rival.rival_name);
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
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			List<RivalInfo> rivalList = service.getSyncableRivalScore();
			for(RivalInfo rival : rivalList){
				m_store.update(rival);
			}

			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result != null && result)
				DdN.notifyPlayRecordChanged();
		}
	}

}
