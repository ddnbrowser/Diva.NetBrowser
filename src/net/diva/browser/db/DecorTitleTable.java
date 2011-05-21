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

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private DecorTitleTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(NAME).append(" text,")
		.append(STATUS).append(" integer")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, DecorTitle title) {
		ContentValues values = new ContentValues(3);
		values.put(ID, title.id);
		values.put(NAME, title.name);
		values.put(STATUS, title.purchased ? 1 : 0);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, DecorTitle title) {
		ContentValues values = new ContentValues(1);
		values.put(STATUS, title.purchased ? 1 : 0);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { title.id }) == 1;
	}
}
