package net.diva.browser;

import net.diva.browser.settings.ConfigActivationIndividual;
import net.diva.browser.settings.ConfigActivity;
import net.diva.browser.settings.ConfigBorder;
import net.diva.browser.settings.ConfigCategory;
import net.diva.browser.settings.ConfigCommonModule;
import net.diva.browser.settings.ConfigInterimRanking;
import net.diva.browser.settings.ConfigItem;
import net.diva.browser.settings.ConfigRename;
import net.diva.browser.settings.ConfigResetCommon;
import net.diva.browser.settings.ConfigResetIndividual;
import net.diva.browser.settings.ConfigSetButtonSE;
import net.diva.browser.settings.ConfigSetSkin;
import net.diva.browser.settings.ConfigSyncIndividual;
import net.diva.browser.settings.ConfigTitle;
import net.diva.browser.settings.ConfigTitleReplace;
import net.diva.browser.settings.ConfigUnsetSkin;

public class CommonConfigActivity extends ConfigActivity {
	@Override
	protected ConfigItem[] createItems() {
		return new ConfigItem[] {
				new ConfigCategory(getText(R.string.category_player)),
				new ConfigRename(this),
				new ConfigTitle(this),
				new ConfigTitleReplace.Name(this),
				new ConfigTitleReplace.Plate(this),
				new ConfigCategory(getText(R.string.category_module_common)),
				new ConfigCommonModule(this, 1),
				new ConfigCommonModule(this, 2),
				new ConfigCommonModule(this, 3),
				new ConfigResetCommon(this),
				new ConfigCategory(getText(R.string.category_skin_common)),
				new ConfigSetSkin(this),
				new ConfigUnsetSkin(this),
				new ConfigCategory(getText(R.string.category_common_buttonse)),
				new ConfigSetButtonSE(this, 0),
				new ConfigSetButtonSE(this, 1),
				new ConfigSetButtonSE(this, 2),
				new ConfigSetButtonSE(this, 3),
				new ConfigCategory(getText(R.string.category_misc_common)),
				new ConfigBorder(this),
				new ConfigInterimRanking(this),
				new ConfigCategory(getText(R.string.category_individual)),
				new ConfigResetIndividual(this),
				new ConfigActivationIndividual(this),
				new ConfigSyncIndividual(this),
		};
	}
}
