package net.diva.browser.service.parser;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.model.History;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.service.ParseException;
import net.diva.browser.util.DdNUtil;
/**
 *
 * @author silvia
 *
 */
public class HistoryParser {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy/MM/dd HH:mm");
	private final static Pattern RE_HISTORY = Pattern.compile(
			"<font color=\"#00FFFF\">\\[(.+)\\]</font><br>\\s*<a href=\"/divanet/pv/info/(\\w+)/0/0\">.+<br></a>\\s*"
			+"\\[.+\\] .+<br>\\s*達成率：.+%<br>SCORE：.+<br>\\s*"
			+"┗<a href=\"/divanet/personal/playHistoryDetail/(.+?)/"
			);
	public static String parsePlayHistory(InputStream content, List<String> newHistorys, long[] params)
			throws ParseException {
		String body = Parser.read(content);
		Matcher m = RE_HISTORY.matcher(body);

		try {
			while (m.find()) {
				long playTime = DATE_FORMAT.parse(m.group(1)).getTime();
				if (playTime <= params[0])
					return null;
				if (playTime > params[1])
					params[1] = playTime;
				final String historyId = m.group(3);
				newHistorys.add(historyId);
			}
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	private static final Pattern HIST_DATE = Pattern.compile("\\[日時\\] </font>(.+?)<br>");
	private static final Pattern HIST_PLACE = Pattern.compile("\\[場所\\] </font>(.+?)<br>");
	private static final Pattern HIST_MUSIC_ID = Pattern.compile("<a href=\"/divanet/pv/info/(\\w+)/0/0\">");
	private static final Pattern HIST_RANK = Pattern.compile("\\s*(.+?)　★");
	private static final Pattern HIST_CLEAR_STATUS = Pattern.compile("\\[CLEAR RANK\\]</font><br>(.+?)<br>");
	private static final Pattern HIST_ACHIEVEMENT = Pattern.compile("\\[達成率\\]</font><br>(.+?)％<br>");
	private static final Pattern HIST_SCORE = Pattern.compile("\\[SCORE\\]</font><br>(.+?)<br>");
	private static final Pattern HIST_COOL = Pattern.compile("COOL：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(.+?)%</TD>");
	private static final Pattern HIST_FINE = Pattern.compile("FINE：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(.+?)%</TD>");
	private static final Pattern HIST_SAFE = Pattern.compile("SAFE：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(.+?)%</TD>");
	private static final Pattern HIST_SAD = Pattern.compile("SAD：</TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(.+?)%</TD>");
	private static final Pattern HIST_WORST = Pattern.compile("WORST/WRONG：</TD>\\s*</TR>\\s*<TR>\\s*<TD align=\"left\"></TD>\\s*<TD align=\"right\">(.+?)</TD>\\s*<TD>/</TD>\\s*<TD align=\"right\">(.+?)%</TD>");
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
			history.play_date = DATE_FORMAT.parse(getMatchText(HIST_DATE, body)[0]).getTime();
			history.play_place = getMatchText(HIST_PLACE, body)[0];
			history.music_id = getMatchText(HIST_MUSIC_ID, body)[0];
			history.rank = DdNUtil.getDifficultyCord((getMatchText(HIST_RANK, body)[0]));
			history.clear_status = DdNUtil.getClearStatusCord((getMatchText(HIST_CLEAR_STATUS, body)[0]));
			history.achievement = (int)(Double.valueOf(getMatchText(HIST_ACHIEVEMENT, body)[0]) * 100);
			history.score = Integer.valueOf(getMatchText(HIST_SCORE, body)[0]);
			{
				String[] cool = getMatchText(HIST_COOL, body);
				history.cool = Integer.valueOf(cool[0]);
			}
			{
				String[] fine = getMatchText(HIST_FINE, body);
				history.fine = Integer.valueOf(fine[0]);
			}
			{
				String[] safe = getMatchText(HIST_SAFE, body);
				history.safe = Integer.valueOf(safe[0]);
			}
			{
				String[] sad = getMatchText(HIST_SAD, body);
				history.sad = Integer.valueOf(sad[0]);
			}
			{
				String[] worst = getMatchText(HIST_WORST, body);
				history.worst = Integer.valueOf(worst[0]);
			}
			history.combo = Integer.valueOf(getMatchText(HIST_COMBO, body)[0]);
			history.challange_time = Integer.valueOf(getMatchText(HIST_CHALLENGE_TIME, body)[0]);
			history.hold = Integer.valueOf(getMatchText(HIST_HOLD, body)[0]);
			{
				String[] trial = getMatchText(HIST_TRIAL, body);
				if(trial != null){
					history.trial = DdNUtil.getTrialsCord((trial[0]));
					history.trial_result = DdNUtil.getTrialResultsCord((trial[1]));
				}
			}
			history.module1_id = getModuleId(getMatchText(HIST_MODULE1, body)[1]);
			{
				String[] mod2 = getMatchText(HIST_MODULE2, body);
				if(mod2 != null)
					history.module2_id = getModuleId(mod2[1]);
			}
			{
				String[] se = getMatchText(HIST_SE, body);
				if(se != null)
					history.se_id = getSeId(se[0]);
			}
			{
				String[] skin = getMatchText(HIST_SKIN, body);
				if(skin != null)
					history.skin_id = getSkinId(skin[0]);
			}
		} catch(Exception e){
			throw new ParseException(e);
		}

		return history;
	}

	private static String[] getMatchText(Pattern p, String source) {
		Matcher m = p.matcher(source);
		if(!m.find()){
			return null;
		}

		String[] texts = new String[m.groupCount()];
		for(int i = 1; i <= m.groupCount(); i++){
			texts[i-1] = m.group(i);
		}
		return texts;
	}

	private static String getModuleId(String moduleName) {
		List<ModuleGroup> groups = DdN.getModules();
		for(ModuleGroup mg : groups) {
			for(Module m : mg.modules){
				if(m.name.equals(moduleName))
					return m.id;
			}
		}

		return "unknown";
	}

	private static String getSeId(String name) {
		String se = DdN.getLocalStore().getButtonSeId(name);
		return "".equals(se) ? "unknown" : se;
	}

	private static String getSkinId(String name){
		if(name != null)
			return DdN.getLocalStore().getSkinId(name);

		return "unknown";
	}
}
