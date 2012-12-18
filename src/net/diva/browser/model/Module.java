package net.diva.browser.model;

import java.io.File;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class Module {
	public String id;
	public String name;
	public boolean purchased;
	public String thumbnail;
	public String image;

	public File getThumbnailPath(Context context) {
		if (thumbnail == null)
			return null;
		File directory = context.getDir("module_thumbnail", Context.MODE_PRIVATE);
		return new File(directory, new File(thumbnail).getName());
	}

	public Drawable getThumbnail(Context context) {
		if (thumbnail == null)
			return null;
		return new BitmapDrawable(context.getResources(), getThumbnailPath(context).getAbsolutePath());
	}
}
