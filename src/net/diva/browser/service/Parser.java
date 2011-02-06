package net.diva.browser.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;

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
	private static final Pattern RE_LEVEL = Pattern.compile("\\[LEVEL/RANK\\].*<br>\\s*(.+)<br>\\s*<img src=\"/divanet/img/title/(\\w+)\"><br>");

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
		record.level = m.group(1);
		record.title_id = m.group(2);
		return record;
	}

	private static final Pattern RE_MUSIC_TITLE = Pattern.compile("<a href=\"/divanet/pv/info/(\\w+)/0/\\d+\">(.+)</a>");
	private static final Pattern RE_NEXT = Pattern.compile("<a href=\"(.+)\">次へ</a>");

	public static String parseListPage(InputStream content, List<MusicInfo> list) {
		String body = read(content);
		Matcher m = RE_MUSIC_TITLE.matcher(body);
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
	private static final Pattern RE_ACHIVEMENT = Pattern.compile("(\\d+)\\.(\\d)(\\d)?%");
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
		String body = read(content);
		Pattern RE_COVERART = Pattern.compile(Pattern.quote(music.title) + "<br>\\s*\\[(.*)\\]\\s*<br>\\s*<img src=\"(.+?)\"");
		Matcher m = RE_COVERART.matcher(body);
		if (!m.find())
			throw new ParseException();
		music.part = "ソロ".equals(m.group(1)) ? 1 : 2;
		music.coverart = m.group(2);
		for (int i = 0; i < DIFFICULTIES.length; ++i)
			music.records[i] = parseScore(m, DIFFICULTIES[i]);
	}

	private static final Pattern RE_RANKING_TITLE = Pattern.compile("<a href=\".*/(\\w+)/rankingList/\\d+\">(.+)</a>");

	public static String parseRankingList(InputStream content, List<Ranking> list) throws ParseException {
		String body = read(content);
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

		m = m.usePattern(RE_NEXT);
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

	private static final Pattern RE_TITLE_NAME = Pattern.compile("<a href=\"/divanet/title/confirm/(\\w+)/\\d+\">(.+)</a>");

	public static String parseTitleList(InputStream content, List<TitleInfo> titles) {
		String body = read(content);
		Matcher m = RE_TITLE_NAME.matcher(body);
		while (m.find()) {
			TitleInfo title = new TitleInfo(m.group(1), m.group(2));
			if (!titles.contains(title))
				titles.add(title);
		}

		m = m.usePattern(RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern RE_TITLE_IMAGE = Pattern.compile("<img src=\"/divanet/img/title/(\\w+)\"");

	public static String parseTitlePage(InputStream content) {
		String body = read(content);
		Matcher m = RE_TITLE_IMAGE.matcher(body);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern RE_MODULE_GROUP = Pattern.compile("<a href=\"/divanet/module/list/(\\d+)/\\d+\">(.+)\\(\\d+/\\d+\\)</a>");

	public static List<ModuleGroup> parseModuleIndex(InputStream content) {
		List<ModuleGroup> modules = new ArrayList<ModuleGroup>();
		Matcher m = RE_MODULE_GROUP.matcher(read(content));
		while (m.find())
			modules.add(new ModuleGroup(Integer.valueOf(m.group(1)), m.group(2)));
		return modules;
	}

	private static final Pattern RE_MODULE = Pattern.compile("<a href=\"/divanet/module/detail/(\\w+)/\\d+/\\d+\">(.+)</a>\\s*(\\(未購入\\))?");
	private static final Pattern RE_MODULE_IMAGE = Pattern.compile("<img src=\"(/divanet/img/module/\\w+)\"");

	public static String parseModuleList(InputStream content, List<Module> modules) {
		String body = read(content);
		Matcher m = RE_MODULE.matcher(body);
		while (m.find()) {
			Module module = new Module();
			module.id = m.group(1);
			module.name = m.group(2);
			module.purchased = m.group(3) == null;
			modules.add(module);
		}

		m = m.usePattern(RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	static void parseModuleDetail(InputStream content, Module module) {
		String body = read(content);
		Matcher m = RE_MODULE_IMAGE.matcher(body);
		if (m.find())
			module.image = m.group(1);
	}

	static class Skin {
		private static final Pattern RE_GROUP = Pattern.compile("<a href=\"/divanet/skin/select/(\\w+)/\\d+\">(.+)</a>");
		private static final Pattern RE_SKIN = Pattern.compile("<a href=\"/divanet/skin/confirm/(\\w+)/(\\w+)/\\d+\">(.+)</a>");
		private static final Pattern RE_IMAGE = Pattern.compile("<img src=\"(/divanet/img/skin/\\w+)\"");

		static String parse(InputStream content, List<String> groups) {
			String body = read(content);
			Matcher m = RE_GROUP.matcher(body);
			while (m.find())
				groups.add(m.group(1));

			m = m.usePattern(RE_NEXT);
			return m.find() ? m.group(1) : null;
		}

		static void parse(InputStream content, List<SkinInfo> skins) {
			String body = read(content);
			Matcher m = RE_SKIN.matcher(body);
			while (m.find())
				skins.add(new SkinInfo(m.group(1), m.group(2), m.group(3)));
		}

		static void parse(InputStream content, SkinInfo skin) {
			String body = read(content);
			Matcher m = RE_IMAGE.matcher(body);
			if (m.find())
				skin.image_path = m.group(1);
		}
	}

	static class Shop {
		private static final Pattern RE_IMAGE = Pattern.compile("<img src=\"(/divanet/[^\"]+)\"");
		private static final Pattern RE_ITEM = Pattern.compile("(\\[.+\\])</font><br>\\s*(.+)<");

		static String parse(InputStream content, List<NameValuePair> details) {
			String body = read(content);
			Matcher m = RE_ITEM.matcher(body);
			while (m.find())
				details.add(new BasicNameValuePair(m.group(1), m.group(2)));

			m = m.usePattern(RE_IMAGE);
			return m.find() ? m.group(1) : null;
		}

		static boolean isSuccess(InputStream content) {
			String body = read(content);
			return body.contains("購入しました");
		}
	}
}
