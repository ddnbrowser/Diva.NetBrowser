package net.diva.browser.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class CheckedFrameLayout extends FrameLayout implements Checkable {
	private boolean mChecked;
	private int mCheckMarkResource;
	private int mBasePaddingRight;
	private int mCheckMarkWidth;

	private static final int[] CHECKED_STATE_SET = {
		android.R.attr.state_checked
	};

	public CheckedFrameLayout(Context context) {
		super(context);
		setForegroundGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
	}

	public CheckedFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		setForegroundGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
	}

	public CheckedFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setForegroundGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
	}

	public void toggle() {
		setChecked(!mChecked);
	}

	public boolean isChecked() {
		return mChecked;
	}

	public void setChecked(boolean checked) {
		if (mChecked != checked) {
			mChecked = checked;
			refreshDrawableState();
		}
	}

	protected void setPaddingRight(int padding) {
		super.setPadding(getPaddingLeft(), getPaddingTop(), padding, getPaddingBottom());
	}

	public void setCheckMarkDrawable(int resid) {
		if (resid != 0 && resid == mCheckMarkResource) {
			return;
		}

		mCheckMarkResource = resid;

		Drawable d = null;
		if (mCheckMarkResource != 0) {
			d = getResources().getDrawable(mCheckMarkResource);
		}
		setCheckMarkDrawable(d);
	}

	public void setCheckMarkDrawable(Drawable d) {
		if (d != null) {
			setMinimumHeight(d.getIntrinsicHeight());

			mCheckMarkWidth = d.getIntrinsicWidth();
			setPaddingRight(mCheckMarkWidth + mBasePaddingRight);
			d.setState(getDrawableState());
		} else {
			setPaddingRight(mBasePaddingRight);
		}

		setForeground(d);
		requestLayout();
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		super.setPadding(left, top, right, bottom);
		mBasePaddingRight = getPaddingRight();
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		boolean populated = super.dispatchPopulateAccessibilityEvent(event);
		if (!populated) {
			event.setChecked(mChecked);
		}
		return populated;
	}
}
