package net.diva.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.diva.browser.common.UpdateSaturaionPoints;
import net.diva.browser.model.MyList;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		addMyLists((ListPreference)findPreference("default_tab"));
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
		int index = values.indexOf("mylist");
		if (index == -1)
			index = values.size();
		else {
			names.remove(index);
			values.remove(index);
		}
		for (MyList mylist: DdN.getLocalStore().loadMyLists()) {
			names.add(index, mylist.name);
			values.add(index, mylist.tag);
			++index;
		}
		lp.setEntries(names.toArray(new CharSequence[names.size()]));
		lp.setEntryValues(values.toArray(new CharSequence[values.size()]));
	}
}
