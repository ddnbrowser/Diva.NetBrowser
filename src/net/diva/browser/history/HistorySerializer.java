package net.diva.browser.history;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.diva.browser.db.HistoryStore;
import net.diva.browser.db.HistoryTable;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.text.TextUtils;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class HistorySerializer {
	private static final String[] COLUMNS = new String[] {
		HistoryTable.PLAY_DATE,
		HistoryTable.PLAY_PLACE,
		HistoryTable.MUSIC_TITLE,
		HistoryTable.RANK,
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
	};

	private HistoryStore m_store;

	public HistorySerializer(HistoryStore store) {
		m_store = store;
	}

	public int importFrom(InputStream in) throws IOException {
		CSVReader reader = null;
		try {
			reader = new CSVReader(new InputStreamReader(in, "UTF-8"));
			return m_store.insertHistory(new ImportBinder(reader), COLUMNS);
		}
		finally {
			if (reader != null)
				reader.close();
		}
	}

	public int exportTo(OutputStream out) throws IOException {
		Cursor c = null;
		CSVWriter writer = null;
		try {
			c = m_store.query(HistoryStore.URI_HISTORIES, COLUMNS, null, null, HistoryTable.PLAY_DATE);
			if (c == null || !c.moveToNext())
				return 0;

			int exported = 0;
			writer = new CSVWriter(new OutputStreamWriter(out, "UTF-8"));
			String[] values = new String[COLUMNS.length];
			do {
				for (int i = 0; i < COLUMNS.length; ++i)
					values[i] = c.getString(i);
				writer.writeNext(values);
				++exported;
			} while (c.moveToNext());

			return exported;
		}
		finally {
			if (c != null)
				c.close();
			if (writer != null)
				writer.close();
		}
	}

	private static class ImportBinder implements HistoryStore.DataBinder {
		private CSVReader m_reader;

		public ImportBinder(CSVReader reader) {
			m_reader = reader;
		}

		@Override
		public boolean bindNext(InsertHelper helper, int[] indices) {
			try {
				String[] values = m_reader.readNext();
				if (values == null)
					return false;

				for (int i = 0; i < indices.length; ++i) {
					if (TextUtils.isEmpty(values[i]))
						helper.bindNull(indices[i]);
					else
						helper.bind(indices[i], values[i]);
				}

				return true;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

	}
}
