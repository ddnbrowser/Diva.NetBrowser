package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.util.CheckedFrameLayout;
import net.diva.browser.util.SortableListView;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ListView;

public class MyListEditActivity extends ListActivity {
	private View m_commit;
	private View[] m_selectors;
	private EditText m_edit;
	private SortableListView m_list;
	private MusicAdapter m_adapter;

	private PlayRecord m_record;
	private String m_name;
	private List<MusicInfo> m_musics;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mylist_edit);

		m_commit = findViewById(R.id.button_commit);
		m_selectors = new View[] {
				findViewById(R.id.button_easy),
				findViewById(R.id.button_normal),
				findViewById(R.id.button_hard),
				findViewById(R.id.button_extreme),
		};

		Intent intent = getIntent();
		List<String> ids = intent.getStringArrayListExtra("ids");

		m_record = DdN.getPlayRecord();
		m_name = intent.getStringExtra("name");
		m_musics = new ArrayList<MusicInfo>(ids.size());
		for (String id: ids)
			m_musics.add(m_record.getMusic(id));

		m_edit = (EditText)findViewById(R.id.name);
		m_edit.setText(m_name);

		m_adapter = new EditAdapter(this);
		m_adapter.setLayout(intent.getIntExtra("layout", R.layout.music_item));
		m_adapter.setSortOrder(SortOrder.by_original, false);
		setListAdapter(m_adapter);

		m_list = (SortableListView)getListView();
		m_list.setDragListener(m_adapter);

		boolean isSort = !m_musics.isEmpty();
		Checkable selector = (Checkable)findViewById(R.id.button_mode);
		selector.setChecked(isSort);
		if (isSort)
			setSortMode();
		else
			setSelectionMode();

		setDifficulty(m_selectors[intent.getIntExtra("difficulty", 0)]);
		updateStatus();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mylist_edit_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_sort:
			m_adapter.selectSortOrder();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		if (isSortMode())
			return;

		MusicInfo music = m_adapter.getItem(position);
		if (list.isItemChecked(position))
			m_musics.add(music);
		else
			m_musics.remove(music);
		updateStatus();
	}

	private void updateStatus() {
		CharSequence title;
		final int total = m_musics.size();
		final boolean isValid = 0 < total && total <= 20;
		if (isValid)
			title = String.format("%s (%d/20)", m_name, total);
		else
			title = Html.fromHtml(String.format("%s <font color=#FF0000>(%d/20)</font>", m_name, total));
		setTitle(title);
		m_commit.setEnabled(isValid);
	}

	private boolean isSortMode() {
		return m_list.getChoiceMode() == ListView.CHOICE_MODE_NONE;
	}

	public void commit(View sender) {
		List<String> ids = new ArrayList<String>(m_musics.size());
		for (int i = 0; i < m_adapter.getCount(); ++i) {
			final MusicInfo music = m_adapter.getItem(i);
			if (m_musics.contains(music))
				ids.add(music.id);
		}

		Intent intent = new Intent();
		intent.putExtra("ids", ids.toArray(new String[ids.size()]));
		intent.putExtra("name", m_edit.getText().toString());
		setResult(RESULT_OK, intent);
		finish();
	}

	public void toggleEditMode(View sender) {
		if (((Checkable)sender).isChecked())
			setSortMode();
		else
			setSelectionMode();
	}

	public void setDifficulty(View sender) {
		for (int i = 0; i < m_selectors.length; ++i) {
			boolean selected = m_selectors[i] == sender;
			m_selectors[i].setEnabled(!selected);
			if (selected)
				m_adapter.setDifficulty(i);
		}
		m_adapter.update();
	}

	private void setSortMode() {
		m_list.setSortable(true);
		m_list.setChoiceMode(ListView.CHOICE_MODE_NONE);
		m_adapter.setData(m_musics);
		m_adapter.update();
	}

	private void setSelectionMode() {
		List<MusicInfo> musics = new ArrayList<MusicInfo>(m_record.musics);
		musics.removeAll(m_musics);
		musics.addAll(0, m_musics);

		m_list.setSortable(false);
		m_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		m_adapter.setData(musics);
		m_adapter.update();
	}

	private class EditAdapter extends MusicAdapter {
		EditAdapter(Context context) {
			super(context);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			view = super.getView(position, view, parent);
			if (view instanceof CheckedFrameLayout) {
				if (isSortMode())
					((CheckedFrameLayout) view).setCheckMarkDrawable(0);
				else {
					((CheckedFrameLayout) view).setCheckMarkDrawable(R.drawable.btn_check);
					MusicInfo music = getItem(position);
					m_list.setItemChecked(position, m_musics.contains(music));
				}
			}
			return view;
		}
	}
}
