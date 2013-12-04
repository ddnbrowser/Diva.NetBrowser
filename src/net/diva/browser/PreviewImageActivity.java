package net.diva.browser;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class PreviewImageActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preview_image);

		ImageView image = (ImageView)findViewById(R.id.image);
		image.setImageURI(getIntent().getData());
	}

	public void closePopup(View v) {
		finish();
	}
}
