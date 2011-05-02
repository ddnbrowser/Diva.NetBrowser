package net.diva.browser.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class MyListTable implements BaseColumns {
	public static final String TABLE_NAME = "MyList";

	public static final String ID = "id";
	public static final String NAME = "name";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private MyListTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" integer UNIQUE,")
		.append(NAME).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, int id, String name) {
		ContentValues values = new ContentValues(2);
		values.put(ID, id);
		values.put(NAME, name);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, int id, String name) {
		ContentValues values = new ContentValues(1);
		values.put(NAME, name);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { String.valueOf(id) }) == 1;
	}
}
