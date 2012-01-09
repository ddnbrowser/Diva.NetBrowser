package net.diva.browser.page;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.model.MusicInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class AllMusicFragment extends MusicListFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.allmusic_options, menu);
		inflater.inflate(R.menu.main_options, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean update = DdN.isAllowUpdateMusics(m_preferences);
		boolean selection = isSelectionMode();
		MenuItem menuUpdate = menu.findItem(R.id.item_update);
		if (menuUpdate != null)
			menuUpdate.setVisible(!selection);
		menu.findItem(R.id.item_update_all).setVisible(!selection).setEnabled(update);
		menu.findItem(R.id.item_update_in_history).setVisible(!selection).setEnabled(update);
		menu.findItem(R.id.item_update_bulk).setVisible(selection).setEnabled(update);
	}

	protected List<MusicInfo> getMusics() {
		return DdN.getPlayRecord().musics;
	}
}
