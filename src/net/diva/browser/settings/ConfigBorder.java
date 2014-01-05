package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;

public class ConfigBorder extends ConfigMultiChoice {
	public ConfigBorder(Context context) {
		super(context, R.array.border_config_keys, R.array.border_config_names,
				false, R.string.description_border_config, R.string.summary_border_config);
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.activateBorder(m_keys, m_values);
		saveToLocal();
		return Boolean.TRUE;
	}
}
