package net.diva.browser.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.diva.browser.R;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.CustomizeItem;
import net.diva.browser.model.DecorTitle;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.Role;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.util.StringUtils;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.SparseArray;

public class LocalStore extends ContentProvider {
	private static final String DATABASE_NAME = "diva.db";
	private static final int VERSION = 25;

	private static LocalStore m_instance;

	public static LocalStore instance(Context context) {
		return m_instance;
	}

	private SQLiteOpenHelper m_helper;
	private Map<String, String> m_readingMap;

	private SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	public String getReading(String title) {
		if (m_readingMap == null) {
			Map<String, String> map = new HashMap<String, String>();
			String[] items = getContext().getResources().getStringArray(R.array.music_name_map);
			for (int i = 0; i < items.length; i += 2)
				map.put(items[i], items[i+1]);
			m_readingMap = map;
		}
		String reading = m_readingMap.get(title);
		return reading != null ? reading : "";
	}

	public PlayRecord load(String access_code) {
		PlayRecord record = new PlayRecord();
		SharedPreferences prefs = getPreferences();
		record.player_name = prefs.getString("player_name", null);
		record.level = prefs.getString("level_rank", null);
		record.title = prefs.getString("title", null);
		record.vocaloid_point = prefs.getInt("vocaloid_point", 0);
		record.ticket = prefs.getInt("ticket", 0);
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
				MusicTable.READING,
				MusicTable.PUBLISH,
				MusicTable.PART,
				MusicTable.ROLE1,
				MusicTable.CAST1,
				MusicTable.MODULE1,
				MusicTable.MODULE1_ITEMS[0],
				MusicTable.MODULE1_ITEMS[1],
				MusicTable.MODULE1_ITEMS[2],
				MusicTable.MODULE1_ITEMS[3],
				MusicTable.ROLE2,
				MusicTable.CAST2,
				MusicTable.MODULE2,
				MusicTable.MODULE2_ITEMS[0],
				MusicTable.MODULE2_ITEMS[1],
				MusicTable.MODULE2_ITEMS[2],
				MusicTable.MODULE2_ITEMS[3],
				MusicTable.ROLE3,
				MusicTable.CAST3,
				MusicTable.MODULE3,
				MusicTable.MODULE3_ITEMS[0],
				MusicTable.MODULE3_ITEMS[1],
				MusicTable.MODULE3_ITEMS[2],
				MusicTable.MODULE3_ITEMS[3],
				MusicTable.SKIN,
				MusicTable.SOUNDS[0],
				MusicTable.SOUNDS[1],
				MusicTable.SOUNDS[2],
				MusicTable.SOUNDS[3],
		}, null, null, null, null, MusicTable._ID);
		try {
			while (cm.moveToNext()) {
				MusicInfo music = new MusicInfo(cm.getString(0), cm.getString(1));
				music.coverart = cm.getString(2);
				music.reading = cm.getString(3);
				if (music.reading == null)
					music.reading = getReading(music.title);
				music.publish_order = cm.getInt(4);
				music.part = cm.getInt(5);
				music.role1 = createRole(cm, 6);
				music.role2 = createRole(cm, 13);
				music.role3 = createRole(cm, 20);
				music.skin = cm.getString(27);
				for (int i = 0; i < ButtonSE.COUNT; ++i)
					music.sounds[i] = cm.getString(28+1);
				music.ordinal = StringUtils.forLexicographical(music.reading);
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

	private Role createRole(Cursor c, int from) {
		String name = c.getString(from);
		if (TextUtils.isEmpty(name))
			return null;

		Role role = new Role();
		role.name = name;
		role.cast = c.getInt(from+1);
		role.module = c.getString(from+2);
		for (int i = 0; i < CustomizeItem.COUNT; ++i)
			role.items[i] = c.getString(from+3+i);
		return role;
	}

	public Module getModule(String id) {
		List<Module> modules = findModules(new SparseArray<ModuleGroup>(), ModuleTable.WHERE_IDENTITY, id);
		return modules.isEmpty() ? null : modules.get(0);
	}

	public List<ModuleGroup> loadModules() {
		List<ModuleGroup> groups = new ArrayList<ModuleGroup>();
		SparseArray<ModuleGroup> id2group = new SparseArray<ModuleGroup>();

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

		findModules(id2group, null);

		return groups;
	}

	private List<Module> findModules(SparseArray<ModuleGroup> id2group, String selection, String...selectionArgs) {
		SQLiteDatabase db = m_helper.getReadableDatabase();

		List<Module> modules = new ArrayList<Module>();
		Cursor cm = db.query(ModuleTable.TABLE_NAME, new String[] {
				ModuleTable.ID,
				ModuleTable.NAME,
				ModuleTable.STATUS,
				ModuleTable.GROUP_ID,
				ModuleTable.IMAGE,
				ModuleTable.THUMBNAIL,
		}, selection, selectionArgs, null, null, ModuleTable._ID);
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
				else
					modules.add(module);
			}
		}
		finally {
			cm.close();
		}

		return modules;
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
					for (int i = 0; i < music.records.length; ++i) {
						if (!ScoreTable.update(db, music.id, i, music.records[i]))
							ScoreTable.insert(db, music.id, i, music.records[i]);
					}
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
		SharedPreferences.Editor editor = getPreferences().edit();
		editor.putString("player_name", record.player_name);
		editor.putString("level_rank", record.level);
		editor.putString("title", record.title);
		editor.putInt("vocaloid_point", record.vocaloid_point);
		editor.putInt("ticket", record.ticket);
		editor.commit();
	}

	public void update(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.update(db, music);
			for (int i = 0; i < music.records.length; ++i) {
				if (!ScoreTable.update(db, music.id, i, music.records[i]))
					ScoreTable.insert(db, music.id, i, music.records[i]);
			}
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

	public void updateIndividual(List<MusicInfo> musics) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			for (MusicInfo music: musics)
				MusicTable.updateIndividual(db, music);
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
		}, null, null, null, null, TitleTable.ORDER);
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
		return getDecorTitles(false, pre);
	}

	public List<DecorTitle> getDecorPrize() {
		return getDecorTitles(true, null);
	}

	protected List<DecorTitle> getDecorTitles(boolean prize, Boolean pre) {
		StringBuilder selection = new StringBuilder();
		selection.append(DecorTitleTable.STATUS).append(prize ? "=2" : "!=2");
		if (pre != null)
			selection.append(" AND ").append(DecorTitleTable.POSITION).append(pre ? "=1" : "=0");

		List<DecorTitle> titles = new ArrayList<DecorTitle>();
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(DecorTitleTable.TABLE_NAME, new String[] {
				DecorTitleTable.ID,
				DecorTitleTable.NAME,
				DecorTitleTable.STATUS,
				DecorTitleTable.POSITION,
		}, selection.toString(), null, null, null, null);
		try {
			while (c.moveToNext()) {
				DecorTitle title = new DecorTitle(c.getString(0), c.getString(1), c.getInt(2) == 1);
				title.pre = c.getInt(3) == 1;
				title.prize = c.getInt(2) == 2;
				titles.add(title);
			}
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

	public SkinInfo getSkin(String id) {
		List<SkinInfo> skins = findSkins(SkinTable.WHERE_IDENTITY, id);
		return skins.isEmpty() ? null : skins.get(0);
	}

	public List<SkinInfo> loadSkins() {
		return loadSkins(false);
	}

	public List<SkinInfo> loadSkins(boolean prize) {
		return findSkins(String.format(prize ? "%s=2" : "%s!=2", SkinTable.STATUS));
	}

	private List<SkinInfo> findSkins(String selection, String...selectionArgs) {
		List<SkinInfo> skins = new ArrayList<SkinInfo>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(SkinTable.TABLE_NAME, new String[] {
				SkinTable.GROUP_ID,
				SkinTable.ID,
				SkinTable.NAME,
				SkinTable.PATH,
				SkinTable.STATUS,
		}, selection, selectionArgs, null, null, SkinTable._ID);
		try {
			while (c.moveToNext()) {
				SkinInfo skin = new SkinInfo(c.getString(0), c.getString(1), c.getString(2), c.getInt(4) == 1);
				skin.prize = c.getInt(4) == 2;
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

	public ButtonSE getButtonSE(int type, String id) {
		List<ButtonSE> sounds = findButtonSEs(ButtonSETable.WHERE_IDENTITY, String.valueOf(type), id);
		return sounds.isEmpty() ? null : sounds.get(0);
	}

	public List<ButtonSE> loadButtonSEs(int type) {
		return findButtonSEs(ButtonSETable.WHERE_BY_TYPE, String.valueOf(type));
	}

	private List<ButtonSE> findButtonSEs(String selection, String... selectionArgs) {
		List<ButtonSE> buttonSEs = new ArrayList<ButtonSE>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(ButtonSETable.TABLE_NAME, new String[] {
				ButtonSETable.ID,
				ButtonSETable.NAME,
				ButtonSETable.SAMPLE,
				ButtonSETable.TYPE,
		}, selection, selectionArgs, null, null, ButtonSETable._ID);
		try {
			while (c.moveToNext()) {
				ButtonSE se = new ButtonSE(c.getString(0), c.getString(1));
				se.sample = c.getString(2);
				se.type = c.getInt(3);
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
		SharedPreferences preferences = getPreferences();
		return preferences.getInt("active_mylist", -1);
	}

	public void activateMyList(int id) {
		SharedPreferences.Editor editor = getPreferences().edit();
		editor.putInt("active_mylist", id);
		editor.commit();
	}

	public List<String> getDIVARecords() {
		List<String> records = new ArrayList<String>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(RecordTable.TABLE_NAME, new String[] {
				RecordTable.CONTENT,
		}, null, null, null, null, RecordTable._ID);
		try {
			while (c.moveToNext())
				records.add(c.getString(0));
		}
		finally {
			c.close();
		}

		return records;
	}

	public void updateDIVARecords(List<String> records) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			final int count = records.size();
			int id = 0;
			for (; id < count; ++id) {
				if (!RecordTable.update(db, id, records.get(id)))
					break;
			}
			for (; id < count; ++id) {
				if (RecordTable.insert(db, id, records.get(id)) < 0)
					return;
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}

	private static final String AUTHORITY = "net.diva.browser.store";

	private static final int MUSICS = 1;
	private static final int MODULES = 2;
	private static final int SKINS = 3;
	private static final int BUTTON_SES = 4;

	private static final UriMatcher s_matcher;

	static {
		s_matcher = new UriMatcher(UriMatcher.NO_MATCH);
		s_matcher.addURI(AUTHORITY, "musics", MUSICS);
		s_matcher.addURI(AUTHORITY, "modules", MODULES);
		s_matcher.addURI(AUTHORITY, "skins", SKINS);
		s_matcher.addURI(AUTHORITY, "button_ses", BUTTON_SES);
	}

	@Override
	public boolean onCreate() {
		m_helper = new OpenHelper(getContext(), DATABASE_NAME, null, VERSION);
		m_instance = this;
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (s_matcher.match(uri)) {
		case MUSICS:
			qb.setTables(MusicTable.NAME);
			break;
		case MODULES:
			qb.setTables(ModuleTable.TABLE_NAME);
			break;
		case SKINS:
			qb.setTables(SkinTable.TABLE_NAME);
			break;
		case BUTTON_SES:
			qb.setTables(ButtonSETable.TABLE_NAME);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		Cursor c = qb.query(m_helper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
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
			db.execSQL(DecorTitleTable.create_statement());
			db.execSQL(ModuleGroupTable.create_statement());
			db.execSQL(ModuleTable.create_statement());
			db.execSQL(SkinTable.create_statement());
			db.execSQL(ButtonSETable.create_statement());
			db.execSQL(MyListTable.create_statement());
			db.execSQL(MyListEntryTable.create_statement());
			db.execSQL(RecordTable.create_statement());
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
			case 19:
				MusicTable.addVoiceColumns(db);
			case 20:
				db.execSQL(RecordTable.create_statement());
			case 21:
				TitleTable.addOrderColumn(db);
			case 22:
				DecorTitleTable.addPositionColumns(db);
			case 23:
				db.execSQL(String.format("DELETE FROM %s WHERE %s='%s'",
						DecorTitleTable.TABLE_NAME, DecorTitleTable.ID, DecorTitle.OFF.id));
			case 24:
				db.execSQL(String.format("DROP TABLE %s", MusicTable.NAME));
				db.execSQL(MusicTable.create_statement());
				db.execSQL(String.format("DROP TABLE %s", ButtonSETable.TABLE_NAME));
				db.execSQL(ButtonSETable.create_statement());
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
