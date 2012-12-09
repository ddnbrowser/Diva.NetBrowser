package net.diva.browser;

import net.diva.browser.common.DownloadPlayRecord;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {
	private static final int TOOL_SETTINGS = 1;

	public interface Content {
		boolean onSearchRequested();
	}

	private Content m_content;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content);
		if (f instanceof Content)
			m_content = (Content)f;

		DdN.Account account = DdN.Account.load(preferences);
		if (account == null)
			DdN.Account.input(this, new DownloadPlayRecord(this));
		else
			toolSetting();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.item_news:
			WebBrowseActivity.open(this, "/divanet/menu/news/");
			break;
		case R.id.item_vp_history:
			WebBrowseActivity.open(this, "/divanet/personal/vpHistory/");
			break;
		case R.id.item_play_history:
			WebBrowseActivity.open(this, "/divanet/personal/playHistory/0");
			break;
		case R.id.item_contest:
			WebBrowseActivity.open(this, "/divanet/contest/info/");
			break;
		case R.id.item_statistics:
			WebBrowseActivity.open(this, "/divanet/pv/statistics/");
			break;
		case R.id.item_ranking_list:
			WebBrowseActivity.open(this, "/divanet/ranking/list/0");
			break;
		case R.id.item_game_settings: {
			Intent intent = new Intent(getApplicationContext(), CommonConfigActivity.class);
			startActivity(intent);
		}
			break;
		case R.id.item_tool_settings: {
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, TOOL_SETTINGS);
		}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case TOOL_SETTINGS:
			toolSetting();
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onSearchRequested() {
		if (m_content != null)
			return m_content.onSearchRequested();

		return super.onSearchRequested();
	}

	private void toolSetting(){
		DdN.Settings.update(this);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("download_rankin", false))
			DownloadRankingService.reserve(this);
		else
			DownloadRankingService.cancel(this);


	}
}
