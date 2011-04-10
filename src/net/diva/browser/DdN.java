package net.diva.browser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.db.LocalStore;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.TitleInfo;
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
	}

	public static final URI URL = URI.create("http://project-diva-ac.net/divanet/");

	private static DdN m_instance;

	private ServiceClient m_service;

	private PlayRecord m_record;
	private List<TitleInfo> m_titles;
	private List<ModuleGroup> m_modules;

	private Handler m_handler;
	private List<Observer> m_observers;

	@Override
	public void onCreate() {
		super.onCreate();
		CookieSyncManager.createInstance(this);

		m_instance = this;
		m_handler = new Handler();
		m_observers = new ArrayList<Observer>();

		Account account = Account.load(PreferenceManager.getDefaultSharedPreferences(this));
		if (account != null) {
			LocalStore store = LocalStore.instance(this);
			m_titles = store.getTitles();
			m_record = store.load(account.access_code);
		}
	}

	private ServiceClient getServiceClient_(Account account) {
		if (m_service == null && account != null)
			m_service = new ServiceClient(account.access_code, account.password);
		return m_service;
	}

	private PlayRecord setPlayRecord_(PlayRecord record) {
		final boolean noMusic = record.musics == null;
		if (noMusic && m_record != null)
			record.musics = m_record.musics;
		m_record = record;

		m_handler.post(new Runnable() {
			public void run() {
				synchronized (m_observers) {
					for (Observer o: m_observers)
						o.onUpdate(m_record, noMusic);
				}
			}
		});
		return m_record;
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

	public static ServiceClient getServiceClient(Account account) {
		return m_instance == null ? null : m_instance.getServiceClient_(account);
	}

	public static ServiceClient getServiceClient() {
		return getServiceClient(Account.load(PreferenceManager.getDefaultSharedPreferences(m_instance)));
	}

	public static LocalStore getLocalStore() {
		return m_instance == null ? null : LocalStore.instance(m_instance);
	}

	public static PlayRecord getPlayRecord() {
		return m_instance == null ? null : m_instance.m_record;
	}

	public static PlayRecord setPlayRecord(PlayRecord record) {
		return m_instance != null ? m_instance.setPlayRecord_(record) : null;
	}

	public static List<TitleInfo> getTitles() {
		return m_instance == null ? null : m_instance.m_titles;
	}

	public static void setTitles(List<TitleInfo> titles) {
		if (m_instance != null)
			m_instance.m_titles = titles;
	}

	public static String getTitle(String id) {
		if (id == null || m_instance == null || m_instance.m_titles == null)
			return null;

		for (TitleInfo title: m_instance.m_titles) {
			if (id.equals(title.image_id))
				return title.name;
		}

		return null;
	}

	public static List<ModuleGroup> getModules() {
		return m_instance == null ? null : m_instance.getModules_();
	}

	public static void setModules(List<ModuleGroup> modules) {
		if (m_instance != null)
			m_instance.m_modules = modules;
	}

	public static Module getModule(String id) {
		if (id == null || m_instance == null)
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
		synchronized (m_instance.m_observers) {
			m_instance.m_observers.add(observer);
		}
	}

	public static void unregisterObserver(Observer observer) {
		synchronized (m_instance.m_observers) {
			m_instance.m_observers.remove(observer);
		}
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
}
