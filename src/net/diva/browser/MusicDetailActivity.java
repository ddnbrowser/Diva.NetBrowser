package net.diva.browser;

import net.diva.browser.model.Module;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.settings.ModuleListActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MusicDetailActivity extends Activity {
	private MusicInfo m_music;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_detail);

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
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
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

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(m_music.title);
		builder.setMessage(R.string.message_set_module);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new SetModuleTask(m_music).execute(vocal1, vocal2);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private class SetModuleTask extends ServiceTask<Module, Void, Boolean> {
		private MusicInfo m_music;

		public SetModuleTask(MusicInfo music) {
			super(MusicDetailActivity.this, R.string.summary_applying);
			m_music = music;
		}

		@Override
		protected Boolean doTask(ServiceClient service, Module... params) throws Exception {
			Module vocal1 = params[0];
			Module vocal2 = params[1];

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
			DdN.getLocalStore().updateModule(m_music);
			return Boolean.TRUE;
		}
	}

	/* ----------------------------------------------------------------------
	 * モジュールを未設定にする
	 * ---------------------------------------------------------------------- */
	public void resetModule(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(m_music.title);
		builder.setMessage(R.string.message_reset_module);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new ResetModuleTask().execute(m_music);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private class ResetModuleTask extends ServiceTask<MusicInfo, Void, Boolean> {
		public ResetModuleTask() {
			super(MusicDetailActivity.this, R.string.summary_applying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, MusicInfo... params) throws Exception {
			MusicInfo music = params[0];
			service.resetIndividualModule(music.id);
			music.vocal1 = music.vocal2 = null;
			DdN.getLocalStore().updateModule(music);
			return Boolean.TRUE;
		}
	}
}
