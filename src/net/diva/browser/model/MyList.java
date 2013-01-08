package net.diva.browser.model;

public class MyList {
	public int id;
	public String name;
	public int max;
	public String tag;

	public MyList(int id_, String name_, int max_) {
		id = id_;
		name = name_;
		max = max_;
		tag = String.format("mylist%d", id_);
	}
}
