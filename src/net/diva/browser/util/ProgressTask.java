package net.diva.browser.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class ProgressTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
	protected ProgressDialog m_progress;

	public ProgressTask(Context context, int message) {
		m_progress = new ProgressDialog(context);
		m_progress.setMessage(context.getString(message));
		m_progress.setIndeterminate(true);
	}

	@Override
	protected void onPreExecute() {
		m_progress.show();
	}

	@Override
	protected final void onPostExecute(Result result) {
		onResult(result);
		m_progress.dismiss();
	}

	protected void onResult(Result result) {
	}
}
