package net.diva.browser.db;

import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.History;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
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
				HistoryTable.MUSIC_TITLE,
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
				HistoryTable.BUTTON_SE,
				HistoryTable.SKIN,
				HistoryTable.LOCK,
		}, HistoryTable._ID + "=?", new String[] { String.valueOf(rowId) }, null, null, null);
		try {
			if (!c.moveToNext())
				return null;
			History h = new History();
			h.music_title = c.getString(0);
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
			h.module1 = c.getString(17);
			h.module2 = c.getString(18);
			h.button_se = c.getString(19);
			h.skin = c.getString(20);
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

	public void deleteHistory(String music_title, int rank, int limit_date){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.delete(db, music_title, rank, limit_date);
	}

	public void deleteHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.delete(db, history);
	}

	public void lockHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		HistoryTable.lock(db, history);
	}

	public interface DataBinder {
		boolean bindNext(DatabaseUtils.InsertHelper helper, int[] indices);
	}

	public int insertHistory(DataBinder binder, String[] names) {
		int count = 0;
		SQLiteDatabase db = m_helper.getWritableDatabase();
		DatabaseUtils.InsertHelper helper = new DatabaseUtils.InsertHelper(db, HistoryTable.TABLE_NAME);
		db.beginTransaction();
		try {
			int[] indices = new int[names.length];
			for (int i = 0; i < names.length; ++i)
				indices[i] = helper.getColumnIndex(names[i]);

			for (;;) {
				helper.prepareForInsert();
				if (!binder.bindNext(helper, indices))
					break;
				if (helper.execute() != -1)
					++count;
			}

			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
			helper.close();
		}

		getContext().getContentResolver().notifyChange(URI_HISTORIES, null);
		return count;
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
	public Uri insert(Uri uri, ContentValues initialValues) {
		if (s_matcher.match(uri) != PLAY_HISTORIES)
			throw new IllegalArgumentException("Unknown URI: " + uri);

		ContentValues values = null;
		if (initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();

		SQLiteDatabase db = m_helper.getWritableDatabase();
		long rowId = db.insert(HistoryTable.TABLE_NAME, null, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(URI_HISTORIES, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
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
