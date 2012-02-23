package net.diva.browser.db;

import net.diva.browser.model.History;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
/**
 *
 * @author silvia
 *
 */
public class HistoryTable implements BaseColumns {
	public static final String TABLE_NAME = "History";

	public static final String MUSIC_ID = "music_id";
	public static final String RANK = "rank";
	public static final String PLAY_DATE = "play_date";
	public static final String PLAY_PLACE = "play_place";
	public static final String CLEAR_STATUS = "clear_status";
	public static final String ACHIEVEMENT = "achievement";
	public static final String SCORE = "score";
	public static final String COOL = "cool";
	public static final String FINE = "fine";
	public static final String SAFE = "safe";
	public static final String SAD = "sad";
	public static final String WORST = "worst";
	public static final String COMBO = "combo";
	public static final String CHALLANGE_TIME = "challange_time";
	public static final String HOLD = "hold";
	public static final String TRIAL = "trial";
	public static final String TRIAL_RESULT = "trial_result";
	public static final String MODULE1 = "module1";
	public static final String MODULE2 = "module2";
	public static final String BUTTON_SE = "button_se";
	public static final String SKIN = "skin";
	public static final String LOCK = "lock";

	public static final String WHERE_IDENTITY = String.format("%s=?", PLAY_DATE);
	public static final String WHERE_BY_MUSIC = String.format("%s=? and %s=?", MUSIC_ID, RANK);
	public static final String WHERE_BY_DELETE = String.format("%s<? and %s<>?", PLAY_DATE, LOCK);
	public static final String WHERE_BY_MUSIC_DELETE = String.format("%s=? and %s=? and %s<? and %s<>?", MUSIC_ID, RANK, PLAY_DATE, LOCK);

	private HistoryTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(MUSIC_ID).append(" text,")
		.append(RANK).append(" integer,")
		.append(PLAY_DATE).append(" integer,")
		.append(PLAY_PLACE).append(" text,")
		.append(CLEAR_STATUS).append(" integer,")
		.append(ACHIEVEMENT).append(" integer,")
		.append(SCORE).append(" integer,")
		.append(COOL).append(" integer,")
		.append(FINE).append(" integer,")
		.append(SAFE).append(" integer,")
		.append(SAD).append(" integer,")
		.append(WORST).append(" integer,")
		.append(COMBO).append(" integer,")
		.append(CHALLANGE_TIME).append(" integer,")
		.append(HOLD).append(" integer,")
		.append(TRIAL).append(" integer,")
		.append(TRIAL_RESULT).append(" integer,")
		.append(MODULE1).append(" text,")
		.append(MODULE2).append(" text,")
		.append(BUTTON_SE).append(" text,")
		.append(SKIN).append(" text,")
		.append(LOCK).append(" integer")
		.append(")");
		return builder.toString();
	}

	static long insert(SQLiteDatabase db, History history) {

		String exist = null;
		Cursor c = db.query(TABLE_NAME, new String[] {
				PLAY_DATE,
		}, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date) }, null, null, PLAY_DATE);
		try {
			while (c.moveToNext())
				exist = c.getString(0);
		}
		finally {
			c.close();
		}
		if(exist != null)
			return 0;

		ContentValues values = new ContentValues(27);
		values.put(MUSIC_ID, history.music_id);
		values.put(RANK, history.rank);
		values.put(PLAY_DATE, history.play_date);
		values.put(PLAY_PLACE, history.play_place);
		values.put(CLEAR_STATUS, history.clear_status);
		values.put(ACHIEVEMENT, history.achievement);
		values.put(SCORE, history.score);
		values.put(COOL, history.cool);
		values.put(FINE, history.fine);
		values.put(SAFE, history.safe);
		values.put(SAD, history.sad);
		values.put(WORST, history.worst);
		values.put(COMBO, history.combo);
		values.put(CHALLANGE_TIME, history.challange_time);
		values.put(HOLD, history.hold);
		values.put(TRIAL, history.trial);
		values.put(TRIAL_RESULT, history.trial_result);
		values.put(MODULE1, history.module1);
		values.put(MODULE2, history.module2);
		values.put(BUTTON_SE, history.button_se);
		values.put(SKIN, history.skin);
		values.put(LOCK, history.lock);

		return db.insert(TABLE_NAME, null, values);
	}

	static boolean delete(SQLiteDatabase db, String music_id, int rank, int limit_date) {
		String where = null;
		String[] values = null;
		if(music_id != null){
			where = WHERE_BY_MUSIC_DELETE;
			values = new String[] {music_id, String.valueOf(rank), String.valueOf(limit_date), String.valueOf(1)};
		}else{
			where = WHERE_BY_DELETE;
			values = new String[] {String.valueOf(limit_date), String.valueOf(1)};
		}

		return db.delete(TABLE_NAME, where, values) > 0;
	}

	static boolean delete(SQLiteDatabase db, History history) {
		return db.delete(TABLE_NAME, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date) }) == 1;
	}

	static boolean lock(SQLiteDatabase db, History history){
		ContentValues cv = new ContentValues(1);
		cv.put(LOCK, history.lock);
		return db.update(TABLE_NAME, cv, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date) }) == 1;
	}

}
