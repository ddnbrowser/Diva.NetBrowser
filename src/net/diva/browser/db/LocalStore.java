package net.diva.browser.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import net.diva.browser.MusicInfo;
import net.diva.browser.PlayRecord;
import net.diva.browser.Ranking;
import net.diva.browser.ScoreRecord;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.preference.PreferenceManager;

public class LocalStore extends ContextWrapper {
	private static final String DATABASE_NAME = "diva.db";
	private static final int VERSION = 3;

	private static LocalStore m_instance;

	public static LocalStore instance(Context context) {
		if (m_instance == null)
			m_instance = new LocalStore(context.getApplicationContext());
		return m_instance;
	}

	private SQLiteOpenHelper m_helper;

	private LocalStore(Context context) {
		super(context);
		m_helper = new OpenHelper(context, DATABASE_NAME, null, VERSION);
	}

	public PlayRecord load(String access_code) {
		PlayRecord record = new PlayRecord();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		record.player_name = prefs.getString("player_name", null);
		record.level = prefs.getString("level_rank", null);
		record.title_id = prefs.getString("title_id", null);
		record.musics = findMusic();
		return record;
	}

	private List<MusicInfo> findMusic() {
		List<MusicInfo> musics = new ArrayList<MusicInfo>();
		Map<String, MusicInfo> id2music = new HashMap<String, MusicInfo>();

		SQLiteDatabase db = m_helper.getReadableDatabase();

		Cursor cm = db.query(MusicTable.NAME, new String[] {
				MusicTable.ID,
				MusicTable.TITLE,
				MusicTable.COVERART,
		}, null, null, null, null, MusicTable._ID);
		try {
			while (cm.moveToNext()) {
				MusicInfo music = new MusicInfo(cm.getString(0), cm.getString(1));
				music.coverart = cm.getString(2);
				musics.add(music);
				id2music.put(music.id, music);
			}
		}
		finally {
			cm.close();
		}

		Cursor cs = db.query(ScoreTable.NAME, new String[] {
				ScoreTable.MUSIC_ID,
				ScoreTable.RANK,
				ScoreTable.DIFFICULTY,
				ScoreTable.CLEAR_STATUS,
				ScoreTable.TRIAL_STATUS,
				ScoreTable.HIGH_SCORE,
				ScoreTable.ACHIEVEMENT,
				ScoreTable.RANKING,
		}, null, null, null, null, null);
		try {
			while (cs.moveToNext()) {
				ScoreRecord score = new ScoreRecord();
				score.difficulty = cs.getInt(2);
				score.clear_status = cs.getInt(3);
				score.trial_status = cs.getInt(4);
				score.high_score = cs.getInt(5);
				score.achievement = cs.getInt(6);
				score.ranking = cs.isNull(7) ? -1 : cs.getInt(7);

				MusicInfo music = id2music.get(cs.getString(0));
				if (music != null)
					music.records[cs.getInt(1)] = score;
			}
		}
		finally {
			cs.close();
		}

		return musics;
	}

	public void insert(PlayRecord record) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (MusicInfo music: record.musics) {
				if (MusicTable.insert(db, music) < 0) {
					for (int i = 0; i < music.records.length; ++i)
						ScoreTable.update(db, music.id, i, music.records[i]);
				}
				else {
					for (int i = 0; i < music.records.length; ++i)
						ScoreTable.insert(db, music.id, i, music.records[i]);
				}
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}

		update(record);
	}

	public void update(PlayRecord record) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString("player_name", record.player_name);
		editor.putString("level_rank", record.level);
		editor.putString("title_id", record.title_id);
		editor.commit();
	}

	public void update(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (int i = 0; i < music.records.length; ++i)
				ScoreTable.update(db, music.id, i, music.records[i]);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void update(List<Ranking> list) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ScoreTable.clearRanking(db);
			for (Ranking entry: list)
				ScoreTable.update(db, entry);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public List<NameValuePair> getTitles() {
		List<NameValuePair> titles = new ArrayList<NameValuePair>();
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(TitleTable.NAME, new String[] {
				TitleTable.ID,
				TitleTable.TITLE,
		}, null, null, null, null, null);
		try {
			while (c.moveToNext())
				titles.add(new BasicNameValuePair(c.getString(0), c.getString(1)));
		}
		finally {
			c.close();
		}
		return titles;
	}

	public void updateTitles(List<NameValuePair> titles) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (NameValuePair pair: titles)
				TitleTable.insert(db, pair);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	private static class OpenHelper extends SQLiteOpenHelper {
		public OpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(MusicTable.create_statement());
			db.execSQL(ScoreTable.create_statement());
			db.execSQL(TitleTable.create_statement());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			switch (oldVersion) {
			case 1:
				ScoreTable.addRankingColumns(db);
			case 2:
				db.execSQL(TitleTable.create_statement());
			default:
				break;
			}
		}
	}
}
