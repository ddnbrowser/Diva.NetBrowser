package net.diva.browser;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.Module;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.Role;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.settings.ModuleListActivity;
import net.diva.browser.settings.SEListActivity;
import net.diva.browser.settings.SkinListActivity;
import net.diva.browser.util.CodeMap;
import net.diva.browser.util.ComplexAdapter;
import net.diva.browser.util.ComplexAdapter.Action;
import net.diva.browser.util.ComplexAdapter.Item;
import net.diva.browser.util.ComplexAdapter.TextItem;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MusicDetailActivity extends ListActivity {
	private LocalStore m_store;
	private MusicInfo m_music;

	private ComplexAdapter m_adapter;
	private ButtonSE.Player m_player;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_store = DdN.getLocalStore();
		m_music = DdN.getPlayRecord().getMusic(getIntent().getStringExtra("id"));

		m_player = new ButtonSE.Player(getApplicationContext());
		m_adapter = new ComplexAdapter(this);
		setListAdapter(setupAdapter(m_adapter));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_set_module:
			if (resultCode == RESULT_OK)
				setModule(data);
			break;
		case R.id.item_set_skin:
			if (resultCode == RESULT_OK) {
				if (data.getBooleanExtra("setNoUse", false))
					setSkinNoUse();
				else
					setSkin(data);
			}
			break;
		case R.id.item_set_button_se:
			if (resultCode == RESULT_OK) {
				int type = data.getIntExtra("type", 0);
				String id = data.getStringExtra("id");
				if (id ==null)
					resetButtonSE(type);
				else if (id.equals(ButtonSE.INVALIDATE_COMMON))
					setButtonSEInvalidateCommon(type);
				else
					setButtonSE(id, type);
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Item item = m_adapter.getItem(position);
		item.runAction(v);
	}

	private ComplexAdapter setupAdapter(ComplexAdapter adapter) {
		adapter.clear();

		adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.category_module)));
		if (m_music.hasIndividualModule()) {
			addListItemForRole(adapter, m_music.role1, 1);
			addListItemForRole(adapter, m_music.role2, 2);
			addListItemForRole(adapter, m_music.role3, 3);
		}
		else
			adapter.add(new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.no_setting), new SetModule(1)));
		adapter.add(new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.initialize), new ResetModule()));

		adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.category_skin)));
		adapter.add(createSkinItem(m_music.skin));
		adapter.add(new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.initialize), new ResetSkin()));

		adapter.add(new TextItem(android.R.layout.preference_category, android.R.id.title, getText(R.string.category_se)));
		CodeMap seNames = new CodeMap(this, R.array.sound_effects);
		for (int i = 0; i < m_music.sounds.length; ++i) {
			String id = m_music.sounds[i];
			if (ButtonSE.UNSUPPORTED.equals(id))
				continue;

			adapter.add(new TextItem(R.layout.name_list_item, android.R.id.title, String.format("[%s]", seNames.name(i))));
			adapter.add(createSEItem(i, id));
		}
		adapter.add(new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.initialize), new ResetButtonSE()));
		return adapter;
	}

	private void addListItemForRole(ComplexAdapter adapter, Role role, int part) {
		if (role == null)
			return;

		adapter.add(new TextItem(R.layout.name_list_item, android.R.id.title, String.format("[%s]", role.name)));
		adapter.add(new ModuleItem(m_store.getModule(role.module), new SetModule(part), this));
	}

	private Item createSkinItem(String id) {
		SetSkin action = new SetSkin();
		if (id == null)
			return new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.no_setting), action);
		else if (id.equals(SkinInfo.NO_USE))
			return new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.no_use_skin), action);
		else
			return new SkinItem(m_store.getSkin(id), action, this);
	}

	private Item createSEItem(int type, String id) {
		SetButtonSE action = new SetButtonSE(type);
		if (id == null)
			return new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.no_setting), action);
		else if (id.equals(ButtonSE.INVALIDATE_COMMON))
			return new TextItem(R.layout.common_list_item, android.R.id.text1, getText(R.string.invalidate_common_button_se), action);
		else
			return new SEItem(m_store.getButtonSE(type, id), action, m_player);
	}

	private static class ModuleItem extends Item {
		static class Holder {
			TextView name;
			ImageView thumbnail;
		}

		Context m_context;
		Module m_module;

		public ModuleItem(Module module, Action action, Context context) {
			super(R.layout.module_item, action);
			m_module = module;
			m_context = context.getApplicationContext();
		}

		@Override
		public void prepare(View view) {
			Holder h = new Holder();
			h.name = (TextView)view.findViewById(android.R.id.text1);
			h.thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
			view.setTag(h);
		}

		@Override
		public void attach(View view) {
			Holder h = (Holder)view.getTag();
			h.name.setText(m_module.name);
			h.thumbnail.setImageDrawable(m_module.getThumbnail(m_context));
		}
	}

	private static class SkinItem extends Item {
		static class Holder {
			TextView name;
			ImageView thumbnail;
		}

		Context m_context;
		SkinInfo m_skin;

		public SkinItem(SkinInfo skin, Action action, Context context) {
			super(R.layout.skin_item, action);
			m_skin = skin;
			m_context = context.getApplicationContext();
		}

		@Override
		public void prepare(View view) {
			Holder h = new Holder();
			h.name = (TextView)view.findViewById(android.R.id.text1);
			h.thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
			view.setTag(h);
		}

		@Override
		public void attach(View view) {
			Holder h = (Holder)view.getTag();
			h.name.setText(m_skin.name);
			h.thumbnail.setImageDrawable(m_skin.getThumbnail(m_context));
		}
	}

	private static class SEItem extends Item implements View.OnClickListener{
		static class Holder {
			TextView name;
			View button;
		}

		ButtonSE m_se;
		ButtonSE.Player m_player;
		public SEItem(ButtonSE se, Action action, ButtonSE.Player player) {
			super(R.layout.se_item, action);
			m_se = se;
			m_player = player;
		}

		@Override
		public void prepare(View view) {
			Holder h = new Holder();
			h.name = (TextView)view.findViewById(R.id.title);
			h.button = view.findViewById(R.id.button_sound);
			view.setTag(h);
		}

		@Override
		public void attach(View view) {
			Holder h = (Holder)view.getTag();
			h.name.setText(m_se.name);
			h.button.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			m_player.play(m_se);
		}
	}

	private interface Task {
		void run(ServiceClient service) throws Exception;
	}

	private class TaskRunner extends ServiceTask<Task, Void, Boolean> {
		TaskRunner() {
			super(MusicDetailActivity.this, R.string.summary_applying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, Task... params) throws Exception {
			params[0].run(service);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result)
				setupAdapter(m_adapter).notifyDataSetChanged();
		}
	}

	private void confirm(int message_id, final Task task) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(m_music.title);
		builder.setMessage(message_id);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new TaskRunner().execute(task);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	public void closePopup(View v) {
		finish();
	}

	/* ----------------------------------------------------------------------
	 * モジュールを設定する
	 * ---------------------------------------------------------------------- */
	private class SetModule implements Action {
		int m_from;

		SetModule(int from) {
			m_from = from;
		}

		@Override
		public void run(View view) {
			Intent intent = new Intent(getApplicationContext(), ModuleListActivity.class);
			intent.putExtra("request", m_from);
			intent.putExtra("id", m_music.id);
			intent.putExtra("part", m_music.part);
			String[] roles = new String[4];
			if (m_music.role1 != null) {
				intent.putExtra("voice1", m_music.role1.cast);
				intent.putExtra("vocal1", m_music.role1.module);
				roles[1] = m_music.role1.name;
			}
			if (m_music.role2 != null) {
				intent.putExtra("voice2", m_music.role2.cast);
				intent.putExtra("vocal2", m_music.role2.module);
				roles[2] = m_music.role2.name;
			}
			if (m_music.role3 != null) {
				intent.putExtra("voice3", m_music.role3.cast);
				intent.putExtra("vocal3", m_music.role3.module);
				roles[3] = m_music.role3.name;
			}
			intent.putExtra("reference", grouping(roles));
			intent.putExtra("groups", nCopies(roles.length, -1));
			startActivityForResult(intent, R.id.item_set_module);
		}

		int[] nCopies(int length, int value) {
			int[] copies = new int[length];
			Arrays.fill(copies, value);
			return copies;
		}

		int[] grouping(String...roles) {
			final Pattern pattern = Pattern.compile("ボーカル(\\d+)");
			int[] result = new int[roles.length];
			SparseIntArray refs = new SparseIntArray();
			for (int i = 0; i < roles.length; ++i) {
				if (roles[i] == null)
					continue;
				Matcher m = pattern.matcher(roles[i]);
				if (!m.find())
					continue;

				int n = Integer.parseInt(m.group(1));
				int ref = refs.get(n);
				if (ref == 0)
					refs.append(n, i);
				else
					result[i] = ref;
			}
			return result;
		}
	}

	private void setModule(Intent data) {
		final String module1 = data.getStringExtra("vocal1");
		final String module2 = data.getStringExtra("vocal2");
		final String module3 = data.getStringExtra("vocal3");

		confirm(R.string.message_set_module, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.setIndividualModule(m_music.id, module1, module2, module3);
				if (m_music.role1 != null)
					m_music.role1.module = module1;
				if (m_music.role2 != null)
					m_music.role2.module = module2;
				if (m_music.role3 != null)
					m_music.role3.module = module3;
				m_store.updateModule(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * モジュールを未設定にする
	 * ---------------------------------------------------------------------- */
	private class ResetModule implements Action {
		@Override
		public void run(View view) {
			confirm(R.string.message_reset_module, new Task() {
				public void run(ServiceClient service) throws Exception {
					service.resetIndividualModule(m_music.id);
					reset(m_music.role1);
					reset(m_music.role2);
					reset(m_music.role3);
					m_store.updateModule(m_music);
				}
			});
		}

		void reset(Role role) {
			if (role == null)
				return;

			role.module = null;
			Arrays.fill(role.items, null);
		}
	}

	/* ----------------------------------------------------------------------
	 * スキンを設定する
	 * ---------------------------------------------------------------------- */
	private class SetSkin implements Action {
		@Override
		public void run(View view) {
			Intent intent = new Intent(getApplicationContext(), SkinListActivity.class);
			intent.putExtra("hasNoUse", true);
			startActivityForResult(intent, R.id.item_set_skin);
		}
	}

	private void setSkinNoUse() {
		confirm(R.string.message_set_skin, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.setSkinNoUse(m_music.id);
				m_music.skin = SkinInfo.NO_USE;
				m_store.updateSkin(m_music);
			}
		});
	}

	private void setSkin(Intent data) {
		final String group_id = data.getStringExtra("group_id");
		final String id = data.getStringExtra("id");

		confirm(R.string.message_set_skin, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.setSkin(m_music.id, group_id, id);
				m_music.skin = id;
				m_store.updateSkin(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * スキンを未設定にする
	 * ---------------------------------------------------------------------- */
	private class ResetSkin implements Action {
		@Override
		public void run(View view) {
			confirm(R.string.message_reset_skin, new Task() {
				public void run(ServiceClient service) throws Exception {
					service.resetSkin(m_music.id);
					m_music.skin = null;
					m_store.updateSkin(m_music);
				}
			});
		}
	}

	/* ----------------------------------------------------------------------
	 * ボタン音を設定する
	 * ---------------------------------------------------------------------- */
	private class SetButtonSE implements Action {
		private int m_type;

		private SetButtonSE(int type) {
			m_type = type;
		}

		@Override
		public void run(View view) {
			Intent intent = new Intent(getApplicationContext(), SEListActivity.class);
			intent.putExtra("type", m_type);
			intent.putExtra("enable_unset", true);
			intent.putExtra("enable_invalidate", true);
			startActivityForResult(intent, R.id.item_set_button_se);
		}
	}

	private void setButtonSEInvalidateCommon(final int type) {
		confirm(R.string.message_set_button_se, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.setButtonSEInvalidateCommon(m_music.id, type);
				m_music.sounds[type] = ButtonSE.INVALIDATE_COMMON;
				m_store.updateButtonSE(m_music);
			}
		});
	}

	private void setButtonSE(final String id, final int type) {
		confirm(R.string.message_set_button_se, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.setButtonSE(m_music.id, type, id);
				m_music.sounds[type] = id;
				m_store.updateButtonSE(m_music);
			}
		});
	}

	private void resetButtonSE(final int type) {
		confirm(R.string.message_reset_button_se, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.resetButtonSE(m_music.id, type);
				m_music.sounds[type] = null;
				m_store.updateButtonSE(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * ボタン音を未設定にする
	 * ---------------------------------------------------------------------- */
	private class ResetButtonSE implements Action {
		@Override
		public void run(View view) {
			confirm(R.string.message_reset_button_se, new Task() {
				public void run(ServiceClient service) throws Exception {
					service.resetButtonSE(m_music.id);
					m_music.resetIndividualSe();
					m_store.updateButtonSE(m_music);
				}
			});
		}
	}
}
