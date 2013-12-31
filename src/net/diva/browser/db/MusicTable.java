package net.diva.browser.db;

import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.CustomizeItem;
import net.diva.browser.model.MusicInfo;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class MusicTable implements BaseColumns {
	public static final String NAME = "Music";

	public static final String ID = "id";
	public static final String COVERART = "coverart";
	public static final String TITLE = "title";
	public static final String READING = "reading";
	public static final String PUBLISH = "publish";
	public static final String PART = "part";
	public static final String ROLE1 = "role1";
	public static final String CAST1 = "cast1";
	public static final String MODULE1 = "module1";
	public static final String[] MODULE1_ITEMS = createSequential("module1_item%d", CustomizeItem.COUNT);
	public static final String ROLE2 = "role2";
	public static final String CAST2 = "cast2";
	public static final String MODULE2 = "module2";
	public static final String[] MODULE2_ITEMS = createSequential("module2_item%d", CustomizeItem.COUNT);
	public static final String ROLE3 = "role3";
	public static final String CAST3 = "cast3";
	public static final String MODULE3 = "module3";
	public static final String[] MODULE3_ITEMS = createSequential("module3_item%d", CustomizeItem.COUNT);
	public static final String SKIN = "skin";
	public static final String[] SOUNDS = createSequential("sound%d", 4);
	public static final String FAVORITE = "favorite";

	@Deprecated
	private static final String VOICE1 = "voice1";
	@Deprecated
	private static final String VOICE2 = "voice2";
	@Deprecated
	private static final String VOCAL1 = "vocal1";
	@Deprecated
	private static final String VOCAL2 = "vocal2";
	@Deprecated
	private static final String BUTTON = "button";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private static final String[] createSequential(String format, int count) {
		String[] values = new String[count];
		for (int i = 0; i < count; ++i)
			values[i] = String.format(format, i);
		return values;
	}

	private MusicTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(TITLE).append(" text,")
		.append(READING).append(" text,")
		.append(COVERART).append(" text,")
		.append(PUBLISH).append(" integer,")
		.append(PART).append(" integer,")
		.append(ROLE1).append(" text,")
		.append(CAST1).append(" integer,")
		.append(MODULE1).append(" text,")
		.append(MODULE1_ITEMS[0]).append(" text,")
		.append(MODULE1_ITEMS[1]).append(" text,")
		.append(MODULE1_ITEMS[2]).append(" text,")
		.append(MODULE1_ITEMS[3]).append(" text,")
		.append(ROLE2).append(" text,")
		.append(CAST2).append(" integer,")
		.append(MODULE2).append(" text,")
		.append(MODULE2_ITEMS[0]).append(" text,")
		.append(MODULE2_ITEMS[1]).append(" text,")
		.append(MODULE2_ITEMS[2]).append(" text,")
		.append(MODULE2_ITEMS[3]).append(" text,")
		.append(ROLE3).append(" text,")
		.append(CAST3).append(" integer,")
		.append(MODULE3).append(" text,")
		.append(MODULE3_ITEMS[0]).append(" text,")
		.append(MODULE3_ITEMS[1]).append(" text,")
		.append(MODULE3_ITEMS[2]).append(" text,")
		.append(MODULE3_ITEMS[3]).append(" text,")
		.append(SKIN).append(" text,")
		.append(SOUNDS[0]).append(" text,")
		.append(SOUNDS[1]).append(" text,")
		.append(SOUNDS[2]).append(" text,")
		.append(SOUNDS[3]).append(" text,")
		.append(FAVORITE).append(" integer")
		.append(")");
		return builder.toString();
	}

	static void addModuleColumns(SQLiteDatabase db) {
		String format = "ALTER TABLE %s ADD %s %s";
		db.execSQL(String.format(format, NAME, PART, "integer"));
		db.execSQL(String.format(format, NAME, VOCAL1, "text"));
		db.execSQL(String.format(format, NAME, VOCAL2, "text"));

		ContentValues values = new ContentValues(1);
		values.put(PART, 1);
		db.update(NAME, values, null, null);
	}

	static void addReadingColumn(SQLiteDatabase db) {
		db.execSQL(String.format("ALTER TABLE %s ADD %s %s", NAME, READING, "text"));
	}

	static void addFavoriteColumn(SQLiteDatabase db) {
		db.execSQL(String.format("ALTER TABLE %s ADD %s %s", NAME, FAVORITE, "integer"));
	}

	static void addPublishColumn(SQLiteDatabase db) {
		db.execSQL(String.format("ALTER TABLE %s ADD %s %s", NAME, PUBLISH, "integer"));
		db.execSQL(String.format("UPDATE %s SET %s=-1;", NAME, PUBLISH));
	}

	static void addIndividualColumns(SQLiteDatabase db) {
		String format = "ALTER TABLE %s ADD %s %s";
		db.execSQL(String.format(format, NAME, SKIN, "text"));
		db.execSQL(String.format(format, NAME, BUTTON, "text"));
	}

	static void addVoiceColumns(SQLiteDatabase db) {
		String format = "ALTER TABLE %s ADD %s %s";
		db.execSQL(String.format(format, NAME, VOICE1, "integer"));
		db.execSQL(String.format(format, NAME, VOICE2, "integer"));

		ContentValues values = new ContentValues(2);
		values.put(VOICE1, -1);
		values.put(VOICE2, -1);
		db.update(NAME, values, null, null);
	}

	static long insert(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(32);
		values.put(ID, music.id);
		values.put(TITLE, music.title);
		values.put(READING, music.reading);
		values.put(COVERART, music.coverart);
		values.put(PUBLISH, music.publish_order);
		values.put(PART, music.part);
		if (music.role1 != null) {
			values.put(ROLE1, music.role1.name);
			values.put(CAST1, music.role1.cast);
			values.put(MODULE1, music.role1.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE1_ITEMS[i], music.role1.items[i]);
		}
		if (music.role2 != null) {
			values.put(ROLE2, music.role2.name);
			values.put(CAST2, music.role2.cast);
			values.put(MODULE2, music.role2.module);
			for (int i = 0; i < MODULE2_ITEMS.length; ++i)
				values.put(MODULE2_ITEMS[i], music.role2.items[i]);
		}
		if (music.role3 != null) {
			values.put(ROLE3, music.role3.name);
			values.put(CAST3, music.role3.cast);
			values.put(MODULE3, music.role3.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE3_ITEMS[i], music.role3.items[i]);
		}
		values.putNull(SKIN);
		for (int i = 0; i < SOUNDS.length; ++i)
			values.put(SOUNDS[i], music.sounds[1]);
		return db.insert(NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(26);
		values.put(TITLE, music.title);
		values.put(READING, music.reading);
		values.put(COVERART, music.coverart);
		values.put(PUBLISH, music.publish_order);
		values.put(PART, music.part);
		if (music.role1 != null) {
			values.put(ROLE1, music.role1.name);
			values.put(CAST1, music.role1.cast);
			values.put(MODULE1, music.role1.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE1_ITEMS[i], music.role1.items[i]);
		}
		if (music.role2 != null) {
			values.put(ROLE2, music.role2.name);
			values.put(CAST2, music.role2.cast);
			values.put(MODULE2, music.role2.module);
			for (int i = 0; i < MODULE2_ITEMS.length; ++i)
				values.put(MODULE2_ITEMS[i], music.role2.items[i]);
		}
		if (music.role3 != null) {
			values.put(ROLE3, music.role3.name);
			values.put(CAST3, music.role3.cast);
			values.put(MODULE3, music.role3.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE3_ITEMS[i], music.role3.items[i]);
		}
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}

	static void resetIndividualAll(SQLiteDatabase db) {
		for (int i = 0; i < SOUNDS.length; ++i) {
			ContentValues values = new ContentValues(1);
			values.putNull(SOUNDS[i]);
			db.update(NAME, values, String.format("%s <> ?", SOUNDS[i]), new String[] { ButtonSE.UNSUPPORTED });
		}

		ContentValues values = new ContentValues(16);
		values.putNull(MODULE1);
		for (int i = 0; i < MODULE1_ITEMS.length; ++i)
			values.putNull(MODULE1_ITEMS[i]);
		values.putNull(MODULE2);
		for (int i = 0; i < MODULE2_ITEMS.length; ++i)
			values.putNull(MODULE2_ITEMS[i]);
		values.putNull(MODULE3);
		for (int i = 0; i < MODULE3_ITEMS.length; ++i)
			values.putNull(MODULE3_ITEMS[i]);
		values.putNull(SKIN);
		db.update(NAME, values, null, null);
	}

	static boolean updateModule(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(15);
		if (music.role1 != null) {
			values.put(MODULE1, music.role1.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE1_ITEMS[i], music.role1.items[i]);
		}
		if (music.role2 != null) {
			values.put(MODULE2, music.role2.module);
			for (int i = 0; i < MODULE2_ITEMS.length; ++i)
				values.put(MODULE2_ITEMS[i], music.role2.items[i]);
		}
		if (music.role3 != null) {
			values.put(MODULE3, music.role3.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE3_ITEMS[i], music.role3.items[i]);
		}
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}

	static boolean updateSkin(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(1);
		if (music.skin == null)
			values.putNull(SKIN);
		else
			values.put(SKIN, music.skin);
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}

	static boolean updateButtonSE(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(ButtonSE.COUNT);
		for (int i = 0; i < ButtonSE.COUNT; ++i)
			values.put(SOUNDS[i], music.sounds[1]);
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}

	static boolean updateIndividual(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(20);
		if (music.role1 != null) {
			values.put(MODULE1, music.role1.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE1_ITEMS[i], music.role1.items[i]);
		}
		if (music.role2 != null) {
			values.put(MODULE2, music.role2.module);
			for (int i = 0; i < MODULE2_ITEMS.length; ++i)
				values.put(MODULE2_ITEMS[i], music.role2.items[i]);
		}
		if (music.role3 != null) {
			values.put(MODULE3, music.role3.module);
			for (int i = 0; i < MODULE1_ITEMS.length; ++i)
				values.put(MODULE3_ITEMS[i], music.role3.items[i]);
		}
		values.put(SKIN, music.skin);
		for (int i = 0; i < ButtonSE.COUNT; ++i)
			values.put(SOUNDS[i], music.sounds[1]);
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}
}
