package net.diva.browser.service.parser;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.model.History;
import net.diva.browser.service.ParseException;
import net.diva.browser.util.MatchHelper;
/**
 *
 * @author silvia
 *
 */
public class HistoryParser {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy/MM/dd HH:mm");
	private final static Pattern RE_HISTORY = Pattern.compile("<font color=\"#00FFFF\">\\[(.+)\\]</font>\\s*<br>\\s*<a href=\"/divanet/pv/info/(\\w+)/");
	private final static Pattern RE_DETAIL = Pattern.compile("<a href=\"/divanet/personal/playHistoryDetail/(.+?)/");

	public static String parsePlayHistory(InputStream content, List<String> newHistorys, long[] params)
			throws ParseException {
		String body = Parser.read(content);
		Matcher m = RE_HISTORY.matcher(body);

		int end = -1;
		try {
			while (m.find()) {
				long playTime = DATE_FORMAT.parse(m.group(1)).getTime();
				if (playTime <= params[0]) {
					end = m.start();
					break;
				}
				if (playTime > params[1])
					params[1] = playTime;
			}

			m = m.usePattern(RE_DETAIL);
			if (end >= 0)
				m.region(0, end);
			while (m.find())
				newHistorys.add(m.group(1));

			if (end >= 0)
				return null;
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern HIST_DATE = Pattern.compile("\\[日時\\] </font>(.+?)<br>");
	private static final Pattern HIST_PLACE = Pattern.compile("\\[場所\\] </font>(.+?)<br>");
	private static final Pattern HIST_MUSIC = Pattern.compile("<a href=\"/divanet/pv/info/(\\w+)/0/0\"[^>]*>(.+)</a>");
	private static final Pattern HIST_RANK = Pattern.compile("\\s*(.+?)　★");
	private static final Pattern HIST_CLEAR_STATUS = Pattern.compile("\\[CLEAR RANK\\]</font><br>(.+?)\\s*?<br>");
	private static final Pattern HIST_ACHIEVEMENT = Pattern.compile("\\[達成率\\]</font><br>(\\d+)\\.(\\d)(\\d)?％<br>");
	private static final Pattern HIST_SCORE = Pattern.compile("\\[SCORE\\]</font><br>(.+?)<br>");
	private static final Pattern HIST_COOL = Pattern.compile("COOL：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(\\d+)\\.(\\d)(\\d)?%</TD>");
	private static final Pattern HIST_FINE = Pattern.compile("FINE：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(\\d+)\\.(\\d)(\\d)?%</TD>");
	private static final Pattern HIST_SAFE = Pattern.compile("SAFE：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(\\d+)\\.(\\d)(\\d)?%</TD>");
	private static final Pattern HIST_SAD = Pattern.compile("SAD：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(\\d+)\\.(\\d)(\\d)?%</TD>");
	private static final Pattern HIST_WORST = Pattern.compile("WORST/WRONG：</TD>\\s*</TR>\\s*<TR>\\s*<TD align=\"left\"></TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(\\d+)\\.(\\d)(\\d)?%</TD>");
	private static final Pattern HIST_COMBO = Pattern.compile("COMBO：</TD>\\s*<TD align=\"right\">(.+?)</TD>");
	private static final Pattern HIST_CHALLENGE_TIME = Pattern.compile("CHALLENGE TIME：</TD>\\s*</TR>\\s*<TR>\\s*<TD align=\"right\" colspan=\"4\">(.+?)</TD>");
	private static final Pattern HIST_HOLD = Pattern.compile("同時押し/ホールド：</TD>\\s*</TR>\\s*<TR>\\s*<TD align=\"right\" colspan=\"4\">(.+?)</TD>");
	private static final Pattern HIST_SLIDE = Pattern.compile("スライド：</TD>\\s*</TR>\\s*<TR>\\s*<TD align=\"right\" colspan=\"4\">(.+?)</TD>");
	private static final Pattern HIST_TRIAL = Pattern.compile("\\[クリアトライアル\\]</font><br>(.*?)クリアトライアル\\s*(.+?)<br>");
	private static final Pattern HIST_FORK = Pattern.compile("<div>(.+)</div>");
	private static final Pattern HIST_MODULE = Pattern.compile("(.+?)：(.+?)<br>");
	private static final Pattern RE_SINGLE_ITEM = Pattern.compile("\\s*(.+?)<br>");
	private static final Pattern RE_DIV_BEG = Pattern.compile("<div[^>]*>");
	private static final Pattern RE_DIV_END = Pattern.compile("</div>");

	public static History parseHistoryDetail(InputStream content) throws ParseException {
		History history = new History();
		try{
			String body = Parser.read(content);
			MatchHelper m = new MatchHelper(body);
			history.play_date = DATE_FORMAT.parse(m.findString(HIST_DATE, 1)).getTime();
			history.play_place = m.findString(HIST_PLACE, 1);
			history.music_title = m.findString(HIST_MUSIC, 2);
			history.rank = DdN.difficulty().code((m.findString(HIST_RANK, 1)));
			history.clear_status = DdN.clearStatus().code((m.findString(HIST_CLEAR_STATUS, 1)));
			history.achievement = m.find(HIST_ACHIEVEMENT) ? getFixedPointValue(m, 1) : 0;
			history.score = m.findInteger(HIST_SCORE, 1);
			if (!m.find(HIST_COOL))
				throw new ParseException();
			else {
				history.cool = Integer.parseInt(m.group(1));
				history.cool_rate = getFixedPointValue(m, 2);
			}
			if (!m.find(HIST_FINE))
				throw new ParseException();
			else {
				history.fine = Integer.parseInt(m.group(1));
				history.fine_rate = getFixedPointValue(m, 2);
			}
			if (!m.find(HIST_SAFE))
				throw new ParseException();
			else {
				history.safe = Integer.parseInt(m.group(1));
				history.safe_rate = getFixedPointValue(m, 2);
			}
			if (!m.find(HIST_SAD))
				throw new ParseException();
			else {
				history.sad = Integer.parseInt(m.group(1));
				history.sad_rate = getFixedPointValue(m, 2);
			}
			if (!m.find(HIST_WORST))
				throw new ParseException();
			else {
				history.worst = Integer.parseInt(m.group(1));
				history.worst_rate = getFixedPointValue(m, 2);
			}
			history.combo = m.findInteger(HIST_COMBO, 1);
			history.challange_time = m.findInteger(HIST_CHALLENGE_TIME, 1);
			history.hold = m.findInteger(HIST_HOLD, 1);
			history.slide = m.findInteger(HIST_SLIDE, 1, 0);
			if (m.find(HIST_TRIAL)) {
				history.trial = DdN.trial().code(m.group(1));
				history.trial_result = DdN.successOrFail().code(m.group(2));
			}
			if (bindSection(m, "PV分岐")) {
				if (m.find(HIST_FORK))
					history.pv_fork = DdN.successOrFail().code(m.group(1));
				m.unbind();
			}
			if (bindSection(m, "モジュール")) {
				history.module1 = parseModule(m);
				history.module2 = parseModule(m);
				history.module3 = parseModule(m);
				m.unbind();
			}
			history.se_button = findStringInSection(m, "ボタン音", RE_SINGLE_ITEM, 1);
			history.se_slide = findStringInSection(m, "スライド音", RE_SINGLE_ITEM, 1);
			history.se_chain = findStringInSection(m, "チェーンスライド音", RE_SINGLE_ITEM, 1);
			history.se_touch = findStringInSection(m, "スライダータッチ音", RE_SINGLE_ITEM, 1);
			history.skin = findStringInSection(m, "スキン", RE_SINGLE_ITEM, 1);
		}
		catch (ParseException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		return history;
	}

	private static History.Module parseModule(MatchHelper m) {
		History.Module module = new History.Module();
		module.base = m.findString(HIST_MODULE, 2, null);
		if (module.base != null && m.bind(RE_DIV_BEG, RE_DIV_END)) {
			module.head = findCustomizeItem(m, "頭");
			module.face = findCustomizeItem(m, "顔");
			module.front = findCustomizeItem(m, "胸元");
			module.back = findCustomizeItem(m, "背中");
			m.unbind();
		}
		return module;
	}

	private static String findStringInSection(MatchHelper m, String section, Pattern regexp, int group) {
		if (!bindSection(m, section))
			return null;
		try {
			return m.findString(regexp, group, null);
		}
		finally {
			m.unbind();
		}
	}

	private static String findCustomizeItem(MatchHelper m, String part) {
		return m.findString(Pattern.compile(Pattern.quote(part) + "：(.+?)<br>"), 1, null);
	}

	private static int getFixedPointValue(MatchHelper m, int baseIndex) {
		String figure = m.group(baseIndex + 2);
		int value = figure == null ? 0 : Integer.parseInt(figure);
		value += Integer.parseInt(m.group(baseIndex + 1)) * 10;
		value += Integer.parseInt(m.group(baseIndex)) * 100;
		return value;
	}

	private static boolean bindSection(MatchHelper m, String name) {
		String sectionPattern = "(?:<[^>]+>)+\\[%s\\](?:<[^>]+>)+\\s*";
		Pattern from = Pattern.compile(String.format(sectionPattern, Pattern.quote(name)));
		Pattern to = Pattern.compile(String.format(sectionPattern, ".+?"));
		return m.bind(from, to);
	}
}
