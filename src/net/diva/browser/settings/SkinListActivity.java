package net.diva.browser.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ServiceClient;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SkinListActivity extends ListActivity {
	private SkinAdapter m_adapter;
	private LocalStore m_store;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skin_list);
		m_store = LocalStore.instance(this);

		m_adapter = new SkinAdapter(this);
		setListAdapter(m_adapter);
		m_adapter.setSkins(m_store.loadSkins());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SkinInfo skin = m_adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("id", skin.id);
		intent.putExtra("group_id", skin.group_id);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.skin_list_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new SkinDownloader().execute();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private static class SkinAdapter extends BaseAdapter {
		Context m_context;
		List<SkinInfo> m_skins;

		public SkinAdapter(Context context) {
			super();
			m_context = context;
		}

		public void setSkins(List<SkinInfo> skins) {
			m_skins = skins;
			notifyDataSetChanged();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(R.layout.skin_item, parent, false);
			}

			SkinInfo skin = getItem(position);
			TextView tv = (TextView)view.findViewById(android.R.id.text1);
			tv.setText(skin.name);
			Drawable thumbnail = skin.getThumbnail(m_context);
			if (thumbnail != null) {
				ImageView iv = (ImageView)view.findViewById(R.id.thumbnail);
				iv.setImageDrawable(thumbnail);
			}
			return view;
		}

		public int getCount() {
			return m_skins != null ? m_skins.size() : 0;
		}

		public SkinInfo getItem(int position) {
			return m_skins.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
	}

	private class SkinDownloader extends AsyncTask<Void, Void, List<SkinInfo>> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(SkinListActivity.this);
			m_progress.setMessage(getString(R.string.message_skin_updating));
			m_progress.setIndeterminate(true);
			m_progress.show();
		}

		@Override
		protected List<SkinInfo> doInBackground(Void... params) {
			try {
				ServiceClient service = DdN.getServiceClient();
				PlayRecord record = DdN.getPlayRecord();
				if (!service.isLogin()) {
					record = DdN.setPlayRecord(service.login());
					m_store.update(record);
				}

				m_store.updateSkins(service.getSkins());
				List<SkinInfo> skins = m_store.loadSkins();
				for (SkinInfo skin: skins) {
					if (skin.image_path != null)
						continue;

					service.getSkinDetail(skin);
					File file = skin.getThumbnailPath(getApplicationContext());
					if (file != null) {
						FileOutputStream out = new FileOutputStream(file);
						service.download(skin.image_path, out);
						out.close();
						m_store.updateSkin(skin);
					}
				}
				return skins;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (LoginFailedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<SkinInfo> result) {
			m_progress.dismiss();
			if (result != null)
				m_adapter.setSkins(result);
		}
	}
}
