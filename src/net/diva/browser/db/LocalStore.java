package net.diva.browser.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.diva.browser.R;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.DecorTitle;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.util.StringUtils;
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
	private static final int VERSION = 19;

	private static LocalStore m_instance;

	public static LocalStore instance(Context context) {
		if (m_instance == null)
			m_instance = new LocalStore(context.getApplicationContext());
		return m_instance;
	}

	private SQLiteOpenHelper m_helper;
	private Map<String, String> m_readingMap;

	private LocalStore(Context context) {
		super(context);
		m_helper = new OpenHelper(context, DATABASE_NAME, null, VERSION);
	}

	public String getReading(String title) {
		if (m_readingMap == null) {
			Map<String, String> map = new HashMap<String, String>();
			String[] items = getResources().getStringArray(R.array.music_name_map);
			for (int i = 0; i < items.length; i += 2)
				map.put(items[i], items[i+1]);
			m_readingMap = map;
		}
		String reading = m_readingMap.get(title);
		return reading != null ? reading : "";
	}

	public PlayRecord load(String access_code) {
		PlayRecord record = new PlayRecord();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		record.player_name = prefs.getString("player_name", null);
		record.level = prefs.getString("level_rank", null);
		record.title = prefs.getString("title", null);
		record.vocaloid_point = prefs.getInt("vocaloid_point", 0);
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
				MusicTable.READING,
				MusicTable.FAVORITE,
				MusicTable.PUBLISH,
				MusicTable.SKIN,
				MusicTable.BUTTON,
		}, null, null, null, null, MusicTable._ID);
		try {
			while (cm.moveToNext()) {
				MusicInfo music = new MusicInfo(cm.getString(0), cm.getString(1));
				music.coverart = cm.getString(2);
				music.part = cm.getInt(3);
				music.vocal1 = cm.getString(4);
				music.vocal2 = cm.getString(5);
				music.skin = cm.getString(9);
				music.button = cm.getString(10);
				music.reading = cm.getString(6);
				if (music.reading == null)
					music.reading = getReading(music.title);
				music.ordinal = StringUtils.forLexicographical(music.reading);
				music.publish_order = cm.getInt(8);
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
				ScoreTable.SATURATION,
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
				score.saturation = cs.isNull(8) ? 0 : cs.getInt(8);

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
		insert(record.musics);
		update(record);
	}

	public void insert(List<MusicInfo> musics) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (MusicInfo music: musics) {
				if (MusicTable.update(db, music)) {
					for (int i = 0; i < music.records.length; ++i)
						ScoreTable.update(db, music.id, i, music.records[i]);
				}
				else {
					music.reading = getReading(music.title);
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
	}

	public void update(PlayRecord record) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString("player_name", record.player_name);
		editor.putString("level_rank", record.level);
		editor.putString("title", record.title);
		editor.putInt("vocaloid_point", record.vocaloid_point);
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

	public void updateSaturation(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (int i = 0; i < music.records.length; ++i)
				ScoreTable.updateSaturation(db, music.id, i, music.records[i]);
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

	public void resetIndividualAll() {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.resetIndividualAll(db);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateSkin(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.updateSkin(db, music);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateButtonSE(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.updateButtonSE(db, music);
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

	public List<DecorTitle> getDecorTitles(boolean pre) {
		List<DecorTitle> titles = new ArrayList<DecorTitle>();
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(DecorTitleTable.TABLE_NAME, new String[] {
				DecorTitleTable.ID,
				DecorTitleTable.NAME,
				DecorTitleTable.STATUS,
		}, null, null, null, null, null);
		try {
			while (c.moveToNext())
				titles.add(new DecorTitle(c.getString(0), c.getString(1), c.getInt(2) != 0));
		}
		finally {
			c.close();
		}
		return titles;
	}

	public void updateDecorTitles(List<DecorTitle> titles) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (DecorTitle title: titles) {
				if (!DecorTitleTable.update(db, title))
					DecorTitleTable.insert(db, title);
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateDecorTitle(DecorTitle title) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			DecorTitleTable.update(db, title);
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

	public List<ButtonSE> loadButtonSEs() {
		List<ButtonSE> buttonSEs = new ArrayList<ButtonSE>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(ButtonSETable.TABLE_NAME, new String[] {
				ButtonSETable.ID,
				ButtonSETable.NAME,
				ButtonSETable.SAMPLE,
		}, null, null, null, null, ButtonSETable._ID);
		try {
			while (c.moveToNext()) {
				ButtonSE se = new ButtonSE(c.getString(0), c.getString(1));
				se.sample = c.getString(2);
				buttonSEs.add(se);
			}
		}
		finally {
			c.close();
		}

		return buttonSEs;
	}

	public void updateButtonSEs(List<ButtonSE> buttonSEs) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (ButtonSE se: buttonSEs) {
				if (!ButtonSETable.update(db, se))
					ButtonSETable.insert(db, se);
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public List<MyList> loadMyLists() {
		List<MyList> mylists = new ArrayList<MyList>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(MyListTable.TABLE_NAME, new String[] {
				MyListTable.ID,
				MyListTable.NAME,
		}, null, null, null, null, MyListTable.ID);
		try {
			while (c.moveToNext())
				mylists.add(new MyList(c.getInt(0), c.getString(1)));
		}
		finally {
			c.close();
		}

		return mylists;
	}

	public List<String> loadMyList(int id) {
		List<String> mylist = new ArrayList<String>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(MyListEntryTable.TABLE_NAME, new String[] {
				MyListEntryTable.MUSIC_ID,
		}, MyListEntryTable.WHERE_BY_ID, new String[] { String.valueOf(id) },
		null, null, MyListEntryTable.NUMBER);
		try {
			while (c.moveToNext())
				mylist.add(c.getString(0));
		}
		finally {
			c.close();
		}

		return mylist;
	}

	public boolean[] containedInMyLists(List<MyList> myLists, String music_id) {
		final int size = myLists.size();
		boolean[] values = new boolean[size];

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(MyListEntryTable.TABLE_NAME, new String[] {
				MyListEntryTable.ID,
		}, MyListEntryTable.WHERE_BY_MUSIC, new String[] { music_id },
		null, null, MyListEntryTable.ID);
		try {
			while (c.moveToNext()) {
				int id = c.getInt(0);
				for (int i = 0; i < size; ++i) {
					if (myLists.get(i).id == id) {
						values[i] = true;
						break;
					}
				}
			}
		}
		finally {
			c.close();
		}

		return values;
	}

	public void updateMyList(MyList myList) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MyListTable.update(db, myList.id, myList.name);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void clearMyList(int id) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MyListEntryTable.clear(db, id);
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public void updateMyList(int id, List<String> ids) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MyListEntryTable.clear(db, id);
			for (int i = 0; i < ids.size(); ++i)
				MyListEntryTable.update(db, id, i, ids.get(i));
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	public int getActiveMyList() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getInt("active_mylist", -1);
	}

	public void activateMyList(int id) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putInt("active_mylist", id);
		editor.commit();
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
			db.execSQL(ButtonSETable.create_statement());
			db.execSQL(MyListTable.create_statement());
			db.execSQL(MyListEntryTable.create_statement());
			initializeMyList(db);
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
			case 9:
				db.execSQL(String.format("DELETE FROM %s;", SkinTable.TABLE_NAME));
			case 10:
				MusicTable.addReadingColumn(db);
			case 11:
				MusicTable.addFavoriteColumn(db);
			case 12:
				ScoreTable.addSaturationColumn(db);
			case 13:
				db.execSQL(String.format("UPDATE %s SET %s = null;", MusicTable.NAME, MusicTable.READING));
			case 14:
				db.execSQL(ButtonSETable.create_statement());
			case 15:
				db.execSQL(MyListTable.create_statement());
				db.execSQL(MyListEntryTable.create_statement());
				initializeMyList(db);
			case 16:
				MusicTable.addPublishColumn(db);
			case 17:
				MusicTable.addIndividualColumns(db);
			case 18:
				db.execSQL(DecorTitleTable.create_statement());
			default:
				break;
			}
		}

		private void initializeMyList(SQLiteDatabase db) {
			for (int i = 0; i < 5; ++i) {
				MyListTable.insert(db, i, String.format("マイリスト%d", i+1));
				for (int j = 0; j < 20; ++j)
					MyListEntryTable.insert(db, i, j, null);
			}
		}
	}
}
