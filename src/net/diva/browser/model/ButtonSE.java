package net.diva.browser.model;

import java.io.File;

import android.content.Context;

public class ButtonSE {
	public static final String INVALIDATE_COMMON = "INVALIDATE_COMMON";

	public String id;
	public String name;
	public String sample;

	public ButtonSE(String id_, String name_) {
		id = id_;
		name = name_;
	}

	public File getSamplePath(Context context) {
		if (sample == null)
			return null;
		File directory = context.getDir("button_se", Context.MODE_WORLD_READABLE);
		return new File(directory, new File(sample).getName());
	}
}
