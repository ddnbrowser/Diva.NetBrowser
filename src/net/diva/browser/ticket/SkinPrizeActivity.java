package net.diva.browser.ticket;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ListActivity;
import android.content.Context;
import android.graphics.drawable.Drawable;
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

public class SkinPrizeActivity extends ListActivity {
	private LocalStore m_store;
	private SkinAdapter m_adapter;

	private List<SkinInfo> m_skins;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);
		ListView list = getListView();
		list.setFastScrollEnabled(DdN.Settings.enableFastScroll);
		TextView empty = (TextView)findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_prize);

		m_store = DdN.getLocalStore();
		m_adapter = new SkinAdapter(this);
		m_adapter.setSkins(m_skins = m_store.loadSkins(true));
		setListAdapter(m_adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.prize_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateTask().execute();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private SkinInfo findSkin(String group, String id) {
		for (SkinInfo s: m_skins) {
			if (s.group_id.equals(group) && s.id.equals(id))
				return s;
		}
		return null;
	}

	private static class SkinAdapter extends BaseAdapter {
		private static final Pattern RE_NAME = Pattern.compile("(.+)(\\[.+\\])");

		private class Entry {
			CharSequence name;
			SkinInfo skin;

			Entry(CharSequence name_, SkinInfo skin_) {
				name = name_;
				skin = skin_;
			}
		}

		Context m_context;
		List<Entry> m_entries = Collections.emptyList();

		SkinAdapter(Context context) {
			m_context = context;
		}

		void setSkins(List<SkinInfo> skins) {
			if (skins.isEmpty()) {
				m_entries = Collections.emptyList();
				notifyDataSetInvalidated();
				return;
			}

			final CharSequence normal = m_context.getText(R.string.variant_normal);
			List<String> groups = new ArrayList<String>();
			Map<String, List<Entry>> map = new HashMap<String, List<Entry>>();
			for (SkinInfo s: skins) {
				CharSequence gName;
				CharSequence sName;
				Matcher m = RE_NAME.matcher(s.name);
				if (m.matches()) {
					gName = m.group(1);
					sName = m.group(2);
				}
				else {
					gName = s.name;
					sName = normal;
				}

				List<Entry> entries = map.get(s.group_id);
				if (entries == null) {
					map.put(s.group_id, entries = new ArrayList<Entry>());
					entries.add(new Entry(gName, null));
					groups.add(s.group_id);
				}
				entries.add(new Entry(sName, s));
			}

			List<Entry> entries = new ArrayList<Entry>(groups.size() + skins.size());
			for (String group_id: groups)
				entries.addAll(map.get(group_id));

			m_entries = entries;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return m_entries.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public SkinInfo getItem(int position) {
			return m_entries.get(position).skin;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position) == null ? 0 : 1;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItem(position) != null;
		}

		public View getView(int position, View view, ViewGroup parent) {
			Entry entry = m_entries.get(position);
			if (entry.skin == null)
				return getGroupView(entry, view, parent);
			else
				return getChildView(entry, view, parent);
		}

		View getGroupView(Entry entry, View view, ViewGroup parent) {
			TextView title;
			if (view != null)
				title = (TextView)view.getTag();
			else {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(android.R.layout.preference_category, parent, false);
				title = (TextView)view.findViewById(android.R.id.title);
				view.setTag(title);
			}
			title.setText(entry.name);
			return view;
		}

		class Holder {
			ImageView thumbnail;
			TextView text1;
		}

		View getChildView(Entry entry, View view, ViewGroup parent) {
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
				view.findViewById(android.R.id.text2).setVisibility(View.GONE);
			}

			holder.text1.setText(entry.name);
			Drawable thumbnail = entry.skin.getThumbnail(m_context);
			if (thumbnail != null)
				holder.thumbnail.setImageDrawable(thumbnail);

			return view;
		}
	}

	private class UpdateTask extends ServiceTask<Void, Void, List<SkinInfo>> {
		UpdateTask() {
			super(SkinPrizeActivity.this, R.string.message_updating);
		}

		@Override
		protected List<SkinInfo> doTask(ServiceClient service, Void... params)
				throws Exception {
			List<SkinInfo> skins = service.getSkinPrize();
			for (SkinInfo s: skins) {
				SkinInfo si = findSkin(s.group_id, s.id);
				if (si != null)
					s.image_path = si.image_path;
				else
					service.getSkinDetail(s);

				File file = s.getThumbnailPath(getApplicationContext());
				if (file != null && !file.exists()) {
					FileOutputStream out = new FileOutputStream(file);
					service.download(s.image_path, out);
					out.close();
				}
			}

			List<SkinInfo> missing = new ArrayList<SkinInfo>(m_skins);
			missing.removeAll(skins);
			if (!missing.isEmpty()) {
				for (SkinInfo s: missing) {
					s.prize = false;
					s.purchased = true;
				}
				m_store.updateSkins(missing);
			}

			m_store.updateSkins(skins);
			return skins;
		}

		@Override
		protected void onResult(List<SkinInfo> result) {
			if (result != null)
				m_adapter.setSkins(result);
		}
	}
}
