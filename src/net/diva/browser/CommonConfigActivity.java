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
				new ConfigCategory(getText(R.string.category_skin_common)),
				new ConfigSetSkin(this),
				new ConfigUnsetSkin(this),
				new ConfigCategory(getText(R.string.category_individual)),
				new ConfigResetIndividual(this),
				new ConfigCategory(getText(R.string.category_activation_individual)),
				new ConfigActivationIndividual(this, 0, R.string.module, R.string.summary_activation_individual),
				new ConfigActivationIndividual(this, 1, R.string.skin, R.string.summary_activation_individual),
				new ConfigActivationIndividual(this, 2, R.string.button_se, R.string.summary_activation_individual_se),
		};
	}
}
