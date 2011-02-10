package net.diva.browser.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;
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
	private static final int VERSION = 9;

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
				MusicTable.PART,
				MusicTable.VOCAL1,
				MusicTable.VOCAL2,
		}, null, null, null, null, MusicTable._ID);
		try {
			while (cm.moveToNext()) {
				MusicInfo music = new MusicInfo(cm.getString(0), cm.getString(1));
				music.coverart = cm.getString(2);
				music.part = cm.getInt(3);
				music.vocal1 = cm.getString(4);
				music.vocal2 = cm.getString(5);
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

	public List<ModuleGroup> loadModules() {
		List<ModuleGroup> groups = new ArrayList<ModuleGroup>();
		Map<Integer, ModuleGroup> id2group = new HashMap<Integer, ModuleGroup>();

		SQLiteDatabase db = m_helper.getReadableDatabase();

		Cursor cg = db.query(ModuleGroupTable.TABLE_NAME, new String[] {
				ModuleGroupTable.ID,
				ModuleGroupTable.NAME,
		}, null, null, null, null, ModuleGroupTable._ID);
		try {
			while (cg.moveToNext()) {
				ModuleGroup group = new ModuleGroup(cg.getInt(0), cg.getString(1));
				groups.add(group);
				id2group.put(group.id, group);
			}
		}
		finally {
			cg.close();
		}

		Cursor cm = db.query(ModuleTable.TABLE_NAME, new String[] {
				ModuleTable.ID,
				ModuleTable.NAME,
				ModuleTable.STATUS,
				ModuleTable.GROUP_ID,
				ModuleTable.IMAGE,
				ModuleTable.THUMBNAIL,
		}, null, null, null, null, ModuleTable._ID);
		try {
			while (cm.moveToNext()) {
				Module module = new Module();
				module.id = cm.getString(0);
				module.name = cm.getString(1);
				module.purchased = cm.getInt(2) == 1;
				module.image = cm.getString(4);
				module.thumbnail = cm.getString(5);

				ModuleGroup group = id2group.get(cm.getInt(3));
				if (group != null)
					group.modules.add(module);
			}
		}
		finally {
			cm.close();
		}

		return groups;
	}

	public void insert(PlayRecord record) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (MusicInfo music: record.musics) {
				if (MusicTable.update(db, music)) {
					for (int i = 0; i < music.records.length; ++i)
						ScoreTable.update(db, music.id, i, music.records[i]);
				}
				else {
					MusicTable.insert(db, music);
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
			MusicTable.update(db, music);
			for (int i = 0; i < music.records.length; ++i)
				ScoreTable.update(db, music.id, i, music.records[i]);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateModule(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.updateModule(db, music);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void resetIndividualModules() {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.resetModule(db);
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

	public List<TitleInfo> getTitles() {
		List<TitleInfo> titles = new ArrayList<TitleInfo>();
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(TitleTable.NAME, new String[] {
				TitleTable.ID,
				TitleTable.TITLE,
				TitleTable.IMAGE_ID,
		}, null, null, null, null, null);
		try {
			while (c.moveToNext()) {
				TitleInfo title = new TitleInfo(c.getString(0), c.getString(1));
				if (!c.isNull(2))
					title.image_id = c.getString(2);
				titles.add(title);
			}
		}
		finally {
			c.close();
		}
		return titles;
	}

	public void updateTitles(List<TitleInfo> titles) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (TitleInfo title: titles) {
				if (!TitleTable.update(db, title))
					TitleTable.insert(db, title);
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateModules(List<ModuleGroup> groups) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (ModuleGroup group: groups) {
				ModuleGroupTable.insert(db, group);
				for (Module module: group.modules) {
					if (!ModuleTable.update(db, group.id, module))
						ModuleTable.insert(db, group.id, module);
				}
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateModule(Module module) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			ModuleTable.update(db, module);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public List<SkinInfo> loadSkins() {
		List<SkinInfo> skins = new ArrayList<SkinInfo>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(SkinTable.TABLE_NAME, new String[] {
				SkinTable.GROUP_ID,
				SkinTable.ID,
				SkinTable.NAME,
				SkinTable.PATH,
				SkinTable.STATUS,
		}, null, null, null, null, SkinTable._ID);
		try {
			while (c.moveToNext()) {
				SkinInfo skin = new SkinInfo(c.getString(0), c.getString(1), c.getString(2), c.getInt(4) == 1);
				skin.image_path = c.getString(3);
				skins.add(skin);
			}
		}
		finally {
			c.close();
		}

		return skins;
	}

	public void updateSkins(List<SkinInfo> skins) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (SkinInfo skin: skins) {
				if (!SkinTable.update(db, skin))
					SkinTable.insert(db, skin);
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateSkin(SkinInfo skin) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			SkinTable.update(db, skin);
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
			db.execSQL(ModuleGroupTable.create_statement());
			db.execSQL(ModuleTable.create_statement());
			db.execSQL(SkinTable.create_statement());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			switch (oldVersion) {
			case 1:
				ScoreTable.addRankingColumns(db);
			case 2:
				db.execSQL(TitleTable.create_statement());
			case 3:
				db.execSQL(ModuleGroupTable.create_statement());
				db.execSQL(ModuleTable.create_statement());
			case 4:
				MusicTable.addModuleColumns(db);
			case 5:
				TitleTable.addImageIDColumn(db);
			case 6:
				db.execSQL(SkinTable.create_statement());
			case 7:
				ModuleTable.addImagePathColumn(db);
			case 8:
				SkinTable.addStatusColumn(db);
			default:
				break;
			}
		}
	}
}
