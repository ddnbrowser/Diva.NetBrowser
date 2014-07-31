package net.diva.browser.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;

public class CrushReport {
	private static final String REPORT_FILE = "crush_report";

	private static File sReportFile;

	public static void setup(Context context) {
		sReportFile = context.getFileStreamPath(REPORT_FILE);
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(
				context.getApplicationContext(),
				Thread.getDefaultUncaughtExceptionHandler()));
	}

	public static boolean exists() {
		return sReportFile != null && sReportFile.exists();
	}

	public static void clear() {
		if (sReportFile != null && sReportFile.exists())
			sReportFile.delete();
	}

	public static void sendByMail(Context context, String address) {
		if (!exists())
			return;

		InputStream in = null;
		try {
			in = context.openFileInput(REPORT_FILE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String subject = reader.readLine();
			StringBuilder body = new StringBuilder();
			for (String line; (line = reader.readLine()) != null;)
				body.append(line).append('\n');

			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
			intent.putExtra(Intent.EXTRA_SUBJECT, subject);
			intent.putExtra(Intent.EXTRA_TEXT, body.toString());
			context.startActivity(intent);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (in != null)
				try { in.close(); } catch (IOException e) {}
		}
		clear();
	}

	private static class ExceptionHandler implements UncaughtExceptionHandler {
		Context mContext;
		UncaughtExceptionHandler mFallback;

		public ExceptionHandler(Context context, UncaughtExceptionHandler fallback) {
			mContext = context;
			mFallback = fallback;
		}

		@Override
		public void uncaughtException(Thread t, Throwable ex) {
			try {
				writeReport(ex);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			mFallback.uncaughtException(t, ex);
		}

		void writeReport(Throwable error) throws IOException {
			OutputStream out = mContext.openFileOutput(REPORT_FILE, Context.MODE_PRIVATE);
			try {
				PrintWriter writer = new PrintWriter(out);
				try {
					PackageManager manager = mContext.getPackageManager();
					PackageInfo pkg = manager.getPackageInfo(mContext.getPackageName(), 0);
					CharSequence name = manager.getApplicationLabel(pkg.applicationInfo);
					writer.printf(Locale.US, "[BUG] %s/%s (%s:%d)\n", pkg.packageName, name, pkg.versionName, pkg.versionCode);
				}
				catch (NameNotFoundException e) {
					writer.println("[BUG] unknown");
				}

				writer.printf("Device: %s\n", Build.DEVICE);
				writer.printf("Model: %s\n", Build.MODEL);
				writer.printf("SDK-Version: %s\n", Build.VERSION.SDK);
				writer.println();
				writer.println("StackTrace:");
				error.printStackTrace(writer);

				writer.close();
			}
			finally {
				out.close();
			}
		}
	}
}
