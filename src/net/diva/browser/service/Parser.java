package net.diva.browser.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.MusicInfo;
import net.diva.browser.PlayRecord;
import net.diva.browser.ScoreRecord;

public final class Parser {
	private Parser() {}

	private static String read(InputStream in) {
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
	private static final Pattern RE_LEVEL = Pattern.compile("\\[LEVEL/RANK\\].*<br>\\s*(.+)<br>");

	public static PlayRecord parseMenuPage(InputStream content) throws ParseException {
		PlayRecord record = new PlayRecord();
		String body = read(content);
		Matcher m = RE_PLAYER.matcher(body);
		if (!m.find())
			throw new ParseException();
		record.player_name = m.group(1);
		m = m.usePattern(RE_LEVEL);
		if (!m.find())
			throw new ParseException();
		record.level_rank = m.group(1);
		return record;
	}

	private static final Pattern RE_MUSIC = Pattern.compile("<a href=\"/divanet/pv/info/(\\w+)/0/\\d+\">(.+)</a>");
	private static final Pattern RE_NEXT = Pattern.compile("<a href=\"(.+)\">次へ</a>");

	public static String parseListPage(InputStream content, List<MusicInfo> list) {
		String body = read(content);
		Matcher m = RE_MUSIC.matcher(body);
		while (m.find())
			list.add(new MusicInfo(m.group(1), m.group(2)));

		m = m.usePattern(RE_NEXT);
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
	private static final Pattern RE_ACHIVEMENT = Pattern.compile("(\\d+)\\.(\\d+)%");
	private static final Pattern RE_HIGHSCORE = Pattern.compile("(\\d+)pts");

	private static ScoreRecord parseScore(Matcher m, String difficulty) throws ParseException {
		ScoreRecord score = new ScoreRecord();
		Pattern RE_DIFFICULTY = Pattern.compile(Pattern.quote(difficulty)+"</b><br>\\s*★(\\d+)");
		m = m.usePattern(RE_DIFFICULTY);
		if (!m.find())
			return null;
		score.difficulty = Integer.valueOf(m.group(1));

		int end = m.regionEnd();
		int start = m.end();
		m = m.usePattern(RE_BLOCKEND);
		if (!m.find(start))
			throw new ParseException();
		m.region(start, m.start());

		score.clear_status = 4 - findLiteral(m, "clear3.jpg", "clear2.jpg", "clear1.jpg", "-");
		score.trial_status = findLiteral(m, "C-TRIAL", "G-TRIAL", "COMPLETE");
		m = m.usePattern(RE_ACHIVEMENT);
		if (m.find())
			score.achievement = Integer.valueOf(m.group(1)) * 100 + Integer.valueOf(m.group(2));
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
		String body = read(content);
		Pattern RE_COVERART = Pattern.compile(Pattern.quote(music.title) + "<br></br><img src=\"(.+)\"");
		Matcher m = RE_COVERART.matcher(body);
		if (!m.find())
			throw new ParseException();
		music.coverart = m.group(1);
		for (int i = 0; i < DIFFICULTIES.length; ++i)
			music.records[i] = parseScore(m, DIFFICULTIES[i]);
	}
}
