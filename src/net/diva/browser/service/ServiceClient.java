package net.diva.browser.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.net.Uri;

public class ServiceClient {
	private static final int LOGIN_DURATION = 5*60*1000;

	private DefaultHttpClient m_client = new DefaultHttpClient();
	private String m_access_code;
	private String m_password;
	private long m_lastAccess;

	public ServiceClient(String access_code, String password) {
		m_access_code = access_code;
		m_password = password;
		m_lastAccess = 0;
	}

	public void access() {
		m_lastAccess = System.currentTimeMillis();
	}

	public boolean isLogin() {
		return System.currentTimeMillis() - m_lastAccess < LOGIN_DURATION;
	}

	public String cookies() {
		if (!isLogin())
			return null;

		StringBuilder builder = new StringBuilder();
		CookieStore store = m_client.getCookieStore();
		for (Cookie cookie: store.getCookies())
			builder.append(String.format("; %s=%s", cookie.getName(), cookie.getValue()));
		return builder.substring(2).toString();
	}

	public PlayRecord login() throws LoginFailedException {
		Uri url = Uri.parse(DdN.URL.resolve("/divanet/login/").toString()).buildUpon().scheme("https").build();
		HttpPost request = new HttpPost(url.toString());
		try {
			request.setHeader("Referer", DdN.URL.toString());
			request.setHeader("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)");
			request.setHeader("Cache-Control", "no-cache");

			List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("accessCode", m_access_code));
			params.add(new BasicNameValuePair("password", m_password));
			request.setEntity(new UrlEncodedFormEntity(params, "US-ASCII"));
			HttpResponse response = m_client.execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
				throw new LoginFailedException();
			access();
			return Parser.parseMenuPage(response.getEntity().getContent());
		}
		catch (Exception e) {
			throw new LoginFailedException(e);
		}
	}

	public void update(PlayRecord record) throws NoLoginException {
		List<MusicInfo> list = new ArrayList<MusicInfo>();
		String path = "/divanet/pv/list/0/0";
		while (path != null) {
			try {
				HttpResponse response = getFrom(path);
				path = Parser.parseListPage(response.getEntity().getContent(), list);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (list.isEmpty())
			throw new NoLoginException();

		record.musics = list;
	}

	public void update(MusicInfo music) throws NoLoginException {
		try {
			HttpResponse response = getFrom("/divanet/pv/info/%s/0/0", music.id);
			Parser.parseInfoPage(response.getEntity().getContent(), music);
		}
		catch (ParseException e) {
			throw new NoLoginException(e);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public InputStream download(String path) throws IOException {
		HttpResponse response = getFrom(path);
		return response.getEntity().getContent();
	}

	public void download(String path, OutputStream out) throws IOException {
		InputStream in = download(path);
		byte[] buffer = new byte[1024];
		for (int read; (read = in.read(buffer)) != -1;)
			out.write(buffer, 0, read);
	}

	public List<Ranking> getRankInList() throws IOException, ParseException {
		List<Ranking> list = new ArrayList<Ranking>();
		String path = "/divanet/ranking/list/0";
		while (path != null) {
			HttpResponse response = getFrom(path);
			path = Parser.parseRankingList(response.getEntity().getContent(), list);
		}

		return list;
	}

	public List<TitleInfo> getTitles(List<TitleInfo> titles) throws IOException {
		if (titles == null)
			titles = new ArrayList<TitleInfo>();
		String path = "/divanet/title/list/0";
		while (path != null) {
			HttpResponse response = getFrom(path);
			path = Parser.parseTitleList(response.getEntity().getContent(), titles);
		}

		for (TitleInfo title: titles) {
			if (title.image_id != null)
				continue;

			HttpResponse response = getFrom("/divanet/title/confirm/%s/0", title.id);
			title.image_id = Parser.parseTitlePage(response.getEntity().getContent());
		}

		return titles;
	}

	public List<ModuleGroup> getModules() throws IOException {
		HttpResponse response = getFrom("/divanet/module/");
		List<ModuleGroup> modules = Parser.parseModuleIndex(response.getEntity().getContent());
		for (ModuleGroup group: modules) {
			String path = String.format("/divanet/module/list/%d/0", group.id);
			while (path != null) {
				HttpResponse res = getFrom(path);
				path = Parser.parseModuleList(res.getEntity().getContent(), group.modules);
			}
		}

		return modules;
	}

	public void getModuleDetail(Module module) throws IOException {
		HttpResponse response = getFrom("/divanet/module/detail/%s/0/0", module.id);
		Parser.parseModuleDetail(response.getEntity().getContent(), module);
		module.thumbnail = String.format("/divanet/img/moduleTmb/%s", new File(module.image).getName());
	}

	public List<SkinInfo> getSkins() throws IOException {
		List<String> groups = new ArrayList<String>();

		String path = "/divanet/skin/list/0";
		while (path != null) {
			HttpResponse response = getFrom(path);
			path = Parser.Skin.parse(response.getEntity().getContent(), groups);
		}

		List<SkinInfo> skins = new ArrayList<SkinInfo>();
		for (String group_id: groups) {
			HttpResponse response = getFrom("/divanet/skin/select/%s/0", group_id);
			Parser.Skin.parse(response.getEntity().getContent(), skins);
		}

		return skins;
	}

	public List<SkinInfo> getSkinsFromShop() throws IOException {
		List<SkinInfo> skins = new ArrayList<SkinInfo>();

		String path = "/divanet/skin/shop/0";
		while (path != null) {
			HttpResponse response = getFrom(path);
			path = Parser.Skin.parseShop(response.getEntity().getContent(), skins);
		}

		return skins;
	}

	public void getSkinDetail(SkinInfo skin) throws IOException {
		HttpResponse response = getFrom("/divanet/skin/%s/%s/%s/0",
				skin.purchased ? "confirm" : "detail", skin.id, skin.group_id);
		Parser.Skin.parse(response.getEntity().getContent(), skin);
	}

	public String getShopDetail(String path, List<NameValuePair> details) throws IOException {
		HttpResponse response = getFrom(path);
		return Parser.Shop.parse(response.getEntity().getContent(), details);
	}

	private HttpResponse getFrom(String relative, Object... args) throws IOException {
		return getFrom(String.format(relative, args));
	}

	private HttpResponse getFrom(String relative) throws IOException {
		HttpResponse response = m_client.execute(new HttpGet(DdN.URL.resolve(relative)));
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException();

		access();
		return response;
	}

	private HttpResponse postTo(String relative) throws IOException {
		return postTo(relative, null);
	}

	private HttpResponse postTo(String relative, HttpEntity entity) throws IOException {
		HttpPost request = new HttpPost(DdN.URL.resolve(relative));
		if (entity != null)
			request.setEntity(entity);

		HttpResponse response = m_client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException();

		access();
		return response;
	}

	public void rename(String name) throws OperationFailedException, IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("name", name));
		HttpResponse response = postTo("/divanet/personal/updateName/", new UrlEncodedFormEntity(params, "UTF-8"));
		String error = Parser.parseRenameResult(response.getEntity().getContent());
		if (error != null)
			throw new OperationFailedException(error);
	}

	public void setTitle(String title_id) throws IOException {
		postTo(String.format("/divanet/title/update/%s", title_id));
	}

	public void setCommonModule(String key, String module_id) throws IOException {
		postTo(String.format("/divanet/module/update/COMMON/%s/%s/0", key, module_id));
	}

	public void resetCommonModules() throws IOException {
		postTo("/divanet/module/resetCommon/");
	}

	public void setIndividualModule(String music_id, String vocal1) throws IOException {
		postTo(String.format("/divanet/module/update/%s/vocal1/%s/0", music_id, vocal1));
	}

	public void setIndividualModule(String music_id, String vocal1, String vocal2) throws IOException {
		getFrom("/divanet/module/selectPv/%s/0", music_id);
		getFrom("/divanet/module/confirm/%s/vocal1/%s/0/0/0", music_id, vocal1);
		getFrom("/divanet/module/confirm/%s/vocal2/%s/0/0/0", music_id, vocal2);
		postTo(String.format("/divanet/module/updateDuet/%s/%s/%s/0", music_id, vocal1, vocal2));
	}

	public void resetIndividualModule(String music_id) throws IOException {
		postTo(String.format("/divanet/module/resetIndividual/%s", music_id));
	}

	public void resetIndividualModules() throws IOException {
		postTo("/divanet/module/resetIndividualAll/");
	}

	public void activateIndividualModules(boolean on) throws IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("activation", on ? "true" : "false"));
		postTo("/divanet/module/updateConfig/", new UrlEncodedFormEntity(params, "US-ASCII"));
	}

	public void setSkin(String group_id, String skin_id) throws IOException {
		postTo(String.format("/divanet/skin/update/%s/%s", skin_id, group_id));
	}

	public void unsetSkin() throws IOException {
		postTo("/divanet/skin/unset/");
	}

	public void buyModule(String id) throws OperationFailedException, IOException {
		HttpResponse response = postTo(String.format("/divanet/module/buy/%s", id));
		if (!Parser.Shop.isSuccess(response.getEntity().getContent()))
			throw new OperationFailedException();
	}

	public void buySkin(String group_id, String skin_id) throws OperationFailedException, IOException {
		HttpResponse response = postTo(String.format("/divanet/skin/buy/%s/%s", skin_id, group_id));
		if (!Parser.Shop.isSuccess(response.getEntity().getContent()))
			throw new OperationFailedException();
	}
}
