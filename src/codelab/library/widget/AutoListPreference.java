package codelab.library.widget;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class AutoListPreference extends ListPreference {

	public AutoListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		// Notify activity about changes (to update preference summary line)
		notifyChanged();
	}
	
	@Override
	public CharSequence getTitle() {
		if(super.getSummary() == null || super.getEntry() == null) {
			return super.getTitle();
		} else {
			return super.getTitle() + ": " + super.getEntry();
		}
	}
	
	@Override
	public CharSequence getSummary() {
		if(super.getSummary() == null && super.getEntry() != null) {
			return super.getEntry();
		} else if(super.getSummary() != null) {
			return super.getSummary();
		} else {
			return super.getValue();
		}
	}
}
