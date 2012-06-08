package net.diva.browser.compatibility;

import android.app.Activity;
import android.support.v4.app.Fragment;

public class Compatibility {
	interface VersionImpl {
		public void invalidateOptionsMenu(Activity activity);
	}

	static class BaseVersionImpl implements VersionImpl {
		@Override
		public void invalidateOptionsMenu(Activity activity) {
		}
	}

	static class HoneycombVersionImpl implements VersionImpl {
		@Override
		public void invalidateOptionsMenu(Activity activity) {
			activity.invalidateOptionsMenu();
		}
	}

	static final VersionImpl IMPL;
	static {
		if (android.os.Build.VERSION.SDK_INT >= 11)
			IMPL = new HoneycombVersionImpl();
		else
			IMPL = new BaseVersionImpl();
	}

	public static void invalidateOptionsMenu(Fragment fragment) {
		IMPL.invalidateOptionsMenu(fragment.getActivity());
	}

	public static void invalidateOptionsMenu(Activity activity) {
		IMPL.invalidateOptionsMenu(activity);
	}
}