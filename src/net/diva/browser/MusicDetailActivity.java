package net.diva.browser;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MusicDetailActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_detail);

		ImageView image = (ImageView)findViewById(R.id.cover_art);
		image.setImageBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("coverart")));
	}

	public void closePopup(View v) {
		finish();
	}
}
