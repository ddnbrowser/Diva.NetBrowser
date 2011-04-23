package net.diva.browser.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ExpandableListActivity;
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
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SkinListActivity extends ExpandableListActivity {
	private static final Pattern RE_NAME = Pattern.compile("(.+)(\\[.+\\])");

	private class Skin {
		int id;
		String name;
		boolean hasPurchased = false;
		List<Variant> variants = new ArrayList<Variant>();
	}

	private class Variant {
		String name;
		SkinInfo detail;
	}

	private Map<String, Skin> m_skins;
	private List<Skin> m_purchased;

	private SkinAdapter m_adapter;
	private LocalStore m_store;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skin_list);
		m_store = LocalStore.instance(this);

		m_skins = reconstruct(m_store.loadSkins());
		m_purchased = filterPurchased(m_skins.values());

		m_adapter = new SkinAdapter(this);
		refresh(savedInstanceState != null && savedInstanceState.getBoolean("showAll"));
		setListAdapter(m_adapter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("showAll", m_adapter.getSkins() != m_purchased);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View view, int group, int child, long id) {
		SkinInfo skin = m_adapter.getChild(group, child).detail;
		if (skin.purchased) {
			Intent intent = getIntent();
			intent.putExtra("id", skin.id);
			intent.putExtra("group_id", skin.group_id);
			setResult(RESULT_OK, intent);
			finish();
		}
		else {
			Intent intent = new Intent(getApplicationContext(), ShopActivity.class);
			intent.setData(Uri.parse(DdN.url("/divanet/skin/detail/%s/%s/0", skin.id, skin.group_id).toString()));
			intent.putExtra("id", skin.id);
			intent.putExtra("group_id", skin.group_id);
			startActivityForResult(intent, R.id.item_confirm_buying);
		}
		return true;
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new SkinDownloader().execute();
			break;
		case R.id.item_toggle_show_all:
			refresh(!isShowAll());
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private boolean isShowAll() {
		return m_adapter.getSkins() != m_purchased;
	}

	private void refresh(boolean showAll) {
		if (showAll)
			m_adapter.setSkins(new ArrayList<Skin>(m_skins.values()));
		else
			m_adapter.setSkins(m_purchased);
	}

	private Map<String, Skin> reconstruct(List<SkinInfo> skins) {
		int id = 0;
		Map<String, Skin> map = new TreeMap<String, Skin>();
		for (SkinInfo s: skins) {
			String sName = s.name;
			String vName;
			Matcher m = RE_NAME.matcher(sName);
			if (m.matches()) {
				sName = m.group(1);
				vName = m.group(2);
			}
			else {
				vName = getString(R.string.variant_normal);
			}

			Variant variant = new Variant();
			variant.name = vName;
			variant.detail = s;

			Skin skin = map.get(s.group_id);
			if (skin == null) {
				skin = new Skin();
				skin.id = id++;
				skin.name = sName;
				map.put(s.group_id, skin);
			}
			skin.hasPurchased |= s.purchased;
			skin.variants.add(variant);
		}
		return map;
	}

	private List<Skin> filterPurchased(Collection<Skin> skins) {
		List<Skin> purchased = new ArrayList<Skin>(skins.size());
		for (Skin s: skins) {
			if (!s.hasPurchased)
				continue;

			Skin skin = new Skin();
			skin.id = s.id;
			skin.name = s.name;
			for (Variant v: s.variants) {
				if (v.detail.purchased)
					skin.variants.add(v);
			}
			purchased.add(skin);
		}
		return purchased;
	}

	SkinInfo getSkinInfo(String id, String group_id) {
		Skin skin = m_skins.get(group_id);
		if (skin == null)
			return null;

		for (Variant variant: skin.variants) {
			SkinInfo si = variant.detail;
			if (si.id.equals(id))
				return si;
		}

		return null;
	}

	private static class SkinAdapter extends BaseExpandableListAdapter {
		Context m_context;
		List<Skin> m_skins;

		SkinAdapter(Context context) {
			m_context = context;
		}

		void setSkins(List<Skin> skins) {
			m_skins = skins;
			if (m_skins.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		List<Skin> getSkins() {
			return m_skins;
		}

		public Variant getChild(int group, int child) {
			return m_skins.get(group).variants.get(child);
		}

		public long getChildId(int group, int child) {
			return child;
		}

		class Holder {
			ImageView thumbnail;
			TextView text1;
			TextView text2;
		}

		public View getChildView(int group, int child, boolean isLastChild, View view, ViewGroup parent) {
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

			Variant variant = getChild(group, child);
			holder.text1.setText(variant.name);
			if (variant.detail.purchased)
				holder.text2.setVisibility(View.GONE);
			else {
				holder.text2.setText(R.string.not_purchased);
				holder.text2.setVisibility(View.VISIBLE);
			}
			Drawable thumbnail = variant.detail.getThumbnail(m_context);
			if (thumbnail != null)
				holder.thumbnail.setImageDrawable(thumbnail);

			return view;
		}

		public int getChildrenCount(int group) {
			return m_skins.get(group).variants.size();
		}

		public Skin getGroup(int group) {
			return m_skins.get(group);
		}

		public int getGroupCount() {
			return m_skins.size();
		}

		public long getGroupId(int group) {
			return getGroup(group).id;
		}

		public View getGroupView(int group, boolean isExpanded, View view, ViewGroup parent) {
			TextView text;
			if (view != null)
				text = (TextView)view.getTag();
			else {
				final LayoutInflater inflater =  LayoutInflater.from(m_context);
				view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
				text = (TextView)view.findViewById(android.R.id.text1);
				view.setTag(text);
			}

			Skin skin = getGroup(group);
			text.setText(skin.name);

			return view;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isChildSelectable(int group, int child) {
			return true;
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

			m_skins = reconstruct(result);
			m_purchased = filterPurchased(m_skins.values());
			m_adapter.setSkins(m_purchased);
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

			service.buySkin(skin.group_id, skin.id);
			skin.purchased = true;
			store.updateSkin(skin);
			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if (result) {
				boolean showAll = isShowAll();
				m_purchased = filterPurchased(m_skins.values());
				refresh(showAll);
			}
			else
				Toast.makeText(SkinListActivity.this, R.string.faile_buying, Toast.LENGTH_SHORT).show();
		}
	}
}
