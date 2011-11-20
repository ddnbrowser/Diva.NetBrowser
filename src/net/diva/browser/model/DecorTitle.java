package net.diva.browser.model;

public class DecorTitle {
	public static DecorTitle OFF = new DecorTitle("OFF", "未設定にする", true);

	public String id;
	public String name;
	public boolean pre;
	public boolean purchased;
	public boolean prize;

	public DecorTitle(String id_, String name_, boolean purchased_) {
		id = id_;
		name = name_;
		purchased = purchased_;
		prize = false;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && (o instanceof DecorTitle) && id.equals(((DecorTitle)o).id);
	}
}
