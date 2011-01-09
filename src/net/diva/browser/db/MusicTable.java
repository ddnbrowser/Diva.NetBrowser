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

	private MusicTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(TITLE).append(" text,")
		.append(COVERART).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, MusicInfo music) {
		ContentValues values = new ContentValues(3);
		values.put(ID, music.id);
		values.put(TITLE, music.title);
		values.put(COVERART, music.coverart);
		return db.insert(NAME, null, values);
	}
}
