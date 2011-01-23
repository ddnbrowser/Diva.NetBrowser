package net.diva.browser.db;

import net.diva.browser.model.ModuleGroup;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class ModuleGroupTable implements BaseColumns {
	public static final String TABLE_NAME = "ModuleGroup";

	public static final String ID = "id";
	public static final String NAME = "name";

	private ModuleGroupTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(ID).append(" integer UNIQUE,")
		.append(NAME).append(" text")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, ModuleGroup group) {
		ContentValues values = new ContentValues(2);
		values.put(ID, group.id);
		values.put(NAME, group.name);
		return db.insert(TABLE_NAME, null, values);
	}
}
