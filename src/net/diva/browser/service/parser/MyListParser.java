/**
 *
 */
package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.MyList;

public class MyListParser {
	private static final Pattern RE_NAME = Pattern.compile("\\[マイリスト名\\]</font><br>\\s+(.+)<br>");
	private static final Pattern RE_MUSIC = Pattern.compile("name=\"cryptoPvIdList\" value=\"(\\w+)\"");

	public static void parseSummary(InputStream content, MyList myList) {
		String body = Parser.read(content);
		Matcher m = RE_NAME.matcher(body);
		if (m.find())
			myList.name = m.group(1);
	}

	public static void parseList(InputStream content, List<String> ids) {
		String body = Parser.read(content);
		Matcher m = RE_MUSIC.matcher(body);
		while (m.find())
			ids.add(m.group(1));
	}

	public static String parseActivateResult(InputStream content)  {
		String body = Parser.read(content);
		if (body.contains("筐体で使用するマイリストとして設定しました"))
			return null;
		if (body.contains("このマイリストは設定済です"))
			return null;

		String error = "登録されている楽曲がありません";
		if (body.contains(error))
			return error;

		return "設定に失敗しました";
	}
}