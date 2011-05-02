package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.OperationFailedException;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
			editMyListName();
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

	private void editMyListName() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.input_reading, null);
		final EditText edit = (EditText)view.findViewById(R.id.reading);
		edit.setText(m_myList.name);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_mylist_name);
		builder.setMessage(R.string.message_edit_mylist_name);
		builder.setView(view);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new RenameMyList().execute(edit.getText().toString());
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
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

	private class RenameMyList extends ServiceTask<String, Void, String> {
		public RenameMyList() {
			super(MyListActivity.this, R.string.message_updating);
		}

		@Override
		protected String doTask(ServiceClient service, String... params) throws Exception {
			String newName = params[0];
			try {
				service.renameMyList(m_myList.id, newName);
				m_myList.name = newName;
				m_store.updateMyList(m_myList);
				return null;
			}
			catch (OperationFailedException e) {
				return e.getMessage();
			}
		}

		@Override
		protected void onResult(String result) {
			if (result != null)
				Toast.makeText(MyListActivity.this, result, Toast.LENGTH_LONG).show();
		}
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
