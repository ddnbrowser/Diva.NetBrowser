package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class ConfigTitleReplace extends ConfigSingleChoice {
	private static final String[] KEYS = new String[] { "overwriteType", "overwriteTypePlate" };

	private int m_index;

	public ConfigTitleReplace(Context context, int index, int names, int title) {
		super(context, KEYS[index], names, 0, title);
		m_index = index;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.setTitleReplace(KEYS, values(store.getContext()));
		saveToLocal();
		return Boolean.TRUE;
	}

	private int[] values(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int[] values = new int[KEYS.length];
		for (int i = 0; i < KEYS.length; ++i)
			values[i] = prefs.getInt(KEYS[i], 0);
		values[m_index] = m_value;
		return values;
	}

	public static class Name extends ConfigTitleReplace {
		public Name(Context context) {
			super(context, 0, R.array.title_overwrite_types, R.string.description_title_replace);
		}
	}

	public static class Plate extends ConfigTitleReplace {
		public Plate(Context context) {
			super(context, 1, R.array.plate_overwrite_types, R.string.description_plate_replace);
		}
	}
}
