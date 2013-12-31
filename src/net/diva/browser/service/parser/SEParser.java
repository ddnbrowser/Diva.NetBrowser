/**
 *
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdNIndex;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.MusicInfo;

public class SEParser {
	private static final Pattern RE_BUTTONSE = Pattern.compile("<a href=\"/divanet/buttonSE/confirm/\\d+/\\w+/(\\w+)/\\d+/\\d+\">(.+)</a>");
	private static final Pattern RE_SAMPLE = Pattern.compile("<a href=\"(/divanet/sound/\\w+/\\w+)\".*?>(.+)</a>");

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

	private static final Pattern RE_INDIVIDUAL = Pattern.compile("<a href=\"/divanet/buttonSE/list/(\\d+)/\\w+/\\d+/\\d+\"[^>]*>(.+?)<");

	public static void parseIndividual(InputStream content, MusicInfo music, DdNIndex index) {
		String body = Parser.read(content);
		Matcher m = RE_INDIVIDUAL.matcher(body);

		Arrays.fill(music.sounds, ButtonSE.UNSUPPORTED);
		while (m.find()) {
			int type = Integer.parseInt(m.group(1));
			music.sounds[type] = index.se(type).id(m.group(2));
		}
	}
}