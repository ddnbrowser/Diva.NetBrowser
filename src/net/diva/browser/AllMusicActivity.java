package net.diva.browser;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import android.view.Menu;


public class AllMusicActivity extends MusicListActivity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.allmusic_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean enable_all = DdN.isAllowUpdateMusics(m_preferences);
		menu.findItem(R.id.item_update_all).setVisible(enable_all);
		menu.findItem(R.id.item_update_new).setVisible(!enable_all);
		return super.onPrepareOptionsMenu(menu);
	}

	protected List<MusicInfo> getMusics(PlayRecord record) {
		return record.musics;
	}
}
