package net.diva.browser;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import android.view.Menu;


public class AllMusicActivity extends MusicListActivity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.allmusic_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean update = DdN.isAllowUpdateMusics(m_preferences);
		boolean selection = isSelectionMode();
		menu.findItem(R.id.item_update_all).setVisible(update && !selection);
		menu.findItem(R.id.item_update_bulk).setVisible(update && selection);
		menu.findItem(R.id.item_update_new).setVisible(!update);
		return super.onPrepareOptionsMenu(menu);
	}

	protected List<MusicInfo> getMusics() {
		return DdN.getPlayRecord().musics;
	}
}
