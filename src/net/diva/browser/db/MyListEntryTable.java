package net.diva.browser.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class MyListEntryTable implements BaseColumns {
	public static final String TABLE_NAME = "MyListEntry";

	public static final String ID = "id";
	public static final String NUMBER = "number";
	public static final String MUSIC_ID = "music_id";

	public static final String WHERE_BY_ID = String.format("%s=? AND %s is not null", ID, MUSIC_ID);
	public static final String WHERE_BY_MUSIC = String.format("%s=?", MUSIC_ID);
	public static final String WHERE_IDENTITY = String.format("%s=? AND %s=?", ID, NUMBER);

	private MyListEntryTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" integer,")
		.append(NUMBER).append(" integer,")
		.append(MUSIC_ID).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, int id, int number, String music_id) {
		ContentValues values = new ContentValues(3);
		values.put(ID, id);
		values.put(NUMBER, number);
		values.put(MUSIC_ID, music_id);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, int id, int number, String music_id) {
		ContentValues values = new ContentValues(1);
		values.put(MUSIC_ID, music_id);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY,
				new String[] { String.valueOf(id), String.valueOf(number) }) == 1;
	}

	static void clear(SQLiteDatabase db, int id) {
		ContentValues values = new ContentValues(1);
		values.putNull(MUSIC_ID);
		db.update(TABLE_NAME, values, WHERE_BY_ID, new String[] { String.valueOf(id) });
	}
}
