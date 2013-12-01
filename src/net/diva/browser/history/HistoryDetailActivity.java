package net.diva.browser.history;


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
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
	private HistoryStore m_store;
	private History m_history;
	private MusicInfo m_music;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_detail);

		m_store = HistoryStore.getInstance(this);

		Intent intent = getIntent();
		m_history = m_store.getPlayHistory(intent.getLongExtra("history_id", 0));
		m_music = DdN.getPlayRecord().getMusicByTitle(m_history.music_title);

		ViewGroup contentRoot = (ViewGroup)findViewById(android.R.id.content);
		new ViewAdapter(this, contentRoot.getChildAt(0)).setData(m_history, m_music);

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
			share();
			break;
		case R.id.item_delete:
			deleteHistory();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void share(){
		final long id = getIntent().getLongExtra("history_id", 0);
		final Uri uri = ContentUris.withAppendedId(HistoryStore.URI_HISTORIES, id);

		Intent intent = new  Intent(Intent.ACTION_SEND);
		intent.setDataAndType(uri, "text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, sharingMessage());
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private String sharingMessage(){
		final int difficulty = m_music != null ? m_music.records[m_history.rank].difficulty : 0;
		StringBuffer sb = new StringBuffer();
		sb.append(m_history.music_title);
		sb.append(" / ");
		sb.append(getResources().getStringArray(R.array.difficulty_names)[m_history.rank]);
		sb.append(" ★").append(difficulty);
		sb.append(" / ");
		sb.append(m_history.score).append(" pts");
		sb.append(" / ");
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

	private static class ViewAdapter {
		private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

		Context m_context;
		View m_root;

		private ViewAdapter(Context context, View view) {
			m_context = context;
			m_root = view;
		}

		private void setData(History h, MusicInfo m){
			final Resources res = m_context.getResources();
			int difficulty = 0;
			Drawable coverArt = null;
			if (m != null) {
				difficulty = m.records[h.rank].difficulty;
				coverArt = m.getCoverArt(m_context.getApplicationContext());
			}

			ImageView image = (ImageView)m_root.findViewById(R.id.cover_art);
			if (image != null)
				image.setImageDrawable(coverArt);

			setText(R.id.detail_title1, res.getString(R.string.hist_title1));
			setText(R.id.play_date, DATE_FORMAT.format(new Date(h.play_date)));
			setText(R.id.play_place, h.play_place);
			setText(R.id.detail_title2, res.getString(R.string.hist_title2));
			setText(R.id.music_title, h.music_title);
			setText(R.id.rank, String.format("%s ★%d", res.getStringArray(R.array.difficulty_names)[h.rank], difficulty));
			setText(R.id.clear_status, res.getStringArray(R.array.clear_status_names)[h.clear_status]);
			setText(R.id.achievement, String.format("%d.%02d%%", h.achievement/100, h.achievement%100));
			setText(R.id.score, String.format("%d pts", h.score));
			if (h.trial == 0)
				m_root.findViewById(R.id.trial_result).setVisibility(View.INVISIBLE);
			else
				setText(R.id.trial_result, DdNUtil.getTrialsName(h.trial) + "クリアトライアル " + DdNUtil.getTrialResultsName(h.trial_result));
			setText(R.id.detail_title3, res.getString(R.string.hist_title3));
			setText(R.id.cool, formatNotes(h.cool, h.cool_rate));
			setText(R.id.fine, formatNotes(h.fine, h.fine_rate));
			setText(R.id.safe, formatNotes(h.safe, h.safe_rate));
			setText(R.id.sad, formatNotes(h.sad, h.sad_rate));
			setText(R.id.worst, formatNotes(h.worst, h.worst_rate));
			setText(R.id.combo, String.valueOf(h.combo));
			setText(R.id.challenge_time, String.format("%d pts", h.challange_time));
			setText(R.id.hold, String.format("%d pts", h.hold));
			setText(R.id.slide, String.format("%d pts", h.slide));
		}

		private void setText(int id, CharSequence text) {
			TextView view = (TextView)m_root.findViewById(id);
			if (view != null)
				view.setText(text);
		}

		private String formatNotes(int notes, int rate) {
			return String.format("%3s/%3s.%02d%%", notes, rate/100, rate%100);
		}
	}

}
