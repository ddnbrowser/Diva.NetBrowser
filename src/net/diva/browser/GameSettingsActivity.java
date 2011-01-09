package net.diva.browser;
import net.diva.browser.model.PlayRecord;
import net.diva.browser.settings.TitleListActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class GameSettingsActivity extends Activity {
	private static final int SET_TITLE = 1;;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_settings);

		initializeTitleItem(findViewById(R.id.title_item));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SET_TITLE:
			if (resultCode == RESULT_OK) {
				String title = null;
				final PlayRecord record = DdN.getPlayRecord();
				if (record != null)
					title = DdN.getTitle(record.title_id);
				setSummary(findViewById(R.id.title_item), title);
				setResult(RESULT_OK);
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	private void initializeTitleItem(View view) {
		String title = null;
		final PlayRecord record = DdN.getPlayRecord();
		if (record != null)
			title = DdN.getTitle(record.title_id);

		Button button = new Button(this);
		button.setText(R.string.button_change);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(GameSettingsActivity.this, TitleListActivity.class);
				intent.putExtra("title_id", record.title_id);
				startActivityForResult(intent, SET_TITLE);
			}
		});

		setup(view, R.string.description_title, title, button);
	}

	private void setSummary(View item, CharSequence summary) {
		TextView v = (TextView)item.findViewById(R.id.summary);
		v.setText(summary == null ? "" : summary);
	}

	private void setup(View item, int title, CharSequence summary, View widget) {
		if (title != 0) {
			TextView v = (TextView)item.findViewById(R.id.title);
			v.setText(title);
		}
		setSummary(item, summary);
		if (widget != null) {
			ViewGroup v = (ViewGroup)item.findViewById(R.id.widget_frame);
			v.addView(widget);
		}
	}
}
