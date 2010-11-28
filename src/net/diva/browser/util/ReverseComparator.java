package net.diva.browser.util;

import java.util.Comparator;


public class ReverseComparator<T> implements Comparator<T> {
	private Comparator<T> m_base;

	public ReverseComparator(Comparator<T> base) {
		m_base = base;
	}

	public int compare(T lhs, T rhs) {
		return -m_base.compare(lhs, rhs);
	}
}
