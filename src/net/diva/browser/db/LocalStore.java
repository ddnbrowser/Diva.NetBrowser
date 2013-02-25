package net.diva.browser.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.diva.browser.R;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.DecorTitle;
import net.diva.browser.model.History;
import net.diva.browser.model.Module;
import net.diva.browser.model.ModuleGroup;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.model.MyList;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.model.Ranking;
import net.diva.browser.model.RivalInfo;
import net.diva.browser.model.ScoreRecord;
import net.diva.browser.model.SkinInfo;
import net.diva.browser.model.TitleInfo;
import net.diva.browser.util.StringUtils;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class LocalStore extends ContextWrapper {
	private static final String DATABASE_NAME = "diva.db";
	private static final int VERSION = 29;

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
				MusicTable.PART,
				MusicTable.VOCAL1,
				MusicTable.VOCAL2,
				MusicTable.READING,
				MusicTable.FAVORITE,
				MusicTable.PUBLISH,
				MusicTable.SKIN,
				MusicTable.BUTTON,
				MusicTable.VOICE1,
				MusicTable.VOICE2,
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
				music.voice1 = cm.getInt(11);
				music.voice2 = cm.getInt(12);
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
		}, ScoreTable.RIVAL_CODE + " is null", null, null, null, null);
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

		loadRivalScore(musics);

		return musics;
	}

	public void loadRivalScore(List<MusicInfo> musics) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String rivalCode = preferences.getString("rival_code", null);
		if(rivalCode == null)
			return;

		SQLiteDatabase db = m_helper.getReadableDatabase();

		Map<String, MusicInfo> id2music = new HashMap<String, MusicInfo>();
		for(MusicInfo music : musics){
			id2music.put(music.id, music);
		}

		Cursor cr = db.query(ScoreTable.NAME, new String[] {
				ScoreTable.MUSIC_ID,
				ScoreTable.RANK,
				ScoreTable.DIFFICULTY,
				ScoreTable.CLEAR_STATUS,
				ScoreTable.TRIAL_STATUS,
				ScoreTable.HIGH_SCORE,
				ScoreTable.ACHIEVEMENT,
				ScoreTable.RANKING,
		}, ScoreTable.RIVAL_CODE + "=?", new String[]{rivalCode}, null, null, null);
				try {
			while (cr.moveToNext()) {
				ScoreRecord score = new ScoreRecord();
				score.difficulty = cr.getInt(2);
				score.clear_status = cr.getInt(3);
				score.trial_status = cr.getInt(4);
				score.high_score = cr.getInt(5);
				score.achievement = cr.getInt(6);
				score.ranking = cr.isNull(7) ? -1 : cr.getInt(7);

				MusicInfo music = id2music.get(cr.getString(0));
				if (music != null)
					music.rival_records[cr.getInt(1)] = score;
			}
		}
		finally {
			cr.close();
		}
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
						ScoreTable.update(db, music.id, i, null, music.records[i]);
				}
				else {
					music.reading = getReading(music.title);
					MusicTable.insert(db, music);
					for (int i = 0; i < music.records.length; ++i)
						ScoreTable.insert(db, music.id, i, null, music.records[i]);
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
		editor.putInt("ticket", record.ticket);
		editor.commit();
	}

	public void update(MusicInfo music) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		db.beginTransaction();
		try {
			MusicTable.update(db, music);
			for (int i = 0; i < music.records.length; ++i)
				ScoreTable.update(db, music.id, i, null, music.records[i]);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			db.close();
		}
	}

	public void update(RivalInfo rival) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		for (MusicInfo music : rival.musics) {
			for (int i = 0; i < music.rival_records.length; ++i) {
				boolean exist = ScoreTable.update(db, music.id, i, rival, music.rival_records[i]);
				if (!exist) {
					ScoreTable.insert(db, music.id, i, rival, music.rival_records[i]);
				}
			}
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

	public List<SkinInfo> loadSkins() {
		return loadSkins(false);
	}

	public List<SkinInfo> loadSkins(boolean prize) {
		List<SkinInfo> skins = new ArrayList<SkinInfo>();

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(SkinTable.TABLE_NAME, new String[] {
				SkinTable.GROUP_ID,
				SkinTable.ID,
				SkinTable.NAME,
				SkinTable.PATH,
				SkinTable.STATUS,
		}, String.format(prize ? "%s=2" : "%s!=2", SkinTable.STATUS), null, null, null, SkinTable._ID);
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
				MyListTable.MAX,
		}, null, null, null, null, MyListTable.ID);
		try {
			while (c.moveToNext())
				mylists.add(new MyList(c.getInt(0), c.getString(1), c.getInt(2)));
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
			if(myList.max == 0)
				MyListTable.update(db, myList.id, myList.name);
			else
				MyListTable.update(db, myList.id, myList.name, myList.max);
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

	public int getActiveMyList(int id){
		if(id == -1)
			return -1;
		int[] actives = activeMyList();
		for(int i = 0; i < actives.length; i++){
			if(id == actives[i])
				return i;
		}
		return -1;
	}

	public int[] activeMyList(){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int mylist_active = getResources().getInteger(R.integer.mylist_actives);
		int[] actives = new int[mylist_active];
		for(int i = 0; i < mylist_active; i++)
			actives[i] = preferences.getInt("active_mylist" + (char)('a' + i), -1);
		return actives;
	}

	public void activateMyList(int id, int target) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		int[] actives = activeMyList();
		for(int i = 0; i < actives.length; i++){
			if(actives[i] == id){
				int targetId = preferences.getInt("active_mylist" + (char)('a' + target), -1);
				if(targetId != -1){
					editor.putInt("active_mylist" + (char)('a' + i), targetId);
				}else{
					editor.putInt("active_mylist" + (char)('a' + i), -1);
				}
				break;
			}
		}
		editor.putInt("active_mylist" + (char)('a' + target), id);
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

	public List<History> getPlayHistoryList(List<String> dateList, String orderBy) {
		List<History> records = new ArrayList<History>();

		String where = HistoryTable.PLAY_DATE + " in (";
		for(int i = 0; i < dateList.size(); i++){
			if(i != 0)
				where += ",";
			where += "?";
		}
		where += ")";

		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(HistoryTable.TABLE_NAME, new String[] {
				HistoryTable.MUSIC_ID,
				HistoryTable.RANK,
				HistoryTable.PLAY_DATE,
				HistoryTable.PLAY_PLACE,
				HistoryTable.CLEAR_STATUS,
				HistoryTable.ACHIEVEMENT,
				HistoryTable.SCORE,
				HistoryTable.COOL,
				HistoryTable.COOL_PER,
				HistoryTable.FINE,
				HistoryTable.FINE_PER,
				HistoryTable.SAFE,
				HistoryTable.SAFE_PER,
				HistoryTable.SAD,
				HistoryTable.SAD_PER,
				HistoryTable.WORST,
				HistoryTable.WORST_PER,
				HistoryTable.COMBO,
				HistoryTable.CHALLANGE_TIME,
				HistoryTable.HOLD,
				HistoryTable.TRIAL,
				HistoryTable.TRIAL_RESULT,
				HistoryTable.MODULE1,
				HistoryTable.MODULE2,
				HistoryTable.SE,
				HistoryTable.SKIN,
				HistoryTable.LOCK,
				HistoryTable.RESULT_PICTURE,
		}, where, dateList.toArray(new String[0]), null, null, orderBy);
		try {
			while (c.moveToNext()){
				History h = new History();
				h.music_id = c.getString(0);
				h.rank = c.getInt(1);
				h.play_date = c.getInt(2);
				h.play_place = c.getString(3);
				h.clear_status = c.getInt(4);
				h.achievement = c.getInt(5);
				h.score = c.getInt(6);
				h.cool = c.getInt(7);
				h.cool_per = c.getInt(8);
				h.fine = c.getInt(9);
				h.fine_per = c.getInt(10);
				h.safe = c.getInt(11);
				h.safe_per = c.getInt(12);
				h.sad = c.getInt(13);
				h.sad_per = c.getInt(14);
				h.worst = c.getInt(15);
				h.worst_per = c.getInt(16);
				h.combo = c.getInt(17);
				h.challange_time = c.getInt(18);
				h.hold = c.getInt(19);
				h.trial = c.getInt(20);
				h.trial_result = c.getInt(21);
				h.module1_id = c.getString(22);
				h.module2_id = c.getString(23);
				h.se_id = c.getString(24);
				h.skin_id = c.getString(25);
				h.lock = c.getInt(26);
				h.result_picture = c.getString(27);

				records.add(h);
			}
		}
		finally {
			c.close();
		}

		return records;
	}

	public List<String> getPlayHistoryList(String where, List<String> params, String orderBy){
		List<String> ret = new ArrayList<String>();
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(HistoryTable.TABLE_NAME, new String[] {
				HistoryTable.PLAY_DATE,
		}, where, params.toArray(new String[0]), null, null, orderBy);
		try {
			while (c.moveToNext()){
				ret.add(c.getString(0));
			}
		}
		finally {
			c.close();
		}

		return ret;
	}

	public void insert(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.insert(db, history);
	}

	public void deleteHistory(String music_id, int rank, int limit_date){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.delete(db, music_id, rank, limit_date);
	}

	public void deleteHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.delete(db, history);
	}

	public void lockHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.lock(db, history);
	}

	public String getButtonSeId(String name){
		String retId = "unknown";
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(ButtonSETable.TABLE_NAME, new String[] {
				ButtonSETable.ID,
		}, String.format("%s=?", ButtonSETable.NAME), new String[] { String.valueOf(name) }, null, null, ButtonSETable.ID);
		try {
			while (c.moveToNext())
				retId = c.getString(0);
		}
		finally {
			c.close();
		}
		return retId;
	}

	public String getSkinId(String name){
		String retId = "unknown";
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(SkinTable.TABLE_NAME, new String[] {
				SkinTable.ID,
		}, String.format("%s=?", SkinTable.NAME), new String[] { String.valueOf(name) }, null, null, SkinTable.ID);
		try {
			while (c.moveToNext())
				retId = c.getString(0);
		}
		finally {
			c.close();
		}
		return retId;
	}

	public SkinInfo getSkinInfo(String skin_id){
		SkinInfo retSkin = null;
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(SkinTable.TABLE_NAME, new String[] {
				SkinTable.GROUP_ID,
				SkinTable.ID,
				SkinTable.NAME,
				SkinTable.PATH,
				SkinTable.STATUS,
		}, String.format("%s=?", SkinTable.ID), new String[]{skin_id}, null, null, SkinTable._ID);
		try {
			while (c.moveToNext()) {
				retSkin = new SkinInfo(c.getString(0), c.getString(1), c.getString(2), c.getInt(4) == 1);
				retSkin.prize = c.getInt(4) == 2;
				retSkin.image_path = c.getString(3);
			}
		}
		finally {
			c.close();
		}
		return retSkin;
	}

	public Module getModule(String mod_id){
		Module retMod = null;
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor cm = db.query(ModuleTable.TABLE_NAME, new String[] {
				ModuleTable.ID,
				ModuleTable.NAME,
				ModuleTable.STATUS,
				ModuleTable.GROUP_ID,
				ModuleTable.IMAGE,
				ModuleTable.THUMBNAIL,
		}, String.format("%s=?", ModuleTable.ID), new String[]{mod_id}, null, null, ModuleTable._ID);
		try {
			while (cm.moveToNext()) {
				retMod = new Module();
				retMod.id = cm.getString(0);
				retMod.name = cm.getString(1);
				retMod.purchased = cm.getInt(2) == 1;
				retMod.image = cm.getString(4);
				retMod.thumbnail = cm.getString(5);
			}
		}
		finally {
			cm.close();
		}

		return retMod;
	}

	public Cursor getAllHistory() throws IOException {
		Cursor c = m_helper.getReadableDatabase().query(HistoryTable.TABLE_NAME, new String[] {
				HistoryTable.MUSIC_ID,
				HistoryTable.RANK,
				HistoryTable.PLAY_DATE,
				HistoryTable.PLAY_PLACE,
				HistoryTable.CLEAR_STATUS,
				HistoryTable.ACHIEVEMENT,
				HistoryTable.SCORE,
				HistoryTable.COOL,
				HistoryTable.COOL_PER,
				HistoryTable.FINE,
				HistoryTable.FINE_PER,
				HistoryTable.SAFE,
				HistoryTable.SAFE_PER,
				HistoryTable.SAD,
				HistoryTable.SAD_PER,
				HistoryTable.WORST,
				HistoryTable.WORST_PER,
				HistoryTable.COMBO,
				HistoryTable.CHALLANGE_TIME,
				HistoryTable.HOLD,
				HistoryTable.TRIAL,
				HistoryTable.TRIAL_RESULT,
				HistoryTable.MODULE1,
				HistoryTable.MODULE2,
				HistoryTable.SE,
				HistoryTable.SKIN,
				HistoryTable.LOCK,
				HistoryTable.RESULT_PICTURE,
		}, null, null, null, null, null);
		return c;
	}

	public int historyCount(){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		String sql = "select count(*) as count from " + HistoryTable.TABLE_NAME;
		Cursor c = db.rawQuery(sql, null);
		c.moveToFirst();
		return c.getInt(0);
	}

	public Map<String, String> getModuleMap(){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		Cursor c = db.query(
				ModuleTable.TABLE_NAME,
				new String[] {
						ModuleTable.ID,
						ModuleTable.NAME,
				}, null, null, null, null, null);
		Map<String, String> map = new HashMap<String, String>();
		while(c.moveToNext()){
			map.put(c.getString(0), c.getString(1));
		}
		return map;
	}

	public Map<String, String> getSeMap(){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		Cursor c = db.query(
				ButtonSETable.TABLE_NAME,
				new String[] {
						ButtonSETable.ID,
						ButtonSETable.NAME,
				}, null, null, null, null, null);
		Map<String, String> map = new HashMap<String, String>();
		while(c.moveToNext()){
			map.put(c.getString(0), c.getString(1));
		}
		return map;
	}

	public Map<String, String> getSkinMap(){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		Cursor c = db.query(
				SkinTable.TABLE_NAME,
				new String[] {
						SkinTable.ID,
						SkinTable.NAME,
				}, null, null, null, null, null);
		Map<String, String> map = new HashMap<String, String>();
		while(c.moveToNext()){
			map.put(c.getString(0), c.getString(1));
		}
		return map;
	}

	public List<RivalInfo> getRivalList(){
		SQLiteDatabase db = m_helper.getWritableDatabase();

		Cursor c = db.query(
				ScoreTable.NAME,
				new String[]{
						ScoreTable.RIVAL_CODE,
						ScoreTable.RIVAL_NAME,
				},
				ScoreTable.RIVAL_CODE + " is not null ",
				null,
				ScoreTable.RIVAL_CODE, null, null
				);
		List<RivalInfo> rivalList = new ArrayList<RivalInfo>();
		while(c.moveToNext()){
			RivalInfo rival = new RivalInfo();
			rival.rival_code = c.getString(0);
			rival.rival_name = c.getString(1);
			rivalList.add(rival);
		}

		return rivalList;
	}

	public void setPicture(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.setPicture(db, history);
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
			db.execSQL(HistoryTable.create_statement());
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
				db.execSQL(HistoryTable.create_statement());
			case 24:
				// パーフェクトのコード値を3から4へ
				db.execSQL(String.format("UPDATE %s SET %s = 4 WHERE %s = %d",
						HistoryTable.TABLE_NAME, HistoryTable.CLEAR_STATUS,
						HistoryTable.CLEAR_STATUS, 3));
				// エクセレントがNOT CLEAR状態になっているので達成率が超えている履歴に関しては
				// エクセレントに変更。激唱の95%Over閉店とか閉店コマンドに関しては考慮しない。
				// ロケテ期間はサポート外とする
				// Extreme
				db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
						HistoryTable.TABLE_NAME, HistoryTable.CLEAR_STATUS,
						HistoryTable.PLAY_DATE, 1341439200,
						HistoryTable.CLEAR_STATUS, 0,
						HistoryTable.RANK, 3,
						HistoryTable.ACHIEVEMENT, 9500));
				// Hard
				db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
						HistoryTable.TABLE_NAME, HistoryTable.CLEAR_STATUS,
						HistoryTable.PLAY_DATE, 1341439200,
						HistoryTable.CLEAR_STATUS, 0,
						HistoryTable.RANK, 2,
						HistoryTable.ACHIEVEMENT, 9000));
				// Normal
				db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
						HistoryTable.TABLE_NAME, HistoryTable.CLEAR_STATUS,
						HistoryTable.PLAY_DATE, 1341439200,
						HistoryTable.CLEAR_STATUS, 0,
						HistoryTable.RANK, 1,
						HistoryTable.ACHIEVEMENT, 8500));
				// Easy
				db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
						HistoryTable.TABLE_NAME, HistoryTable.CLEAR_STATUS,
						HistoryTable.PLAY_DATE, 1341439200,
						HistoryTable.CLEAR_STATUS, 0,
						HistoryTable.RANK, 0,
						HistoryTable.ACHIEVEMENT, 8000));
			case 25:
				HistoryTable.addResutPictureColumns(db);
			case 26:
				ScoreTable.addRivalCodeColumns(db);
				HistoryTable.addUniqueKey(db);
			case 27:
				ScoreTable.addRivalNameColumns(db);
			case 28:
				MyListTable.addMaxColumns(db);
				for(int i = 0; i < 5; i++)
					for(int j = 20; j < 30; j++)
						MyListEntryTable.insert(db, i, j, null);
			default:
				break;
			}
		}

		private void initializeMyList(SQLiteDatabase db) {
			for (int i = 0; i < 5; ++i) {
				MyListTable.insert(db, i, String.format("マイリスト%d", i+1));
				for (int j = 0; j < 30; ++j)
					MyListEntryTable.insert(db, i, j, null);
			}
		}
	}
}
