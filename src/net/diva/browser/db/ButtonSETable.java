package net.diva.browser.db;

import net.diva.browser.model.ButtonSE;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class ButtonSETable implements BaseColumns {
	public static final String TABLE_NAME = "ButtonSE";

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String SAMPLE = "sample";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private ButtonSETable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(NAME).append(" text,")
		.append(SAMPLE).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, ButtonSE buttonSE) {
		ContentValues values = new ContentValues(3);
		values.put(ID, buttonSE.id);
		values.put(NAME, buttonSE.name);
		values.put(SAMPLE, buttonSE.sample);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, ButtonSE buttonSE) {
		ContentValues values = new ContentValues(2);
		values.put(NAME, buttonSE.name);
		values.put(SAMPLE, buttonSE.sample);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { buttonSE.id }) == 1;
	}
}
