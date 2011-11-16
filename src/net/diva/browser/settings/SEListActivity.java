package net.diva.browser.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.diva.browser.DdN;
import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.model.ButtonSE;
import net.diva.browser.model.MusicInfo;
import net.diva.browser.service.ServiceClient;
import net.diva.browser.service.ServiceTask;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SEListActivity extends ListActivity {
	private LocalStore m_store;
	private ButtonSEAdapber m_adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_list);
		getListView().setFastScrollEnabled(DdN.Settings.enableFastScroll);

		TextView empty = (TextView)findViewById(R.id.empty_message);
		if (empty != null)
			empty.setText(R.string.no_button_se);

		if (getIntent().getBooleanExtra("enable_invalidate", false))
			getListView().addHeaderView(invalidateCommonView());

		m_store = DdN.getLocalStore();
		m_adapter = new ButtonSEAdapber(this);
		m_adapter.setData(m_store.loadButtonSEs());
		setListAdapter(m_adapter);
	}

	@Override
	protected void onListItemClick(ListView list, View v, int position, long id) {
		ButtonSE se = (ButtonSE)list.getAdapter().getItem(position);
		Intent data = new Intent(getIntent());
		if (se != null)
			data.putExtra("id", se.id);
		else
			data.putExtra("invalidate", true);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.se_list_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_update:
			new UpdateTask().execute();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private View invalidateCommonView() {
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(android.R.layout.simple_list_item_1, getListView(), false);
		TextView text = (TextView)view.findViewById(android.R.id.text1);
		text.setText(R.string.invalidate_common_button_se);
		return view;
	}

	private class UpdateTask extends ServiceTask<Void, Void, List<ButtonSE>> {
		UpdateTask() {
			super(SEListActivity.this, R.string.message_updating);
		}

		@Override
		protected List<ButtonSE> doTask(ServiceClient service, Void... params) throws Exception {
			MusicInfo music = DdN.getPlayRecord().musics.get(0);
			final List<ButtonSE> buttonSEs = service.getButtonSEs(music.id);
			for (ButtonSE se: buttonSEs) {
				File file = se.getSamplePath(getApplicationContext());
				if (file.exists())
					continue;

				FileOutputStream out = null;
				try {
					out = new FileOutputStream(file);
					service.download(se.sample, out);
				}
				finally {
					if (out != null)
						out.close();
				}
			}
			m_store.updateButtonSEs(buttonSEs);
			return buttonSEs;
		}

		@Override
		protected void onResult(List<ButtonSE> result) {
			if (result == null)
				return;

			m_adapter.setData(result);
		}
	}

	private static class ButtonSEAdapber extends BaseAdapter {
		Context m_context;
		MediaPlayer m_player;
		List<ButtonSE> m_buttonSEs = Collections.emptyList();

		public ButtonSEAdapber(Context context) {
			m_context = context;
			m_player = new MediaPlayer();
		}

		void setData(List<ButtonSE> buttonSEs) {
			m_buttonSEs = buttonSEs;
			if (m_buttonSEs.isEmpty())
				notifyDataSetInvalidated();
			else
				notifyDataSetChanged();
		}

		public int getCount() {
			return m_buttonSEs.size();
		}

		public ButtonSE getItem(int position) {
			return m_buttonSEs.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		class Holder implements View.OnClickListener {
			ButtonSE se;
			TextView name;

			Holder(View view) {
				name = (TextView)view.findViewById(R.id.title);
				view.findViewById(R.id.button_sound).setOnClickListener(this);
			}

			void attach(ButtonSE se_) {
				se = se_;
				name.setText(se.name);
			}

			public void onClick(View v) {
				File file = se.getSamplePath(m_context);
				m_player.reset();
				FileInputStream in = null;
				try {
					in = new FileInputStream(file);
					m_player.setDataSource(in.getFD());
					m_player.prepare();
					m_player.start();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				finally {
					if (in != null)
						try { in.close(); } catch (IOException e) {}
				}
			}
		}

		public View getView(int position, View view, ViewGroup parent) {
			Holder holder;
			if (view != null)
				holder = (Holder)view.getTag();
			else {
				LayoutInflater inflater = LayoutInflater.from(m_context);
				view = inflater.inflate(R.layout.se_item, parent, false);
				holder = new Holder(view);
				view.setTag(holder);
			}

			holder.attach(getItem(position));
			return view;
		}

	}
}
