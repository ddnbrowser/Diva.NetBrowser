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

	private static final Pattern RE_RESULT = Pattern.compile("所持VOCALOID POINTが<br><font color=\"#FFFF00\">(\\d+)</font>になりました<br>");

	public static boolean isSuccess(InputStream content, int[] vp) {
		String body = Parser.read(content);
		if (vp != null && vp.length > 0) {
			Matcher m = RE_RESULT.matcher(body);
			vp[0] = m.find() ? Integer.parseInt(m.group(1)) : -1;
		}
		return body.contains("購入しました");
	}
}