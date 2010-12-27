package net.diva.browser;

import java.net.URI;

import android.app.Application;
import android.webkit.CookieSyncManager;

public class DdN extends Application {
	public static final URI URL = URI.create("http://project-diva-ac.net/divanet/");

	@Override
	public void onCreate() {
		super.onCreate();
		CookieSyncManager.createInstance(this);
	}

	public static String url(String relative) {
		return DdN.URL.resolve(relative).toString();
	}
}
