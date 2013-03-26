package net.diva.browser.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.util.ProgressTask;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.widget.Toast;

public class UpdateSaturaionPoints extends ProgressTask<String, Void, String> {
	public static final String WIKI_URL = "http://www31.atwiki.jp/projectdiva_ac/pages/222.html";
	public static final String DECO_URL = "http://deco19.ddo.jp/data/score.dat";
	public static final String[] SOURCE_URLS = new String[] { WIKI_URL, DECO_URL };
	private String accessUrl;

	private String buildRegex() {
		if(WIKI_URL.equals(accessUrl)){
			String title = ">([^>]+)</a></td>";
			String saturation = "\\s*<!--\\d+-\\d+--><td style=\"\">((\\d+).(\\d\\d))?(.*)?</td>";
			StringBuilder b = new StringBuilder();
			b.append(title);
			for (int i = 0; i < 4; ++i)
				b.append(saturation);
			return b.toString();

		}else if(DECO_URL.equals(accessUrl)){
			String regixStr = "\\d+,(.+?),\\d+?,(.+?),\\d+?,(.+?),\\d+?,(.+?),\\d+?#(.+)";
			return regixStr;
		}
		return "";
	}

	public UpdateSaturaionPoints(Context context) {
		super(context, R.string.message_update_saturations);
	}

	@Override
	protected String doInBackground(String... args) {
		if(args.length != 1)
			return null;
		try {
			accessUrl = args[0];
			URI uri = URI.create(accessUrl);
			final LocalStore store = DdN.getLocalStore();
			for (MusicInfo music: parse(read(uri), musicMap()))
				store.updateSaturation(music);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onResult(String result) {
		super.onResult(result);
		if (result != null)
			Toast.makeText(m_progress.getContext(), result, Toast.LENGTH_SHORT).show();
	}

	private String read(URI uri) throws IOException {
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(new HttpGet(uri));
		final int status = response.getStatusLine().getStatusCode();
		if (status != HttpStatus.SC_OK)
			throw new IOException(String.format("Invalid Server Response: %d", status));

		String charset = findCharset(response.getFirstHeader("Content-Type"));

		InputStream in = null;
		try {
			in = response.getEntity().getContent();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int read; (read = in.read(buffer)) != -1;)
				out.write(buffer, 0, read);
			return out.toString(charset);
		}
		finally {
			if (in != null)
				in.close();
		}
	}

	private String findCharset(Header header) {
		for (HeaderElement e: header.getElements()) {
			NameValuePair pair = e.getParameterByName("charset");
			if (pair != null)
				return pair.getValue();
		}
		return "UTF-8";
	}

	private Map<String, MusicInfo> musicMap() {
		List<MusicInfo> musics = DdN.getPlayRecord().musics;
		Map<String, MusicInfo> map = new HashMap<String, MusicInfo>(musics.size());
		for (MusicInfo m: musics)
			map.put(m.title, m);
		return map;
	}

	private List<MusicInfo> parse(String contents, Map<String, MusicInfo> map) {
		List<MusicInfo> updated = new ArrayList<MusicInfo>();
		Pattern regix = Pattern.compile(buildRegex());
		Matcher m = regix.matcher(contents);
		while (m.find()) {
			if (WIKI_URL.equals(accessUrl)) {
				MusicInfo music = map.get(m.group(1));
				if (music == null)
					continue;

				for (int i = 0; i < 4; ++i) {
					final int base = 1 + i * 4;
					final ScoreRecord score = music.records[i];
					if (score == null || m.group(base + 1) == null)
						continue;

					score.saturation = Integer.valueOf(m.group(base + 2) + m.group(base + 3));
				}
				updated.add(music);

			} else if (DECO_URL.equals(accessUrl)) {
				String title = m.group(5);
				MusicInfo music = map.get(title);
				if (music == null)
					continue;

				for (int i = 0; i < 4; ++i) {
					final ScoreRecord score = music.records[i];
					final String satu = m.group(i + 1);
					if (score == null || satu == null || "0.00".equals(satu))
						continue;

					score.saturation = Integer.valueOf(satu.replace(".", ""));
				}
				updated.add(music);
			}
		}
		return updated;
	}
}
