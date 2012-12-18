package net.diva.browser;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.diva.browser.util.DdNUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class DdNMapActivity extends MapActivity {
	private static final String ACTION_LOCATION_UPDATE = "com.android.practice.map.ACTION_LOCATION_UPDATE";

	private LocationManager m_lm;
	private MapController mMapController;
	private MapView mMapView;
	private MyLocationOverlay mMyLocationOverlay;
	private MarkerItemizedOverlay markerOverlay;
	private LocationUpdateReceiver m_receiver;

	private List<StoreInfo> storeList;
	private StoreInfoAdapter m_adapter;
	private AlertDialog m_dialog;

	private int latE6;
	private int lngE6;

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location loc) {
			storeList(loc.getLatitude(), loc.getLongitude());
			m_lm.removeUpdates(locationListener);
		}

		public void onProviderDisabled(String s) {
		}

		public void onProviderEnabled(String s) {
		}

		public void onStatusChanged(String s, int i, Bundle bundle) {
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		initMapSet();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setOverlays();
		setIntentFilterToReceiver();
		requestLocationUpdates();
		if(storeList.isEmpty())
			locationSearch();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.map_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_review:
			viewStoreList();
			break;
		case R.id.item_research:
			locationSearch();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		resetOverlays();
		removeUpdates();
	}

	@Override
	protected void onDestroy() {
		removeUpdates();
		if(m_receiver != null)
			unregisterReceiver(m_receiver);
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void locationSearch(){
		String provider = null;
		if (m_lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			provider = LocationManager.GPS_PROVIDER;
		} else {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			criteria.setSpeedRequired(false);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(false);
			provider = m_lm.getBestProvider(criteria, true);
		}
		m_lm.requestLocationUpdates(provider, 1000, 1, locationListener);
	}

	public void search(View view) {
		TextView searchText = (TextView) findViewById(R.id.map_search_text);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		String q = searchText.getText().toString();
		if("".equals(q)){
			AlertDialog.Builder builder = new AlertDialog.Builder(DdNMapActivity.this);
			builder.setTitle("検索対象が未入力です");
			builder.setMessage("現在地検索を行いますか？");
			builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					locationSearch();
				}
			});
			builder.setNegativeButton("いいえ", null);
			builder.show();
			return;
		}
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());
		Address loc;
		try {
			List<Address> addressList = geocoder.getFromLocationName(q, 1);
			loc = addressList.get(0);
		} catch (Exception e) {
			Toast.makeText(this, "検索に失敗しました", Toast.LENGTH_SHORT).show();
			return;
		}
		storeList(loc.getLatitude(), loc.getLongitude());
	}

	private void storeList(double lat, double lng){
		storeList = new ArrayList<StoreInfo>();
		final URI uri = URI.create(String.format("http://eario.jp/diva/location.cgi?lat=" + lat + "&lng=" + lng));

		(new AsyncTask<Void, Void, String>(){
			@Override
			protected String doInBackground(Void... params) {
				try{
					return DdNUtil.read(uri);
				}catch(Exception e){
				}
				return null;
			}

			@Override
			protected final void onPostExecute(String json) {
				final JSONArray data;
				try{
					data = new JSONArray(json);
					for(int i = 0;i < data.length(); i++){
						StoreInfo info = new StoreInfo();
						JSONObject obj = data.getJSONObject(i);
						info.lat = obj.getDouble("lat");
						info.lng = obj.getDouble("lng");
						info.name = obj.getString("name");
						info.unit = obj.getString("unitCount");
						info.detailInfo = obj.getString("detailInfo");
						storeList.add(info);
					}
				}catch(Exception e){
					Toast.makeText(DdNMapActivity.this, "受け取ったJSONが駄目だったみたいです", Toast.LENGTH_LONG).show();
					return;
				}
				viewStoreList();
			}
		}).execute();
	}

	private void viewStoreList(){
		if(storeList.isEmpty()){
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(DdNMapActivity.this);
		builder.setTitle("店舗検索結果");

		m_adapter = new StoreInfoAdapter(this);
		m_adapter.setData(storeList);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.basic_list, null);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		listView.setAdapter(m_adapter);
		listView.setScrollingCacheEnabled(false);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> list, View v, int position, long id) {
				m_dialog.dismiss();
				StoreInfo info = m_adapter.getItem(position);
				latE6 = (int)(info.lat * 1E6);
				lngE6 = (int)(info.lng * 1E6);
				GeoPoint point = new GeoPoint(latE6, lngE6);
				markerOverlay.clearPoint();
				markerOverlay.addPoint(point);
        		GeoPoint gp = mMyLocationOverlay.getMyLocation();
        		mMapController.setCenter(gp);
        		int latSpanE6 = Math.abs(latE6 - gp.getLatitudeE6()) * 2;
        		int lonSpanE6 = Math.abs(lngE6 - gp.getLongitudeE6()) * 2;
        		mMapController.zoomToSpan(latSpanE6, lonSpanE6);
			}
		});
		builder.setView(view);
		m_dialog = builder.show();
	}

	private void initMapSet(){
		storeList = new ArrayList<StoreInfo>();
		// MapView objectの取得
		mMapView = (MapView) findViewById(R.id.map_view);
		// MapView#setBuiltInZoomControl()でZoom controlをbuilt-in moduleに任せる
		mMapView.setBuiltInZoomControls(true);
		// MapController objectを取得
		mMapController = mMapView.getController();

		Drawable marker = getResources().getDrawable(R.drawable.marker);
		markerOverlay = new MarkerItemizedOverlay(marker);
		mMapView.getOverlays().add(markerOverlay);

		latE6 = (int)(35.69921391872985 * 1E6);
		lngE6 = (int)(139.77094491841927 * 1E6);
		GeoPoint point = new GeoPoint(latE6, lngE6);
		markerOverlay.clearPoint();
		markerOverlay.addPoint(point);

		m_lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}


	private void setOverlays(){
        //User location表示用のMyLocationOverlay objectを取得
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		//初めてLocation情報を受け取った時の処理を記載
		//試しにそのLocationにanimationで移動し、zoom levelを19に変更
        mMyLocationOverlay.runOnFirstFix(new Runnable(){
        	public void run(){
        		GeoPoint gp = mMyLocationOverlay.getMyLocation();
        		mMapController.setCenter(gp);
        		int latSpanE6 = Math.abs(latE6 - gp.getLatitudeE6()) * 2;
        		int lonSpanE6 = Math.abs(lngE6 - gp.getLongitudeE6()) * 2;
        		mMapController.zoomToSpan(latSpanE6, lonSpanE6);
        	}
        });
        //LocationManagerからのLocation update取得
		mMyLocationOverlay.enableMyLocation();

		//overlayのlistにMyLocationOverlayを登録
        List<Overlay> overlays = mMapView.getOverlays();
        overlays.add(mMyLocationOverlay);
	}

	private void resetOverlays(){
        //LocationManagerからのLocation update情報を取得をcancel
		mMyLocationOverlay.disableMyLocation();

		//overlayのlistからMyLocationOverlayを削除
		List<Overlay> overlays = mMapView.getOverlays();
        overlays.remove(mMyLocationOverlay);
	}

	private void setIntentFilterToReceiver(){
		final IntentFilter filter = new IntentFilter();
    	filter.addAction(ACTION_LOCATION_UPDATE);
    	m_receiver = new LocationUpdateReceiver();
    	registerReceiver(m_receiver, filter);
	}

	private void requestLocationUpdates(){
		final PendingIntent requestLocation = getRequestLocationIntent(this);
        for(String providerName: m_lm.getAllProviders()){
			if(m_lm.isProviderEnabled(providerName)){
				m_lm.requestLocationUpdates(providerName, 0, 0, requestLocation);
			}
		}
	}

	private PendingIntent getRequestLocationIntent(Context context){
		return PendingIntent.getBroadcast(context, 0, new Intent(ACTION_LOCATION_UPDATE),
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void removeUpdates(){
    	final PendingIntent requestLocation = getRequestLocationIntent(this);
		m_lm.removeUpdates(requestLocation);
    }

	public class LocationUpdateReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			return;
		}
	}

	public class MarkerItemizedOverlay extends ItemizedOverlay<MarkerOverlayItem> {

	    private List<GeoPoint> points = new ArrayList<GeoPoint>();

	    public MarkerItemizedOverlay(Drawable defaultMarker) {
	        super( boundCenterBottom(defaultMarker) );
	    }

	    @Override
	    protected MarkerOverlayItem createItem(int i) {
	        GeoPoint point = points.get(i);
	        return new MarkerOverlayItem(point);
	    }

	    @Override
	    public int size() {
	        return points.size();
	    }

	    public void addPoint(GeoPoint point) {
	        this.points.add(point);
	        populate();
	    }

	    public void clearPoint() {
	        this.points.clear();
	        populate();
	    }
	}

	public class MarkerOverlayItem extends OverlayItem {

	    public MarkerOverlayItem(GeoPoint point){
	        super(point, "", "");
	    }
	}

	private class StoreInfo{
		private double lat;
		private double lng;
		private String name;
		private String unit;
		private String detailInfo;
	}

	private class StoreInfoAdapter extends BaseAdapter {

		private Context m_context;
		private List<StoreInfo> m_storeList;

		private StoreInfoAdapter(Context context){
			m_context = context;

		}

		private void setData(List<StoreInfo> storeList){
			m_storeList = storeList;
		}

		@Override
		public int getCount() {
			return m_storeList.size();
		}

		@Override
		public StoreInfo getItem(int position) {
			return m_storeList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		private class Holder implements View.OnClickListener {
			StoreInfo m_info;
			TextView name;
			TextView unit;

			private Holder(View view) {
				name = (TextView)view.findViewById(R.id.store_name);
				unit = (TextView)view.findViewById(R.id.store_unit);
				view.findViewById(R.id.button_info).setOnClickListener(this);
			}

			private void attach(StoreInfo info) {
				m_info = info;
				name.setText(m_info.name);
				unit.setText(m_info.unit);
			}

			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(DdNMapActivity.this);
				builder.setTitle("店舗情報詳細");
				builder.setMessage(Html.fromHtml(m_info.detailInfo));
				builder.setPositiveButton("閉じる", null);
				builder.show();
			}
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Holder holder;
			if (view != null)
				holder = (Holder)view.getTag();
			else {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(R.layout.store_item, parent, false);
				holder = new Holder(view);
				view.setTag(holder);
			}

			holder.attach(getItem(position));
			return view;
		}

	}
}
