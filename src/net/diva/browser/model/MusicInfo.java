package net.diva.browser.model;

import java.io.File;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;


public class MusicInfo {
	private static final int[] STATUS_POINTS = new int[] { 0, 1, 4, 6 };
	private static final int[] DIFFICULTY_POINTS = new int[] { 1, 2, 5, 7 };

	public String id;
	public String title;
	public String reading;
	public String coverart;

	public int part;
	public String vocal1;
	public String vocal2;

	public ScoreRecord[] records;

	public boolean favorite;

	public MusicInfo(String id_, String title_) {
		id = id_;
		title = title_;
		records = new ScoreRecord[4];
	}

	public int rankPoint() {
		int max = 0;
		for (int difficulty = 0; difficulty < records.length; ++difficulty) {
			ScoreRecord score = records[difficulty];
			if (score == null)
				continue;
			int point = DIFFICULTY_POINTS[difficulty] * STATUS_POINTS[score.clear_status];
			if (point > max)
				max = point;
		}
		return max;
	}

	public File getCoverArtPath(Context context) {
		return context.getFileStreamPath(new File(coverart).getName());
	}

	public Drawable getCoverArt(Context context) {
		return new BitmapDrawable(getCoverArtPath(context).getAbsolutePath());
	}

	@Override
	public boolean equals(Object o) {
		return o != null && (o instanceof MusicInfo) && id.equals(((MusicInfo)o).id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
