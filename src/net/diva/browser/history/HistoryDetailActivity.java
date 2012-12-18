package net.diva.browser.history;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.History;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import net.diva.browser.util.DdNUtil;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
/**
 *
 * @author silvia
 *
 */
public class HistoryDetailActivity extends Activity {

	private static final String PACKAGE_NAME = "net.diva.browser";
	private static final String OUT_STRAGE_IMAGE_DIR = "/" + PACKAGE_NAME + "/files";
	private static final String LOCAL_STRAGE_IMAGE_DIR = "/data/data/" + PACKAGE_NAME + "/files";
	private static final String IMAGE_FILE_NAME_HEADER = "DdNB_";

	private History m_history;
	private MusicInfo m_music;
	private String m_localFilePath;

	private Holder h;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_detail);

		for(File file : new File(LOCAL_STRAGE_IMAGE_DIR).listFiles()){
			if(file.getName().matches("DdNB_.+\\.jpg")){
				getContentResolver().delete(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						MediaStore.Images.Media.DATA + " = ?",
						new String[]{file.getPath()});
				file.delete();
			}
		}

		Intent intent = new Intent();
		m_history = (History) getIntent().getSerializableExtra("history");
		m_music = getMusic(DdN.getPlayRecord().musics, m_history.music_id);

		intent.putExtra("history", m_history);
		setResult(RESULT_FIRST_USER, intent);

		h = new Holder(this, null);
		h.attach(m_history, m_music);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.history_detail_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history_capture:
			shareHistory();
			break;
		case R.id.history_ranking:
			ranking();
			break;
		case R.id.history_result_picture:
			confirmResultPicture();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void shareHistory(){
		if(m_history.result_picture != null && !"".equals(m_history.result_picture) && !"null".equals(m_history.result_picture)){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.rp_choose_share_title);
			builder.setMessage(R.string.rp_choose_share_message);
			builder.setPositiveButton(R.string.rp_choose_create, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					shareImage(screenCapture());
				}
			});
			builder.setNegativeButton(R.string.rp_choose_rp, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					shareImage(getResultPicture());
				}
			});
			builder.show();
		}else{
			shareImage(screenCapture());
		}
	}

	private void shareImage(Uri uri){
		Intent send = new  Intent(Intent.ACTION_SEND);
		send.setType("image/jpg");
		send.putExtra(Intent.EXTRA_STREAM, uri);
		send.putExtra(Intent.EXTRA_TEXT, createTweetMsg());
		send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		send.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(send);
	}

	private Uri screenCapture(){

		DisplayMetrics dm = getResources().getDisplayMetrics();
		final float backup = dm.scaledDensity;
		if(backup != 1.5f)
			changeDensity(dm.scaledDensity * 540 / (dm.widthPixels < dm.heightPixels ? dm.widthPixels : dm.heightPixels));

		Bitmap bitmap = createBitmap();

		String filePath = LOCAL_STRAGE_IMAGE_DIR;
		String fileName = IMAGE_FILE_NAME_HEADER + m_history.play_date + ".jpg";

		//ファイルに保存
		try {
			byte[] w = bmp2data(bitmap, Bitmap.CompressFormat.JPEG, 80);
			writeDataFile(fileName, w);

			if(isMountedExSD()){
				String outStragePath = Environment.getExternalStorageDirectory().getPath() + OUT_STRAGE_IMAGE_DIR;
				File imgDir = new File(outStragePath);
				if(!imgDir.exists())
					if(!imgDir.mkdirs())
						android.util.Log.e("", "make dir failed.");

				m_localFilePath = LOCAL_STRAGE_IMAGE_DIR + "/" + fileName;
				File localFile = new File(m_localFilePath);
				File sdCardFile = new File(outStragePath + "/" + fileName);

				if(copyFile(localFile, sdCardFile)){
					localFile.delete();
					filePath = sdCardFile.getParent();
				}
			}

		} catch (Exception e) {
		} finally {
			changeDensity(backup);
		}

		ContentResolver cr = getContentResolver();

		ContentValues values = new ContentValues();
		values.put(Images.Media.TITLE, fileName);
		values.put(Images.Media.DISPLAY_NAME, fileName);
		values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.DATA, filePath + "/" + fileName);

		Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		return uri;
	}

	private void confirmResultPicture(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.rp_download_confirm_title);
		builder.setMessage(R.string.rp_download_confirm_msg);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				resultPicture();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	private void resultPicture() {
		(new ServiceTask<Void, Void, String>(this, R.string.message_downloading){
			@Override
			protected String doTask(ServiceClient service, Void... params) throws Exception {
				try{
					return service.checkResultPicture(m_history);
				}catch(Exception e){
				}
				return null;
			}

			protected void onResult(String historyId) {
				if(historyId == null)
					Toast.makeText(HistoryDetailActivity.this, R.string.rp_download_error, Toast.LENGTH_LONG).show();
				else
					resultPicture(historyId);
			}
		}).execute();
	}

	private void resultPicture(final String historyId){
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.result_picture, null);
		final RadioGroup image_size = (RadioGroup)view.findViewById(R.id.rp_image_size);
		final RadioGroup image_quality = (RadioGroup)view.findViewById(R.id.rp_image_quality);
		final CheckBox print_player = (CheckBox)view.findViewById(R.id.print_player);
		final CheckBox print_location = (CheckBox)view.findViewById(R.id.print_location);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.rp_picture_setting);
		builder.setView(view);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				String size = null;
				switch(image_size.getCheckedRadioButtonId()){
				case R.id.rp_size_big:
					size = "0";
					break;
				case R.id.rp_size_medium:
					size = "1";
					break;
				case R.id.rp_size_small:
					size = "2";
					break;
				}

				String quality = null;
				switch(image_quality.getCheckedRadioButtonId()){
				case R.id.rp_quality_high:
					quality = "0";
					break;
				case R.id.rp_quality_medium:
					quality = "1";
					break;
				case R.id.rp_quality_low:
					quality = "2";
					break;
				}

				final String[] values = new String[]{
						size,
						quality,
						(print_player.isChecked() ? "on" : null),
						(print_location.isChecked() ? "on" : null)
				};

				(new ServiceTask<Void, Void, String>(HistoryDetailActivity.this, R.string.rp_choose_rp){
					protected String doTask(ServiceClient service, Void... params) throws Exception {
						try{
							String path = service.buyResultPicture(historyId, values);
							return path;
						}catch(Exception e){
						}
						return null;
					}
					protected void onResult(String dlPath) {
						if(dlPath != null)
							new DownloadPictureTask(dlPath, m_history.play_date, values).execute();
						else
							Toast.makeText(HistoryDetailActivity.this, "購入に失敗しました", Toast.LENGTH_LONG).show();
					}
				}).execute();
			}
		});
		builder.show();
	}

	private Bitmap createBitmap(){
		final int[] captureSize = getResources().getIntArray(R.array.capture_size);
		int captureHeight = captureSize[0];
		int captureWidth = captureSize[1];
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			captureHeight = captureSize[1];
			captureWidth = captureSize[0];
		}

		Bitmap bmp = Bitmap.createBitmap(captureWidth, captureHeight, Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);

		View captureView = View.inflate(this, R.layout.history_detail, null);
		new Holder(this, captureView).attach(m_history, m_music);

		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(preference.getBoolean("history_location_display", false)){
			TextView play_place = (TextView) captureView.findViewById(R.id.play_place);
			play_place.setVisibility(View.INVISIBLE);
		}

		int measuredWidth = View.MeasureSpec.makeMeasureSpec(captureWidth, View.MeasureSpec.EXACTLY);
		int measuredHeight = View.MeasureSpec.makeMeasureSpec(captureHeight, View.MeasureSpec.EXACTLY);

		captureView.measure(measuredWidth, measuredWidth);
		captureView.layout(0, 0, measuredWidth, measuredHeight);
		captureView.invalidate();
		captureView.draw(canvas);
		return bmp;
	}

	private void changeDensity(float zoom){
		DisplayMetrics dm = getResources().getDisplayMetrics();
		dm.densityDpi = (int) (160 * zoom);
		dm.density = zoom;
		dm.xdpi = 160 * zoom;
		dm.ydpi = 160 * zoom;
		dm.scaledDensity = zoom;
	}

