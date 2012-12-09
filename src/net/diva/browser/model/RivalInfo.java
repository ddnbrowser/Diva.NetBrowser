package net.diva.browser.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.service.LoginFailedException;
import net.diva.browser.service.ParseException;
import net.diva.browser.service.ServiceClient;

public class RivalInfo {

	public String rival_code;
	public String rival_name;
	public String rival_token;
	public List<MusicInfo> musics;

	public RivalInfo(){
		musics = new ArrayList<MusicInfo>();
	}

	public MusicInfo getMusic(String music_id, String title){
		if(music_id == null)
			return null;

		for(MusicInfo music : musics){
			if(music_id.equals(music.id))
				return music;
		}

		return new MusicInfo(music_id, title);
	}

	public void put(MusicInfo music){
		for(MusicInfo m_music : musics){
			if(m_music == music)
				return;
		}
		musics.add(music);
	}

	public boolean regist() throws LoginFailedException, ParseException, IOException {
		if (rival_code == null)
			return false;

		ServiceClient service = DdN.getServiceClient();
		if (!service.isLogin())
			service.login();
		return service.setRival(rival_code);
	}

	public boolean remove() throws LoginFailedException, ParseException, IOException {
		if (rival_token == null)
			return false;

		ServiceClient service = DdN.getServiceClient();
		if (!service.isLogin())
			service.login();
		return service.rivalRemove(rival_token);
	}

	public void getData() throws LoginFailedException, ParseException, IOException {
		if (rival_code == null)
			return;

		ServiceClient service = DdN.getServiceClient();
		if (!service.isLogin())
			service.login();

		RivalInfo tmp = service.getRivalScore(rival_code);
		this.rival_name = tmp.rival_name;
		this.rival_token = tmp.rival_token;
		this.musics = tmp.musics;
	}
}
