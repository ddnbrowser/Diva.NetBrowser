/**
 *
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.DecorTitle;
import net.diva.browser.model.TitleInfo;

public class TitleParser {
	private static final Pattern RE_TITLE_NAME = Pattern.compile("<a href=\"/divanet/title/confirmMain/(\\w+)/\\d+\">(.+)</a>");
	private static final Pattern RE_TITLE_IMAGE = Pattern.compile("<img src=\"/divanet/img/title/(\\w+)\"");

	public static String parseTitleList(InputStream content, List<TitleInfo> titles) {
		String body = Parser.read(content);
		Matcher m = TitleParser.RE_TITLE_NAME.matcher(body);
		while (m.find())
			titles.add(new TitleInfo(m.group(1), m.group(2)));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static String parseTitlePage(InputStream content) {
		String body = Parser.read(content);
		Matcher m = TitleParser.RE_TITLE_IMAGE.matcher(body);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern RE_DECOR = Pattern.compile("<a href=\"/divanet/title/updateDecor/(?:true|false)/(\\w+)\">(.+)</a>");
	private static final Pattern RE_GROUP = Pattern.compile("<a href=\"(/divanet/title/decorCommodity/\\d+/\\d+)\">(.+)</a>");
	private static final Pattern RE_COMMODITY = Pattern.compile("<a href=\"/divanet/title/decorDetail/(\\w+)/\\d+/\\d+\">(.+)</a>");
	private static final Pattern RE_RESULT = Pattern.compile("\\s*(.+)<br><br>\\s*.*メイン称号を設定しました<br>");

	public static String parseDecorTitles(InputStream content, List<DecorTitle> titles) {
		String body = Parser.read(content);
		Matcher m = RE_DECOR.matcher(body);
		while (m.find())
			titles.add(new DecorTitle(m.group(1), m.group(2), true));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static List<String> parseDecorShop(InputStream content) {
		List<String > urls = new ArrayList<String>();

		String body = Parser.read(content);
		Matcher m = RE_GROUP.matcher(body);
		while (m.find())
			urls.add(m.group(1));

		return urls;
	}

	public static String parseShopGroup(InputStream content, List<DecorTitle> titles) {
		String body = Parser.read(content);
		Matcher m = RE_COMMODITY.matcher(body);
		while (m.find())
			titles.add(new DecorTitle(m.group(1), m.group(2), false));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static String parseSetResult(InputStream content) {
		String body = Parser.read(content);
		Matcher m = RE_RESULT.matcher(body);
		return m.find() ? m.group(1) : null;
	}
}