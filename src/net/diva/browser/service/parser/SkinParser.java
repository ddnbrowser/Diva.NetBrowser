/**
 * 
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.SkinInfo;

public class SkinParser {
	private static final Pattern RE_GROUP = Pattern.compile("<a href=\"/divanet/skin/(?:select/COMMON|commodity)/(\\w+)/\\d+(?:/\\d+)?\">(.+)</a>");
	private static final Pattern RE_SKIN = Pattern.compile("<a href=\"/divanet/skin/(?:confirm/COMMON|detail)/(\\w+)/(\\w+)/\\d+(?:/\\d+)?\">(.+)</a>");
	private static final Pattern RE_IMAGE = Pattern.compile("<img src=\"(/divanet/img/skin/\\w+)\"");

	public static String parse(InputStream content, List<String> groups) {
		String body = Parser.read(content);
		Matcher m = RE_GROUP.matcher(body);
		while (m.find())
			groups.add(m.group(1));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static void parse(InputStream content, List<SkinInfo> skins, boolean purchased) {
		String body = Parser.read(content);
		Matcher m = RE_SKIN.matcher(body);
		while (m.find())
			skins.add(new SkinInfo(m.group(2), m.group(1), m.group(3), purchased));
	}

	public static void parse(InputStream content, SkinInfo skin) {
		String body = Parser.read(content);
		Matcher m = RE_IMAGE.matcher(body);
		if (m.find())
			skin.image_path = m.group(1);
	}
}