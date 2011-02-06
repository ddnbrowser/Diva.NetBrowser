package net.diva.browser.db;

import net.diva.browser.model.Module;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class ModuleTable implements BaseColumns {
	public static final String TABLE_NAME = "Module";

	public static final String ID = "id";
	public static final String GROUP_ID = "group_id";
	public static final String NAME = "name";
	public static final String STATUS = "status";
	public static final String IMAGE = "image";
	public static final String THUMBNAIL = "thumbnail";

	private static final String WHERE_IDENTITY = String.format("%s=?", ID);

	private ModuleTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" text UNIQUE,")
		.append(GROUP_ID).append(" integer,")
		.append(NAME).append(" text,")
		.append(IMAGE).append(" text,")
		.append(THUMBNAIL).append(" text,")
		.append(STATUS).append(" integer")
		.append(")");
		return builder.toString();
	}

	static void addImagePathColumn(SQLiteDatabase db) {
		db.execSQL(String.format("ALTER TABLE %s ADD %s text", TABLE_NAME, IMAGE));
		db.execSQL(String.format("ALTER TABLE %s ADD %s text", TABLE_NAME, THUMBNAIL));
	}

	static long insert(SQLiteDatabase db, int group_id, Module module) {
		ContentValues values = new ContentValues(5);
		values.put(ID, module.id);
		values.put(GROUP_ID, group_id);
		values.put(NAME, module.name);
		values.put(STATUS, module.purchased ? 1 : 0);
		values.putNull(IMAGE);
		values.putNull(THUMBNAIL);
		return db.insert(TABLE_NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, int group_id, Module module) {
		ContentValues values = new ContentValues(3);
		values.put(GROUP_ID, group_id);
		values.put(NAME, module.name);
		values.put(STATUS, module.purchased ? 1 : 0);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { module.id }) == 1;
	}

	static boolean update(SQLiteDatabase db, Module module) {
		ContentValues values = new ContentValues(3);
		values.put(STATUS, module.purchased ? 1 : 0);
		values.put(IMAGE, module.image);
		values.put(THUMBNAIL, module.thumbnail);
		return db.update(TABLE_NAME, values, WHERE_IDENTITY, new String[] { module.id }) == 1;
	}
}
