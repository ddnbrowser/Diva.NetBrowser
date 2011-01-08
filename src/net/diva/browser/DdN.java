package net.diva.browser;

import java.net.URI;
import java.util.List;

import net.diva.browser.service.ServiceClient;

import org.apache.http.NameValuePair;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.TextView;

public class DdN extends Application {
	public static final URI URL = URI.create("http://project-diva-ac.net/divanet/");

	private static DdN m_instance;

	private ServiceClient m_service;
	private List<NameValuePair> m_titles;

	@Override
	public void onCreate() {
		super.onCreate();
		CookieSyncManager.createInstance(this);
		m_instance = this;
	}

	private ServiceClient getServiceClient_(Account account) {
		if (m_service == null && account != null)
			m_service = new ServiceClient(account.access_code, account.password);
		return m_service;
	}

	public static String url(String relative) {
		return DdN.URL.resolve(relative).toString();
	}

	public static ServiceClient getServiceClient(Account account) {
		return m_instance == null ? null : m_instance.getServiceClient_(account);
	}

	public static ServiceClient getServiceClient() {
		return getServiceClient(Account.load(PreferenceManager.getDefaultSharedPreferences(m_instance)));
	}

	public static void setTitles(List<NameValuePair> titles) {
		if (m_instance != null)
			m_instance.m_titles = titles;
	}

	public static String getTitle(String id) {
		if (id == null || m_instance == null)
			return null;

		for (NameValuePair pair: m_instance.m_titles) {
			if (pair.getName().equals(id))
				return pair.getValue();
		}

		return null;
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
