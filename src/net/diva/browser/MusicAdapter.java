/**
 *
 */
package net.diva.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.util.ReverseComparator;
import net.diva.browser.util.SortableListView;
import net.diva.browser.util.StringUtils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class MusicAdapter extends BaseAdapter implements Filterable, SortableListView.DragListener {
	private int m_itemLayout = R.layout.music_item;

	private Context m_context;
	private String[] m_trial_labels;
	private Drawable[] m_clear_icons;

	private List<MusicInfo> m_original;
	private List<MusicInfo> m_musics = Collections.emptyList();

	private Filter m_filter;
	private String m_constraintTitle;
	private String m_constraintReading;

	private int m_difficulty;
	private SortOrder m_sortOrder;
	private boolean m_reverseOrder;

	private int m_dragging = -1;

	public MusicAdapter(Context context) {
		super();
		m_context = context;

		Resources resources = context.getResources();
		m_trial_labels = resources.getStringArray(R.array.trial_labels);
		m_clear_icons = new Drawable[] {
				resources.getDrawable(R.drawable.clear0),
				resources.getDrawable(R.drawable.clear1),
				resources.getDrawable(R.drawable.clear2),
				resources.getDrawable(R.drawable.clear3),
				resources.getDrawable(R.drawable.clear4),
		};

		m_difficulty = 0;
		m_sortOrder = SortOrder.by_name;
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

	public int getLayout() {
		return m_itemLayout;
	}

	public boolean setLayout(int resId) {
		boolean changed = resId != m_itemLayout;
		m_itemLayout = resId;
		return changed;
	}

	public void setData(List<MusicInfo> music) {
		m_original = music;
	}

	public int getDifficulty() {
		return m_difficulty;
	}

	public void setDifficulty(int difficulty) {
		m_difficulty = difficulty;
	}

	public void update() {
		m_musics = getFilteredList();
		if (m_musics.isEmpty())
			notifyDataSetInvalidated();
		else
			notifyDataSetChanged();
	}

	private class Holder {
		ImageView cover;
		TextView title;
		TextView difficulty;
		ImageView clear_status;
		TextView trial_status;
		View module;
		View skin;
		View button;
		TextView ranking;
		TextView high_score;
		TextView rival_score;
		TextView diff_rival_score;
		TextView achivement;
		TextView rival_achivement;
		TextView saturation;
		TextView difference;
		TextView diff_rival_achivement;

		Holder(View view) {
			cover = (ImageView)view.findViewById(R.id.cover_art);
			title = (TextView)view.findViewById(R.id.music_title);
			difficulty = (TextView)view.findViewById(R.id.difficulty);
			clear_status = (ImageView)view.findViewById(R.id.clear_status);
			trial_status = (TextView)view.findViewById(R.id.trial_status);
			module = view.findViewById(R.id.module);
			skin = view.findViewById(R.id.skin);
			button = view.findViewById(R.id.button);
			ranking = (TextView)view.findViewById(R.id.ranking);
			high_score = (TextView)view.findViewById(R.id.high_score);
			rival_score = (TextView)view.findViewById(R.id.rival_high_score);
			diff_rival_score = (TextView)view.findViewById(R.id.difference_to_rival_score);
			achivement = (TextView)view.findViewById(R.id.achivement);
			rival_achivement = (TextView)view.findViewById(R.id.rival_achivement);
			diff_rival_achivement = (TextView)view.findViewById(R.id.difference_to_rival_achivement);
			saturation = (TextView)view.findViewById(R.id.saturation);
			difference = (TextView)view.findViewById(R.id.difference_to_saturation);
		}

		void attach(MusicInfo music, ScoreRecord score, ScoreRecord rival_score) {
			cover.setImageDrawable(music.getCoverArt(m_context));
			title.setText(music.title);
			difficulty.setText(String.format("★%d", score.difficulty));
			clear_status.setImageDrawable(m_clear_icons[score.clear_status]);
			trial_status.setText(m_trial_labels[score.trial_status]);
			if (module != null)
				module.setVisibility(music.vocal1 != null ? View.VISIBLE : View.INVISIBLE);
			if (skin != null)
				skin.setVisibility(music.skin != null ? View.VISIBLE : View.INVISIBLE);
			if (button != null)
				button.setVisibility(music.button != null ? View.VISIBLE : View.INVISIBLE);
			ranking.setText(score.isRankIn() ? String.format("%d位", score.ranking) : "");
			high_score.setText(String.format("%d%s", score.high_score, rival_achivement != null ? "p" : "pts"));
			if (achivement != null)
				achivement.setText(String.format("%d.%02d%%", score.achievement/100, score.achievement%100));
			if (saturation != null) {
				boolean visible = score.saturation > 0;
				saturation.setVisibility(visible ? View.VISIBLE : View.GONE);
				if (visible)
					saturation.setText(String.format("/%d.%02d%%", score.saturation/100, score.saturation%100));
			}
			if (difference != null) {
				boolean visible = score.saturation > 0;
				difference.setVisibility(visible ? View.VISIBLE : View.GONE);
				if (visible) {
					int diff = score.saturation - score.achievement;
					boolean minus = diff < 0;
					if (minus)
						diff = Math.abs(diff);
					difference.setText(String.format("(%s%d.%02d%%)", minus ? "-" : "", diff/100, diff%100));
				}
			}

			if(rival_achivement != null){
				if(rival_score == null)
					rival_score = new ScoreRecord();
				this.rival_score.setText("/" + rival_score.high_score + "p");
				int diffScore = score.high_score - rival_score.high_score;
				boolean minus = diffScore < 0;
				if(minus)
					diffScore = Math.abs(diffScore);
				diff_rival_score.setText(String.format("(%s%dp)", minus ? "-" : "", diffScore));

				rival_achivement.setText(String.format("/%d.%02d%%", rival_score.achievement/100, rival_score.achievement%100));
				int diffAchiv = score.achievement - rival_score.achievement;
				minus = diffAchiv < 0;
				if(minus)
					diffAchiv = Math.abs(diffAchiv);
				diff_rival_achivement.setText(String.format("(%s%d.%02d%%)", minus ? "-" : "", diffAchiv/100, diffAchiv%100));
			}
		}
	}

	public View getView(int position, View view, ViewGroup parent) {
		Holder holder;
		if (view != null)
			holder = (Holder)view.getTag();
		else {
			LayoutInflater inflater = LayoutInflater.from(m_context);
			view = inflater.inflate(m_itemLayout, parent, false);
			holder = new Holder(view);
			view.setTag(holder);
		}
		view.setVisibility(position == m_dragging ? View.INVISIBLE : View.VISIBLE);

		MusicInfo music = getItem(position);
		if (music != null)
			holder.attach(music, music.records[m_difficulty], music.rival_records[m_difficulty]);

		return view;
	}

	public SortOrder sortOrder() {
		return m_sortOrder;
	}

	public boolean isReverseOrder() {
		return m_reverseOrder;
	}

	public void setSortOrder(SortOrder order, boolean reverse) {
		m_sortOrder = order;
		m_reverseOrder = reverse;
	}

	public void sortBy(SortOrder order, boolean reverse) {
		setSortOrder(order, reverse);
		Collections.sort(m_musics, comparator(order, reverse));
		notifyDataSetChanged();
	}

	private Comparator<MusicInfo> comparator(SortOrder order, boolean reverse) {
		Comparator<MusicInfo> cmp = null;
		switch (order) {
		default:
		case by_name:
			cmp = byName();
			break;
		case by_difficulty:
			cmp = byDifficulty();
			break;
		case by_score:
			cmp = byScore();
			break;
		case by_achivement:
			cmp = byAchievement();
			break;
		case by_clear_status:
			cmp = byClearStatus();
			break;
		case by_trial_status:
			cmp = byTrialStatus();
			break;
		case by_difference_to_saturation:
			cmp = byDifferenceToSaturation();
			break;
		case by_original:
			cmp = byOriginal();
			break;
		case by_difference_to_rival_score:
			cmp = byDifferenceToRivalScore();
			break;
		case by_difference_to_rival_satu:
			cmp = byDifferenceToRivalSatu();
			break;
		case by_publish_order:
			return byPublishOrder(reverse);
		}
		if (reverse)
			cmp = new ReverseComparator<MusicInfo>(cmp);
		return cmp;
	}

	private Comparator<MusicInfo> byDifferenceToRivalScore() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				ScoreRecord lScore = lhs.records[m_difficulty];
				ScoreRecord lRivalScore = lhs.rival_records[m_difficulty];
				ScoreRecord rScore = rhs.records[m_difficulty];
				ScoreRecord rRivalScore = rhs.rival_records[m_difficulty];
				if(lRivalScore == null || rRivalScore == null)
					return 0;
				int result = (lScore.high_score-lRivalScore.high_score) - (rScore.high_score-rRivalScore.high_score);
				if (result != 0)
					return result;

				return lhs.reading.compareTo(rhs.reading);
			}
		};
	}

	private Comparator<MusicInfo> byDifferenceToRivalSatu() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				ScoreRecord lScore = lhs.records[m_difficulty];
				ScoreRecord lRivalScore = lhs.rival_records[m_difficulty];
				ScoreRecord rScore = rhs.records[m_difficulty];
				ScoreRecord rRivalScore = rhs.rival_records[m_difficulty];
				if(lRivalScore == null || rRivalScore == null)
					return 0;
				int result = (lScore.achievement-lRivalScore.achievement) - (rScore.achievement-rRivalScore.achievement);
				if (result != 0)
					return result;

				return lhs.reading.compareTo(rhs.reading);
			}
		};
	}

	private Comparator<MusicInfo> byName() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				int result = lhs.ordinal.compareTo(rhs.ordinal);
				if (result != 0)
					return result;
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

	private Comparator<MusicInfo> byDifferenceToSaturation() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				ScoreRecord lScore = lhs.records[m_difficulty];
				ScoreRecord rScore = rhs.records[m_difficulty];
				int result = (lScore.saturation-lScore.achievement) - (rScore.saturation-rScore.achievement);
				if (result != 0)
					return result;

				return lhs.reading.compareTo(rhs.reading);
			}
		};
	}

	private Comparator<MusicInfo> byOriginal() {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				return m_original.indexOf(lhs) - m_original.indexOf(rhs);
			}
		};
	}

	private Comparator<MusicInfo> byPublishOrder(final boolean reverse) {
		return new Comparator<MusicInfo>() {
			public int compare(MusicInfo lhs, MusicInfo rhs) {
				int result = lhs.publish_order - rhs.publish_order;
				if (result != 0)
					return reverse ? -result : result;
				result = lhs.ordinal.compareTo(rhs.ordinal);
				if (result != 0)
					return result;
				return lhs.reading.compareTo(rhs.reading);
			}
		};
	}

	public void selectSortOrder() {
		final List<String> values = Arrays.asList(m_context.getResources().getStringArray(R.array.sort_order_values));
		final int checked = values.indexOf(sortOrder().name());

		View custom = LayoutInflater.from(m_context).inflate(R.layout.descending_order, null);
		final CheckBox descending = (CheckBox)custom.findViewById(R.id.descending);
		descending.setChecked(isReverseOrder());

		AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
		builder.setTitle(R.string.item_sort);
		builder.setSingleChoiceItems(R.array.sort_order_names, checked,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				sortBy(SortOrder.valueOf(values.get(which)), descending.isChecked());
				dialog.dismiss();
			}
		});
		builder.setView(custom);
		builder.show();
	}

	public Filter getFilter() {
		if (m_filter == null)
			m_filter = new MusicFilter();
		return m_filter;
	}

	private List<MusicInfo> getFilteredList() {
		List<MusicInfo> musics;
		List<MusicInfo> original = m_original;
		if (original == null || original.isEmpty()) {
			musics = Collections.emptyList();
		}
		else {
			musics = new ArrayList<MusicInfo>(original.size());
			for (MusicInfo m: original) {
				if (m.records[m_difficulty] == null)
					continue;
				if (m_constraintTitle == null ||
						m.reading.toLowerCase().contains(m_constraintReading) ||
						m.title.toLowerCase().contains(m_constraintTitle))
					musics.add(m);
			}

			if (!musics.isEmpty())
				Collections.sort(musics, comparator(m_sortOrder, m_reverseOrder));
		}
		return musics;
	}

	private class MusicFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			if (TextUtils.isEmpty(constraint))
				m_constraintTitle = null;
			else {
				m_constraintTitle = constraint.toString().toLowerCase();
				m_constraintReading = StringUtils.toKatakana(m_constraintTitle);
			}

			List<MusicInfo> musics = getFilteredList();

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

	public int onStartDrag(int position) {
		m_dragging = position;
		notifyDataSetChanged();
		return position;
	}

	public int onDuringDrag(int positionFrom, int positionTo) {
		if (positionFrom < 0 || positionTo < 0
				|| positionFrom == positionTo) {
			return positionFrom;
		}
       	MusicInfo m = m_musics.remove(positionFrom);
       	m_musics.add(positionTo, m);
       	m_dragging = positionTo;
       	notifyDataSetChanged();
		return positionTo;
	}

	public boolean onStopDrag(int positionFrom, int positionTo) {
		m_dragging = -1;
		notifyDataSetChanged();
		return true;
	}
}