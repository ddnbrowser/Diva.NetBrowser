package net.diva.browser.model;

import java.util.List;

import net.diva.browser.DdN;

public class PlayRecord {
	public String player_name;
	public String level;
	public String title;
	public int vocaloid_point;
	public int ticket;
	public List<MusicInfo> musics;

	public MusicInfo getMusic(String id) {
		for (MusicInfo music: musics) {
			if (music.id.equals(id))
				return music;
		}
		return null;
	}

	public MusicInfo getMusicByTitle(String title) {
		for (MusicInfo music: musics) {
			if (music.title.equals(title))
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

	private int maxRankPoint() {
		return MusicInfo.maxRankPoint() * musics.size();
	}

	public int rank(int[] nextOut) {
		final int[] rank_points = DdN.RANK_POINTS;
		final int limit = maxRankPoint();

		final int point = rankPoint();
		int rank = 0;
		int next = rank_points[0];
		while (point >= next) {
			++rank;
			next += rank < rank_points.length ? rank_points[rank] : 150;
		}
		if (next > limit && point == limit)
			++rank;

		if (nextOut != null && nextOut.length > 0)
			nextOut[0] = Math.min(next, limit) - point;

		return rank;
	}

	public long experience() {
		long points = 0;
		for (MusicInfo m: musics)
			points += m.experience();
		return points;
	}

	public int nextPublishOrder() {
		int order = -1;
		for (MusicInfo m: musics)
			order = Math.max(order, m.publish_order);
		return order + 1;
	}
}
