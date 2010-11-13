package net.diva.browser;


public class MusicInfo {
	public String id;
	public String title;
	public String coverart;
	public ScoreRecord[] records;

	public MusicInfo(String id_, String title_) {
		id = id_;
		title = title_;
		records = new ScoreRecord[4];
	}
}
