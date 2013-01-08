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
	private static final Pattern RE_MAX = Pattern.compile("\\[楽曲リスト\\]</font>\\s*\\((\\d+)/(\\d+)\\)");

	public static void parseSummary(InputStream content, MyList myList) {
		String body = Parser.read(content);
		Matcher m = RE_NAME.matcher(body);
		if (m.find())
			myList.name = m.group(1);

		m = RE_MAX.matcher(body);
		if(m.find())
			myList.max = Integer.valueOf(m.group(2));
	}

	public static void parseList(InputStream content, List<String> ids) {
		String body = Parser.read(content);
		Matcher m = RE_MUSIC.matcher(body);
		while (m.find())
			ids.add(m.group(1));
	}

	private static final Pattern MYLIST_REGST_OK = Pattern.compile("筐体で使用するマイリスト(.+)として設定しました");
	public static String parseActivateResult(InputStream content)  {
		String body = Parser.read(content);
		if (MYLIST_REGST_OK.matcher(body).find())
			return null;
		if (body.contains("このマイリストは設定済です"))
			return null;

		String error = "登録されている楽曲がありません";
		if (body.contains(error))
			return error;

		return "設定に失敗しました";
	}
}