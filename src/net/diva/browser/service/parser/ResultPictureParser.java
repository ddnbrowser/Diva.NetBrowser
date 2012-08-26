package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.service.ParseException;

public class ResultPictureParser {
	private final static Pattern RE_RESULT_PICTURE_TOKEN = Pattern.compile("<input type=\"hidden\" name=\"org.apache.struts.taglib.html.TOKEN\"\\s*value=\"(.+?)\"");
	private final static Pattern RE_RESULT_PICTURE_URL = Pattern.compile("<form name=\"imageActionForm\" method=\"post\" action=\"(.+?)\"");

	public static String parseToken(InputStream content) throws ParseException{
		String body = Parser.read(content);
		Matcher m = RE_RESULT_PICTURE_TOKEN.matcher(body);

		String token = null;
		try {
			if(m.find()) {
				token = m.group(1);
			}
		}catch(Exception e){
			throw new ParseException(e);
		}

		return token;
	}

	public static String parseImageUrl(InputStream content)throws ParseException{
		String body = Parser.read(content);
		Matcher m = RE_RESULT_PICTURE_URL.matcher(body);

		String url = null;
		try{
			if(m.find()){
				url = m.group(1);
			}
		}catch(Exception e){
			throw new ParseException(e);
		}
		return url;
	}

}
