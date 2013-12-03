package net.diva.browser.service.parser;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.service.ParseException;

public class MusicParser {
	private static final Pattern RE_MUSIC_TITLE = Pattern.compile("<a href=\"/divanet/pv/info/(\\w+)/0/\\d+\">(.+)</a>");

	public static String parseListPage(InputStream content, List<MusicInfo> list) {
		String body = Parser.read(content);
		Matcher m = RE_MUSIC_TITLE.matcher(body);
		while (m.find())
			list.add(new MusicInfo(m.group(1), m.group(2)));

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static int findLiteral(Matcher m, String... candidates) {
		for (int i = 0; i < candidates.length; ++i) {
			m = m.usePattern(Pattern.compile(Pattern.quote(candidates[i])));
			if (m.find())
				return i + 1;
		}
		return 0;
	}

	private static final Pattern RE_BLOCKEND = Pattern.compile("</table");
	private static final Pattern RE_ACHIVEMENT = Pattern.compile("(\\d+)\\.(\\d)(\\d)?%");
	private static final Pattern RE_HIGHSCORE = Pattern.compile("(\\d+)pts");

	private static ScoreRecord parseScore(Matcher m, String difficulty, ScoreRecord score) throws ParseException {
		if (score == null)
			score = new ScoreRecord();

		Pattern RE_DIFFICULTY = Pattern.compile(Pattern.quote(difficulty)+"</b><br>\\s*★(\\d+)(?:\\.(\\d))?");
		m = m.usePattern(RE_DIFFICULTY);
		if (!m.find())
			return null;
		score.difficulty = Integer.valueOf(m.group(1)) * 10;
		if (m.group(2) != null)
			score.difficulty += Integer.parseInt(m.group(2));

		int end = m.regionEnd();
		int start = m.end();
		m = m.usePattern(RE_BLOCKEND);
		if (!m.find(start))
			throw new ParseException();
		m.region(start, m.start());

		score.clear_status = 5 - findLiteral(m, "clear4.jpg", "clear3.jpg", "clear2.jpg", "clear1.jpg", "-");
		score.trial_status = findLiteral(m, "C-TRIAL", "G-TRIAL", "E-TRIAL", "COMPLETE");
		m = m.usePattern(RE_ACHIVEMENT);
		if (m.find()) {
			score.achievement = m.group(3) == null ? 0 : Integer.valueOf(m.group(3));
			score.achievement += Integer.valueOf(m.group(2)) * 10;
			score.achievement += Integer.valueOf(m.group(1)) * 100;
		}
		m = m.usePattern(RE_HIGHSCORE);
		if (m.find())
			score.high_score = Integer.valueOf(m.group(1));

		m.region(m.end(), end);
		return score;
	}

	private static final String[] DIFFICULTIES = new String[] {
		"EASY", "NORMAL", "HARD", "EXTREME"
	};

	public static void parseInfoPage(InputStream content, MusicInfo music) throws ParseException {
		String body = Parser.read(content);
		Pattern RE_COVERART = Pattern.compile(Pattern.quote(music.title) + "<br>\\s*\\[(.*)\\]\\s*<br>\\s*<img src=\"(.+?)\"");
		Matcher m = RE_COVERART.matcher(body);
		if (!m.find())
			throw new ParseException();
		music.part = "ソロ".equals(m.group(1)) ? 1 : 2;
		music.coverart = m.group(2);
		for (int i = 0; i < DIFFICULTIES.length; ++i)
			music.records[i] = parseScore(m, DIFFICULTIES[i], music.records[i]);
	}

	private static final Pattern RE_VOICE = Pattern.compile("\\[ボイス(1|2)?　(.+)\\]");

	public static String[] parseVoice(InputStream content) throws ParseException {
		String body = Parser.read(content);
		Matcher m = RE_VOICE.matcher(body);
		if (!m.find())
			throw new ParseException();
		String voice1 = m.group(2);
		String voice2 = m.find() ? m.group(2) : null;
		return new String[] { voice1, voice2 };
	}

	static final Pattern RE_RANKING_TITLE = Pattern.compile("<a href=\".*/(\\w+)/rankingList/\\d+\".*?>(.+)</a>");

	public static String parseRankingList(InputStream content, List<Ranking> list) throws ParseException {
		String body = Parser.read(content);
		Matcher m = RE_RANKING_TITLE.matcher(body);
		int last = m.regionEnd();
		for (MatchResult r = m.find() ? m.toMatchResult() : null; r != null; ) {
			String id = r.group(1);
			String title = r.group(2);
			int start = r.end();

			m = m.usePattern(RE_RANKING_TITLE);
			m.region(start, last);
			if (m.find()) {
				r = m.toMatchResult();
				m.region(start, r.start());
			}
			else {
				r = null;
			}

			for (int rank = 3; rank > 1; --rank) {
				Ranking entry = parseRankIn(m, DIFFICULTIES[rank]);
				if (entry != null) {
					entry.id = id;
					entry.title = title;
					entry.rank = rank;
					list.add(entry);
				}
			}
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern RE_RANKIN_SCORE = Pattern.compile(">(\\d+)</");
	private static final Pattern RE_RANKIN_DATE = Pattern.compile(">(\\d+/\\d+/\\d+)</");
	private static final Pattern RE_RANKING = Pattern.compile(">(\\d+)位</");
	private static final SimpleDateFormat RANKING_DATE = new SimpleDateFormat("yy/MM/dd");

	private static String find(Matcher m, Pattern pattern, int group) throws ParseException {
		int from = m.end();
		m.usePattern(pattern);
		if (!m.find(from))
			throw new ParseException();
		return m.group(group);
	}

	private static Ranking parseRankIn(Matcher m, String difficulty) throws ParseException {
		Pattern RE_DIFFICULTY = Pattern.compile(Pattern.quote(difficulty));
		m = m.usePattern(RE_DIFFICULTY);
		if (!m.find())
			return null;
		Ranking entry = new Ranking();
		try {
			entry.score = Integer.valueOf(find(m, RE_RANKIN_SCORE, 1));
			entry.date = RANKING_DATE.parse(find(m, RE_RANKIN_DATE, 1)).getTime();
			entry.ranking = Integer.valueOf(find(m, RE_RANKING, 1));
		}
		catch (ParseException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		return entry;
	}

	private final static Pattern RE_HISTORY = Pattern.compile("<font color=\"#00FFFF\">\\[(.+)\\]</font>\\s*<br>\\s*<a href=\"/divanet/pv/info/(\\w+)/");
	private static final SimpleDateFormat HISTORY_DATE = new SimpleDateFormat("yy/MM/dd HH:mm");

	public static String parsePlayHistory(InputStream content, List<String> ids, long[] params)
			throws ParseException {
		String body = Parser.read(content);
		Matcher m = RE_HISTORY.matcher(body);
		try {
			while (m.find()) {
				long playTime = HISTORY_DATE.parse(m.group(1)).getTime();
				if (playTime <= params[0])
					return null;
				if (playTime > params[1])
					params[1] = playTime;
				final String id = m.group(2);
				if (!ids.contains(id))
					ids.add(id);
			}
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}
}
