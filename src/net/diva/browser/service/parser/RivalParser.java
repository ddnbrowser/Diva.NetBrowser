package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.RivalInfo;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.service.ParseException;

public class RivalParser {

	private static final Pattern RIVAL_TOKEN_BY_DELPAGE = Pattern.compile("<input type=\"checkbox\" name=\"deletePlayer\" value=\"(.+?)\">(.+?)</label>");
	private static final Pattern RIVAL_CODE = Pattern.compile("\\[ライバルコード\\].*?\\s+(.+?)<br>", Pattern.DOTALL);
	private static final Pattern RIVAL_CN = Pattern.compile("\\[プレイヤー名\\].*?\\s+(.+?)<br>", Pattern.DOTALL);
	private static final Pattern RIVAL_TOKEN1 = Pattern.compile("\"/divanet/rival/setRival/(.*?)\"");
	private static final Pattern RIVAL_TOKEN2 = Pattern.compile("\"/divanet/statistics/compare/(.*?)\"");

	public static List<RivalInfo> getRivalTokens(InputStream content){
		String body = Parser.read(content);
		Matcher m = RIVAL_TOKEN_BY_DELPAGE.matcher(body);
		List<RivalInfo> rivalList = new ArrayList<RivalInfo>();
		while(m.find()){
			RivalInfo rival = new RivalInfo();
			rival.rival_token = m.group(1);
			rival.rival_name = m.group(2);
			rivalList.add(rival);
		}
		return rivalList;
	}

	public static void getRivalInfo(InputStream content, RivalInfo rival) throws ParseException {
		String body = Parser.read(content);
		Matcher m = RIVAL_CODE.matcher(body);
		if(!m.find())
			throw new ParseException();

		rival.rival_code = "非公開".equals(m.group(1)) ? null : m.group(1);
		m = RIVAL_CN.matcher(body);
		if(!m.find())
			throw new ParseException();

		rival.rival_name = m.group(1);
		m = RIVAL_TOKEN1.matcher(body);
		if(!m.find()){
			m = RIVAL_TOKEN2.matcher(body);
			if(!m.find())
				throw new ParseException();
		}

		rival.rival_token = m.group(1);
	}

	private static final Pattern SIMPLE_RIVAL_SCORE_TEXT = Pattern.compile("<FONT size=\"-1\" class=\"smaller\">(.*?)</FONT></TD>\\s*?" +
			"<TD style=\"text-align : center;\" align=\"center\" width=\"20%\"><FONT.*?>\\s*?(.+?)%</FONT></TD>\\s*?" +
			"<TD style=\"text-align : center;\" align=\"center\" width=\"20%\"><FONT.*?>\\s*?(\\d*?)</FONT>");

	public static String parseRivalScore(InputStream content, RivalInfo rival, int difficulty){
		String body = Parser.read(content);
		Matcher m = SIMPLE_RIVAL_SCORE_TEXT.matcher(body);
		while(m.find()){
			String title = m.group(1).replaceAll("<.*?>", "");
			String achievement = m.group(2);
			String score = m.group(3);
			MusicInfo music = rival.getMusic(title);
			ScoreRecord scoreRecord = new ScoreRecord();
			scoreRecord.achievement = (int) (Double.valueOf(achievement) * 100);
			scoreRecord.high_score = Integer.valueOf(score);
			music.rival_records[difficulty] = scoreRecord;
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern RIVAL_REGIST_RESULT = Pattern.compile("ライバルに設定しました。");
	private static final Pattern RIVAL_REMOVE_RESULT = Pattern.compile("削除しました。");

	public static boolean setRivalResult(InputStream content) {
		String body = Parser.read(content);
		Matcher m = RIVAL_REGIST_RESULT.matcher(body);
		return m.find();
	}

	public static boolean removeRivalResult(InputStream content) {
		String body = Parser.read(content);
		Matcher m = RIVAL_REMOVE_RESULT.matcher(body);
		return m.find();
	}
}
