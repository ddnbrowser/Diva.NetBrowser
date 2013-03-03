package net.diva.browser.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.diva.browser.service.ParseException;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.util.DdNUtil;

public class RivalInfo {

	public String rival_code;
	public String rival_name;
	public String rival_token;
	public Map<String, MusicInfo> musics;

	public RivalInfo(){
		musics = new HashMap<String, MusicInfo>();
	}

	public MusicInfo getMusic(String title){
		MusicInfo music = musics.get(title);
		if(music == null){
			music = new MusicInfo(DdNUtil.getMusicId(title), title);
			musics.put(title, music);
		}
		return music;
	}

	public boolean regist(ServiceClient service) throws ParseException, IOException {
		if (rival_code == null)
			return false;

		return service.setRival(this);
	}

	public boolean remove(ServiceClient service) throws ParseException, IOException {
		if (rival_token == null)
			return false;

		return service.rivalRemove(this);
	}

	public boolean getData(ServiceClient service) throws ParseException, IOException {
		return service.getRivalScore(this);
	}
}
