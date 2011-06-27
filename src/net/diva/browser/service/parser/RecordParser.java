/**
 * 
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecordParser {
	private static Pattern RE_RECORD = Pattern.compile("<hr>(\\s*(.+<br>)\\s*(.+<br>))?\\s*(.+)<br>\\s*(?=<hr>)");

	public static String parseList(InputStream content, List<String> records) {
		String body = Parser.read(content);
		Matcher m = RE_RECORD.matcher(body);
		while (m.find()) {
			StringBuilder b = new StringBuilder();
			if (m.group(1) != null) {
				b.append(m.group(2));
				b.append(m.group(3));
			}
			b.append(m.group(4));
			records.add(b.toString());
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static boolean parseResult(InputStream content) {
		String body = Parser.read(content);
		Matcher m = RE_RECORD.matcher(body);
		return m.find();
	}
}