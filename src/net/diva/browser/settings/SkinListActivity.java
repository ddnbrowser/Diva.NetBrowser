package net.diva.browser.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SkinListActivity extends Activity implements AdapterView.OnItemClickListener {
	private LocalStore m_store;
	private GridView m_grid;
	private SkinAdapter m_adapter;

	private int m_mode = R.id.item_show_purchased;
	private List<SkinInfo> m_skins;
	private List<SkinInfo> m_purchased;
	private List<SkinInfo> m_notPurchased;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skin_list);

		boolean hasNoUse = getIntent().getBooleanExtra("hasNoUse", false);

		m_store = DdN.getLocalStore();
		m_adapter = new SkinAdapter(this, hasNoUse);
		m_grid = (GridView)findViewById(R.id.grid);
		m_grid.setEmptyView(findViewById(android.R.id.empty));
		m_grid.setAdapter(m_adapter);
		m_grid.setOnItemClickListener(this);

		m_skins = m_store.loadSkins();
		refresh(savedInstanceState != null ? savedInstanceState.getInt("displayMode") : m_mode);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("displayMode", m_mode);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		SkinInfo skin = (SkinInfo)parent.getAdapter().getItem(position);
		if (skin.purchased) {
			Intent intent = getIntent();
			intent.putExtra("id", skin.id);
			intent.putExtra("group_id", skin.group_id);
			intent.putExtra("setNoUse", skin.id == null);
			setResult(RESULT_OK, intent);
			finish();
		}
		else {
			Intent intent = new Intent(getApplicationContext(), ShopActivity.class);
			intent.setData(Uri.parse(DdN.url("/divanet/skin/detail/%s/%s/0", skin.id, skin.group_id)));
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
				SkinInfo skin = getSkinInfo(data.getStringExtra("id"), data.getStringExtra("group_id"));
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(m_mode);
		if (item != null)
			item.setChecked(true);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int id = item.getItemId();
		switch (id) {
		case R.id.item_update:
			new SkinDownloader().execute();
			break;
		case R.id.item_show_all:
		case R.id.item_show_purchased:
		case R.id.item_show_not_purchased:
			refresh(id);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private void refresh(int mode) {
		switch (mode) {
		case R.id.item_show_all:
			m_adapter.setSkins(m_skins);
			break;
		case R.id.item_show_purchased:
			if (m_purchased == null)
				m_purchased = filter(m_skins, true);
			m_adapter.setSkins(m_purchased);
			break;
		case R.id.item_show_not_purchased:
			if (m_notPurchased == null)
				m_notPurchased = filter(m_skins, false);
			m_adapter.setSkins(m_notPurchased);
			break;
		}
		m_mode = mode;
	}

	private List<SkinInfo> filter(Collection<SkinInfo> skins, boolean purchased) {
		List<SkinInfo> filtered = new ArrayList<SkinInfo>();
		for (SkinInfo skin: skins) {
			if (skin.purchased == purchased)
				filtered.add(skin);
		}
		return filtered;
	}

	SkinInfo getSkinInfo(String id, String group_id) {
		for (SkinInfo skin: m_skins) {
			if (skin.id.equals(id) && skin.group_id.equals(group_id))
				return skin;
		}
		return null;
	}

	private static class SkinAdapter extends BaseAdapter {
		Context m_context;
		List<SkinInfo> m_skins;
		SkinInfo m_noUse;

		SkinAdapter(Context context, boolean hasNoUse) {
			m_context = context;
			m_skins = Collections.emptyList();
			if (hasNoUse) {
				m_noUse = new SkinInfo(null, null, context.getString(R.string.no_use_skin), true);
			}
		}

		void setSkins(List<SkinInfo> skins) {
			if (m_noUse == null)
				m_skins = skins;
			else {
				m_skins = new ArrayList<SkinInfo>(skins);
				m_skins.add(0, m_noUse);
			}

			if (m_skins.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		public int getCount() {
			return m_skins.size();
		}

		public SkinInfo getItem(int position) {
			return m_skins.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		class Holder {
			ImageView thumbnail;
			TextView text1;
			TextView text2;
		}

		public View getView(int position, View view, ViewGroup parent) {
			Holder holder;
			if (view != null)
				holder = (Holder)view.getTag();
			else {
				holder = new Holder();
				final LayoutInflater inflater =  LayoutInflater.from(m_context);
				view = inflater.inflate(R.layout.skin_item, parent, false);
				view.setTag(holder);
				holder.thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
				holder.text1 = (TextView)view.findViewById(android.R.id.text1);
				holder.text2 = (TextView)view.findViewById(android.R.id.text2);
			}

			SkinInfo skin = getItem(position);
			holder.text1.setText(skin.name);
			if (skin.purchased)
				holder.text2.setVisibility(View.GONE);
			else {
				holder.text2.setText(R.string.not_purchased);
				holder.text2.setVisibility(View.VISIBLE);
			}
			Drawable thumbnail = skin.getThumbnail(m_context);
			if (thumbnail == null)
				thumbnail = m_context.getResources().getDrawable(R.drawable.no_skin);
			holder.thumbnail.setImageDrawable(thumbnail);

			return view;
		}
	}

	private class SkinDownloader extends ServiceTask<Void, Void, List<SkinInfo>> {
		SkinDownloader() {
			super(SkinListActivity.this, R.string.message_updating);
		}

		@Override
		protected List<SkinInfo> doTask(ServiceClient service, Void... params) throws Exception {
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

		@Override
		protected void onResult(List<SkinInfo> result) {
			if (result == null)
				return;

			m_skins = result;
			m_purchased = m_notPurchased = null;
			refresh(m_mode);
		}
	}

	private class BuyTask extends ServiceTask<SkinInfo, Void, Boolean> {
		BuyTask() {
			super(SkinListActivity.this, R.string.buying);
		}

		@Override
		protected Boolean doTask(ServiceClient service, SkinInfo... params) throws Exception {
			SkinInfo skin = params[0];
			LocalStore store = DdN.getLocalStore();

			int vp = service.buySkin(skin.group_id, skin.id);
			if (vp >= 0)
				DdN.setVocaloidPoint(vp);
			skin.purchased = true;
			store.updateSkin(skin);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result) {
				m_purchased = m_notPurchased = null;
				refresh(m_mode);
			}
			else
				Toast.makeText(SkinListActivity.this, R.string.faile_buying, Toast.LENGTH_SHORT).show();
		}
	}
}
