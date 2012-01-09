package net.diva.browser.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.MyListEditActivity;
import net.diva.browser.R;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.OperationFailedException;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MyListFragment extends MusicListFragment {
	private static final int EDIT_MYLIST = 1;

	private MyList m_myList;
	private List<MusicInfo> m_musics;
	private List<String> m_ids;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		Bundle args = getArguments();
		m_myList = new MyList(args.getInt("id", 0), args.getString("name"));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.mylist_options, menu);
		inflater.inflate(R.menu.main_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		final boolean canUpdate = DdN.isAllowUpdateMusics(m_preferences);
		menu.findItem(R.id.item_activate_mylist).setEnabled(m_musics != null && !m_musics.isEmpty());
		menu.findItem(R.id.item_update_bulk).setEnabled(canUpdate);
		MenuItem updateInHistory = menu.findItem(R.id.item_update_in_history);
		if (updateInHistory != null)
			updateInHistory.setEnabled(canUpdate);
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
		case R.id.item_edit_mylist:
			startEdit();
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case EDIT_MYLIST:
			if (resultCode == Activity.RESULT_OK) {
				new UpdateMyList(data.getStringExtra("name")).execute(data.getStringArrayExtra("ids"));
				return;
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onUpdate(PlayRecord record, boolean noMusic) {
		m_ids = m_store.loadMyList(m_myList.id);
		List<MusicInfo> musics = new ArrayList<MusicInfo>(m_ids.size());
		for (String id: m_ids)
			musics.add(record.getMusic(id));
		m_musics = musics;

		super.onUpdate(record, noMusic);
	}

	@Override
	public void onUpdate(MyList myList, boolean noMusic) {
		if (myList.id != m_myList.id)
			return;

		m_myList = myList;
		if (!noMusic)
			onUpdate(DdN.getPlayRecord(), false);
	}

	@Override
	protected List<MusicInfo> getMusics() {
		return m_musics;
	}

	@Override
	protected void makeTitle(StringBuilder title, PlayRecord record) {
		title.append('(');
		title.append(m_musics.size());
		title.append("/20) ");
		super.makeTitle(title, record);
	}

	private void editMyListName() {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.input_reading, null);
		final EditText edit = (EditText)view.findViewById(R.id.reading);
		edit.setText(m_myList.name);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

	private void startEdit() {
		ArrayList<String> ids;
		if (m_musics instanceof ArrayList<?>)
			ids = (ArrayList<String>)m_ids;
		else
			ids = new ArrayList<String>(m_ids);

		Intent intent = new Intent(getActivity(), MyListEditActivity.class);
		intent.putExtra("name", m_myList.name);
		intent.putStringArrayListExtra("ids", ids);
		intent.putExtra("layout", m_adapter.getLayout());
		intent.putExtra("difficulty", m_adapter.getDifficulty());
		startActivityForResult(intent, EDIT_MYLIST);
	}

	private void deleteMyList() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
			super(getActivity(), R.string.message_updating);
		}

		@Override
		protected String doTask(ServiceClient service, String... params) throws Exception {
			String newName = params[0];
			try {
				MyList myList = new MyList(m_myList.id, newName);
				service.renameMyList(myList.id, myList.name);
				m_store.updateMyList(myList);
				DdN.notifyChanged(myList, true);
				return null;
			}
			catch (OperationFailedException e) {
				return e.getMessage();
			}
		}

		@Override
		protected void onResult(String result) {
			if (result != null)
				Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
		}
	}

	private class SyncMyList extends ServiceTask<Integer, Void, MyList> {
		SyncMyList() {
			super(getActivity(), R.string.synchronizing);
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
				DdN.notifyChanged(result, false);
		}
	}

	private class UpdateMyList extends ServiceTask<String, Void, String> {
		MyList myList;

		public UpdateMyList(String name) {
			super(getActivity(), R.string.message_updating);
			myList = new MyList(m_myList.id, name);
		}

		@Override
		protected String doTask(ServiceClient service, String... ids) throws Exception {
			boolean isActive = myList.id == m_store.getActiveMyList();

			try {
				service.deleteMyList(myList.id);

				for (String id: ids)
					service.addToMyList(myList.id, id);
				m_store.updateMyList(myList.id, Arrays.asList(ids));

				if (isActive)
					service.activateMyList(myList.id);

				service.renameMyList(m_myList.id, myList.name);
				m_store.updateMyList(myList);

				DdN.notifyChanged(myList, false);
				return null;
			}
			catch (OperationFailedException e) {
				DdN.notifyChanged(m_myList, false);
				return e.getMessage();
			}
		}

		@Override
		protected void onResult(String result) {
			if (result != null)
				Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
		}

	}

	private class DeleteMyList extends ServiceTask<Integer, Void, MyList> {
		DeleteMyList() {
			super(getActivity(), R.string.deleting);
		}

		@Override
		protected MyList doTask(ServiceClient service, Integer... params) throws Exception {
			int id = params[0];
			service.deleteMyList(id);
			MyList myList = service.getMyList(id);
			m_store.updateMyList(myList);
			m_store.clearMyList(id);
			if (id == m_store.getActiveMyList())
				m_store.activateMyList(-1);
			return myList;
		}

		@Override
		protected void onResult(MyList result) {
			if (result != null)
				DdN.notifyChanged(result, false);
		}
	}

	private class ActivateMyList extends ServiceTask<Integer, Void, String> {
		ActivateMyList() {
			super(getActivity(), R.string.activating);
		}

		@Override
		protected String doTask(ServiceClient service, Integer... params) throws Exception {
			int id = params[0];
			try {
				service.activateMyList(id);
				m_store.activateMyList(id);
				return null;
			}
			catch (Exception e) {
				return e.getMessage();
			}
		}

		@Override
		protected void onResult(String error) {
			if (error != null)
				Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
			else
				DdN.notifyChanged(m_myList, true);
		}
	}
}
