package net.diva.browser;

import java.util.List;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.SkinInfo;

import android.content.Context;
import android.text.TextUtils;

public class DdNIndex {
	private Context m_context;

	public DdNIndex(Context context) {
		m_context = context.getApplicationContext();
	}

	private List<ModuleGroup> m_modules;

	public class ModuleIndex {
		public String id(String name) {
			if (name == null)
				return null;

			for (ModuleGroup group: m_modules) {
				for (Module module: group.modules) {
					if (name.equals(module.name))
						return module.id;
				}
			}

			return null;
		}
	}

	public ModuleIndex module() {
		if (m_modules == null)
			m_modules = LocalStore.instance(m_context).loadModules();
		return new ModuleIndex();
	}

	public class CharacterIndex {
		public int code(String name) {
			if (TextUtils.isEmpty(name))
				return -1;

			for (ModuleGroup group: m_modules) {
				if (group.name.equals(name))
					return group.id;
			}

			return -1;
		}
	}

	public CharacterIndex character() {
		if (m_modules == null)
			m_modules = LocalStore.instance(m_context).loadModules();
		return new CharacterIndex();
	}

	public static class CustomizeItemIndex {
		public String id(String name) {
			return null;
		}
	}

	private CustomizeItemIndex m_customizeItemIndex;

	public CustomizeItemIndex customizeItem() {
		if (m_customizeItemIndex == null)
			m_customizeItemIndex = new CustomizeItemIndex();
		return m_customizeItemIndex;
	}

	public static class SkinIndex {
		private List<SkinInfo> m_skins;

		private SkinIndex(Context context) {
			m_skins = LocalStore.instance(context).loadSkins();
		}

		public String id(String name) {
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
	}

	private SkinIndex m_skinIndex;

	public SkinIndex skin() {
		if (m_skinIndex == null)
			m_skinIndex = new SkinIndex(m_context);
		return m_skinIndex;
	}

	public static class SEIndex {
		private List<ButtonSE> m_sounds;

		private SEIndex(Context context, int type) {
			m_sounds = LocalStore.instance(context).loadButtonSEs(type);
		}

		public String id(String name) {
			if (name == null)
				return null;
			if (name.equals("共通ボタン音無効"))
				return ButtonSE.INVALIDATE_COMMON;

			for (ButtonSE se: m_sounds) {
				if (se.name.equals(name))
					return se.id;
			}
			return null;
		}
	}

	private SEIndex[] m_seIndex = new SEIndex[ButtonSE.COUNT];

	public SEIndex se(int type) {
		if (m_seIndex[type] == null)
			m_seIndex[type] = new SEIndex(m_context, type);
		return m_seIndex[type];
	}
}
