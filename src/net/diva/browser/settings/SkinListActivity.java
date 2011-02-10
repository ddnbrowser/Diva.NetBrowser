package net.diva.browser.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import android.net.Uri;
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
import android.widget.Toast;

public class SkinListActivity extends ListActivity {
	private SkinAdapter m_adapter;
	private LocalStore m_store;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skin_list);
		m_store = LocalStore.instance(this);

		m_adapter = new SkinAdapter(this);
		m_adapter.setSkins(m_store.loadSkins());
		setListAdapter(m_adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SkinInfo skin = m_adapter.getItem(position);
		if (skin.purchased) {
			Intent intent = getIntent();
			intent.putExtra("id", skin.id);
			intent.putExtra("group_id", skin.group_id);
			setResult(RESULT_OK, intent);
			finish();
		}
		else {
			Intent intent = new Intent(getApplicationContext(), ShopActivity.class);
			intent.setData(Uri.parse(DdN.url("/divanet/skin/detail/%s/%s/0", skin.group_id, skin.id).toString()));
			intent.putExtra("id", skin.id);
			intent.putExtra("group_id", skin.group_id);
			startActivityForResult(intent, R.id.item_confirm_buying);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case R.id.item_confirm_buying:
			if (resultCode == RESULT_OK) {
				SkinInfo skin = m_adapter.getSkinInfo(data.getStringExtra("id"), data.getStringExtra("group_id"));
				new BuyTask().execute(skin);
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
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
		case R.id.item_toggle_show_all:
			m_adapter.toggleShowAll();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private static class SkinAdapter extends BaseAdapter {
		Context m_context;
		List<SkinInfo> m_skins;
		List<SkinInfo> m_purchased;
		boolean m_showAll = false;

		SkinAdapter(Context context) {
			super();
			m_context = context;
		}

		void setSkins(List<SkinInfo> skins) {
			m_skins = skins;
			List<SkinInfo> purchased = new ArrayList<SkinInfo>();
			for (SkinInfo skin: skins) {
				if (skin.purchased)
					purchased.add(skin);
			}
			m_purchased = purchased;
			notifyDataSetChanged();
		}

		void toggleShowAll() {
			m_showAll = !m_showAll;
			notifyDataSetChanged();
		}

		SkinInfo getSkinInfo(String id, String group_id) {
			for (SkinInfo skin: m_skins) {
				if (skin.id.equals(id) && skin.group_id.equals(group_id))
					return skin;
			}
			return null;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(R.layout.skin_item, parent, false);
			}

			SkinInfo skin = getItem(position);
			TextView text1 = (TextView)view.findViewById(android.R.id.text1);
			text1.setText(skin.name);
			TextView text2 = (TextView)view.findViewById(android.R.id.text2);
			if (skin.purchased)
				text2.setVisibility(View.GONE);
			else {
				text2.setText(R.string.not_purchased);
				text2.setVisibility(View.VISIBLE);
			}
			Drawable thumbnail = skin.getThumbnail(m_context);
			if (thumbnail != null) {
				ImageView iv = (ImageView)view.findViewById(R.id.thumbnail);
				iv.setImageDrawable(thumbnail);
			}
			return view;
		}

		public int getCount() {
			return m_showAll ? m_skins.size() : m_purchased.size();
		}

		public SkinInfo getItem(int position) {
			return m_showAll ? m_skins.get(position) : m_purchased.get(position);
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

				List<SkinInfo> skins = service.getSkins();
				skins.addAll(service.getSkinsFromShop());
				m_store.updateSkins(skins);
				skins = m_store.loadSkins();

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

	private class BuyTask extends AsyncTask<SkinInfo, Void, Boolean> {
		private ProgressDialog m_progress;

		@Override
		protected void onPreExecute() {
			m_progress = new ProgressDialog(SkinListActivity.this);
			m_progress.setMessage(getString(R.string.buying));
			m_progress.setIndeterminate(true);
			m_progress.show();
		}

		@Override
		protected Boolean doInBackground(SkinInfo... params) {
			SkinInfo skin = params[0];
			ServiceClient service = DdN.getServiceClient();
			LocalStore store = DdN.getLocalStore();
			try {
				if (!service.isLogin())
					service.login();

				service.buySkin(skin.group_id, skin.id);
				skin.purchased = true;
				store.updateSkin(skin);
				return Boolean.TRUE;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			m_progress.dismiss();
			if (result)
				m_adapter.notifyDataSetChanged();
			else
				Toast.makeText(SkinListActivity.this, R.string.faile_buying, Toast.LENGTH_SHORT).show();
		}
	}
}
