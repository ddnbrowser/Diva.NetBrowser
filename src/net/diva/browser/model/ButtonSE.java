package net.diva.browser.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;

public class ButtonSE {
	public static final int COUNT = 4;
	public static final String UNSUPPORTED = "<>";
	public static final String INVALIDATE_COMMON = "INVALIDATE_COMMON";

	public int type;
	public String id;
	public String name;
	public String sample;

	public ButtonSE(String id_, String name_) {
		id = id_;
		name = name_;
	}

	public File getSamplePath(Context context) {
		if (sample == null)
			return null;
		File directory = new File(context.getDir("button_se", Context.MODE_WORLD_READABLE),String.valueOf(type));
		if (!directory.exists())
			directory.mkdirs();
		return new File(directory, new File(sample).getName());
	}

	public static class Player {
		private Context m_context;
		private MediaPlayer m_player;

		public Player(Context context) {
			m_context = context;
			m_player = new MediaPlayer();
		}

		public void play(ButtonSE se) {
			File file = se.getSamplePath(m_context);
			m_player.reset();
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				m_player.setDataSource(in.getFD());
				m_player.prepare();
				m_player.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				if (in != null)
					try { in.close(); } catch (IOException e) {}
			}
		}
	}
}
