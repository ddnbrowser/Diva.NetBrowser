package net.diva.browser.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class RecordTable implements BaseColumns {
	public static final String TABLE_NAME = "Record";

	public static final String CONTENT = "content";

	private RecordTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(CONTENT).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, int id, String record) {
		ContentValues values = new ContentValues(2);
		values.put(_ID, id);
		values.put(CONTENT, record);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, int id, String record) {
		ContentValues values = new ContentValues(1);
		values.put(CONTENT, record);
		return db.update(TABLE_NAME, values, String.format("%s=?", _ID), new String[] { String.valueOf(id) }) == 1;
	}
}
