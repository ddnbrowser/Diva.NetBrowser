package net.diva.browser.db;

import net.diva.browser.model.SkinInfo;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class SkinTable implements BaseColumns {
	public static final String TABLE_NAME = "Skin";

	public static final String ID = "id";
	public static final String GROUP_ID = "group_id";
	public static final String NAME = "name";
	public static final String PATH = "path";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private SkinTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(GROUP_ID).append(" text,")
		.append(NAME).append(" text,")
		.append(PATH).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, SkinInfo skin) {
		ContentValues values = new ContentValues(4);
		values.put(ID, skin.id);
		values.put(GROUP_ID, skin.group_id);
		values.put(NAME, skin.name);
		if (skin.image_path == null)
			values.putNull(PATH);
		else
			values.put(PATH, skin.image_path);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, SkinInfo skin) {
		if (skin.image_path == null)
			return true;

		ContentValues values = new ContentValues(1);
		values.put(PATH, skin.image_path);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { skin.id }) == 1;
	}
}
