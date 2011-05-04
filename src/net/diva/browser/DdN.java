package net.diva.browser;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
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
	private List<TitleInfo> m_titles;
	private List<ModuleGroup> m_modules;

	private Handler m_handler;
	private List<Observer> m_observers;

	private AsyncTask<?, ?, ?> m_updateTitles;

	@Override
	public void onCreate() {
		super.onCreate();
		CookieSyncManager.createInstance(this);
		RANK_POINTS = getResources().getIntArray(R.array.rank_points);

		s_instance = this;

		m_handler = new Handler();
		m_observers = new ArrayList<Observer>();

		Account account = Account.load(PreferenceManager.getDefaultSharedPreferences(this));
		if (account != null) {
			LocalStore store = LocalStore.instance(this);
			m_titles = store.getTitles();
			m_record = store.load(account.access_code);
		}
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

	private String getTitle_(String id) {
		if (id == null)
			return getString(R.string.unknown_title);

		if (m_titles != null) {
			for (TitleInfo title: m_titles) {
				if (id.equals(title.image_id))
					return title.name;
			}
		}

		if (m_updateTitles == null)
			m_updateTitles = new UpdateTitles().execute();

		return getString(R.string.title_getting);
	}

	private List<ModuleGroup> getModules_() {
		if (m_modules == null)
			m_modules = getLocalStore().loadModules();
		return m_modules;
	}

	public static String url(String relative) {
		return DdN.URL.resolve(relative).toString();
	}

	public static URI url(String relative, Object...args) {
		return DdN.URL.resolve(String.format(relative, args));
	}

	public static void setUpdateTime(SharedPreferences.Editor editor, int count) {
		editor.putLong("allow_update_time", System.currentTimeMillis() + count * (2 * 60 * 1000));
	}

	public static boolean isAllowUpdateMusics(SharedPreferences preferences) {
		return preferences.getLong("allow_update_time", 0) < System.currentTimeMillis();
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

	public static List<TitleInfo> getTitles() {
		return s_instance == null ? null : s_instance.m_titles;
	}

	public static void setTitles(List<TitleInfo> titles) {
		if (s_instance != null)
			s_instance.m_titles = titles;
	}

	public static String getTitle(String id) {
		return s_instance.getTitle_(id);
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

	private class UpdateTitles extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			PlayRecord newRecord = null;
			LocalStore store = DdN.getLocalStore();
			try {
				ServiceClient service = DdN.getServiceClient();
				if (!service.isLogin())
					newRecord = service.login();

				m_titles = service.getTitles(m_titles);
				store.updateTitles(m_titles);
				m_updateTitles = null;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}

			if (newRecord != null) {
				store.update(newRecord);
				setPlayRecord_(newRecord);
			}
			else {
				notifyUpdate(true);
			}

			return null;
		}
	}
}
