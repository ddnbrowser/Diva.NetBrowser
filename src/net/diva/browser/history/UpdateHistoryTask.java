package net.diva.browser.history;

import net.diva.browser.DdN;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
/**
 *
 * @author silvia
 *
 */
public class UpdateHistoryTask extends AsyncTask<Void, Integer, Boolean> {
	protected ProgressDialog m_progress;
	private UpdateHistory m_uh;
	private Fragment fragment;

	public UpdateHistoryTask(Fragment fragment, int message) {
		this.fragment = fragment;
		Activity act = fragment.getActivity();
		m_uh = new UpdateHistory(act);
		m_progress = new ProgressDialog(act);
		m_progress.setMessage(act.getString(message));
		m_progress.setIndeterminate(true);
	}

	@Override
	protected void onPreExecute() {
		m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		m_progress.show();
		super.onPreExecute();
	}

	protected Boolean doTask(ServiceClient service)
			throws Exception {

		m_uh.setTask(this);
		m_uh.update(service);

		return Boolean.TRUE;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (values.length > 1) {
			m_progress.setMax(values[1]);
			m_progress.setIndeterminate(false);
		}
		m_progress.incrementProgressBy(values[0]);
	}

	@Override
	protected final void onPostExecute(Boolean result) {
		m_progress.dismiss();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		ServiceClient service = DdN.getServiceClient();
		try {
			if (!service.isLogin()) {
				PlayRecord record = service.login();
				DdN.getLocalStore().update(record);
				DdN.setPlayRecord(record);
			}

			return doTask(service);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return Boolean.FALSE;
	}

	protected void myPublishProgress(Integer ... is){
		super.publishProgress(is);
	}
}
