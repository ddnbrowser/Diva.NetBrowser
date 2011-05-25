package net.diva.browser.settings;

import java.util.List;
import java.util.Map;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.IndividualSetting;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class ConfigSyncIndividual extends ConfigItem {
	private Context m_context;

	private CharSequence m_title;
	private CharSequence m_summary;

	private List<SkinInfo> m_skins;
	private List<ButtonSE> m_buttonSEs;

	public ConfigSyncIndividual(Context context) {
		m_context = context;
		m_title = context.getText(R.string.description_sync_individual);
		m_summary = context.getText(R.string.summary_sync_individual);
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, m_summary);
	}

	@Override
	public Intent dispatch(Context context, Callback callback) {
		confirm(context, callback, R.string.description_sync_individual, R.string.message_sync_individual);
		return null;
	}

	@Override
	public void onResult(int result, Intent data, Callback callback) {
		if (result == Activity.RESULT_OK)
			new SyncTask(m_context, callback).execute();
	}

	private String moduleId(String name) {
		if (name == null)
			return null;

		for (ModuleGroup group: DdN.getModules()) {
			for (Module module: group.modules) {
				if (name.equals(module.name))
					return module.id;
			}
		}
		return null;
	}

	private String skinId(String name) {
		if (name == null)
			return null;
		if (name.equals("使用しない"))
			return SkinInfo.NO_USE;

		for (SkinInfo skin: m_skins) {
			if (name.equals(skin.name))
				return skin.id;
		}
		return null;
	}

	private String seId(String name) {
		if (name == null)
			return null;

		for (ButtonSE se: m_buttonSEs) {
			if (name.equals(se.name))
				return se.id;
		}
		return null;
	}

	private class SyncTask extends ServiceTask<Void, Void, Boolean> {
		Callback m_callback;

		public SyncTask(Context context, Callback callback) {
			super(context, R.string.message_updating);
			m_callback = callback;
		}

		@Override
		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {
			Map<String, IndividualSetting> settings = service.getIndividualSettings();
			LocalStore store = DdN.getLocalStore();
			m_skins = store.loadSkins();
			m_buttonSEs = store.loadButtonSEs();

			PlayRecord record = DdN.getPlayRecord();
			for (MusicInfo music: record.musics) {
				IndividualSetting setting = settings.get(music.id);
				music.vocal1 = moduleId(setting.vocal1);
				music.vocal2 = moduleId(setting.vocal2);
				music.skin = skinId(setting.skin);
				music.button = seId(setting.button);
			}
			store.updateIndividual(record.musics);
			DdN.setPlayRecord(record);

			m_skins = null;
			m_buttonSEs = null;
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			m_callback.onUpdated();
		}
	}
}
