package net.diva.browser.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.diva.browser.model.History;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryStore {
	private static final String DATABASE_NAME = "history.db";
	private static final int VERSION = 1;

	private static HistoryStore s_instance;
	private static Object[] s_lock = new Object[0];

	public static HistoryStore getInstance(Context context) {
		if (s_instance != null)
			return s_instance;

		synchronized (s_lock) {
			if (s_instance == null)
				s_instance = new HistoryStore(context.getApplicationContext());
		}
		return s_instance;
	}

	private OpenHelper m_helper;

	private HistoryStore(Context context) {
		m_helper = new OpenHelper(context, DATABASE_NAME, null, VERSION);
	}

	public List<History> getPlayHistoryList(List<String> dateList, String orderBy) {
		List<History> records = new ArrayList<History>();

		String where = HistoryTable.PLAY_DATE + " in (";
		for(int i = 0; i < dateList.size(); i++){
			if(i != 0)
				where += ",";
			where += "?";
		}
		where += ")";

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
				HistoryTable.COOL_PER,
				HistoryTable.FINE,
				HistoryTable.FINE_PER,
				HistoryTable.SAFE,
				HistoryTable.SAFE_PER,
				HistoryTable.SAD,
				HistoryTable.SAD_PER,
				HistoryTable.WORST,
				HistoryTable.WORST_PER,
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
		}, where, dateList.toArray(new String[0]), null, null, orderBy);
		try {
			while (c.moveToNext()){
				History h = new History();
				h.music_id = c.getString(0);
				h.rank = c.getInt(1);
				h.play_date = c.getInt(2);
				h.play_place = c.getString(3);
				h.clear_status = c.getInt(4);
				h.achievement = c.getInt(5);
				h.score = c.getInt(6);
				h.cool = c.getInt(7);
				h.cool_per = c.getInt(8);
				h.fine = c.getInt(9);
				h.fine_per = c.getInt(10);
				h.safe = c.getInt(11);
				h.safe_per = c.getInt(12);
				h.sad = c.getInt(13);
				h.sad_per = c.getInt(14);
				h.worst = c.getInt(15);
				h.worst_per = c.getInt(16);
				h.combo = c.getInt(17);
				h.challange_time = c.getInt(18);
				h.hold = c.getInt(19);
				h.trial = c.getInt(20);
				h.trial_result = c.getInt(21);
				h.module1_id = c.getString(22);
				h.module2_id = c.getString(23);
				h.se_id = c.getString(24);
				h.skin_id = c.getString(25);
				h.lock = c.getInt(26);

				records.add(h);
			}
		}
		finally {
			c.close();
		}

		return records;
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
				HistoryTable.COOL_PER,
				HistoryTable.FINE,
				HistoryTable.FINE_PER,
				HistoryTable.SAFE,
				HistoryTable.SAFE_PER,
				HistoryTable.SAD,
				HistoryTable.SAD_PER,
				HistoryTable.WORST,
				HistoryTable.WORST_PER,
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
			sb.append(c.getInt(17)).append(",");
			sb.append(c.getInt(18)).append(",");
			sb.append(c.getInt(19)).append(",");
			sb.append(c.getInt(20)).append(",");
			sb.append(c.getInt(21)).append(",");
			sb.append(c.getString(22)).append(",");
			sb.append(c.getString(23)).append(",");
			sb.append(c.getString(24)).append(",");
			sb.append(c.getString(25)).append(",");
			sb.append(c.getInt(26)).append("\r\n");

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
}
