package net.diva.browser.model;

import java.io.File;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;


public class MusicInfo {
	private static final double[] STATUS_POINTS = new double[] { 0, 1, 4, 4.5, 6 };
	private static final int[] DIFFICULTY_POINTS = new int[] { 1, 2, 5, 7 };

	public String id;
	public String title;
	public String reading;
	public String ordinal;
	public String coverart;
	public int publish_order;

	public int part;
	public int voice1;
	public int voice2;

	public String vocal1;
	public String vocal2;
	public String skin;
	public String button;

	private Drawable image;

	public ScoreRecord[] records;

	public static int maxRankPoint() {
		return (int) STATUS_POINTS[STATUS_POINTS.length-1] * DIFFICULTY_POINTS[DIFFICULTY_POINTS.length-1];
	}

	public MusicInfo(String id_, String title_) {
		id = id_;
		title = title_;
		publish_order = -1;
		voice1 = -1;
		voice2 = -1;
		records = new ScoreRecord[4];
	}

	public int rankPoint() {
		int max = 0;
		for (int difficulty = 0; difficulty < records.length; ++difficulty) {
			ScoreRecord score = records[difficulty];
			if (score == null)
				continue;
			int point = (int) Math.ceil(DIFFICULTY_POINTS[difficulty] * STATUS_POINTS[score.clear_status]);
			if (point > max)
				max = point;
		}
		return max;
	}

	public long experience() {
		long points = 0;
		for (int difficulty = 0; difficulty < records.length; ++difficulty) {
			ScoreRecord score = records[difficulty];
			if (score != null)
				points += score.achievement;
		}
		return points;
	}

	public File getCoverArtPath(Context context) {
		return context.getFileStreamPath(new File(coverart).getName());
	}

	public Drawable getCoverArt(Context context) {
		if(image == null)
			image = new BitmapDrawable(getCoverArtPath(context).getAbsolutePath());
		return image;
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
