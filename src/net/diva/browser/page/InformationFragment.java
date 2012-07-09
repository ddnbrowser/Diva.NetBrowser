package net.diva.browser.page;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.DdNMapActivity;
import net.diva.browser.R;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.ticket.DecorPrizeActivity;
import net.diva.browser.ticket.SkinPrizeActivity;
import net.diva.browser.util.ProgressTask;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class InformationFragment extends ListFragment implements DdN.Observer {
	private InformationAdapter m_adapter;

	private Item m_name = new Item(R.layout.info_item, R.id.text1, 0);
	private Item m_title = new Item(R.layout.info_item, R.id.text1, 0);
	private Item m_level = new Item(R.layout.info_item, R.id.text1, 0);
	private ProgressItem m_experience = new ProgressItem(DdN.EXPERIENCE_UNIT);
	private Item m_total = new Item(R.layout.info_right, R.id.text1, R.id.text2);
	private Item m_toNextLevel = new Item(R.layout.info_right, R.id.text1, R.id.text2);
	private Item m_toNextRank = new Item(R.layout.info_right, R.id.text1, R.id.text2);
	private Item m_vp = new Item(R.layout.info_right, R.id.text1, R.id.text2);
	private Item m_ticket = new Item(R.layout.info_right, R.id.text1, R.id.text2);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.basic_list, container, false);
		TextView empty = (TextView)v.findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_record);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		m_total.text1 = getText(R.string.total_exp);
		m_toNextLevel.text1 = getText(R.string.next_level);
		m_toNextRank.text1 = getText(R.string.next_rank);

		m_adapter = new InformationAdapter(getActivity(),
				new Item(android.R.layout.preference_category, android.R.id.title, getText(R.string.player_name)),
				m_name,
				new Item(android.R.layout.preference_category, android.R.id.title, getText(R.string.level_rank)),
				m_title,
				m_level,
				m_experience,
				m_total,
				m_toNextLevel,
				m_toNextRank,
				new Item(android.R.layout.preference_category, android.R.id.title, getText(R.string.vocaloid_point)),
				m_vp,
				new Item(android.R.layout.preference_category, android.R.id.title, getText(R.string.diva_ticket)),
				m_ticket);
		setListAdapter(m_adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		final PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			onUpdate(record, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.information_options, menu);
		inflater.inflate(R.menu.main_options, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateTask(getActivity()).execute();
			break;
		case R.id.item_exchange_skin:
			startActivity(new Intent(getActivity(), SkinPrizeActivity.class));
			break;
		case R.id.item_exchange_title:
			startActivity(new Intent(getActivity(), DecorPrizeActivity.class));
			break;
// silvia add start
		case R.id.location_search:
			locationSearch();
			break;
// silvia add end
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void onUpdate(MyList myList, boolean noMusic) {
		// do nothing.
	}

	public void onUpdate(PlayRecord record, boolean noMusic) {
		long total = record.experience();
		int experience = (int)(total % DdN.EXPERIENCE_UNIT);
		int nextLevel = DdN.EXPERIENCE_UNIT - experience;

		int[] nextRank = new int[1];
		record.rank(nextRank);

		m_name.text1 = record.player_name;
		m_title.text1 = record.title;
		m_level.text1 = record.level;
		m_experience.value = experience;
		m_total.text2 = String.format("%d.%02d %%", total/100, total%100);
		m_toNextLevel.text2 = String.format("%d.%02d %%", nextLevel/100, nextLevel%100);
		m_toNextRank.text2 = String.format("%d pts", nextRank[0]);
		m_vp.text2 = String.format("%d VP", record.vocaloid_point);
		m_ticket.text2 = String.format("%d 枚", record.ticket);

		m_adapter.notifyDataSetChanged();
	}

	private void locationSearch(){

		LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		String provider = null;
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			provider = LocationManager.GPS_PROVIDER;
		} else {
			Criteria criteria = new Criteria();
	        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
	        criteria.setPowerRequirement(Criteria.POWER_LOW);
	        criteria.setSpeedRequired(false);
	        criteria.setAltitudeRequired(false);
	        criteria.setBearingRequired(false);
	        criteria.setCostAllowed(false);
			provider = lm.getBestProvider(criteria, true);
		}
		Location loc = lm.getLastKnownLocation(provider);

		final URI uri = URI.create(String.format("http://eario.jp/diva/location.cgi?lat=" + loc.getLatitude() + "&lng=" + loc.getLongitude()));
		try{
			String json = read(uri);
			final JSONArray data = new JSONArray(json);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("店舗検索結果");
			String[] items = new String[data.length()];
			for(int i = 0;i < data.length(); i++){
				JSONObject obj = data.getJSONObject(i);
				items[i] = "【" + obj.getString("unitCount") + "】" + obj.getString("name");
			}
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					try{
						showMap(data.getJSONObject(which));
					}catch(Exception e){
					}
					dialog.dismiss();
				}
			});
			builder.show();
		}catch(Exception e){
		}
	}

	private void showMap(JSONObject loc) throws JSONException, UnsupportedEncodingException {
		String lat = loc.getString("lat");
		String lng = loc.getString("lng");
		Intent i = new Intent(getActivity(), DdNMapActivity.class);
		i.putExtra("lat", Double.valueOf(lat));
		i.putExtra("lng", Double.valueOf(lng));
		startActivity(i);
	}

	private String read(URI uri) throws IOException {
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(new HttpGet(uri));
		final int status = response.getStatusLine().getStatusCode();
		if (status != HttpStatus.SC_OK)
			throw new IOException(String.format("Invalid Server Response: %d", status));

		String charset = findCharset(response.getFirstHeader("Content-Type"));

		InputStream in = null;
		try {
			in = response.getEntity().getContent();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int read; (read = in.read(buffer)) != -1;)
				out.write(buffer, 0, read);
			return out.toString(charset);
		}
		finally {
			if (in != null)
				in.close();
		}
	}
	private String findCharset(Header header) {
		for (HeaderElement e: header.getElements()) {
			NameValuePair pair = e.getParameterByName("charset");
			if (pair != null)
				return pair.getValue();
		}
		return "UTF-8";
	}

	private static class InformationAdapter extends BaseAdapter {
		Context m_context;
		Item[] m_items;
		List<Integer> m_types = new ArrayList<Integer>();

		InformationAdapter(Context context, Item... items) {
			m_context = context;
			m_items = items;

			for (Item item: m_items) {
				if (!m_types.contains(item.layout))
					m_types.add(item.layout);
			}
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public int getViewTypeCount() {
			return m_types.size();
		}

		@Override
		public int getItemViewType(int position) {
			return m_types.indexOf(m_items[position].layout);
		}

		@Override
		public int getCount() {
			return m_items.length;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Item getItem(int position) {
			return m_items[position];
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Item item = getItem(position);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(item.layout, parent, false);
				item.prepare(view);
			}
			item.attach(view);
			return view;
		}
	}

	private static class Item {
		int layout;
		int item1;
		int item2;

		CharSequence text1;
		CharSequence text2;

		static class Holder {
			View view1;
			View view2;
		}

		Item(int layout_, int item1_, int item2_) {
			layout = layout_;
			item1 = item1_;
			item2 = item2_;
		}

		Item(int layout_, int item, CharSequence text) {
			layout = layout_;
			item1 = item;
			text1 = text;
		}

		void prepare(View view) {
			Holder holder = new Holder();
			if (item1 != 0)
				holder.view1 = view.findViewById(item1);
			if (item2 != 0)
				holder.view2 = view.findViewById(item2);
			view.setTag(holder);
		}

		void attach(View view) {
			Holder holder = (Holder)view.getTag();
			setText(holder.view1, text1);
			setText(holder.view2, text2);
		}

		static void setText(View view, CharSequence text) {
			if (view == null || !(view instanceof TextView))
				return;

			TextView tv = (TextView)view;
			if (text == null)
				tv.setVisibility(View.GONE);
			else {
				tv.setVisibility(View.VISIBLE);
				tv.setText(text);
			}
		}
	}

	private static class ProgressItem extends Item {
		int max;
		int value;

		ProgressItem(int max_) {
			super(R.layout.info_bar, R.id.progress, 0);
			max = max_;
		}

		@Override
		void attach(View view) {
			Holder holder = (Holder)view.getTag();
			ProgressBar progress = (ProgressBar)holder.view1;
			progress.setMax(max);
			progress.setProgress(value);
		}
	}

	private static class UpdateTask extends ProgressTask<Void, Void, Boolean> {
		public UpdateTask(Context context) {
			super(context, R.string.message_updating);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			ServiceClient service = DdN.getServiceClient();
			try {
				PlayRecord record = service.login();
				DdN.getLocalStore().update(record);
				DdN.setPlayRecord(record);
				return Boolean.TRUE;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return Boolean.FALSE;
		}
	}
}
