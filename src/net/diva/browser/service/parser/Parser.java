package net.diva.browser.service.parser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import net.diva.browser.model.PlayRecord;
import net.diva.browser.service.IndividualSetting;
import net.diva.browser.service.ParseException;

public final class Parser {
	static final Pattern RE_NEXT = Pattern.compile("<a href=\"([/\\w]+)\".*>次へ.*</a>");

	Parser() {}

	static String read(InputStream in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			for (int read; (read = in.read(buffer)) != -1;)
				out.write(buffer, 0, read);
			return out.toString("UTF-8");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final Pattern RE_PLAYER = Pattern.compile("\\[プレイヤー名\\].*<br>\\s*(.+)<br>");
	private static final Pattern RE_LEVEL = Pattern.compile("\\[LEVEL/称号\\].*<br>\\s*(.+)\\s*(.+)<br>");
	private static final Pattern RE_VP = Pattern.compile("\\[VOCALOID POINT\\].*<br>\\s*(\\d+)VP<br>");
	private static final Pattern RE_NEWS = Pattern.compile("DIVA.NETニュース\\((.+)\\)</a><br>");

	public static class Result {
		public PlayRecord record;
		public String newsTimestamp;
	}

	public static Result parseMenuPage(InputStream content) throws ParseException {
		Result result = new Result();
		PlayRecord record = result.record = new PlayRecord();
		String body = read(content);
		Matcher m = RE_PLAYER.matcher(body);
		if (!m.find())
			throw new ParseException();
		record.player_name = m.group(1);
		m = m.usePattern(RE_LEVEL);
		if (m.find()) {
			record.level = m.group(1);
			record.title = m.group(2);
		}
		m = m.usePattern(RE_VP);
		if (m.find())
			record.vocaloid_point = Integer.valueOf(m.group(1));
		m = m.usePattern(RE_NEWS);
		if (m.find())
			result.newsTimestamp = m.group(1);
		return result;
	}

	private static final Pattern RE_RENAME_RESULT = Pattern.compile("<font.*>(.*)</font><br>\\s*<br>\\s*再度入力してください");
	private static final Pattern RE_PAIR = Pattern.compile("<input .*name=\"(.*)\" value=\"(.*)\">");
	public static String parseRenameResult(InputStream content, List<NameValuePair> params) {
		String body = read(content);
		Matcher m = RE_RENAME_RESULT.matcher(body);
		if (m.find())
			return m.group(1);

		if (params != null) {
			m = m.usePattern(RE_PAIR);
			while (m.find()) {
				String name = m.group(1);
				String value = m.group(2);
				params.add(new BasicNameValuePair(name, value));
			}
		}
		return null;
	}

	private static final Pattern RE_SETTING_MODULE = Pattern.compile("/divanet/module/selectPv/(\\w+)/\\d+\">(.*)</a>");
	private static final Pattern RE_SETTING_SKIN = Pattern.compile("/divanet/skin/list/(\\w+)/\\d+/\\d+\">(.*)</a>");
	private static final Pattern RE_SETTING_BUTTON = Pattern.compile("/divanet/buttonSE/list/(\\w+)/\\d+/\\d+\">(.*)</a>");

	public static String parseIndividualSettings(InputStream content, Map<String, IndividualSetting> settings) {
		String body = read(content);
		Matcher m = RE_SETTING_MODULE.matcher(body);
		while (m.find()) {
			String id = m.group(1);
			IndividualSetting setting = settings.get(id);
			if (setting == null)
				settings.put(id, setting = new IndividualSetting());
			if (setting.vocal1 == null)
				setting.vocal1 = m.group(2);
			else
				setting.vocal2 = m.group(2);
		}

		m = m.usePattern(RE_SETTING_SKIN);
		while (m.find())
			settings.get(m.group(1)).skin = m.group(2);

		m = m.usePattern(RE_SETTING_BUTTON);
		while (m.find())
			settings.get(m.group(1)).button = m.group(2);

		m = m.usePattern(RE_NEXT);
		return m.find() ? m.group(1) : null;
	}
}
