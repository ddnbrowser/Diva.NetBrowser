package net.diva.browser;

import java.io.File;
import java.net.URI;

import net.diva.browser.common.DownloadPlayRecord;
import net.diva.browser.compatibility.Compatibility;
import net.diva.browser.util.DdNUtil;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {
	private static final int TOOL_SETTINGS = 1;

	public interface Content {
		boolean onSearchRequested();
	}

	private Content m_content;
	private DlReceiver reciever;
	private long id;
	private File apk;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content);
		if (f instanceof Content)
			m_content = (Content)f;

		DdN.Account account = DdN.Account.load(preferences);
		if (account == null) {
			DdN.Account.input(this, new DownloadPlayRecord(this));
		} else {
			toolSetting();
			versionCheck();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.item_news:
			WebBrowseActivity.open(this, "/divanet/menu/news/");
			break;
		case R.id.item_vp_history:
			WebBrowseActivity.open(this, "/divanet/personal/vpHistory/");
			break;
		case R.id.item_play_history:
			WebBrowseActivity.open(this, "/divanet/personal/playHistory/0");
			break;
		case R.id.item_contest:
			WebBrowseActivity.open(this, "/divanet/contest/info/");
			break;
		case R.id.item_statistics:
			WebBrowseActivity.open(this, "/divanet/pv/statistics/");
			break;
		case R.id.item_ranking_list:
			WebBrowseActivity.open(this, "/divanet/ranking/list/0");
			break;
		case R.id.item_achievment_ranking:
			WebBrowseActivity.open(this, "/divanet/ranking/achievment");
			break;
		case R.id.item_game_settings: {
			Intent intent = new Intent(getApplicationContext(), CommonConfigActivity.class);
			startActivity(intent);
		}
			break;
		case R.id.item_tool_settings: {
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, TOOL_SETTINGS);
		}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case TOOL_SETTINGS:
			toolSetting();
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onSearchRequested() {
		if (m_content != null)
			return m_content.onSearchRequested();

		return super.onSearchRequested();
	}

	private void toolSetting(){
		DdN.Settings.update(this);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("download_rankin", false))
			DownloadRankingService.reserve(this);
		else
			DownloadRankingService.cancel(this);
	}

	private void versionCheck() {
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		if(!preference.contains("check_new_version")){
			SharedPreferences.Editor editor = preference.edit();
			editor.putBoolean("check_new_version", true);
			editor.commit();
		}
		if(!preference.getBoolean("check_new_version", false))
			return;

		int version;
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (Exception e) {
			version = 0;
		}
		final URI uri = URI.create(String.format("http://eario.jp/diva/versionCheck.cgi?version=%d", version));
		(new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					for (File file : new File(Environment.getExternalStorageDirectory().getPath() + "/net.diva.browser").listFiles()) {
						if (file.getName().matches(".+\\.apk"))
							file.delete();
					}
					return DdNUtil.read(uri);
				} catch (Exception e) {
				}
				return null;
			}

			@Override
			protected void onPostExecute(String res) {
				if(res == null)
					return;
				final String result = res.trim().replace("\n", "");
				if ("latest".equals(result) || !result.matches("^http://.*"))
					return;

				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle(R.string.confirm);
				builder.setMessage("新しいバージョンが公開されています。\nダウンロードしますか？");
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/net.diva.browser");
						if(!dir.exists())
							if(!dir.mkdirs())
								android.util.Log.e("", "make dir failed.");

						DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
						Uri uri = Uri.parse(result);
						apk = new File(dir, "Diva.NetBrowser.apk");
						for(int i = 1; apk.exists(); i++)
							apk = new File(dir, "Diva.NetBrowser-" + i + ".apk");
						DownloadManager.Request request = new DownloadManager.Request(uri);
						request.setDestinationUri(Uri.fromFile(apk));
						request.setTitle(apk.getName());
						reciever = new DlReceiver();
						IntentFilter filter = new IntentFilter();
						filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
						registerReceiver(reciever, filter);
						id = manager.enqueue(request);
						dialog.dismiss();
					}
				});
				builder.setNegativeButton(R.string.cancel, null);
				builder.show();
			}
		}).execute();
	}

	@Override
	public void onDestroy() {
		if (reciever != null)
			unregisterReceiver(reciever);
		super.onDestroy();
	}

	private class DlReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context cxt, Intent intent) {
			if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))
				return;

			long recieveId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			if(id == recieveId){
				Intent installIntent = new Intent(Intent.ACTION_VIEW);
				installIntent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
				PendingIntent operation = PendingIntent.getActivity(cxt, 0, installIntent, 0);
				CharSequence title = getText(R.string.app_name);
				CharSequence text = getText(R.string.notification_new_version);
				Notification n = Compatibility.getNotification(cxt, operation, R.drawable.icon_module, title, title, text, System.currentTimeMillis(), true);
				NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(R.id.notification_ranking_updated, n);

				unregisterReceiver(reciever);
				reciever = null;
			}
		}
	}
}
