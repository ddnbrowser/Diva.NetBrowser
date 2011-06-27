/**
 * 
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.ButtonSE;

public class SEParser {
	private static final Pattern RE_BUTTONSE = Pattern.compile("<a href=\"/divanet/buttonSE/confirm/\\w+/(\\w+)/\\d+/\\d+\">(.+)</a>");
	private static final Pattern RE_SAMPLE = Pattern.compile("<a href=\"(/divanet/sound/se/\\w+)\">(.+)</a>");

	public static String parse(InputStream content, List<ButtonSE> buttonSEs) {
		String body = Parser.read(content);
		Matcher m = RE_BUTTONSE.matcher(body);
		while (m.find())
			buttonSEs.add(new ButtonSE(m.group(1), m.group(2)));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static String parse(InputStream content, Map<String, String> map) {
		String body = Parser.read(content);
		Matcher m = RE_SAMPLE.matcher(body);
		while (m.find())
			map.put(m.group(2), m.group(1));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}
}