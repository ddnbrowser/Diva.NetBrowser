package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MyListActivity extends MusicListActivity {
	private int m_myListId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		m_myListId = intent.getIntExtra("id", 0);
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
			new SyncMyList().execute(m_myListId);
			break;
		case R.id.item_edit_name:
			break;
		case R.id.item_delete_mylist:
			break;
		case R.id.item_activate_mylist:
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected List<MusicInfo> getMusics(PlayRecord record) {
		List<String> ids = m_store.loadMyList(m_myListId);
		List<MusicInfo> musics = new ArrayList<MusicInfo>(ids.size());
		for (String id: ids)
			musics.add(record.getMusic(id));
		return musics;
	}

	private class SyncMyList extends ServiceTask<Integer, Void, Boolean> {
		SyncMyList() {
			super(MyListActivity.this, R.string.synchronizing);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Integer... params) throws Exception {
			int id = params[0];
			MyList myList = service.getMyList(id);
			m_store.updateMyList(myList);
			m_store.updateMyList(id, service.getMyListEntries(id));
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result)
				onUpdate(DdN.getPlayRecord(), false);
		}
	}
}
