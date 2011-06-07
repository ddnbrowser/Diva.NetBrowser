package net.diva.browser.model;

public class TitleInfo {
	public String id;
	public String name;
	public String image_id;
	public int order;

	public TitleInfo(String id_, String name_) {
		id = id_;
		name = name_;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && (o instanceof TitleInfo) && id.equals(((TitleInfo)o).id);
	}
}
