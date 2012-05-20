package net.diva.browser.history;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.HistoryStore;
import net.diva.browser.model.History;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.util.DdNUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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

	private HistoryStore m_store;
	private History m_history;
	private MusicInfo m_music;
	private String m_localFilePath;

	private Holder h;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_detail);

		m_store = HistoryStore.getInstance(this);

		for(File file : new File(LOCAL_STRAGE_IMAGE_DIR).listFiles()){
			if(file.getName().matches("DdNB_.+\\.jpg")){
				getContentResolver().delete(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						MediaStore.Images.Media.DATA + " = ?",
						new String[]{file.getPath()});
				file.delete();
			}
		}

		Intent intent = getIntent();
		m_history = m_store.getPlayHistory(intent.getLongExtra("history_id", 0));
		m_music = DdN.getPlayRecord().getMusicByTitle(m_history.music_title);

		h = new Holder(this, null);
		h.attach(m_history, m_music);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.history_detail_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final boolean locked = m_history.isLocked();
		menu.findItem(R.id.item_lock).setVisible(!locked);
		menu.findItem(R.id.item_unlock).setVisible(locked);
		menu.findItem(R.id.item_delete).setEnabled(!locked);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_lock:
			lock(true);
			break;
		case R.id.item_unlock:
			lock(false);
			break;
		case R.id.item_share:
			shareOtherApp(screenCapture());
			break;
		case R.id.item_delete:
			deleteHistory();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void shareOtherApp(Uri uri){
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
		final int difficulty = m_music != null ? m_music.records[m_history.rank].difficulty : 0;
		StringBuffer sb = new StringBuffer();
		sb.append(m_history.music_title + " / ");
		sb.append(String.format("%s ★%d / ", getResources().getStringArray(R.array.difficulty_names)[m_history.rank], difficulty));
		sb.append(String.format("%d pts / ", m_history.score));
		sb.append(String.format("%d.%02d %% ", m_history.achievement/100, m_history.achievement%100));
		sb.append("#DdNBrowser");
		return sb.toString();
	}

	public void deleteHistory(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirm_delete_history_title);
		builder.setMessage(R.string.confirm_delete_history);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				m_store.deleteHistory(m_history);
				dialog.dismiss();
				finish();
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	public void lock(boolean on){
		m_history.setLocked(on);
		m_store.lockHistory(m_history);
	}

	private static class Holder{
		private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

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
		}

		private void attach(History h, MusicInfo m){

			final Resources res = m_act.getResources();
			int difficulty = 0;
			Drawable coverArt = null;
			if (m != null) {
				difficulty = m.records[h.rank].difficulty;
				coverArt = m.getCoverArt(m_act.getApplicationContext());
			}

			if(title1 != null)
				title1.setText(res.getString(R.string.hist_title1));
			if(play_date != null)
				play_date.setText(DATE_FORMAT.format(new Date(h.play_date)));
			if(play_place != null)
				play_place.setText(h.play_place);
			title2.setText(res.getString(R.string.hist_title2));
			cover_art.setImageDrawable(coverArt);
			music_title.setText(h.music_title);
			rank.setText(String.format("%s ★%d", res.getStringArray(R.array.difficulty_names)[h.rank], difficulty));
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
			cool.setText(formatNotes(h.cool, h.cool_rate));
			cool.setTypeface(Typeface.MONOSPACE);
			fine.setText(formatNotes(h.fine, h.fine_rate));
			fine.setTypeface(Typeface.MONOSPACE);
			safe.setText(formatNotes(h.safe, h.safe_rate));
			safe.setTypeface(Typeface.MONOSPACE);
			sad.setText(formatNotes(h.sad, h.sad_rate));
			sad.setTypeface(Typeface.MONOSPACE);
			worst.setText(formatNotes(h.worst, h.worst_rate));
			worst.setTypeface(Typeface.MONOSPACE);
			combo.setText(String.valueOf(h.combo));
			challenge_time.setText(String.format("%d pts", h.challange_time));
			hold.setText(String.format("%d pts", h.hold));
		}

		private String formatNotes(int notes, int rate) {
			return String.format("%3s/%3s.%02d%%", notes, rate/100, rate%100);
		}
	}

}
