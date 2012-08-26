package net.diva.browser.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.diva.browser.DdN;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.DecorTitle;
import net.diva.browser.model.History;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.service.parser.HistoryParser;
import net.diva.browser.service.parser.ModuleParser;
import net.diva.browser.service.parser.MusicParser;
import net.diva.browser.service.parser.MyListParser;
import net.diva.browser.service.parser.Parser;
import net.diva.browser.service.parser.RecordParser;
import net.diva.browser.service.parser.ResultPictureParser;
import net.diva.browser.service.parser.SEParser;
import net.diva.browser.service.parser.ShopParser;
import net.diva.browser.service.parser.SkinParser;
import net.diva.browser.service.parser.TitleParser;

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
		Uri url = Uri.parse(DdN.url("/divanet/login/")).buildUpon().scheme("https").build();
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

			Parser.Result result = Parser.parseMenuPage(response.getEntity().getContent());
			DdN.setNewsTimestamp(result.newsTimestamp);
			return result.record;
		}
		catch (Exception e) {
			throw new LoginFailedException(e);
		}
	}

	private List<MusicInfo> getMusics(String path) throws IOException {
		List<MusicInfo> list = new ArrayList<MusicInfo>();
		while (path != null)
			path = MusicParser.parseListPage(getFrom(path), list);
		return list;
	}

	public List<MusicInfo> getMusics() throws NoLoginException, IOException {
		List<MusicInfo> list = getMusics("/divanet/pv/sort/0/false/0");
		if (list.isEmpty())
			throw new NoLoginException();
		return list;
	}

	public long getMusicsInHistory(List<String> musics, long since) throws IOException, ParseException {
		final long[] params = new long[] { since, since };
		String path = "/divanet/personal/playHistory/0";
		while (path != null)
			path = MusicParser.parsePlayHistory(getFrom(path), musics, params);
		return params[1];
	}

	public void updatePublishOrder(List<MusicInfo> musics, int order) throws IOException {
		List<MusicInfo> list = getMusics("/divanet/pv/sort/0/true/0");
		final int lSize = musics.size();
		final int rSize = list.size();
		int lIndex = 0;
		for (; lIndex < lSize && musics.get(lIndex).publish_order >= 0; ++lIndex);

		for (; lIndex < lSize; ++order) {
			MusicInfo m = musics.get(lIndex);
			int rIndex = list.indexOf(m);
			for (; lIndex < lSize && rIndex < rSize && musics.get(lIndex).equals(list.get(rIndex)); ++lIndex, ++rIndex)
				musics.get(lIndex).publish_order = order;
		}
	}

	public void update(MusicInfo music) throws NoLoginException {
		try {
			MusicParser.parseInfoPage(getFrom("/divanet/pv/info/%s/0/0", music.id), music);
		}
		catch (ParseException e) {
			throw new NoLoginException(e);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String[] getVoice(String id) throws IOException {
		try {
			return MusicParser.parseVoice(getFrom("/divanet/module/selectPv/%s/0", id));
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return new String[] { null, null };
	}

	public InputStream download(String path) throws IOException {
		return getFrom(path);
	}

	public void download(String path, OutputStream out) throws IOException {
		InputStream in = getFrom(path);
		byte[] buffer = new byte[1024];
		for (int read; (read = in.read(buffer)) != -1;)
			out.write(buffer, 0, read);
	}

	public boolean cacheContent(String path, File file) {
		if (file.exists())
			return false;

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			download(path, out);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null)
				try { out.close(); } catch (IOException e) {}
		}
		return true;
	}

	public List<Ranking> getRankInList() throws IOException, ParseException {
		List<Ranking> list = new ArrayList<Ranking>();
		String path = "/divanet/ranking/list/0";
		while (path != null)
			path = MusicParser.parseRankingList(getFrom(path), list);

		return list;
	}

	public List<TitleInfo> getTitles(List<TitleInfo> old) throws IOException {
		List<TitleInfo> titles = new ArrayList<TitleInfo>();
		String path = "/divanet/title/selectMain/0";
		while (path != null)
			path = TitleParser.parseTitleList(getFrom(path), titles);

		for (int i = 0; i < titles.size(); ++i) {
			TitleInfo title = titles.get(i);
			int index = old.indexOf(title);
			if (index >= 0)
				titles.set(i, title = old.get(index));

			title.order = i;
			if (title.image_id == null)
				title.image_id = TitleParser.parseTitlePage(getFrom("/divanet/title/confirmMain/%s/0", title.id));
		}

		return titles;
	}

	public List<DecorTitle> getDecorTitles(boolean pre) throws IOException {
		List<DecorTitle> titles = new ArrayList<DecorTitle>();

		// Purchased
		for (String path: TitleParser.parseDecorDir(getFrom("/divanet/title/selectDecorDir/%b", pre), titles)) {
			while (path != null)
				path = TitleParser.parseDecorTitles(getFrom(path), titles);
		}

		// Not purchased
		for (String url: TitleParser.parseDecorShop(getFrom("/divanet/title/decorShop/"))) {
			while (url != null)
				url = TitleParser.parseShopGroup(getFrom(url), titles);
		}

		return titles;
	}

	public List<DecorTitle> getDecorPrize() throws IOException {
		List<DecorTitle> titles = new ArrayList<DecorTitle>();
		TitleParser.parseDecorPrize(getFrom("/divanet/divaTicket/exchange/"), titles);
		return titles;
	}

	public List<ModuleGroup> getModules() throws IOException {
		List<ModuleGroup> modules = ModuleParser.parseModuleIndex(getFrom("/divanet/module/"));
		for (ModuleGroup group: modules) {
			String path = String.format("/divanet/module/list/%d/0", group.id);
			while (path != null)
				path = ModuleParser.parseModuleList(getFrom(path), group.modules);
		}

		return modules;
	}

	public void getModuleDetail(Module module) throws IOException {
		ModuleParser.parseModuleDetail(getFrom("/divanet/module/detail/%s/0/0", module.id), module);
		module.thumbnail = String.format("/divanet/img/moduleTmb/%s", new File(module.image).getName());
	}

	public List<SkinInfo> getSkins() throws IOException {
		List<String> groups = new ArrayList<String>();

		String path = "/divanet/skin/list/COMMON/0/0";
		while (path != null)
			path = SkinParser.parse(getFrom(path), groups);

		List<SkinInfo> skins = new ArrayList<SkinInfo>();
		for (String group_id: groups) {
			InputStream content = getFrom("/divanet/skin/select/COMMON/%s/0/0", group_id);
			SkinParser.parse(content, skins, true);
		}

		return skins;
	}

	public List<SkinInfo> getSkinsFromShop() throws IOException {
		List<String> groups = new ArrayList<String>();

		String path = "/divanet/skin/shop/0";
		while (path != null)
			path = SkinParser.parse(getFrom(path), groups);

		List<SkinInfo> skins = new ArrayList<SkinInfo>();
		for (String group_id: groups) {
			InputStream content = getFrom("/divanet/skin/commodity/%s/0", group_id);
			SkinParser.parse(content, skins, false);
		}

		return skins;
	}

	public List<SkinInfo> getSkinPrize() throws IOException {
		List<String> groups = new ArrayList<String>();
		for (String index: SkinParser.parseExchange(getFrom("/divanet/divaTicket/exchange/"))) {
			SkinParser.parsePrizeGroup(getFrom(index), groups);
		}

		List<SkinInfo> skins = new ArrayList<SkinInfo>();
		for (String group_id: groups) {
			SkinParser.parsePrizeSkin(
					getFrom("/divanet/divaTicket/selectSkin/%s", group_id), group_id, skins);
		}
		return skins;
	}

	public void getSkinDetail(SkinInfo skin) throws IOException {
		final String path = skin.purchased
				? "/divanet/skin/confirm/COMMON/%s/%s/0/0"
				: skin.prize
					? "/divanet/divaTicket/confirmExchangeSkin/%1$s"
					: "/divanet/skin/detail/%s/%s/0";
		SkinParser.parse(getFrom(path, skin.id, skin.group_id), skin);
	}

	public String getShopDetail(String path, List<NameValuePair> details) throws IOException {
		return ShopParser.parse(getFrom(path), details);
	}

	public List<ButtonSE> getButtonSEs(String music_id) throws IOException {
		List<ButtonSE> ses = new ArrayList<ButtonSE>();
		String path = String.format("/divanet/buttonSE/list/%s/0/0", music_id);
		while (path != null)
			path = SEParser.parse(getFrom(path), ses);

		Map<String, String> samples = getSESamples(music_id);
		for (ButtonSE se: ses)
			se.sample = samples.get(se.name);

		return ses;
	}

	private Map<String, String> getSESamples(String music_id) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		String path = String.format("/divanet/buttonSE/sample/%s/0/0", music_id);
		while (path != null)
			path = SEParser.parse(getFrom(path), map);
		return map;
	}

	public Map<String, IndividualSetting> getIndividualSettings() throws IOException {
		Map<String, IndividualSetting> settings = new HashMap<String, IndividualSetting>();
		String path = "/divanet/setting/individual/0/true/0";
		while (path != null)
			path = Parser.parseIndividualSettings(getFrom(path), settings);
		return settings;
	}

	public List<String> getDIVARecords() throws IOException {
		List<String> records = new ArrayList<String>();
		String path = "/divanet/record/list/0";
		while (path != null)
			path = RecordParser.parseList(getFrom(path), records);
		return records;
	}

	public boolean checkDIVARecord() throws IOException {
		return RecordParser.parseResult(getFrom("/divanet/record/check/"));
	}

	private InputStream getFrom(String relative, Object... args) throws IOException {
		return getFrom(String.format(relative, args));
	}

	private synchronized InputStream getFrom(String relative) throws IOException {
		HttpResponse response = m_client.execute(new HttpGet(DdN.URL.resolve(relative)));
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException();

		access();
		return response.getEntity().getContent();
	}

	private HttpResponse postTo(String relative) throws IOException {
		return postTo(relative, (HttpEntity)null);
	}

	private synchronized HttpResponse postTo(String relative, HttpEntity entity) throws IOException {
		HttpPost request = new HttpPost(DdN.URL.resolve(relative));
		if (entity != null)
			request.setEntity(entity);

		HttpResponse response = m_client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException();

		access();
		return response;
	}

	private HttpResponse postTo(String relative, String... pairs) throws IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(pairs.length/2);
		for (int i = 0; i < pairs.length; i += 2)
			params.add(new BasicNameValuePair(pairs[i], pairs[i+1]));

		return postTo(relative, new UrlEncodedFormEntity(params, "UTF-8"));
	}

	public void rename(String name) throws OperationFailedException, IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);

		HttpResponse response = postTo("/divanet/personal/confirmUpdateName/", "name", name);
		String error = Parser.parseRenameResult(response.getEntity().getContent(), params);
		if (error != null)
			throw new OperationFailedException(error);

		postTo("/divanet/personal/updateName/", new UrlEncodedFormEntity(params, "UTF-8"));
	}

	public String setTitle(String title_id, boolean noDecor) throws IOException, OperationFailedException {
		HttpResponse response = postTo(String.format("/divanet/title/updateMain/%s/%b", title_id, noDecor));
		String title = TitleParser.parseSetResult(response.getEntity().getContent());
		if (title == null)
			throw new OperationFailedException();
		return title;
	}

	public void setDecorTitle(String decor_id, boolean pre) throws IOException {
		postTo(String.format("/divanet/title/updateDecor/%b/%s", pre, decor_id));
	}

	public void setTitleReplace(boolean on) throws IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("overwrite", String.valueOf(on)));
		postTo("/divanet/title/updateReplaceSetting/", new UrlEncodedFormEntity(params, "UTF-8"));
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
		postTo(String.format("/divanet/module/resetIndividual/%s/0", music_id));
	}

	public void resetIndividualAll() throws IOException {
		postTo("/divanet/setting/resetIndividualAll/");
	}

	public void activateIndividual(String[] keys, boolean[] values) throws IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(keys.length);
		for (int i = 0; i < keys.length; ++i) {
			if (values[i])
				params.add(new BasicNameValuePair(keys[i], "on"));
		}
		postTo("/divanet/setting/updateConfig/", new UrlEncodedFormEntity(params, "US-ASCII"));
	}

	public void setSkin(String group_id, String skin_id) throws IOException {
		setSkin("COMMON", group_id, skin_id);
	}

	public void unsetSkin() throws IOException {
		resetSkin("COMMON");
	}

	public void setSkinNoUse(String music_id) throws IOException {
		postTo(String.format("/divanet/skin/noUse/%s/0/0", music_id));
	}

	public void setSkin(String music_id, String group_id, String skin_id) throws IOException {
		postTo(String.format("/divanet/skin/update/%s/%s/%s/0", music_id, skin_id, group_id));
	}

	public void resetSkin(String music_id) throws IOException {
		postTo(String.format("/divanet/skin/unset/%s/0/0", music_id));
	}

	public void setButtonSE(String music_id, String se_id) throws IOException {
		postTo(String.format("/divanet/buttonSE/update/%s/%s/0/0", music_id, se_id));
	}

	public void setButtonSEInvalidateCommon(String music_id) throws IOException {
		postTo(String.format("/divanet/buttonSE/invalidateCommonSetting/%s/0/0", music_id));
	}

	public void resetButtonSE(String music_id) throws IOException {
		postTo(String.format("/divanet/buttonSE/unset/%s/0/0", music_id));
	}

	private int checkShopResult(HttpResponse response) throws OperationFailedException, IOException {
		int[] vp = new int[1];
		if (!ShopParser.isSuccess(response.getEntity().getContent(), vp))
			throw new OperationFailedException();
		return vp[0];
	}

	public int buyModule(String id) throws OperationFailedException, IOException {
		return checkShopResult(postTo(String.format("/divanet/module/buy/%s", id)));
	}

	public int buySkin(String group_id, String skin_id) throws OperationFailedException, IOException {
		return checkShopResult(postTo(String.format("/divanet/skin/buy/%s/%s", skin_id, group_id)));
	}

	public int buyDecorTitle(String decor_id) throws OperationFailedException, IOException {
		return checkShopResult(postTo(String.format("/divanet/title/buyDecor/%s", decor_id)));
	}

	private int checkExchangeResult(HttpResponse response) throws OperationFailedException, IOException {
		int[] ticket = new int[1];
		if (!Parser.isSuccessExchange(response.getEntity().getContent(), ticket))
			throw new OperationFailedException();
		return ticket[0];
	}

	public int exchangeSkin(String id) throws IOException, OperationFailedException {
		return checkExchangeResult(postTo(String.format("/divanet/divaTicket/exchangeSkin/%s", id)));
	}

	public int exchangeDecorTitle(String id) throws IOException, OperationFailedException {
		return checkExchangeResult(postTo(String.format("/divanet/divaTicket/exchangeTitle/%s", id)));
	}

	public MyList getMyList(int id) throws IOException {
		final MyList myList = new MyList(id, null);
		MyListParser.parseSummary(getFrom("/divanet/myList/selectMyList/%d", id), myList);
		return myList;
	}

	public List<String> getMyListEntries(int id) throws IOException {
		List<String> ids = new ArrayList<String>();
		MyListParser.parseList(getFrom("/divanet/myList/edit/%d/delete", id), ids);
		return ids;
	}

	public void renameMyList(int id, String newName) throws IOException, OperationFailedException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("name", newName));
		final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		HttpResponse response = postTo(String.format("/divanet/myList/updateName/%d", id), entity);
		String error = Parser.parseRenameResult(response.getEntity().getContent(), null);
		if (error != null)
			throw new OperationFailedException(error);
	}

	public void activateMyList(int id) throws IOException, OperationFailedException {
		HttpResponse response = postTo(String.format("/divanet/myList/activate/%d", id));
		String error = MyListParser.parseActivateResult(response.getEntity().getContent());
		if (error != null)
			throw new OperationFailedException(error);
	}

	public void addToMyList(int id, String music_id) throws IOException {
		postTo(String.format("/divanet/myList/update/%d/%s/0", id, music_id));
	}

	public void removeFromMyList(int id, String music_id) throws IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("cryptoPvIdList", music_id));
		postTo(String.format("/divanet/myList/deletePv/%d/true", id), new UrlEncodedFormEntity(params, "UTF-8"));
	}

	public void deleteMyList(int id) throws IOException {
		postTo(String.format("/divanet/myList/deleteMyList/%d", id));
	}

	public long[] getHistory(List<String> newHistorys, long since, long score) throws IOException, ParseException {
		final long[] params = new long[] { since, since, score, score };
		String path = "/divanet/personal/playHistory/0";
		while (path != null)
			path = HistoryParser.parsePlayHistory(getFrom(path), newHistorys, params);

		long[] ret = new long[]{params[1], params[3]};

		return ret;
	}

	public History getHistoryDetail(String historyId) throws NoLoginException {
		History history = null;
		try {
			history = HistoryParser.parseHistoryDetail(getFrom("/divanet/personal/playHistoryDetail/%s/0", historyId));
		}
		catch (ParseException e) {
			throw new NoLoginException(e);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return history;
	}

	public String checkResultPicture(History history) throws IOException, ParseException {
		String[] id = new String[]{null};

		String path = "/divanet/personal/playHistory/0";
		while (path != null){
			path = HistoryParser.parsePlayHistoryForResultPicture(getFrom(path), history, id);
		}

		return id[0];
	}

	public String preBuyingResultPicture(String id, String[] values) throws IOException, ParseException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("size", values[0]));
		params.add(new BasicNameValuePair("quality", values[1]));
		if(values[2] != null)
			params.add(new BasicNameValuePair("dispPlayerName", values[2]));
		if(values[3] != null)
			params.add(new BasicNameValuePair("dispPlaceName", values[3]));
		HttpResponse response = postTo(String.format("/divanet/image/confirmResult/%d/0", Integer.valueOf(id)), new UrlEncodedFormEntity(params, "UTF-8"));
		String token = ResultPictureParser.parseToken(response.getEntity().getContent());

		return token;
	}

	public String buyResultPicture(String id, String token, String[] values) throws IOException, ParseException {
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("org.apache.struts.taglib.html.TOKEN", token));
		params.add(new BasicNameValuePair("size", values[0]));
		params.add(new BasicNameValuePair("quality", values[1]));
		params.add(new BasicNameValuePair("dispPlayerName", String.valueOf(values[2] != null)));
		params.add(new BasicNameValuePair("dispPlaceName", String.valueOf(values[3] != null)));
		postTo(String.format("/divanet/image/buyResult/%d/0", Integer.valueOf(id)), new UrlEncodedFormEntity(params, "UTF-8"));

		return String.format("/divanet/image/viewResult/%d", Integer.valueOf(id));
	}

	public HttpResponse downloadByPost(String relative) throws IOException {
		return postTo(relative);
	}
}
