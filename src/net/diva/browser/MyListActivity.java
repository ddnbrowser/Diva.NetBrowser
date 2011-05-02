package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
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
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected List<MusicInfo> getMusics(PlayRecord record) {
		List<String> ids = m_store.loadMyList(m_myListId);
		List<MusicInfo> musics = new ArrayList<MusicInfo>(ids.size());
		for (String id: ids)
			musics.add(record.getMusic(id));
		return musics;
	}
}