// 画面キャプチャ(現在画面に表示されているものをキャプチャする)
//	private Bitmap createBitmap(){
//		View view = this.getWindow().getDecorView();
//		view.setDrawingCacheEnabled(true);
//		Bitmap bitmap = view.getDrawingCache();
//		if (bitmap == null)
//			return null;
//		return bitmap;
//	}

	private static byte[] bmp2data(Bitmap src, Bitmap.CompressFormat format, int quality) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		src.compress(format, quality, os);
		return os.toByteArray();
	}

	private void writeDataFile(String name, byte[] w) throws Exception {
		OutputStream out = null;
		try {
			out = openFileOutput(name, Context.MODE_WORLD_READABLE);
			out.write(w, 0, w.length);
			out.close();
		} catch (Exception e) {
			try {
				if (out != null)
					out.close();
			} catch (Exception e2) {
			}
			throw e;
		}
	}

	private boolean isMountedExSD(){
		boolean ret = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
		return ret;
	}

	private boolean copyFile(File src, File dst) throws Exception {
		Class<?> fileUtils = Class.forName("android.os.FileUtils");
		Method m = fileUtils.getMethod("copyFile", File.class, File.class);
		return (Boolean) m.invoke(fileUtils, src, dst);
	}

	private String createTweetMsg(){
		StringBuffer sb = new StringBuffer();
		sb.append(m_music.title + " / ");
		sb.append(String.format("%s★%d / ", getResources().getStringArray(R.array.difficulty_names)[m_history.rank], m_music.records[m_history.rank].difficulty));
		sb.append(String.format("%dpts / ", m_history.score));
		sb.append(String.format("%d.%02d%% ", m_history.achievement/100, m_history.achievement%100));
		sb.append("#DdNBrowser");
		return sb.toString();
	}

	private MusicInfo getMusic(List<MusicInfo> musics, String id){
		for(MusicInfo m : musics){
			if(m.id.equals(id))
				return m;
		}
		return null;
	}

	public void delete(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("プレイ履歴削除確認");
		builder.setMessage(R.string.hist_delete_msg);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				delete();
				dialog.dismiss();
				setResult(RESULT_OK);
				finish();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	public void lock(View view){
		m_history.lock = m_history.lock == 0 ? 1 : 0;
		h.lock.setText(m_history.isLocked() ? "ロック解除" : "ロック");
		h.delete.setEnabled(!m_history.isLocked());
		DdN.getLocalStore().lockHistory(m_history);
	}

	public void picture(View view){
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(getResultPicture());

		startActivity(intent);
	}

	private Uri getResultPicture(){
		String filePath = null;
		if(isMountedExSD()){
			filePath = Environment.getExternalStorageDirectory().getPath() + OUT_STRAGE_IMAGE_DIR + "/" + m_history.result_picture;
		}else{
			filePath = LOCAL_STRAGE_IMAGE_DIR + "/" + m_history.result_picture;
		}

		Cursor c = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				null,
				MediaStore.Images.ImageColumns.DATA + " = ?",
				new String[]{filePath},
				null);
		c.moveToFirst();
		String contentname = "content://media/external/images/media/" + c.getInt(c.getColumnIndex(MediaStore.MediaColumns._ID));

		return Uri.parse(contentname);
	}

	public void ranking(){
		try{
			String musicTitle = URLEncoder.encode(DdNUtil.getMusicTitle(m_history.music_id), "UTF-8");
			final URI uri = URI.create(String.format("http://eario.jp/diva/ranking.cgi?music_name=%s&rank=%s&score=%s", musicTitle, m_history.rank, m_history.score));
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
				protected void onPostExecute(String ranking) {
					if(ranking != null)
						Toast.makeText(HistoryDetailActivity.this, ranking, Toast.LENGTH_LONG).show();
					else
						Toast.makeText(HistoryDetailActivity.this, "ランキング取得に失敗しました", Toast.LENGTH_SHORT).show();
				}
			}).execute();
		}catch(Exception e){
			Toast.makeText(this, "楽曲情報のURLエンコードで失敗しました", Toast.LENGTH_SHORT).show();
		}
	}

	private void delete(){
		DdN.getLocalStore().deleteHistory(m_history);
	}

	private class DownloadPictureTask extends ServiceTask<Void, Integer, Boolean> {
		private String fileName;
		private String filePath;
		private String m_path;

		public DownloadPictureTask(String path, int dateInt, String[] values) {
			super(HistoryDetailActivity.this, R.string.rp_download_result_picture);
			m_path = path;

			fileName = new SimpleDateFormat("yyMMdd_HHmm").format(new Date(dateInt * (long)1000));

			if("0".equals(values[0])){
				fileName += "_LS";
			}else if("1".equals(values[0])){
				fileName += "_MS";
			}else if("2".equals(values[0])){
				fileName += "_SS";
			}

			if("0".equals(values[1])){
				fileName += "_HQ";
			}else if("1".equals(values[1])){
				fileName += "_MQ";
			}else if("2".equals(values[1])){
				fileName += "_LQ";
			}

			if("on".equals(values[2]))
				fileName += "_P";

			if("on".equals(values[3]))
				fileName += "_S";

			fileName += ".jpg";
		}

		@Override
		protected void onPreExecute() {
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			super.onPreExecute();
		}

		protected Boolean doTask(ServiceClient service, Void... params) throws Exception {

			if(isMountedExSD()){
				String outStragePath = Environment.getExternalStorageDirectory().getPath() + OUT_STRAGE_IMAGE_DIR;
				File imgDir = new File(outStragePath);
				if(!imgDir.exists())
					if(!imgDir.mkdirs())
						android.util.Log.e("", "make dir failed.");

				filePath = outStragePath + "/" + fileName;
			}else{
				filePath = LOCAL_STRAGE_IMAGE_DIR + "/" + fileName;
			}

			HttpResponse response = service.downloadByPost(m_path);

			InputStream in = response.getEntity().getContent();
			OutputStream out = new FileOutputStream(new File(filePath));

			byte[] buffer = new byte[1024];
			int size = 0;
			m_progress.setMax((int) response.getEntity().getContentLength());
			publishProgress(size, (int) response.getEntity().getContentLength());
			for (int read; (read = in.read(buffer)) != -1;){
				out.write(buffer, 0, read);
				size += read;
				publishProgress(size);
			}

			return Boolean.TRUE;
		}

		@Override
		protected void onResult(Boolean result) {
			if(!result)
				return;

			m_history.result_picture = fileName;
			LocalStore store = DdN.getLocalStore();
			store.setPicture(m_history);
			h.attach(m_history, m_music);

			ContentResolver cr = getContentResolver();
			ContentValues values = new ContentValues();
			values.put(Images.Media.TITLE, fileName);
			values.put(Images.Media.DISPLAY_NAME, fileName);
			values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(Images.Media.DATA, filePath);

			cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values.length > 1) {
				m_progress.setIndeterminate(false);
				m_progress.setMax(values[1]);
			}
			m_progress.incrementProgressBy(values[0]);
		}

	}


	private static class Holder{

		Activity m_act;

		TextView title1;
		TextView play_date;
		TextView play_place;
		TextView title2;
		ImageView cover_art;
		TextView music_title;
		TextView rank;
		TextView clear_status;
		TextView achievement;
		TextView score;
		TextView trial_result;
		TextView title3;
		TextView cool;
		TextView fine;
		TextView safe;
		TextView sad;
		TextView worst;
		TextView combo;
		TextView challenge_time;
		TextView hold;
		Button delete;
		Button lock;
		Button result_picture;

		private Holder(Activity act, View view) {
			m_act = act;
			if(view == null){
				ViewGroup contentRoot = (ViewGroup)act.findViewById(android.R.id.content);
				view = contentRoot.getChildAt(0);
			}

			title1 = (TextView)view.findViewById(R.id.detail_title1);
			play_date = (TextView)view.findViewById(R.id.play_date);
			play_place = (TextView)view.findViewById(R.id.play_place);
			title2 = (TextView)view.findViewById(R.id.detail_title2);
			cover_art = (ImageView)view.findViewById(R.id.cover_art);
			music_title = (TextView)view.findViewById(R.id.music_title);
			rank = (TextView)view.findViewById(R.id.rank);
			clear_status = (TextView)view.findViewById(R.id.clear_status);
			achievement = (TextView)view.findViewById(R.id.achievement);
			score = (TextView)view.findViewById(R.id.score);
			trial_result = (TextView)view.findViewById(R.id.trial_result);
			title3 = (TextView)view.findViewById(R.id.detail_title3);
			cool = (TextView)view.findViewById(R.id.cool);
			fine = (TextView)view.findViewById(R.id.fine);
			safe = (TextView)view.findViewById(R.id.safe);
			sad = (TextView)view.findViewById(R.id.sad);
			worst = (TextView)view.findViewById(R.id.worst);
			combo = (TextView)view.findViewById(R.id.combo);
			challenge_time = (TextView)view.findViewById(R.id.challenge_time);
			hold = (TextView)view.findViewById(R.id.hold);
			lock = (Button)view.findViewById(R.id.lock_button);
			delete = (Button)view.findViewById(R.id.delete_button);
			result_picture = (Button)view.findViewById(R.id.result_picture_button);
		}

		private void attach(History h, MusicInfo m){

			final Resources res = m_act.getResources();

			if(title1 != null)
				title1.setText(res.getString(R.string.hist_title1));
			if(play_date != null)
				play_date.setText(h.getPlayDateStr());
			if(play_place != null)
				play_place.setText(h.play_place);
			title2.setText(res.getString(R.string.hist_title2));
			cover_art.setImageDrawable(m.getCoverArt(m_act.getApplicationContext()));
			music_title.setText(m.title);
			rank.setText(String.format("%s ★%d", res.getStringArray(R.array.difficulty_names)[h.rank], m.records[h.rank].difficulty));
			clear_status.setText(res.getStringArray(R.array.clear_status_names)[h.clear_status]);
			achievement.setText(String.format("%d.%02d%%", h.achievement/100, h.achievement%100));
			score.setText(String.format("%d pts", h.score));
			if(h.trial == 0){
				trial_result.setVisibility(View.INVISIBLE);
			}else{
				trial_result.setText(DdNUtil.getTrialsName(h.trial) + "クリアトライアル " + DdNUtil.getTrialResultsName(h.trial_result));
			}
			if(title3 != null)
				title3.setText(res.getString(R.string.hist_title3));
			cool.setText(String.format("%3s/%3s.%02d%%", h.cool, h.cool_per/100, h.cool_per%100));
			cool.setTypeface(Typeface.MONOSPACE);
			fine.setText(String.format("%3s/%3s.%02d%%", h.fine, h.fine_per/100, h.fine_per%100));
			fine.setTypeface(Typeface.MONOSPACE);
			safe.setText(String.format("%3s/%3s.%02d%%", h.safe, h.safe_per/100, h.safe_per%100));
			safe.setTypeface(Typeface.MONOSPACE);
			sad.setText(String.format("%3s/%3s.%02d%%", h.sad, h.sad_per/100, h.sad_per%100));
			sad.setTypeface(Typeface.MONOSPACE);
			worst.setText(String.format("%3s/%3s.%02d%%", h.worst, h.worst_per/100, h.worst_per%100));
			worst.setTypeface(Typeface.MONOSPACE);
			combo.setText(String.valueOf(h.combo));
			challenge_time.setText(String.format("%d pts", h.challange_time));
			hold.setText(String.format("%d pts", h.hold));
			lock.setText(h.isLocked() ? "ロック解除" : "ロック");
			delete.setEnabled(!h.isLocked());
			result_picture.setEnabled(h.result_picture != null && !"".equals(h.result_picture) && !"null".equals(h.result_picture));
		}
	}

}
