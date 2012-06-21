package net.diva.browser.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.DdNBrowserReceiver;
import net.diva.browser.MainActivity;
import net.diva.browser.R;
import net.diva.browser.db.HistoryStore;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ParseException;
import net.diva.browser.service.ServiceClient;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
/**
 *
 * @author silvia
 *
 */
public class DownloadHistoryService extends Service {
	private static final int ONE_HOUR = 60*60*1000;
	private static final int FIVE_MINUTES = 5*60*1000;

	private static PowerManager.WakeLock m_lock;

	private IBinder m_binder = new LocalBinder();
	private SharedPreferences m_preferences;

	@Override
	public void onCreate() {
		super.onCreate();
		m_preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	public void onStart(final Intent intent, int startId) {
		new Thread(new Runnable() {
			public void run() {
				Scheduler scheduler = new Scheduler(getApplicationContext());
				try {
					boolean updated = downloadHistory(null);
					if (updated && m_preferences.getBoolean("notify_history_download", false))
						sendNotification();
				}
				catch (IOException e) {
					scheduler.reserveAt(FIVE_MINUTES);
				}
				catch (Throwable e) {
					e.printStackTrace();
					m_preferences.edit().putBoolean("download_history", false).commit();
				}
				finally {
					unlock();
					stopSelf();
				}
			}
		}).start();
	}

	public class LocalBinder extends Binder {
		public DownloadHistoryService getService() {
			return DownloadHistoryService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return m_binder;
	}

	public interface ProgressListener {
		void onProgress(int value, int max);
	}

	public boolean downloadHistory(ProgressListener listener)
			throws LoginFailedException, IOException, ParseException {
		DdN.Account account = DdN.Account.load(m_preferences);
		if (account == null)
			return false;

		ServiceClient service = new ServiceClient(account.access_code, account.password);
		service.login();

		List<String> newHistories = new ArrayList<String>();
		long lastPlayed = m_preferences.getLong("last_played_use_history", 0);
		lastPlayed = service.getHistory(newHistories, lastPlayed);
		final int count = newHistories.size();
		final boolean hasItem = count > 0;

		final Editor editor = m_preferences.edit();
		if (hasItem) {
			if (listener != null)
				listener.onProgress(0, count);

			HistoryStore store = HistoryStore.getInstance(getApplicationContext());
			for (int i = 0; i < count; ++i) {
				store.insert(service.getHistoryDetail(newHistories.get(i)));

				if (listener != null)
					listener.onProgress(i, count);
			}

			editor.putLong("last_played_use_history", lastPlayed);
		}
		editor.putLong("history_last_download_time", System.currentTimeMillis());
		editor.commit();

		if (m_preferences.getBoolean("download_history", false))
			new Scheduler(this).reserve(!hasItem && listener == null);

		return hasItem;
	}

	public static void lock(Context context) {
		if (m_lock == null) {
			PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			m_lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getPackageName());
		}
		m_lock.acquire();
	}

	public static void unlock() {
		PowerManager.WakeLock lock = m_lock;
		if (lock == null)
			return;
		lock.release();
		if (!lock.isHeld())
			m_lock = null;
	}

	public static void forceReserve(Context context) {
		new Scheduler(context).reserve(false);
	}

	public static void autoReserve(Context context) {
		new Scheduler(context).reserve(true);
	}

	public static void cancel(Context context) {
		new Scheduler(context).cancel();
	}

	public static boolean isReserved(Context context){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean reservedFlag = pref.getBoolean("history_reserved", false);
		if(!reservedFlag)
			return false;

		long lastDownloadTime = pref.getLong("history_last_download_time", 0L);
		if(System.currentTimeMillis() - lastDownloadTime > ONE_HOUR)
			return false;

		return true;
	}

	private void sendNotification() {
		Context context = getApplicationContext();
		CharSequence title = getText(R.string.app_name);
		CharSequence text = getText(R.string.notification_history_get);

		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent operation = PendingIntent.getActivity(context, 0, intent, 0);

		Notification notification = new Notification(
				R.drawable.icon_module, text, System.currentTimeMillis());
		notification.setLatestEventInfo(context, title, text, operation);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(R.id.notification_history_get, notification);
	}

	private static class Scheduler {

		Context m_context;
		AlarmManager m_alarm;
		SharedPreferences m_prefs;

		public Scheduler(Context context) {
			m_context = context;
			m_alarm = (AlarmManager)m_context.getSystemService(Context.ALARM_SERVICE);
			m_prefs = PreferenceManager.getDefaultSharedPreferences(context);
		}

		void reserve(boolean isAuto) {
			final long now = System.currentTimeMillis();
			long after = ONE_HOUR;
			if(isAuto){
				int autoReserveTime = m_prefs.getInt("history_auto_reserve_time", 0);
				if(autoReserveTime < 3){
					reserve(now, after);
					autoReserveTime++;
					m_prefs.edit().putInt("history_auto_reserve_time", autoReserveTime).commit();
				}else{
					m_prefs.edit().putBoolean("history_reserved", false).commit();
				}
			} else {
				m_prefs.edit().putInt("history_auto_reserve_time", 0).commit();
				reserve(now, after);
			}
		}

		void reserveAt(long after) {
			reserve(System.currentTimeMillis(), after);
		}

		void reserve(long time, long after) {
			PendingIntent operation = DdNBrowserReceiver.downloadHistoryOperation(m_context);
			m_alarm.set(AlarmManager.RTC_WAKEUP, time + after, operation);
			m_prefs.edit().putBoolean("history_reserved", true).commit();
		}

		void cancel() {
			m_alarm.cancel(DdNBrowserReceiver.downloadHistoryOperation(m_context));
		}
	}
}
