package net.diva.browser.db;

import net.diva.browser.model.TitleInfo;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class TitleTable implements BaseColumns {
	public static final String NAME = "Title";

	public static final String ID = "id";
	public static final String TITLE = "title";
	public static final String IMAGE_ID = "image_id";

	private TitleTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(TITLE).append(" text,")
		.append(IMAGE_ID).append(" text")
		.append(")");
		return builder.toString();
	}

	static void addImageIDColumn(SQLiteDatabase db) {
		db.execSQL(String.format("ALTER TABLE %s ADD %s text", NAME, IMAGE_ID));
	}

	static long insert(SQLiteDatabase db, TitleInfo title) {
		ContentValues values = new ContentValues(3);
		values.put(ID, title.id);
		values.put(TITLE, title.name);
		values.put(IMAGE_ID, title.image_id);
		return db.insert(NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, TitleInfo title) {
		ContentValues values = new ContentValues(1);
		values.put(IMAGE_ID, title.image_id);
		return db.update(NAME, values, String.format("%s=?", ID), new String[] { title.id }) == 1;
	}
}
