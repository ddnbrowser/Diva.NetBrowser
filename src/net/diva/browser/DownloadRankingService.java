package net.diva.browser;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.ScoreRecord;
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
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.format.Time;

public class DownloadRankingService extends Service {
	private static final int ONE_HOUR = 60*60*1000;

	private static PowerManager.WakeLock m_lock;

	private SharedPreferences m_preferences;

	@Override
	public void onCreate() {
		super.onCreate();
		m_preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(final Intent intent, int startId) {
		new Thread(new Runnable() {
			public void run() {
				Scheduler scheduler = new Scheduler(getApplicationContext());
				try {
					List<Ranking> ranking = downloadRanking();
					scheduler.reserveNext();
					updateRanking(ranking);
					if (m_preferences.getBoolean("notify_ranking_updated", false))
						sendNotification();
				}
				catch (IOException e) {
					scheduler.reserveAt(System.currentTimeMillis() + ONE_HOUR);
				}
				catch (Exception e) {
					m_preferences.edit().putBoolean("download_rankin", false).commit();
				}
				finally {
					unlock();
					stopSelf();
				}
			}
		}).start();
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

	public static void reserve(Context context) {
		new Scheduler(context).reserve();
	}

	public static void cancel(Context context) {
		new Scheduler(context).cancel();
	}

	private static class Scheduler {
		static final String RESERVE_TIME = "download_ranking_reserve_time";
		static final String CHECK_TIME = "ranking_check_time";

		Context m_context;
		AlarmManager m_alarm;
		SharedPreferences m_prefs;

		public Scheduler(Context context) {
			m_context = context;
			m_alarm = (AlarmManager)m_context.getSystemService(Context.ALARM_SERVICE);
			m_prefs = PreferenceManager.getDefaultSharedPreferences(context);
		}

		void reserve() {
			final long time = m_prefs.getLong(RESERVE_TIME, 0);
			final long now = System.currentTimeMillis();
			if (time < now)
				reserveInternal(now, 0);
			else
				reserve(time, now);
		}

		void reserveNext() {
			reserve(m_prefs.getLong(RESERVE_TIME, 0), Long.MAX_VALUE);
		}

		void reserveAt(long time) {
			final long reserved = m_prefs.getLong(RESERVE_TIME, 0);
			reserve(reserved, time - reserved);
		}

		void reserve(long base, long after) {
			final int minOfDay = m_prefs.getInt(CHECK_TIME, 0);
			Time time = new Time("Asia/Tokyo");
			time.set(base);
			time.hour = minOfDay / 60;
			time.minute = minOfDay % 60;
			time.second = 0;

			long t = time.toMillis(true);
			if (t < after) {
				time.monthDay += 1;
				t = time.toMillis(true);
			}
			reserveInternal(t, new Random().nextInt(ONE_HOUR));
		}

		void reserveInternal(long time, int delay) {
			PendingIntent operation = DdNBrowserReceiver.downloadRankingOperation(m_context);
			m_alarm.set(AlarmManager.RTC_WAKEUP, time + delay, operation);
			m_prefs.edit().putLong(RESERVE_TIME, time).commit();
		}

		void cancel() {
			m_alarm.cancel(DdNBrowserReceiver.downloadRankingOperation(m_context));
		}
	}

	private List<Ranking> downloadRanking() throws LoginFailedException, IOException, ParseException {
		ServiceClient service = new ServiceClient(
				m_preferences.getString("access_code", null),
				m_preferences.getString("password", null));
		service.login();
		List<Ranking> ranking = service.getRankInList();
		LocalStore.instance(this).update(ranking);
		return ranking;
	}

	private int getRank(String id, int rank, List<Ranking> ranking) {
		if (id == null)
			return -1;

		for (Ranking r: ranking) {
			if (id.equals(r.id) && rank == r.rank)
				return r.ranking;
		}
		return -1;
	}

	private void updateRanking(List<Ranking> ranking) {
		final PlayRecord record = DdN.getPlayRecord();
		for (MusicInfo m: record.musics) {
			for (int rank = 0; rank < m.records.length; ++rank) {
				ScoreRecord score = m.records[rank];
				if (score != null)
					score.ranking = getRank(m.id, rank, ranking);
			}
		}
		DdN.notifyPlayRecordChanged();
	}

	private void sendNotification() {
		Context context = getApplicationContext();
		CharSequence title = getText(R.string.app_name);
		CharSequence text = getText(R.string.notification_ranking_updated);

		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent operation = PendingIntent.getActivity(context, 0, intent, 0);

		Notification notification = new Notification(
				android.R.drawable.stat_sys_download_done, text, System.currentTimeMillis());
		notification.setLatestEventInfo(context, title, text, operation);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(R.id.notification_ranking_updated, notification);
	}
}
