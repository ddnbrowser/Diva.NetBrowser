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

public class UpdateSaturaionPoints extends ProgressTask<Void, Void, String> {
	private static final URI PAGE_URI = URI.create("http://www31.atwiki.jp/projectdiva_ac/pages/222.html");
	private static final Pattern REGEX = Pattern.compile(buildRegex());

	private static String buildRegex() {
		final String title = ">([^>]+)</a></td>";
		final String saturation = "\\s*<!--\\d+-\\d+--><td style=\"\">((\\d+).(\\d\\d))?(.*)?</td>";
		StringBuilder b = new StringBuilder();
		b.append(title);
		for (int i = 0; i < 4; ++i)
			b.append(saturation);
		return b.toString();
	}

	public UpdateSaturaionPoints(Context context) {
		super(context, R.string.message_update_saturations);
	}

	@Override
	protected String doInBackground(Void... args) {
		try {
			List<MusicInfo> updated = parse(read(PAGE_URI), musicMap());
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
		Matcher m = REGEX.matcher(contents);
		while (m.find()) {
			MusicInfo music = map.get(m.group(1));
			if (music == null)
				continue;

			for (int i = 0; i < 4; ++i) {
				final int base = 1 + i * 4;
				final ScoreRecord score = music.records[i];
				if (score == null || m.group(base+1) == null)
					continue;

				score.saturation = Integer.valueOf(m.group(base+2) + m.group(base+3));
			}
			updated.add(music);
		}
		return updated;
	}
}
