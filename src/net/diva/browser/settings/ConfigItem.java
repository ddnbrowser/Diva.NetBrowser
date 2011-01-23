package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public abstract class ConfigItem {
	private ApplyTask m_task;

	public abstract boolean isCategory();
	public abstract void setContent(View view);
	public abstract Intent dispatch(Context context, Callback callback);

	public interface Callback {
		void onUpdated();
	}

	public void onResult(int result, Intent data, Callback callback) {
		if (result == Activity.RESULT_OK)
			new ApplyTask(callback).execute(data);
	}

	protected void confirm(Context context, final Callback callback, int title, int message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				onResult(Activity.RESULT_OK, null, callback);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	public boolean inProgress() {
		return m_task != null;
	}

	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		return Boolean.FALSE;
	}

	protected void setText(View view, int resId, CharSequence text) {
		TextView tv = (TextView)view.findViewById(resId);
		if (tv != null)
			tv.setText(text);
	}

	private class ApplyTask extends AsyncTask<Intent, Void, Boolean> {
		Callback m_callback;

		public ApplyTask(Callback callback) {
			m_callback = callback;
		}

		@Override
		protected void onPreExecute() {
			m_task = this;
			m_callback.onUpdated();
		}

		@Override
		protected Boolean doInBackground(Intent... params) {
			try {
				ServiceClient service = DdN.getServiceClient();
				if (!service.isLogin()) {
					PlayRecord record = service.login();
					DdN.setPlayRecord(record);
				}

				return apply(service, DdN.getLocalStore(), params[0]);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			m_task = null;
			m_callback.onUpdated();
		}

	}
}
