package net.diva.browser.compatibility;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.app.Fragment;

@SuppressWarnings("deprecation")
public class Compatibility {
	interface VersionImpl {
		public void invalidateOptionsMenu(Activity activity);
		public Notification getNotification(Context context, PendingIntent operation, int icon, CharSequence ticker, CharSequence title, CharSequence text, long when, boolean autoCancel);
		public void copyText(Activity activity, CharSequence text);
	}


	static class BaseVersionImpl implements VersionImpl {
		@Override
		public void invalidateOptionsMenu(Activity activity) {
		}

		public Notification getNotification(Context context, PendingIntent operation, int icon, CharSequence ticker, CharSequence title, CharSequence text, long when, boolean autoCancel) {
			Notification n = new Notification(icon, text, when);
			n.setLatestEventInfo(context, title, text, operation);
			if(autoCancel)
				n.flags = Notification.FLAG_AUTO_CANCEL;
			return n;
		}

		public void copyText(Activity activity, CharSequence text){
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard != null)
				clipboard.setText(text);
		}
	}

	static class HoneycombVersionImpl implements VersionImpl {
		@Override
		public void invalidateOptionsMenu(Activity activity) {
			activity.invalidateOptionsMenu();
		}

		public Notification getNotification(Context context, PendingIntent operation, int icon, CharSequence ticker, CharSequence title, CharSequence text, long when, boolean autoCancel) {
			Notification.Builder builder = new Notification.Builder(context)
			.setContentIntent(operation)
			.setSmallIcon(icon)
			.setTicker(ticker)
			.setContentTitle(title)
			.setContentText(text)
			.setWhen(when)
			.setAutoCancel(autoCancel);
			return builder.getNotification();
		}
		public void copyText(Activity activity, CharSequence text){
			ClipboardManager clipboard = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard != null) {
				ClipData cd = new ClipData(new ClipDescription("text_data", new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN }), new ClipData.Item(text));
				clipboard.setPrimaryClip(cd);
			}

		}
	}

	static final VersionImpl IMPL;
	static {
		if (android.os.Build.VERSION.SDK_INT >= 11)
			IMPL = new HoneycombVersionImpl();
		else
			IMPL = new BaseVersionImpl();
	}

	public static void invalidateOptionsMenu(Fragment fragment) {
		IMPL.invalidateOptionsMenu(fragment.getActivity());
	}

	public static void invalidateOptionsMenu(Activity activity) {
		IMPL.invalidateOptionsMenu(activity);
	}

	public static Notification getNotification(Context context, PendingIntent operation, int icon, CharSequence ticker, CharSequence title, CharSequence text, long when, boolean autoCancel){
		return IMPL.getNotification(context, operation, icon, ticker, title, text, when, autoCancel);
	}

	public static void copyText(Activity activity, CharSequence text){
		IMPL.copyText(activity, text);
	}
}