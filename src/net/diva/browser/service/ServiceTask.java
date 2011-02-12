package net.diva.browser.service;

import net.diva.browser.DdN;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.util.ProgressTask;
import android.content.Context;

public class ServiceTask<Params, Progress, Result> extends ProgressTask<Params, Progress, Result> {
	public ServiceTask(Context context, int message) {
		super(context, message);
	}

	@Override
	protected Result doInBackground(Params... params) {
		ServiceClient service = DdN.getServiceClient();
		try {
			if (!service.isLogin()) {
				PlayRecord record = service.login();
				DdN.getLocalStore().update(record);
				DdN.setPlayRecord(record);
			}

			return doTask(service, params);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected Result doTask(ServiceClient service, Params... params) throws Exception {
		return null;
	}
}
