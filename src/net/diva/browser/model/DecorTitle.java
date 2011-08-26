package net.diva.browser.model;

public class DecorTitle {
	public static DecorTitle OFF = new DecorTitle("OFF", "未設定にする", true);

	public String id;
	public String name;
	public boolean purchased;

	public DecorTitle(String id_, String name_, boolean purchased_) {
		id = id_;
		name = name_;
		purchased = purchased_;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && (o instanceof DecorTitle) && id.equals(((DecorTitle)o).id);
	}
}
