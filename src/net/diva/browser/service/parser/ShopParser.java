/**
 * 
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class ShopParser {
	private static final Pattern RE_IMAGE = Pattern.compile("<img src=\"(/divanet/[^\"]+)\"");
	private static final Pattern RE_ITEM = Pattern.compile("(\\[.+\\])</font><br>\\s*(.+)<");

	public static String parse(InputStream content, List<NameValuePair> details) {
		String body = Parser.read(content);
		Matcher m = RE_ITEM.matcher(body);
		while (m.find())
			details.add(new BasicNameValuePair(m.group(1), m.group(2)));

		m = m.usePattern(RE_IMAGE);
		return m.find() ? m.group(1) : null;
	}

	public static boolean isSuccess(InputStream content) {
		String body = Parser.read(content);
		return body.contains("購入しました");
	}
}