package net.diva.browser;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;

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

	private MapController mMapController;
	private MapView mMapView;
	private MyLocationOverlay mMyLocationOverlay;

	private int latE6;
	private int lngE6;

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
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void initMapSet(){
		// MapView objectの取得
		mMapView = (MapView) findViewById(R.id.map_view);
		// MapView#setBuiltInZoomControl()でZoom controlをbuilt-in moduleに任せる
		mMapView.setBuiltInZoomControls(true);
		// MapController objectを取得
		mMapController = mMapView.getController();

		Drawable marker = getResources().getDrawable(R.drawable.marker);
		MarkerItemizedOverlay markerOverlay = new MarkerItemizedOverlay(marker);
		mMapView.getOverlays().add(markerOverlay);

		Intent intent = getIntent();

		latE6 = (int)(intent.getDoubleExtra("lat", 35.681396) * 1E6);
		lngE6 = (int)(intent.getDoubleExtra("lng", 139.766049) * 1E6);
		GeoPoint point = new GeoPoint(latE6, lngE6);
		markerOverlay.addPoint(point);
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
    	registerReceiver(new LocationUpdateReceiver(), filter);
	}

	private void requestLocationUpdates(){
		final PendingIntent requestLocation = getRequestLocationIntent(this);
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        for(String providerName: lm.getAllProviders()){
			if(lm.isProviderEnabled(providerName)){
				lm.requestLocationUpdates(providerName, 0, 0, requestLocation);
			}
		}
	}

	private PendingIntent getRequestLocationIntent(Context context){
		return PendingIntent.getBroadcast(context, 0, new Intent(ACTION_LOCATION_UPDATE),
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void removeUpdates(){
    	final PendingIntent requestLocation = getRequestLocationIntent(this);
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(requestLocation);
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
}
