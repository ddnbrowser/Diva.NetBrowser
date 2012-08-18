package net.diva.browser.service.parser;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.History;
import net.diva.browser.service.ParseException;
import net.diva.browser.util.DdNUtil;
import net.diva.browser.util.MatchHelper;
/**
 *
 * @author silvia
 *
 */
public class HistoryParser {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy/MM/dd HH:mm");
	private final static Pattern RE_HISTORY = Pattern.compile("<font color=\"#00FFFF\">\\[(.+)\\]</font><br>\\s*<a href=\"/divanet/pv/info/(\\w+)/");
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
	private static final Pattern HIST_TRIAL = Pattern.compile("\\[クリアトライアル\\]</font><br>(.*?)クリアトライアル\\s*(.+?)<br>");
	private static final Pattern HIST_MODULE1 = Pattern.compile("(ボーカル|衣装)1：(.+?)<br>");
	private static final Pattern HIST_MODULE2 = Pattern.compile("(ボーカル|衣装)2：(.+?)<br>");
	private static final Pattern HIST_SE = Pattern.compile("\\[ボタン音\\]</font><br>\\s*(.+?)<br>");
	private static final Pattern HIST_SKIN = Pattern.compile("\\[スキン\\]</font><br>\\s*(.+?)<br>");

	public static History parseHistoryDetail(InputStream content) throws ParseException {
		History history = new History();
		try{
			String body = Parser.read(content);
			MatchHelper m = new MatchHelper(body);
			history.play_date = DATE_FORMAT.parse(m.findString(HIST_DATE, 1)).getTime();
			history.play_place = m.findString(HIST_PLACE, 1);
			history.music_title = m.findString(HIST_MUSIC, 2);
			history.rank = DdNUtil.getDifficultyCord((m.findString(HIST_RANK, 1)));
			history.clear_status = DdNUtil.getClearStatusCord((m.findString(HIST_CLEAR_STATUS, 1)));
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
			if (m.find(HIST_TRIAL)) {
				history.trial = DdNUtil.getTrialsCord(m.group(1));
				history.trial_result = DdNUtil.getTrialResultsCord(m.group(2));
			}
			history.module1 = m.findString(HIST_MODULE1, 2, null);
			history.module2 = m.findString(HIST_MODULE2, 2, null);
			history.button_se = m.findString(HIST_SE, 1, null);
			history.skin = m.findString(HIST_SKIN, 1, null);
		}
		catch (ParseException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		return history;
	}

	private static int getFixedPointValue(MatchHelper m, int baseIndex) {
		String figure = m.group(baseIndex + 2);
		int value = figure == null ? 0 : Integer.parseInt(figure);
		value += Integer.parseInt(m.group(baseIndex + 1)) * 10;
		value += Integer.parseInt(m.group(baseIndex)) * 100;
		return value;
	}
}
