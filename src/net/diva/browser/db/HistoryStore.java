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
	private static final int VERSION = 3;

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
				HistoryTable.COOL_RATE,
				HistoryTable.FINE,
				HistoryTable.FINE_RATE,
				HistoryTable.SAFE,
				HistoryTable.SAFE_RATE,
				HistoryTable.SAD,
				HistoryTable.SAD_RATE,
				HistoryTable.WORST,
				HistoryTable.WORST_RATE,
				HistoryTable.COMBO,
				HistoryTable.CHALLANGE_TIME,
				HistoryTable.HOLD,
				HistoryTable.SLIDE,
				HistoryTable.TRIAL,
				HistoryTable.TRIAL_RESULT,
				HistoryTable.PV_FORK,
				HistoryTable.MODULE1,
				HistoryTable.MODULE1_HEAD,
				HistoryTable.MODULE1_FACE,
				HistoryTable.MODULE1_FRONT,
				HistoryTable.MODULE1_BACK,
				HistoryTable.MODULE2,
				HistoryTable.MODULE2_HEAD,
				HistoryTable.MODULE2_FACE,
				HistoryTable.MODULE2_FRONT,
				HistoryTable.MODULE2_BACK,
				HistoryTable.MODULE3,
				HistoryTable.MODULE3_HEAD,
				HistoryTable.MODULE3_FACE,
				HistoryTable.MODULE3_FRONT,
				HistoryTable.MODULE3_BACK,
				HistoryTable.SE_BUTTON,
				HistoryTable.SE_SLIDE,
				HistoryTable.SE_CHAIN,
				HistoryTable.SE_TOUCH,
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
			h.cool_rate = c.getInt(8);
			h.fine = c.getInt(9);
			h.fine_rate = c.getInt(10);
			h.safe = c.getInt(11);
			h.safe_rate = c.getInt(12);
			h.sad = c.getInt(13);
			h.sad_rate = c.getInt(14);
			h.worst = c.getInt(15);
			h.worst_rate = c.getInt(16);
			h.combo = c.getInt(17);
			h.challange_time = c.getInt(18);
			h.hold = c.getInt(19);
			h.slide = c.getInt(20);
			h.trial = c.getInt(21);
			h.trial_result = c.getInt(22);
			h.pv_fork = c.getInt(23);
			h.module1 = createModule(c, 24);
			h.module2 = createModule(c, 29);
			h.module3 = createModule(c, 34);
			h.se_button = c.getString(39);
			h.se_slide = c.getString(40);
			h.se_chain = c.getString(41);
			h.se_touch = c.getString(42);
			h.skin = c.getString(43);
			h.lock = c.getInt(44);
			return h;
		}
		finally {
			c.close();
		}
	}

	private static History.Module createModule(Cursor c, int from) {
		History.Module module = new History.Module();
		module.base = c.getString(from);
		module.head = c.getString(from+1);
		module.face = c.getString(from+2);
		module.front = c.getString(from+3);
		module.back = c.getString(from+4);
		return module;
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
		long rowId = HistoryTable.insert(db, history);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(URI_HISTORIES, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
		}
	}

	public int deleteHistories(String selection, String[] args) {
		SQLiteDatabase db = m_helper.getWritableDatabase();
		final int deleted = db.delete(HistoryTable.TABLE_NAME, selection, args);
		if (deleted > 0)
			getContext().getContentResolver().notifyChange(URI_HISTORIES, null);
		return deleted;
	}

	public void deleteHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		if (HistoryTable.delete(db, history))
			getContext().getContentResolver().notifyChange(URI_HISTORIES, null);
	}

	public void lockHistory(History history){
		SQLiteDatabase db = m_helper.getWritableDatabase();
		if (HistoryTable.updateLockStatus(db, history))
			getContext().getContentResolver().notifyChange(URI_HISTORIES, null);
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
			switch (oldVersion) {
			case 1:
				HistoryTable.upgradeToVerB(db);
			case 2:
				HistoryTable.upgradeToFutureTone(db);
			default:
				break;
			}
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
