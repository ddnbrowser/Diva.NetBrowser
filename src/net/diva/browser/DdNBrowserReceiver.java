package net.diva.browser;

import net.diva.browser.history.DownloadHistoryService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DdNBrowserReceiver extends BroadcastReceiver {
	public static final String ACTION_DOWNLOAD_RANKING = "net.diva.browser.action.DOWNLOAD_RANKING";
	public static final String ACTION_DOWNLOAD_HISTORY = "net.diva.browser.action.DOWNLOAD_HISTORY";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (ACTION_DOWNLOAD_RANKING.equals(action)) {
			startDownloadRanking(context);
		}
		else if(ACTION_DOWNLOAD_HISTORY.equals(action)) {
			startDownloadHistory(context);
		}
	}

	public static PendingIntent downloadRankingOperation(Context context) {
		Intent intent = new Intent(context, DdNBrowserReceiver.class);
		intent.setAction(ACTION_DOWNLOAD_RANKING);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	private void startDownloadRanking(Context context) {
		DownloadRankingService.lock(context);
		Intent intent = new Intent(context, DownloadRankingService.class);
		try {
			if (context.startService(intent) == null)
				DownloadRankingService.unlock();
		}
		catch (Throwable t) {
			DownloadRankingService.unlock();
		}
	}

	public static PendingIntent downloadHistoryOperation(Context context) {
		Intent intent = new Intent(context, DdNBrowserReceiver.class);
		intent.setAction(ACTION_DOWNLOAD_HISTORY);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	private void startDownloadHistory(Context context) {
		DownloadHistoryService.lock(context);
		Intent intent = new Intent(context, DownloadHistoryService.class);
		try{
			if (context.startService(intent) == null)
				DownloadHistoryService.unlock();
		}
		catch (Throwable t) {
			DownloadHistoryService.unlock();
		}
	}
}
