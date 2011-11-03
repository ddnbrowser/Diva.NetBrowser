package net.diva.browser.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
	private TimePicker m_picker;
	private int m_value;

	public TimePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setValue(int value) {
		m_value = value;
		persistInt(m_value);
	}

	public int getValue() {
		return m_value;
	}

	public void setTime(int hour, int minute) {
		setValue(hour * 60 + minute);
	}

	public int getHour() {
		return m_value / 60;
	}

	public int getMinute() {
		return m_value % 60;
	}

	@Override
	protected View onCreateDialogView() {
		m_picker = new TimePicker(getContext());
		m_picker.setIs24HourView(Boolean.TRUE);
		m_picker.setCurrentHour(getHour());
		m_picker.setCurrentMinute(getMinute());
		return m_picker;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			int value = m_picker.getCurrentHour()*60 + m_picker.getCurrentMinute();
			if (callChangeListener(value))
				setValue(value);
		}
		m_picker = null;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setValue(restoreValue ? getPersistedInt(m_value) : (Integer)defaultValue);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent())
			return superState;

		final SavedState state = new SavedState(superState);
		state.value = m_value;
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState)state;
		setValue(myState.value);
		super.onRestoreInstanceState(myState.getSuperState());
	}

	private static class SavedState extends BaseSavedState {
		int value;

		public SavedState(Parcel source) {
			super(source);
			value = source.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(value);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<TimePreference.SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel source) {
				return new SavedState(source);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
