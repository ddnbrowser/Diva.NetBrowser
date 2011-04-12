package net.diva.browser.model;

import java.util.List;

import net.diva.browser.DdN;

public class PlayRecord {
	public String player_name;
	public String level;
	public String title_id;
	public List<MusicInfo> musics;

	public MusicInfo getMusic(String id) {
		for (MusicInfo music: musics) {
			if (music.id.equals(id))
				return music;
		}
		return null;
	}

	public int rankPoint() {
		int point = 0;
		for (MusicInfo m: musics)
			point += m.rankPoint();
		return point;
	}

	public int rank(int[] next) {
		final int[] rank_points = DdN.RANK_POINTS;

		int point = rankPoint();
		int rank = 0;
		while (rank < rank_points.length && point >= rank_points[rank])
			point -= rank_points[rank++];
		rank += point/150;
		point %= 150;

		if (next != null && next.length > 0)
			next[0] = (rank < rank_points.length ? rank_points[rank] : 150) - point;

		return rank;
	}

	public long experience() {
		long points = 0;
		for (MusicInfo m: musics)
			points += m.experience();
		return points;
	}
}
