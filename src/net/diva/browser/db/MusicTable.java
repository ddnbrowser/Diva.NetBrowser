package net.diva.browser.db;

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
	public static final String VOCAL1 = "vocal1";
	public static final String VOCAL2 = "vocal2";
	public static final String SKIN = "skin";
	public static final String BUTTON = "button";
	public static final String FAVORITE = "favorite";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

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
		.append(VOCAL1).append(" text,")
		.append(VOCAL2).append(" text,")
		.append(SKIN).append(" text,")
		.append(BUTTON).append(" text,")
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

	static long insert(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(10);
		values.put(ID, music.id);
		values.put(TITLE, music.title);
		values.put(READING, music.reading);
		values.put(COVERART, music.coverart);
		values.put(PUBLISH, music.publish_order);
		values.put(PART, music.part);
		values.putNull(VOCAL1);
		values.putNull(VOCAL2);
		values.putNull(SKIN);
		values.putNull(BUTTON);
		return db.insert(NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(4);
		values.put(TITLE, music.title);
		values.put(READING, music.reading);
		values.put(COVERART, music.coverart);
		values.put(PUBLISH, music.publish_order);
		values.put(PART, music.part);
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}

	static void resetIndividualAll(SQLiteDatabase db) {
		ContentValues values = new ContentValues(4);
		values.putNull(VOCAL1);
		values.putNull(VOCAL2);
		values.putNull(SKIN);
		values.putNull(BUTTON);
		db.update(NAME, values, null, null);
	}

	static boolean updateModule(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(2);
		if (music.vocal1 == null)
			values.putNull(VOCAL1);
		else
			values.put(VOCAL1, music.vocal1);
		if (music.vocal2 == null)
			values.putNull(VOCAL2);
		else
			values.put(VOCAL2, music.vocal2);
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
		ContentValues values = new ContentValues(1);
		if (music.button == null)
			values.putNull(BUTTON);
		else
			values.put(BUTTON, music.button);
		return db.update(NAME, values, WHERE_IDENTITY, new String[] { music.id }) == 1;
	}
}
