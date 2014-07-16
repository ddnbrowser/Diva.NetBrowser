package net.diva.browser.util;

import java.util.NoSuchElementException;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseArray;

public class CodeMap {
	private SparseArray<String> m_names;

	public CodeMap(Context context, int nameId) {
		final Resources res = context.getResources();
		String[] names = res.getStringArray(nameId);

		m_names = new SparseArray<String>(names.length);
		for (int i = 0; i < names.length; ++i)
			m_names.append(i, names[i]);
	}

	public CodeMap(Context context, int codeId, int nameId) {
		final Resources res = context.getResources();
		int[] ids = res.getIntArray(codeId);
		String[] names = res.getStringArray(nameId);

		m_names = new SparseArray<String>(ids.length);
		for (int i = 0; i < ids.length; ++i)
			m_names.append(ids[i], names[i]);
	}

	public int count() {
		return m_names.size();
	}

	public int code(String name) {
		for (int i = 0; i < m_names.size(); ++i) {
			if (m_names.valueAt(i).equals(name))
				return m_names.keyAt(i);
		}

		throw new NoSuchElementException();
	}

	public String name(int code) {
		return m_names.get(code);
	}
}
