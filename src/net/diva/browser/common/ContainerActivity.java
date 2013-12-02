package net.diva.browser.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.FrameLayout;

public class ContainerActivity extends FragmentActivity {
	private static final String KEY_FRAGMENT = "fragment";
	private static final String KEY_ARGS = "args";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final FrameLayout content = new FrameLayout(this);
		content.setId(android.R.id.content);
		setContentView(content);

		FragmentManager manager = getSupportFragmentManager();
		Fragment f = manager.findFragmentById(android.R.id.content);
		if (f == null) {
			Intent i = getIntent();
			f = Fragment.instantiate(this, i.getStringExtra(KEY_FRAGMENT), i.getBundleExtra(KEY_ARGS));
			manager.beginTransaction().add(android.R.id.content, f).commit();
		}
	}

	public static Intent makeIntent(Context context, String fragment, Bundle args) {
		Intent i = new Intent(context, ContainerActivity.class);
		i.putExtra(KEY_FRAGMENT, fragment);
		i.putExtra(KEY_ARGS, args);
		return i;
	}
}
