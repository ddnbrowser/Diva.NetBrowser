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
	public Role role1;
	public Role role2;
	public Role role3;
	public String skin;
	public String[] sounds;

	public ScoreRecord[] records;

	public static int maxRankPoint() {
		return (int) Math.ceil(STATUS_POINTS[STATUS_POINTS.length-1] * DIFFICULTY_POINTS[DIFFICULTY_POINTS.length-1]);
	}

	public MusicInfo(String id_, String title_) {
		id = id_;
		title = title_;
		publish_order = -1;
		sounds = new String[ButtonSE.COUNT];
		records = new ScoreRecord[4];
	}

	public boolean hasIndividualModule() {
		return role1 != null && role1.module != null;
	}

	public boolean hasIndividualSkin() {
		return skin != null;
	}

	public boolean hasIndividualSe() {
		for (String se: sounds) {
			if (se != null && !se.equals(ButtonSE.UNSUPPORTED))
				return true;
		}
		return false;
	}

	public void resetIndividualSe() {
		for (int i = 0; i < sounds.length; ++i) {
			String se = sounds[i];
			if (se != null && !se.equals(ButtonSE.UNSUPPORTED))
				sounds[i] = null;
		}
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
