package net.diva.browser.service.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;

public class ModuleParser {
	private static final Pattern RE_MODULE_GROUP = Pattern.compile("<a href=\"/divanet/module/list/(\\d+)/\\d+\">(.+)\\(\\d+/\\d+\\)</a>");

	public static List<ModuleGroup> parseModuleIndex(InputStream content) {
		List<ModuleGroup> modules = new ArrayList<ModuleGroup>();
		Matcher m = RE_MODULE_GROUP.matcher(Parser.read(content));
		while (m.find())
			modules.add(new ModuleGroup(Integer.valueOf(m.group(1)), m.group(2)));
		return modules;
	}

	private static final Pattern RE_MODULE = Pattern.compile("<a href=\"/divanet/module/detail/(\\w+)/\\d+/\\d+\">(.+)</a>\\s*<[^>]+>\\s*(\\(未購入\\))?");
	private static final Pattern RE_MODULE_IMAGE = Pattern.compile("<img src=\"(/divanet/img/module/\\w+)\"");

	public static String parseModuleList(InputStream content, List<Module> modules) {
		String body = Parser.read(content);
		Matcher m = RE_MODULE.matcher(body);
		while (m.find()) {
			Module module = new Module();
			module.id = m.group(1);
			module.name = m.group(2);
			module.purchased = m.group(3) == null;
			modules.add(module);
		}

		m = m.usePattern(Parser.RE_NEXT);
		return m.find() ? m.group(1) : null;
	}

	public static void parseModuleDetail(InputStream content, Module module) {
		String body = Parser.read(content);
		Matcher m = RE_MODULE_IMAGE.matcher(body);
		if (m.find())
			module.image = m.group(1);
	}
}
