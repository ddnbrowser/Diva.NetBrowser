package net.diva.browser.model;

import java.util.ArrayList;
import java.util.List;

public class ModuleGroup {
	public int id;
	public String name;
	public List<Module> modules;

	public ModuleGroup(int id_, String name_) {
		id = id_;
		name = name_;
		modules = new ArrayList<Module>();
	}
}
