package net.diva.browser;

import net.diva.browser.settings.ConfigActivationIndividual;
import net.diva.browser.settings.ConfigActivity;
import net.diva.browser.settings.ConfigCategory;
import net.diva.browser.settings.ConfigCommonModule;
import net.diva.browser.settings.ConfigItem;
import net.diva.browser.settings.ConfigRename;
import net.diva.browser.settings.ConfigResetCommon;
import net.diva.browser.settings.ConfigResetIndividual;
import net.diva.browser.settings.ConfigSetSkin;
import net.diva.browser.settings.ConfigTitle;
import net.diva.browser.settings.ConfigUnsetSkin;

public class CommonConfigActivity extends ConfigActivity {
	@Override
	protected ConfigItem[] createItems() {
		return new ConfigItem[] {
				new ConfigCategory(getText(R.string.category_player)),
				new ConfigRename(this),
				new ConfigTitle(this),
				new ConfigCategory(getText(R.string.category_module_common)),
				new ConfigCommonModule(this, 1),
				new ConfigCommonModule(this, 2),
				new ConfigResetCommon(this),
				new ConfigCategory(getText(R.string.category_module_individual)),
				new ConfigResetIndividual(this),
				new ConfigActivationIndividual(this),
				new ConfigCategory(getText(R.string.category_skin)),
				new ConfigSetSkin(this),
				new ConfigUnsetSkin(this),
		};
	}
}
