package net.diva.browser.db;

import net.diva.browser.model.History;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
/**
 *
 * @author silvia
 *
 */
public class HistoryTable implements BaseColumns {
	public static final String TABLE_NAME = "History";

	public static final String MUSIC_TITLE = "music_title";
	public static final String RANK = "rank";
	public static final String PLAY_DATE = "play_date";
	public static final String PLAY_PLACE = "play_place";
	public static final String CLEAR_STATUS = "clear_status";
	public static final String ACHIEVEMENT = "achievement";
	public static final String SCORE = "score";
	public static final String COOL = "cool";
	public static final String COOL_RATE = "cool_rate";
	public static final String FINE = "fine";
	public static final String FINE_RATE = "fine_rate";
	public static final String SAFE = "safe";
	public static final String SAFE_RATE = "safe_rate";
	public static final String SAD = "sad";
	public static final String SAD_RATE = "sad_rate";
	public static final String WORST = "worst";
	public static final String WORST_RATE = "worst_rate";
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

	public static final int LOCKED = 1;

	public static final String WHERE_IDENTITY = String.format("%s=?", PLAY_DATE);

	private HistoryTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(TABLE_NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(PLAY_DATE).append(" integer UNIQUE,")
		.append(PLAY_PLACE).append(" text,")
		.append(MUSIC_TITLE).append(" text,")
		.append(RANK).append(" integer,")
		.append(CLEAR_STATUS).append(" integer,")
		.append(ACHIEVEMENT).append(" integer,")
		.append(SCORE).append(" integer,")
		.append(COOL).append(" integer,")
		.append(COOL_RATE).append(" integer,")
		.append(FINE).append(" integer,")
		.append(FINE_RATE).append(" integer,")
		.append(SAFE).append(" integer,")
		.append(SAFE_RATE).append(" integer,")
		.append(SAD).append(" integer,")
		.append(SAD_RATE).append(" integer,")
		.append(WORST).append(" integer,")
		.append(WORST_RATE).append(" integer,")
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
		ContentValues values = new ContentValues(27);
		values.put(MUSIC_TITLE, history.music_title);
		values.put(RANK, history.rank);
		values.put(PLAY_DATE, history.play_date);
		values.put(PLAY_PLACE, history.play_place);
		values.put(CLEAR_STATUS, history.clear_status);
		values.put(ACHIEVEMENT, history.achievement);
		values.put(SCORE, history.score);
		values.put(COOL, history.cool);
		values.put(COOL_RATE, history.cool_rate);
		values.put(FINE, history.fine);
		values.put(FINE_RATE, history.fine_rate);
		values.put(SAFE, history.safe);
		values.put(SAFE_RATE, history.safe_rate);
		values.put(SAD, history.sad);
		values.put(SAD_RATE, history.sad_rate);
		values.put(WORST, history.worst);
		values.put(WORST_RATE, history.worst_rate);
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

	static boolean delete(SQLiteDatabase db, History history) {
		return db.delete(TABLE_NAME, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date) }) == 1;
	}

	static boolean updateLockStatus(SQLiteDatabase db, History history) {
		ContentValues cv = new ContentValues(1);
		cv.put(LOCK, history.lock);
		return db.update(TABLE_NAME, cv, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date) }) == 1;
	}

	static void upgradeToVerB(SQLiteDatabase db) {
		// パーフェクトのコード値を3から4へ
		db.execSQL(String.format("UPDATE %s SET %s = 4 WHERE %s = %d",
				TABLE_NAME, CLEAR_STATUS,
				CLEAR_STATUS, 3));

		// エクセレントがNOT CLEAR状態になっているので達成率が超えている履歴に関しては
		// エクセレントに変更。激唱の95%Over閉店とか閉店コマンドに関しては考慮しない。
		// ロケテ期間はサポート外とする

		// Extreme
		db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
				TABLE_NAME, CLEAR_STATUS,
				PLAY_DATE, 1341439200,
				CLEAR_STATUS, 0,
				RANK, 3,
				ACHIEVEMENT, 9500));

		// Hard
		db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
				TABLE_NAME, CLEAR_STATUS,
				PLAY_DATE, 1341439200,
				CLEAR_STATUS, 0,
				RANK, 2,
				ACHIEVEMENT, 9000));

		// Normal
		db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
				TABLE_NAME, CLEAR_STATUS,
				PLAY_DATE, 1341439200,
				CLEAR_STATUS, 0,
				RANK, 1,
				ACHIEVEMENT, 8500));

		// Easy
		db.execSQL(String.format("UPDATE %s SET %s = 3 WHERE %s > %d AND %s = %d AND %s = %d AND %s >= %d",
				TABLE_NAME, CLEAR_STATUS,
				PLAY_DATE, 1341439200,
				CLEAR_STATUS, 0,
				RANK, 0,
				ACHIEVEMENT, 8000));
	}
}
