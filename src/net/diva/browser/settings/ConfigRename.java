package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.OperationFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ConfigRename extends ConfigItem {
	private CharSequence m_title;
	private CharSequence m_summary;
	private CharSequence m_applying;

	private String m_name;
	private CharSequence m_error;

	public ConfigRename(Context context) {
		m_title = context.getText(R.string.description_rename);
		m_summary = context.getText(R.string.summary_rename);
		m_applying = context.getText(R.string.summary_applying);
		m_name = DdN.getPlayRecord().player_name;
	}

	@Override
	public void setContent(View view) {
		setText(view, R.id.title, m_title);
		setText(view, R.id.summary, inProgress() ? m_applying : m_error != null ? m_error : m_summary);
	}

	@Override
	public Intent dispatch(Context context, final Callback callback) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.rename_dialog, null);
		final TextView edit = (TextView)view.findViewById(R.id.player_name);
		edit.setText(m_name);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(m_title);
		if (m_error != null)
			builder.setMessage(m_error);
		builder.setView(view);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				m_name = edit.getText().toString();
				onResult(Activity.RESULT_OK, null, callback);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
		return null;
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		m_error = null;
		try {
			service.rename(m_name);

			PlayRecord record = DdN.getPlayRecord();
			record.player_name = m_name;
			store.update(record);
			DdN.notifyPlayRecordChanged();
			return Boolean.TRUE;
		}
		catch (OperationFailedException e) {
			m_error = e.getMessage();
		}
		return Boolean.FALSE;
	}
}
