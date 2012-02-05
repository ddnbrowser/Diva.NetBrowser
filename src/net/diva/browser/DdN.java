package net.diva.browser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.util.DdNUtil;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.TextView;

public class DdN extends Application {
	public interface Observer {
		void onUpdate(PlayRecord record, boolean noMusic);
		void onUpdate(MyList myList, boolean noMusic);
	}

	public static final URI URL = URI.create("http://project-diva-ac.net/divanet/");
	public static int[] RANK_POINTS;
	public static final int EXPERIENCE_UNIT = 13979;

	private static DdN s_instance;

	private ServiceClient m_service;

	private PlayRecord m_record;
	private List<ModuleGroup> m_modules;

	private Handler m_handler;
	private List<Observer> m_observers;

	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.settings, true);
		CookieSyncManager.createInstance(this);
		RANK_POINTS = getResources().getIntArray(R.array.rank_points);

		s_instance = this;

		m_handler = new Handler();
		m_observers = new ArrayList<Observer>();

		Settings.update(this);

		Account account = Account.load(PreferenceManager.getDefaultSharedPreferences(this));
		if (account != null) {
			LocalStore store = LocalStore.instance(this);
			m_record = store.load(account.access_code);
		}

		DdNUtil.init(getResources());
	}

	private PlayRecord setPlayRecord_(PlayRecord record) {
		final boolean noMusic = record.musics == null;
		if (noMusic && m_record != null)
			record.musics = m_record.musics;
		m_record = record;

		notifyUpdate(noMusic);
		return m_record;
	}

	private void notifyUpdate(final boolean noMusic) {
		m_handler.post(new Runnable() {
			public void run() {
				synchronized (m_observers) {
					for (Observer o: m_observers)
						o.onUpdate(m_record, noMusic);
				}
			}
		});
	}

	private void notifyChanged_(final MyList myList, final boolean noMusic) {
		m_handler.post(new Runnable() {
			public void run() {
				synchronized (m_observers) {
					for (Observer o: m_observers)
						o.onUpdate(myList, noMusic);
				}
			}
		});
	}

	private void setVocaloidPoint_(int vp) {
		m_record.vocaloid_point = vp;
		LocalStore.instance(this).update(m_record);
		notifyUpdate(true);
	}

	private void setTicketCount_(int count) {
		m_record.ticket = count;
		LocalStore.instance(this).update(m_record);
		notifyUpdate(true);
	}

	private List<ModuleGroup> getModules_() {
		if (m_modules == null)
			m_modules = getLocalStore().loadModules();
		return m_modules;
	}

	public static String url(String relative) {
		return DdN.URL.resolve(relative).toString();
	}

	public static String url(String relative, Object...args) {
		return url(String.format(relative, args));
	}

	public static void setUpdateTime(SharedPreferences.Editor editor, int count) {
		editor.putLong("allow_update_time", System.currentTimeMillis() + count * (2 * 60 * 1000));
	}

	public static boolean isAllowUpdateMusics(SharedPreferences preferences) {
		return preferences.getLong("allow_update_time", 0) < System.currentTimeMillis();
	}

	public static void setNewsTimestamp(String timestamp) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(s_instance);
		String old = prefs.getString("news_timestamp", "");
		if (old.equals(timestamp))
			return;

		prefs.edit().putString("news_timestamp", timestamp).commit();
		if (!prefs.getBoolean("notify_news_updated", false))
			return;

		Context ctx = s_instance;
		CharSequence title = ctx.getText(R.string.news_updated_title);
		CharSequence ticker = ctx.getText(R.string.news_updated_ticker);

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url("/divanet/menu/news/")));
		intent.setClass(ctx, WebBrowseActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification notice = new Notification(R.drawable.icon_module, ticker, System.currentTimeMillis());
		notice.setLatestEventInfo(ctx, title, timestamp, PendingIntent.getActivity(ctx, 0, intent, 0));
		notice.flags = Notification.FLAG_AUTO_CANCEL;

		NotificationManager nm = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(R.id.notification_news_updated, notice);
	}

	public static ServiceClient getServiceClient(Account account) {
		return s_instance.m_service = new ServiceClient(account.access_code, account.password);
	}

	public static ServiceClient getServiceClient() {
		if (s_instance.m_service != null)
			return s_instance.m_service;
		return getServiceClient(Account.load(PreferenceManager.getDefaultSharedPreferences(s_instance)));
	}

	public static LocalStore getLocalStore() {
		return s_instance == null ? null : LocalStore.instance(s_instance);
	}

	public static PlayRecord getPlayRecord() {
		return s_instance == null ? null : s_instance.m_record;
	}

	public static PlayRecord setPlayRecord(PlayRecord record) {
		return s_instance != null ? s_instance.setPlayRecord_(record) : null;
	}

	public static void setVocaloidPoint(int vp) {
		if (s_instance != null)
			s_instance.setVocaloidPoint_(vp);
	}

	public static void setTicketCount(int count) {
		if (s_instance != null)
			s_instance.setTicketCount_(count);
	}

	public static List<ModuleGroup> getModules() {
		return s_instance == null ? null : s_instance.getModules_();
	}

	public static void setModules(List<ModuleGroup> modules) {
		if (s_instance != null)
			s_instance.m_modules = modules;
	}

	public static Module getModule(String id) {
		if (id == null || s_instance == null)
			return null;

		for (ModuleGroup group: getModules()) {
			for (Module module: group.modules) {
				if (id.equals(module.id))
					return module;
			}
		}

		return null;
	}

	public static int getVoice(String name) {
		if (TextUtils.isEmpty(name))
			return -1;

		for (ModuleGroup group: getModules()) {
			if (group.name.equals(name))
				return group.id;
		}
		return -1;
	}

	public static int getModuleGroup(Module module) {
		for (ModuleGroup group: getModules()) {
			if (group.modules.contains(module))
				return group.id;
		}
		return -1;
	}

	public static void registerObserver(Observer observer) {
		synchronized (s_instance.m_observers) {
			s_instance.m_observers.add(observer);
		}
	}

	public static void unregisterObserver(Observer observer) {
		synchronized (s_instance.m_observers) {
			s_instance.m_observers.remove(observer);
		}
	}

	public static void notifyPlayRecordChanged() {
		s_instance.notifyUpdate(false);
	}

	public static void notifyChanged(MyList myList, boolean noMusic) {
		s_instance.notifyChanged_(myList, noMusic);
	}

	public static class Account {
		private static final String ACCESS_CODE = "access_code";
		private static final String PASSWORD = "password";

		public String access_code;
		public String password;

		private Account(String access_code_, String password_) {
			access_code = access_code_;
			password = password_;
		}

		public static Account load(SharedPreferences preferences) {
			final String access_code = preferences.getString(ACCESS_CODE, null);
			if (access_code == null)
				return null;
			final String password = preferences.getString(PASSWORD, null);
			if (password == null)
				return null;

			return new Account(access_code, password);
		}

		public static void input(Context context, final AsyncTask<Account, ?, ?> task) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View view = inflater.inflate(R.layout.account_input, null);
			final TextView access_code = (TextView)view.findViewById(R.id.edit_access_code);
			final TextView password = (TextView)view.findViewById(R.id.edit_password);

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setView(view)
			.setTitle(R.string.account_input_title)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					task.execute(new Account(
							access_code.getText().toString(),
							password.getText().toString()));
				}
			})
			.show();
		}

		public SharedPreferences.Editor putTo(SharedPreferences.Editor editor) {
			editor.putString(ACCESS_CODE, access_code);
			editor.putString(PASSWORD, password);
			return editor;
		}
	}

	public static class Settings {
		public static boolean enableFastScroll = false;

		public static void update(Context context) {
			SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
			enableFastScroll = p.getBoolean("fast_scroll", enableFastScroll);
		}
	}
}
