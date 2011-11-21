package net.diva.browser.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;

import org.apache.http.NameValuePair;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

public class ShopActivity extends Activity implements View.OnClickListener {
	private Bitmap m_thumbnail;
	private List<NameValuePair> m_details = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.waiting);
		TextView text = (TextView)findViewById(R.id.message);
		text.setText(R.string.message_loading);
		new LoadDetailTask().execute(getIntent().getDataString());
	}

	public void onClick(View view) {
		setResult(RESULT_OK, getIntent());
		finish();
	}

	@Override
	protected void onDestroy() {
		if (m_thumbnail != null)
			m_thumbnail.recycle();
		super.onDestroy();
	}

	private void showDetails() {
		setContentView(R.layout.shop);

		ImageView image = (ImageView)findViewById(R.id.thumbnail);
		if (m_thumbnail != null)
			image.setImageBitmap(m_thumbnail);
		else
			image.setVisibility(View.GONE);

		LinearLayout parent = (LinearLayout)findViewById(R.id.details);
		LayoutInflater inflater = getLayoutInflater();
		for (NameValuePair item: m_details) {
			TwoLineListItem view = (TwoLineListItem)inflater.inflate(R.layout.shop_item, parent, false);
			view.getText1().setText(item.getName());
			view.getText2().setText(item.getValue());
			parent.addView(view);
		}

		Button button = (Button)findViewById(R.id.button_buy);
		button.setOnClickListener(this);
		button.setText(getIntent().getIntExtra("label", R.string.button_buy));
	}

	private void showError() {
		Toast.makeText(this, R.string.failed_load_details, Toast.LENGTH_SHORT).show();
		finish();
	}

	private class LoadDetailTask extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			ServiceClient service = DdN.getServiceClient();
			try {
				if (!service.isLogin())
					service.login();

				String image = service.getShopDetail(params[0], m_details);
				if (image != null)
					m_thumbnail = BitmapFactory.decodeStream(service.download(image));
				return Boolean.TRUE;
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				showDetails();
			else
				showError();
		}
	}
}
