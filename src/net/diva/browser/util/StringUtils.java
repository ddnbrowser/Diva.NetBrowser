package net.diva.browser.util;

public final class StringUtils {
	private static final String NORMALIZE_MAP =
		"アアイイウウエエオオカカキキククケケココササシシススセセソソ"
		+ "タタチチツツツテテトトナニヌネノハハハヒヒヒフフフヘヘヘホホホ"
		+ "マミムメモヤヤユユヨヨラリルレロワワヰヱヲンウカケワヰヱヲ";

	private static final String VOWEL_MAP =
		"アアイイウウエエオオアアイイウウエエオオアアイイウウエエオオ"
		+ "アアイイウウウエエオオアイウエオアアアイイイウウウエエエオオオ"
		+ "アイウエオアアウウオオアイウエオアアイエオンウアエアイエオ";

	private StringUtils() {
	}

	public static String forLexicographical(CharSequence sequence) {
		StringBuilder builder = new StringBuilder(sequence);
		final int length = builder.length();
		for (int i = 0; i < length; ++i) {
			char c = builder.charAt(i);
			if ('ァ' <= c && c < 'ヶ')
				builder.setCharAt(i, NORMALIZE_MAP.charAt(c - 'ァ'));
			else if (i > 0 && c == 'ー')
				builder.setCharAt(i, VOWEL_MAP.charAt(i-1));
		}
		return builder.toString();
	}

	public static String toKatakana(CharSequence sequence) {
		StringBuffer builder = new StringBuffer(sequence);
		final int length = builder.length();
		for (int i = 0; i < length; i++) {
			char c = builder.charAt(i);
			if ('ぁ' <= c && c <= 'ん')
				builder.setCharAt(i, (char)(c - 'ぁ' + 'ァ'));
		}
		return builder.toString();
	}
}
