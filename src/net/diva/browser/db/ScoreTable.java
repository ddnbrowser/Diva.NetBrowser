package net.diva.browser.db;

import net.diva.browser.model.Ranking;
import net.diva.browser.model.ScoreRecord;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

final class ScoreTable implements BaseColumns {
	public static final String NAME = "Score";

	public static final String MUSIC_ID = "music_id";
	public static final String RANK = "rank";
	public static final String DIFFICULTY = "difficulty";
	public static final String CLEAR_STATUS = "clear_status";
	public static final String TRIAL_STATUS = "trial_status";
	public static final String HIGH_SCORE = "high_score";
	public static final String ACHIEVEMENT = "achievement";

	public static final String RANKING = "ranking";
	public static final String RANKIN_DATE = "rankin_date";
	public static final String RANKIN_SCORE = "rankin_score";

	private static final String WHERE_IDENTITY = String.format("%s=? AND %s=?", MUSIC_ID, RANK);

	private ScoreTable() {}

	static String create_statement() {
		StringBuilder builder = new StringBuilder();
		builder.append("create table ").append(NAME).append(" (")
		.append(_ID).append(" integer primary key autoincrement,")
		.append(MUSIC_ID).append(" text,")
		.append(RANK).append(" integer,")
		.append(DIFFICULTY).append(" integer,")
		.append(CLEAR_STATUS).append(" integer,")
		.append(TRIAL_STATUS).append(" integer,")
		.append(HIGH_SCORE).append(" integer,")
		.append(ACHIEVEMENT).append(" integer,")
		.append(RANKING).append(" integer,")
		.append(RANKIN_DATE).append(" integer,")
		.append(RANKIN_SCORE).append(" integer,")
		.append("UNIQUE(").append(MUSIC_ID).append(", ").append(RANK)
		.append("))");
		return builder.toString();
	}

	static void addRankingColumns(SQLiteDatabase db) {
		String format = "ALTER TABLE %s ADD %s INTEGER";
		db.execSQL(String.format(format, NAME, RANKING));
		db.execSQL(String.format(format, NAME, RANKIN_DATE));
		db.execSQL(String.format(format, NAME, RANKIN_SCORE));
	}

	static long insert(SQLiteDatabase db, String music_id, int rank, ScoreRecord score) {
		if (score == null)
			return -1;
		ContentValues values = new ContentValues(7);
		values.put(MUSIC_ID, music_id);
		values.put(RANK, rank);
		values.put(DIFFICULTY, score.difficulty);
		values.put(CLEAR_STATUS, score.clear_status);
		values.put(TRIAL_STATUS, score.trial_status);
		values.put(HIGH_SCORE, score.high_score);
		values.put(ACHIEVEMENT, score.achievement);
		return db.insert(NAME, null, values);
	}

	static boolean update(SQLiteDatabase db, String music_id, int rank, ScoreRecord score) {
		if (score == null)
			return false;
		ContentValues values = new ContentValues(5);
		values.put(DIFFICULTY, score.difficulty);
		values.put(CLEAR_STATUS, score.clear_status);
		values.put(TRIAL_STATUS, score.trial_status);
		values.put(HIGH_SCORE, score.high_score);
		values.put(ACHIEVEMENT, score.achievement);
		return db.update(NAME, values, WHERE_IDENTITY,
				new String[] { music_id, String.valueOf(rank) }) == 1;
	}

	static void clearRanking(SQLiteDatabase db) {
		ContentValues values = new ContentValues(3);
		values.putNull(RANKING);
		values.putNull(RANKIN_DATE);
		values.putNull(RANKIN_SCORE);
		db.update(NAME, values, null, null);
	}

	static boolean update(SQLiteDatabase db, Ranking entry) {
		ContentValues values = new ContentValues(3);
		values.put(RANKING, entry.ranking);
		values.put(RANKIN_DATE, entry.date);
		values.put(RANKIN_SCORE, entry.score);
		return db.update(NAME, values, WHERE_IDENTITY,
				new String[] { entry.id, String.valueOf(entry.rank) }) == 1;
	}
}
