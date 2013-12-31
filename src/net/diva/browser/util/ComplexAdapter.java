package net.diva.browser.util;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ComplexAdapter extends BaseAdapter {
	public interface Action {
		void run(View view);
	}

	public static abstract class Item {
		private int m_layout;
		private Action m_action;

		public Item(int layout, Action action) {
			m_layout = layout;
			m_action = action;
		}

		public abstract void prepare(View view);

		public abstract void attach(View view);

		public void runAction(View view) {
			if (m_action != null)
				m_action.run(view);
		}
	}

	private LayoutInflater m_inflater;
	private List<Item> m_items;
	private List<Integer> m_types;

	public ComplexAdapter(Context context) {
		m_inflater = LayoutInflater.from(context);
		m_items = new ArrayList<Item>();
		m_types = new ArrayList<Integer>();
	}

	public void add(Item item) {
		m_items.add(item);
		if (!m_types.contains(item.m_layout))
			m_types.add(item.m_layout);
	}

	public void clear() {
		m_items.clear();
	}

	@Override
	public boolean areAllItemsEnabled() {
		for (Item item: m_items) {
			if (item.m_action == null)
				return false;
		}
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItem(position).m_action != null;
	}

	@Override
	public int getViewTypeCount() {
		return m_types.size();
	}

	@Override
	public int getItemViewType(int position) {
		return m_types.indexOf(getItem(position).m_layout);
	}

	@Override
	public int getCount() {
		return m_items.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public Item getItem(int position) {
		return m_items.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Item item = getItem(position);
		if (convertView == null) {
			convertView = m_inflater.inflate(item.m_layout, parent, false);
			item.prepare(convertView);
		}
		item.attach(convertView);
		return convertView;
	}

	public static class TextItem extends Item {
		private int m_itemId;
		private CharSequence m_text;

		public TextItem(int layout, int itemId, CharSequence text) {
			this(layout, itemId, text, null);
		}

		public TextItem(int layout, int itemId, CharSequence text, Action action) {
			super(layout, action);
			m_itemId = itemId;
			m_text = text;
		}

		@Override
		public void prepare(View view) {
			view.setTag(view.findViewById(m_itemId));
		}

		@Override
		public void attach(View view) {
			TextView text = (TextView)view.getTag();
			text.setText(m_text);
		}
	}
}
