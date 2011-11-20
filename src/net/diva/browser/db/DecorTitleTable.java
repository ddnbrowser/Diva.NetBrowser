package net.diva.browser.db;

import net.diva.browser.model.DecorTitle;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class DecorTitleTable implements BaseColumns {
	public static final String TABLE_NAME = "DecorTitle";

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String STATUS = "status";
	public static final String POSITION = "position";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private DecorTitleTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(NAME).append(" text,")
		.append(POSITION).append(" integer,")
		.append(STATUS).append(" integer")
		.append(")");
		return builder.toString();
	}

	static void addPositionColumns(SQLiteDatabase db) {
		String format = "ALTER TABLE %s ADD %s %s";
		db.execSQL(String.format(format, TABLE_NAME, POSITION, "integer"));

		ContentValues values = new ContentValues(1);
		values.put(POSITION, 1);
		db.update(TABLE_NAME, values, null, null);
	}

	static long insert(SQLiteDatabase db, DecorTitle title) {
		ContentValues values = new ContentValues(4);
		values.put(ID, title.id);
		values.put(NAME, title.name);
		values.put(POSITION, title.pre ? 1 : 0);
		values.put(STATUS, title.purchased ? 1 : title.prize ? 2 : 0);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, DecorTitle title) {
		ContentValues values = new ContentValues(1);
		values.put(STATUS, title.purchased ? 1 : title.prize ? 2 : 0);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { title.id }) == 1;
	}
}
