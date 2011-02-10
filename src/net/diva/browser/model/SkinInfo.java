package net.diva.browser.model;

import java.io.File;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class SkinInfo {
	public String group_id;
	public String id;
	public String name;
	public String image_path;
	public boolean purchased;

	public SkinInfo(String group_id_, String id_, String name_, boolean purchased_) {
		group_id = group_id_;
		id = id_;
		name = name_;
		purchased = purchased_;
	}

	public File getThumbnailPath(Context context) {
		if (image_path == null)
			return null;
		File directory = context.getDir("skin", Context.MODE_PRIVATE);
		return new File(directory, new File(image_path).getName());
	}

	public Drawable getThumbnail(Context context) {
		if (image_path == null)
			return null;
		return new BitmapDrawable(getThumbnailPath(context).getAbsolutePath());
	}
}
