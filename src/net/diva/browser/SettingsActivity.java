package net.diva.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.diva.browser.common.UpdateSaturaionPoints;
import net.diva.browser.model.MyList;
import net.diva.browser.settings.TabSortActivity;
import net.diva.browser.util.TimePreference;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		addMyLists((ListPreference)findPreference("default_tab"));
		new RankInCheckTimeChangeListener(findPreference("ranking_check_time"));
		setVersion(findPreference("version_number"));

		PreferenceScreen ps = (PreferenceScreen) findPreference("tab_sort");
		ps.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SettingsActivity.this, TabSortActivity.class));
				return true;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.settings_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update_saturations:
			new UpdateSaturaionPoints(this).execute();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void addMyLists(final ListPreference lp) {
		List<CharSequence> names = new ArrayList<CharSequence>(Arrays.asList(lp.getEntries()));
		List<CharSequence> values = new ArrayList<CharSequence>(Arrays.asList(lp.getEntryValues()));
		int mylist_active = getResources().getInteger(R.integer.mylist_actives);
		int index = values.indexOf("mylist");
		if (index == -1){
			index = values.size();
		}else{
			CharSequence selectMylistName = names.remove(index);
			CharSequence selectMylistVal = values.remove(index);
			--index;
			for(int i = 0; i < mylist_active; i++){
				names.add(index, (String) selectMylistName + (char) ('A' + i));
				values.add(index, (String) selectMylistVal + (char) ('a' + i));
				++index;
			}
		}
		for (MyList mylist: DdN.getLocalStore().loadMyLists()) {
			names.add(index, mylist.name);
			values.add(index, mylist.tag);
			++index;
		}
		lp.setEntries(names.toArray(new CharSequence[names.size()]));
		lp.setEntryValues(values.toArray(new CharSequence[values.size()]));
	}

	private class RankInCheckTimeChangeListener implements Preference.OnPreferenceChangeListener {
		public RankInCheckTimeChangeListener(Preference preference) {
			preference.setOnPreferenceChangeListener(this);
			if (preference instanceof TimePreference) {
				onPreferenceChange(preference, ((TimePreference) preference).getValue());
			}
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			int value = (Integer)newValue;
			preference.setSummary(getString(R.string.summary_ranking_check_time, value/60, value%60));
			return true;
		}
	}

	private void setVersion(Preference preference) {
		PackageManager pm = getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
			preference.setSummary(info.versionName);
		}
		catch (NameNotFoundException e) {
			preference.setSummary("Unknown");
		}
	}
}
