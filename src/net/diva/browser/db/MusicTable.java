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
	public static final String PART = "part";
	public static final String VOCAL1 = "vocal1";
	public static final String VOCAL2 = "vocal2";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private MusicTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(TITLE).append(" text,")
		.append(COVERART).append(" text,")
		.append(PART).append(" integer,")
		.append(VOCAL1).append(" text,")
		.append(VOCAL2).append(" text")
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

	static long insert(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(6);
		values.put(ID, music.id);
		values.put(TITLE, music.title);
		values.put(COVERART, music.coverart);
		values.put(PART, music.part);
		values.putNull(VOCAL1);
		values.putNull(VOCAL2);
		return db.insert(NAME, null, values);
	}

	static void resetModule(SQLiteDatabase db) {
		ContentValues values = new ContentValues(2);
		values.putNull(VOCAL1);
		values.putNull(VOCAL2);
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
}
