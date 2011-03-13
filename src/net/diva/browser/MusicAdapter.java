/**
 *
 */
package net.diva.browser;

import java.util.Comparator;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.util.ReverseComparator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class MusicAdapter extends ArrayAdapter<MusicInfo> {
	private static final int LIST_ITEM_ID = R.layout.music_item;

	private LayoutInflater m_inflater;
	private String[] m_trial_labels;
	private Drawable[] m_clear_icons;

	private List<MusicInfo> m_musics;
	private boolean m_favorite;
	private int m_difficulty;
	private int m_sortOrder;
	private boolean m_reverseOrder;

	public MusicAdapter(Context context, int difficulty, boolean favoriteOnly) {
		super(context, LIST_ITEM_ID);
		m_inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Resources resources = context.getResources();
		m_trial_labels = resources.getStringArray(R.array.trial_labels);
		m_clear_icons = new Drawable[] {
				resources.getDrawable(R.drawable.clear0),
				resources.getDrawable(R.drawable.clear1),
				resources.getDrawable(R.drawable.clear2),
				resources.getDrawable(R.drawable.clear3),
		};

		m_difficulty = difficulty;
		m_favorite = favoriteOnly;
		m_sortOrder = R.id.item_sort_by_name;
		m_reverseOrder = false;
	}

	public void setData(List<MusicInfo> music) {
		m_musics = music;
		update();
	}

	public boolean isFavorite() {
		return m_favorite;
	}

	public void toggleFavorite() {
		m_favorite = !m_favorite;
		update();
	}

	public void setDifficulty(int difficulty) {
		m_difficulty = difficulty;
		update();
	}

	private void update() {
		setNotifyOnChange(false);
		clear();
		for (MusicInfo music: m_musics) {
			if (m_favorite && !music.favorite)
				continue;
			if (music.records[m_difficulty] != null)
				add(music);
		}
		sortBy(m_sortOrder, m_reverseOrder);
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	private void setText(View view, int id, String text) {
		TextView tv = (TextView)view.findViewById(id);
		tv.setText(text);
	}

	private void setText(View view, int id, String format, Object... args) {
		setText(view, id, String.format(format, args));
	}

	private void setImage(View view, int id, Drawable image) {
		ImageView iv = (ImageView)view.findViewById(id);
		iv.setImageDrawable(image);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView != null ? convertView : m_inflater.inflate(LIST_ITEM_ID, null);
		MusicInfo music = getItem(position);
		if (music != null) {
			setText(view, R.id.music_title, music.title);
			setImage(view, R.id.cover_art, music.getCoverArt(getContext()));
			ScoreRecord score = music.records[m_difficulty];
			setText(view, R.id.difficulty, "★%d", score.difficulty);
			setImage(view, R.id.clear_status, m_clear_icons[score.clear_status]);
			setText(view, R.id.trial_status, m_trial_labels[score.trial_status]);
			setText(view, R.id.ranking, score.isRankIn() ? "%d位" : "", score.ranking);
			setText(view, R.id.high_score, "スコア: %dpts", score.high_score);
			setText(view, R.id.achivement, "達成率: %d.%02d%%", score.achievement/100, score.achievement%100);
		}
		return view;
	}

	public int sortOrder() {
		return m_sortOrder;
	}

	public void sortBy(int order) {
		sortBy(order, order == m_sortOrder && !m_reverseOrder);
	}

	public void sortBy(int order, boolean reverse) {
		Comparator<MusicInfo> cmp = null;
		switch (order) {
		case R.id.item_sort_by_name:
			cmp = byName();
			break;
		case R.id.item_sort_by_difficulty:
			cmp = byDifficulty();
			break;
		case R.id.item_sort_by_score:
			cmp = byScore();
			break;
		case R.id.item_sort_by_achievement:
			cmp = byAchievement();
			break;
		case R.id.item_sort_by_clear_status:
			cmp = byClearStatus();
			break;
		case R.id.item_sort_by_trial_status:
			cmp = byTrialStatus();
			break;
		default:
			return;
		}
		if (reverse)
			cmp = new ReverseComparator<MusicInfo>(cmp);

		sort(cmp);
		m_sortOrder = order;
		m_reverseOrder = reverse;
	}

	private Comparator<MusicInfo> byName() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				return lhs.reading.compareTo(rhs.reading);
			}
		};
	}

	private Comparator<MusicInfo> byDifficulty() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				int res = lhs.records[m_difficulty].difficulty - rhs.records[m_difficulty].difficulty;
				if (res != 0)
					return res;
				return lhs.reading.compareTo(rhs.reading);
			}
		};
	}

	private Comparator<MusicInfo> byScore() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				return lhs.records[m_difficulty].high_score - rhs.records[m_difficulty].high_score;
			}
		};
	}

	private Comparator<MusicInfo> byAchievement() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				return lhs.records[m_difficulty].achievement - rhs.records[m_difficulty].achievement;
			}
		};
	}

	private Comparator<MusicInfo> byClearStatus() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				int res = lhs.records[m_difficulty].clear_status
						- rhs.records[m_difficulty].clear_status;
				if (res != 0)
					return res;
				return lhs.records[m_difficulty].difficulty - rhs.records[m_difficulty].difficulty;
			}
		};
	}

	private Comparator<MusicInfo> byTrialStatus() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				final int lhs_clear = lhs.records[m_difficulty].clear_status;
				final int rhs_clear = rhs.records[m_difficulty].clear_status;
				final int lhs_trial = lhs.records[m_difficulty].trial_status;
				final int rhs_trial = rhs.records[m_difficulty].trial_status;

				int res = (rhs_clear - rhs_trial) - (lhs_clear - lhs_trial);
				if (res != 0)
					return res;
				res = lhs_trial - rhs_trial;
				if (res != 0)
					return res;
				return lhs.records[m_difficulty].difficulty - rhs.records[m_difficulty].difficulty;
			}
		};
	}
}