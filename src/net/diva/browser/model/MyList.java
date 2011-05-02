package net.diva.browser.model;

public class MyList {
	public int id;
	public String name;
	public String tag;

	public MyList(int id_, String name_) {
		id = id_;
		name = name_;
		tag = String.format("mylist%d", id_);
	}
}
