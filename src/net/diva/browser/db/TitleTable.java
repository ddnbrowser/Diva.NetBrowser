package net.diva.browser.db;

import org.apache.http.NameValuePair;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class TitleTable implements BaseColumns {
	public static final String NAME = "Title";

	public static final String ID = "id";
	public static final String TITLE = "title";

	private TitleTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(TITLE).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, NameValuePair pair) {
		ContentValues values = new ContentValues(2);
		values.put(ID, pair.getName());
		values.put(TITLE, pair.getValue());
		return db.insert(NAME, null, values);
	}
}
