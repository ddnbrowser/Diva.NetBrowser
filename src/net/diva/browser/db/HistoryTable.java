package net.diva.browser.db;

import net.diva.browser.model.History;
import net.diva.browser.util.DdNUtil;
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
	public static final String COOL_PER = "cool_per";
	public static final String FINE = "fine";
	public static final String FINE_PER = "fine_per";
	public static final String SAFE = "safe";
	public static final String SAFE_PER = "safe_per";
	public static final String SAD = "sad";
	public static final String SAD_PER = "sad_per";
	public static final String WORST = "worst";
	public static final String WORST_PER = "worst_per";
	public static final String COMBO = "combo";
	public static final String CHALLANGE_TIME = "challange_time";
	public static final String HOLD = "hold";
	public static final String TRIAL = "trial";
	public static final String TRIAL_RESULT = "trial_result";
	public static final String MODULE1 = "module1";
	public static final String MODULE2 = "module2";
	public static final String SE = "se";
	public static final String SKIN = "skin";
	public static final String LOCK = "lock";
	public static final String RESULT_PICTURE = "result_picture";

	public static final String[] COLUMNS = new String[] {
		MUSIC_ID,
		RANK,
		PLAY_DATE,
		PLAY_PLACE,
		CLEAR_STATUS,
		ACHIEVEMENT,
		SCORE,
		COOL,
		COOL_PER,
		FINE,
		FINE_PER,
		SAFE,
		SAFE_PER,
		SAD,
		SAD_PER,
		WORST,
		WORST_PER,
		COMBO,
		CHALLANGE_TIME,
		HOLD,
		TRIAL,
		TRIAL_RESULT,
		MODULE1,
		MODULE2,
		SE,
		SKIN,
		LOCK,
		RESULT_PICTURE,
	};

	public static final String WHERE_IDENTITY = String.format("%s=? and %s=?", PLAY_DATE, SCORE);
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
		.append(COOL_PER).append(" integer,")
		.append(FINE).append(" integer,")
		.append(FINE_PER).append(" integer,")
		.append(SAFE).append(" integer,")
		.append(SAFE_PER).append(" integer,")
		.append(SAD).append(" integer,")
		.append(SAD_PER).append(" integer,")
		.append(WORST).append(" integer,")
		.append(WORST_PER).append(" integer,")
		.append(COMBO).append(" integer,")
		.append(CHALLANGE_TIME).append(" integer,")
		.append(HOLD).append(" integer,")
		.append(TRIAL).append(" integer,")
		.append(TRIAL_RESULT).append(" integer,")
		.append(MODULE1).append(" text,")
		.append(MODULE2).append(" text,")
		.append(SE).append(" text,")
		.append(SKIN).append(" text,")
		.append(LOCK).append(" integer,")
		.append(RESULT_PICTURE).append(" text,")
		.append("UNIQUE(")
		.append(PLAY_DATE).append(", ")
		.append(SCORE)
		.append("))");
		return builder.toString();
	}

	static void addResutPictureColumns(SQLiteDatabase db){
		String format = "ALTER TABLE %s ADD %s %s";
		db.execSQL(String.format(format, TABLE_NAME, RESULT_PICTURE, "text"));
	}

	static void addUniqueKey(SQLiteDatabase db) {
		db.execSQL(create_statement().replace(TABLE_NAME, TABLE_NAME + "_tmp"));

		tmpInster(db, LOCK + "=1 and " + RESULT_PICTURE + " is not null and " + RESULT_PICTURE + " <> ''");
		tmpInster(db, RESULT_PICTURE + " is not null and " + RESULT_PICTURE + " <> ''");
		tmpInster(db, LOCK + "=1");
		tmpInster(db, null);

		db.execSQL("drop table " + TABLE_NAME);
		db.execSQL(create_statement());

		db.execSQL("insert into " + TABLE_NAME + " select * from " + TABLE_NAME + "_tmp");
		db.execSQL("drop table " + TABLE_NAME + "_tmp");
	}

	static void tmpInster(SQLiteDatabase db, String where){
		Cursor c = db.query(TABLE_NAME, new String[] {
				MUSIC_ID,
				RANK,
				PLAY_DATE,
				PLAY_PLACE,
				CLEAR_STATUS,
				ACHIEVEMENT,
				SCORE,
				COOL,
				COOL_PER,
				FINE,
				FINE_PER,
				SAFE,
				SAFE_PER,
				SAD,
				SAD_PER,
				WORST,
				WORST_PER,
				COMBO,
				CHALLANGE_TIME,
				HOLD,
				TRIAL,
				TRIAL_RESULT,
				MODULE1,
				MODULE2,
				SE,
				SKIN,
				LOCK,
				RESULT_PICTURE,
		}, where, null, null, null, null);

		while (c.moveToNext()){
			ContentValues values = new ContentValues(27);
			values.put(MUSIC_ID, c.getString(0));
			values.put(RANK, c.getInt(1));
			values.put(PLAY_DATE, c.getInt(2));
			values.put(PLAY_PLACE, c.getString(3));
			values.put(CLEAR_STATUS, c.getInt(4));
			values.put(ACHIEVEMENT, c.getInt(5));
			values.put(SCORE, c.getInt(6));
			values.put(COOL, c.getInt(7));
			values.put(COOL_PER, c.getInt(8));
			values.put(FINE, c.getInt(9));
			values.put(FINE_PER, c.getInt(10));
			values.put(SAFE, c.getInt(11));
			values.put(SAFE_PER, c.getInt(12));
			values.put(SAD, c.getInt(13));
			values.put(SAD_PER, c.getInt(14));
			values.put(WORST, c.getInt(15));
			values.put(WORST_PER, c.getInt(16));
			values.put(COMBO, c.getInt(17));
			values.put(CHALLANGE_TIME, c.getInt(18));
			values.put(HOLD, c.getInt(19));
			values.put(TRIAL, c.getInt(20));
			values.put(TRIAL_RESULT, c.getInt(21));
			values.put(MODULE1, c.getString(22));
			values.put(MODULE2, c.getString(23));
			values.put(SE, c.getString(24));
			values.put(SKIN, c.getString(25));
			values.put(LOCK, c.getInt(26));
			values.put(RESULT_PICTURE, c.getString(27));

			db.insert(TABLE_NAME + "_tmp", null, values);
		}

		c.close();
	}

	static long insert(SQLiteDatabase db, History history) {

		ContentValues values = new ContentValues(27);
		values.put(MUSIC_ID, history.music_id);
		values.put(RANK, history.rank);
		values.put(PLAY_DATE, history.play_date);
		values.put(PLAY_PLACE, history.play_place);
		values.put(CLEAR_STATUS, history.clear_status);
		values.put(ACHIEVEMENT, history.achievement);
		values.put(SCORE, history.score);
		values.put(COOL, history.cool);
		values.put(COOL_PER, history.cool_per);
		values.put(FINE, history.fine);
		values.put(FINE_PER, history.fine_per);
		values.put(SAFE, history.safe);
		values.put(SAFE_PER, history.safe_per);
		values.put(SAD, history.sad);
		values.put(SAD_PER, history.sad_per);
		values.put(WORST, history.worst);
		values.put(WORST_PER, history.worst_per);
		values.put(COMBO, history.combo);
		values.put(CHALLANGE_TIME, history.challange_time);
		values.put(HOLD, history.hold);
		values.put(TRIAL, history.trial);
		values.put(TRIAL_RESULT, history.trial_result);
		values.put(MODULE1, history.module1_id);
		values.put(MODULE2, history.module2_id);
		values.put(SE, history.se_id);
		values.put(SKIN, history.skin_id);
		values.put(LOCK, history.lock);
		values.put(RESULT_PICTURE, history.result_picture);

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
		return db.delete(TABLE_NAME, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date), String.valueOf(history.score) }) == 1;
	}

	static boolean lock(SQLiteDatabase db, History history){
		ContentValues cv = new ContentValues(1);
		cv.put(LOCK, history.lock);
		return db.update(TABLE_NAME, cv, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date), String.valueOf(history.score) }) == 1;
	}

	static boolean setPicture(SQLiteDatabase db, History history){
		ContentValues cv = new ContentValues(1);
		cv.put(RESULT_PICTURE, history.result_picture);
		return db.update(TABLE_NAME, cv, WHERE_IDENTITY, new String[] { String.valueOf(history.play_date), String.valueOf(history.score) }) == 1;
	}

	public static void decodeVal(String[] values){
		for(int i = 0; i < values.length; i++){
			switch(i){
			case 0:
				// MUSIC_ID
				String musicTitle = DdNUtil.getMusicTitle(values[i]);
				if(!"".equals(musicTitle))
					values[i] = musicTitle;
				break;
			case 22:
			case 23:
				// MODULE1, MODULE2
				values[i] = DdNUtil.getModuleName(values[i]);
				break;
			case 24:
				// SE
				values[i] = DdNUtil.getSeName(values[i]);
				break;
			case 25:
				// SKIN
				values[i] = DdNUtil.getSkinName(values[i]);
				break;
			default:
				break;
			}
		}
	}

}
