package net.diva.browser;

import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.MailTo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebBrowseActivity extends Activity {
	public static void open(Context context, String relative) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DdN.url(relative)));
		intent.setClass(context.getApplicationContext(), WebBrowseActivity.class);
		context.startActivity(intent);
	}

	private WebView m_view;
	private ServiceClient m_service;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		m_service = DdN.getServiceClient();

		m_view = new WebView(this);
		m_view.setBackgroundColor(Color.BLACK);
		m_view.setInitialScale(67);
		m_view.setWebViewClient(new ViewClient());
		m_view.setWebChromeClient(new ChromeClient());

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		WebSettings settings = m_view.getSettings();
		settings.setBuiltInZoomControls(prefs.getBoolean("enable_builtin_zoom", false));

		Intent intent = getIntent();
		String url = intent.getDataString();
		String cookies = m_service.cookies();
		if (cookies == null)
			new LoginTask().execute(url);
		else
			displayPage(url, cookies);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.webbrowse_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.item_back).setEnabled(m_view.canGoBack());
		menu.findItem(R.id.item_forward).setEnabled(m_view.canGoForward());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_back:
			m_view.goBack();
			break;
		case R.id.item_forward:
			m_view.goForward();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void displayPage(String url, String cookies) {
		CookieManager manager = CookieManager.getInstance();
		manager.setAcceptCookie(true);
		manager.setCookie(DdN.url("/"), cookies);
		CookieSyncManager.getInstance().sync();

		setProgress(0);
		m_view.loadUrl(url);
		setContentView(m_view);
	}

	private class ViewClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			setProgressBarIndeterminateVisibility(true);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			m_service.access();
			setProgressBarIndeterminateVisibility(false);
			WebBrowseActivity.this.invalidateOptionsMenu();
			//Compatibility.invalidateOptionsMenu(WebBrowseActivity.this);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (MailTo.isMailTo(url)) {
				Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
				startActivity(i);
				return true;
			}
			if (m_service.isLogin())
				return super.shouldOverrideUrlLoading(view, url);

			new LoginTask().execute(url);
			return true;
		}
	}

	private class ChromeClient extends WebChromeClient {
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			setProgress(newProgress * 1000);
		}
	}

	private class LoginTask extends AsyncTask<String, Void, String[]> {
		@Override
		protected void onPreExecute() {
			setContentView(R.layout.waiting);
		}

		@Override
		protected String[] doInBackground(String... url) {
			try {
				m_service.login();
				return new String[] { url[0], m_service.cookies() };
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(String[] args) {
			if (args == null)
				finish();
			else
				displayPage(args[0], args[1]);
		}
	}
}
