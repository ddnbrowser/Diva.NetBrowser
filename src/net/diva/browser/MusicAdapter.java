/**
 *
 */
package net.diva.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.util.ReverseComparator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

class MusicAdapter extends BaseAdapter implements Filterable {
	private static final int LIST_ITEM_ID = R.layout.music_item;

	private Context m_context;
	private String[] m_trial_labels;
	private Drawable[] m_clear_icons;

	private List<MusicInfo> m_original;
	private List<MusicInfo> m_musics = Collections.emptyList();

	private Filter m_filter;
	private String m_constraint;
	private boolean m_favorite;
	private int m_difficulty;
	private int m_sortOrder;
	private boolean m_reverseOrder;

	public MusicAdapter(Context context, boolean favoriteOnly) {
		super();
		m_context = context;

		Resources resources = context.getResources();
		m_trial_labels = resources.getStringArray(R.array.trial_labels);
		m_clear_icons = new Drawable[] {
				resources.getDrawable(R.drawable.clear0),
				resources.getDrawable(R.drawable.clear1),
				resources.getDrawable(R.drawable.clear2),
				resources.getDrawable(R.drawable.clear3),
		};

		m_difficulty = 0;
		m_favorite = favoriteOnly;
		m_sortOrder = R.id.item_sort_by_name;
		m_reverseOrder = false;
	}

	public int getCount() {
		return m_musics.size();
	}

	public MusicInfo getItem(int position) {
		return m_musics.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public void setData(List<MusicInfo> music) {
		m_original = music;
		update();
	}

	public boolean isFavorite() {
		return m_favorite;
	}

	public void setDifficulty(int difficulty, boolean update) {
		m_difficulty = difficulty;
		if (update)
			update();
	}

	private void update() {
		getFilter().filter(m_constraint);
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

	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflater = LayoutInflater.from(m_context);
			view = inflater.inflate(LIST_ITEM_ID, parent, false);
		}

		MusicInfo music = getItem(position);
		if (music != null) {
			setText(view, R.id.music_title, music.title);
			setImage(view, R.id.cover_art, music.getCoverArt(m_context));
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
		Collections.sort(m_musics, comparator(order, reverse));
		m_sortOrder = order;
		m_reverseOrder = reverse;
		notifyDataSetChanged();
	}

	private Comparator<MusicInfo> comparator(int order, boolean reverse) {
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
			assert(false);
		}
		if (reverse)
			cmp = new ReverseComparator<MusicInfo>(cmp);
		return cmp;
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

	public Filter getFilter() {
		if (m_filter == null)
			m_filter = new MusicFilter();
		return m_filter;
	}

	private class MusicFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			m_constraint = TextUtils.isEmpty(constraint) ? null : constraint.toString().toLowerCase();

			List<MusicInfo> musics;
			List<MusicInfo> original = m_original;
			if (original == null || original.isEmpty()) {
				musics = Collections.emptyList();
			}
			else {
				musics = new ArrayList<MusicInfo>(original.size());
				for (MusicInfo m: original) {
					if (m_favorite && !m.favorite)
						continue;
					if (m.records[m_difficulty] == null)
						continue;
					if (m_constraint == null ||
							m.reading.toLowerCase().indexOf(m_constraint) >= 0 ||
							m.title.toLowerCase().indexOf(m_constraint) >= 0)
						musics.add(m);
				}

				if (!musics.isEmpty())
					Collections.sort(musics, comparator(m_sortOrder, m_reverseOrder));
			}

			FilterResults results = new FilterResults();
			results.values = musics;
			results.count = musics.size();
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			m_musics = (List<MusicInfo>)results.values;
			if (results.count > 0)
				notifyDataSetChanged();
			else
				notifyDataSetInvalidated();
		}
	}
}