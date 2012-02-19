package net.diva.browser.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.History;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class HistoryStore extends ContentProvider {
	private static final String DATABASE_NAME = "history.db";
	private static final int VERSION = 1;

	private static HistoryStore s_instance;

	public static HistoryStore getInstance(Context context) {
		return s_instance;
	}

	private OpenHelper m_helper;

	public History getPlayHistory(long rowId) {
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(HistoryTable.TABLE_NAME, new String[] {
				HistoryTable.MUSIC_ID,
				HistoryTable.RANK,
				HistoryTable.PLAY_DATE,
				HistoryTable.PLAY_PLACE,
				HistoryTable.CLEAR_STATUS,
				HistoryTable.ACHIEVEMENT,
				HistoryTable.SCORE,
				HistoryTable.COOL,
				HistoryTable.FINE,
				HistoryTable.SAFE,
				HistoryTable.SAD,
				HistoryTable.WORST,
				HistoryTable.COMBO,
				HistoryTable.CHALLANGE_TIME,
				HistoryTable.HOLD,
				HistoryTable.TRIAL,
				HistoryTable.TRIAL_RESULT,
				HistoryTable.MODULE1,
				HistoryTable.MODULE2,
				HistoryTable.SE,
				HistoryTable.SKIN,
				HistoryTable.LOCK,
		}, HistoryTable._ID + "=?", new String[] { String.valueOf(rowId) }, null, null, null);
		try {
			if (!c.moveToNext())
				return null;
			History h = new History();
			h.music_id = c.getString(0);
			h.rank = c.getInt(1);
			h.play_date = c.getLong(2);
			h.play_place = c.getString(3);
			h.clear_status = c.getInt(4);
			h.achievement = c.getInt(5);
			h.score = c.getInt(6);
			h.cool = c.getInt(7);
			h.fine = c.getInt(8);
			h.safe = c.getInt(9);
			h.sad = c.getInt(10);
			h.worst = c.getInt(11);
			h.combo = c.getInt(12);
			h.challange_time = c.getInt(13);
			h.hold = c.getInt(14);
			h.trial = c.getInt(15);
			h.trial_result = c.getInt(16);
			h.module1_id = c.getString(17);
			h.module2_id = c.getString(18);
			h.se_id = c.getString(19);
			h.skin_id = c.getString(20);
			h.lock = c.getInt(21);
			return h;
		}
		finally {
			c.close();
		}
	}

	public List<String> getPlayHistoryList(String where, List<String> params, String orderBy){
		List<String> ret = new ArrayList<String>();
		SQLiteDatabase db = m_helper.getReadableDatabase();
		Cursor c = db.query(HistoryTable.TABLE_NAME, new String[] {
				HistoryTable.PLAY_DATE,
		}, where, params.toArray(new String[0]), null, null, orderBy);
		try {
			while (c.moveToNext()){
				ret.add(c.getString(0));
			}
		}
		finally {
			c.close();
		}

		return ret;
	}

	public void insert(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.insert(db, history);
	}

	public void deleteHistory(String music_id, int rank, int limit_date){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.delete(db, music_id, rank, limit_date);
	}

	public void deleteHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.delete(db, history);
	}

	public void lockHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.lock(db, history);
	}

	public List<byte[]> csvExport() throws IOException {
		List<byte[]> list = new ArrayList<byte[]>();

		Cursor c = m_helper.getReadableDatabase().query(HistoryTable.TABLE_NAME, new String[] {
				HistoryTable.MUSIC_ID,
				HistoryTable.RANK,
				HistoryTable.PLAY_DATE,
				HistoryTable.PLAY_PLACE,
				HistoryTable.CLEAR_STATUS,
				HistoryTable.ACHIEVEMENT,
				HistoryTable.SCORE,
				HistoryTable.COOL,
				HistoryTable.FINE,
				HistoryTable.SAFE,
				HistoryTable.SAD,
				HistoryTable.WORST,
				HistoryTable.COMBO,
				HistoryTable.CHALLANGE_TIME,
				HistoryTable.HOLD,
				HistoryTable.TRIAL,
				HistoryTable.TRIAL_RESULT,
				HistoryTable.MODULE1,
				HistoryTable.MODULE2,
				HistoryTable.SE,
				HistoryTable.SKIN,
				HistoryTable.LOCK,
		}, null, null, null, null, null);

		while (c.moveToNext()){
			StringBuffer sb = new StringBuffer();
			sb.append(c.getString(0)).append(",");
			sb.append(c.getInt(1)).append(",");
			sb.append(c.getInt(2)).append(",");
			sb.append(c.getString(3)).append(",");
			sb.append(c.getInt(4)).append(",");
			sb.append(c.getInt(5)).append(",");
			sb.append(c.getInt(6)).append(",");
			sb.append(c.getInt(7)).append(",");
			sb.append(c.getInt(8)).append(",");
			sb.append(c.getInt(9)).append(",");
			sb.append(c.getInt(10)).append(",");
			sb.append(c.getInt(11)).append(",");
			sb.append(c.getInt(12)).append(",");
			sb.append(c.getInt(13)).append(",");
			sb.append(c.getInt(14)).append(",");
			sb.append(c.getInt(15)).append(",");
			sb.append(c.getInt(16)).append(",");
			sb.append(c.getString(17)).append(",");
			sb.append(c.getString(18)).append(",");
			sb.append(c.getString(19)).append(",");
			sb.append(c.getString(20)).append(",");
			sb.append(c.getInt(21)).append("\r\n");

			list.add(sb.toString().getBytes());
		}

		return list;
	}

	private static class OpenHelper extends SQLiteOpenHelper {
		OpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(HistoryTable.create_statement());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	private static final String AUTHORITY = "net.diva.browser.history";

	private static final int PLAY_HISTORIES = 1;
	private static final int PLAY_HISTORY = 2;

	private static final UriMatcher s_matcher;

	static {
		s_matcher = new UriMatcher(UriMatcher.NO_MATCH);
		s_matcher.addURI(AUTHORITY, "plays", PLAY_HISTORIES);
		s_matcher.addURI(AUTHORITY, "plays/#", PLAY_HISTORY);
	}

	public static final Uri URI_HISTORIES = new Uri.Builder().scheme("content").authority(AUTHORITY).path("plays").build();

	@Override
	public boolean onCreate() {
		m_helper = new OpenHelper(getContext(), DATABASE_NAME, null, VERSION);
		s_instance = this;
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(HistoryTable.TABLE_NAME);

		switch (s_matcher.match(uri)) {
		case PLAY_HISTORIES:
			break;
		case PLAY_HISTORY:
			qb.appendWhere(HistoryTable._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		Cursor c = qb.query(m_helper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
}
