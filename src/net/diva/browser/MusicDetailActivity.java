package net.diva.browser;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.settings.ModuleListActivity;
import net.diva.browser.settings.SEListActivity;
import net.diva.browser.settings.SkinListActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MusicDetailActivity extends Activity {
	private LocalStore m_store;
	private MusicInfo m_music;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_detail);

		m_store = DdN.getLocalStore();
		m_music = DdN.getPlayRecord().getMusic(getIntent().getStringExtra("id"));

		ImageView image = (ImageView)findViewById(R.id.cover_art);
		image.setImageDrawable(m_music.getCoverArt(getApplicationContext()));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_set_module:
			if (resultCode == RESULT_OK)
				setModule(data);
			break;
		case R.id.item_set_skin:
			if (resultCode == RESULT_OK)
				setSkin(data);
			break;
		case R.id.item_set_button_se:
			if (resultCode == RESULT_OK)
				setButtonSE(data);
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
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
	public void setModule(View view) {
		Intent intent = new Intent(getApplicationContext(), ModuleListActivity.class);
		intent.putExtra("request", 1);
		intent.putExtra("id", m_music.id);
		intent.putExtra("part", m_music.part);
		intent.putExtra("vocal1", m_music.vocal1);
		intent.putExtra("vocal2", m_music.vocal2);
		startActivityForResult(intent, R.id.item_set_module);
	}

	private void setModule(Intent data) {
		final Module vocal1 = DdN.getModule(data.getStringExtra("vocal1"));
		final Module vocal2 = DdN.getModule(data.getStringExtra("vocal2"));

		confirm(R.string.message_set_module, new Task() {
			public void run(ServiceClient service) throws Exception {
				if (vocal2 == null) {
					service.setIndividualModule(m_music.id, vocal1.id);
					m_music.vocal1 = vocal1.id;
					m_music.vocal2 = null;
				}
				else {
					service.setIndividualModule(m_music.id, vocal1.id, vocal2.id);
					m_music.vocal1 = vocal1.id;
					m_music.vocal2 = vocal2.id;
				}
				m_store.updateModule(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * モジュールを未設定にする
	 * ---------------------------------------------------------------------- */
	public void resetModule(View view) {
		confirm(R.string.message_reset_module, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.resetIndividualModule(m_music.id);
				m_music.vocal1 = m_music.vocal2 = null;
				m_store.updateModule(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * スキンを設定する
	 * ---------------------------------------------------------------------- */
	public void setSkin(View view) {
		Intent intent = new Intent(getApplicationContext(), SkinListActivity.class);
		startActivityForResult(intent, R.id.item_set_skin);
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
	public void resetSkin(View view) {
		confirm(R.string.message_reset_skin, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.resetSkin(m_music.id);
				m_music.skin = null;
				m_store.updateSkin(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * ボタン音を設定する
	 * ---------------------------------------------------------------------- */
	public void setButtonSE(View view) {
		Intent intent = new Intent(getApplicationContext(), SEListActivity.class);
		startActivityForResult(intent, R.id.item_set_button_se);
	}

	private void setButtonSE(Intent data) {
		final String id = data.getStringExtra("id");
		confirm(R.string.message_set_button_se, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.setButtonSE(m_music.id, id);
				m_music.button = id;
				m_store.updateButtonSE(m_music);
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * ボタン音を未設定にする
	 * ---------------------------------------------------------------------- */
	public void resetButtonSE(View view) {
		confirm(R.string.message_reset_button_se, new Task() {
			public void run(ServiceClient service) throws Exception {
				service.resetButtonSE(m_music.id);
				m_music.button = null;
				m_store.updateButtonSE(m_music);
			}
		});
	}
}
