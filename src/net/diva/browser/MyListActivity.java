package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MyListActivity extends MusicListActivity {
	private MyList m_myList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		m_myList = new MyList(intent.getIntExtra("id", 0), intent.getStringExtra("name"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mylist_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_sync_mylist:
			new SyncMyList().execute(m_myList.id);
			break;
		case R.id.item_edit_name:
			break;
		case R.id.item_delete_mylist:
			deleteMyList();
			break;
		case R.id.item_activate_mylist:
			activateMyList();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected List<MusicInfo> getMusics(PlayRecord record) {
		List<String> ids = m_store.loadMyList(m_myList.id);
		List<MusicInfo> musics = new ArrayList<MusicInfo>(ids.size());
		for (String id: ids)
			musics.add(record.getMusic(id));
		return musics;
	}

	private void deleteMyList() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(m_myList.name);
		builder.setMessage(R.string.confirm_delete_mylist);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new DeleteMyList().execute(m_myList.id);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private void activateMyList() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(m_myList.name);
		builder.setMessage(R.string.confirm_activate_mylist);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new ActivateMyList().execute(m_myList.id);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private class SyncMyList extends ServiceTask<Integer, Void, MyList> {
		SyncMyList() {
			super(MyListActivity.this, R.string.synchronizing);
		}

		@Override
		protected MyList doTask(ServiceClient service, Integer... params) throws Exception {
			int id = params[0];
			MyList myList = service.getMyList(id);
			m_store.updateMyList(myList);
			m_store.updateMyList(id, service.getMyListEntries(id));
			return myList;
		}

		@Override
		protected void onResult(MyList result) {
			if (result != null)
				onUpdate(DdN.getPlayRecord(), false);
		}
	}

	private class DeleteMyList extends ServiceTask<Integer, Void, MyList> {
		DeleteMyList() {
			super(MyListActivity.this, R.string.deleting);
		}

		@Override
		protected MyList doTask(ServiceClient service, Integer... params) throws Exception {
			int id = params[0];
			service.deleteMyList(id);
			MyList myList = service.getMyList(id);
			m_store.updateMyList(myList);
			m_store.clearMyList(id);
			return myList;
		}

		@Override
		protected void onResult(MyList result) {
			if (result != null)
				onUpdate(DdN.getPlayRecord(), false);
		}
	}

	private class ActivateMyList extends ServiceTask<Integer, Void, Boolean> {
		ActivateMyList() {
			super(MyListActivity.this, R.string.activating);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Integer... params) throws Exception {
			int id = params[0];
			service.activateMyList(id);
			return Boolean.TRUE;
		}
	}
}
