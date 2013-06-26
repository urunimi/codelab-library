package codelab.library.widget;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
import codelab.library.R;

/**
 * Custom preference for time selection. Hour and minute are persistent and
 * stored separately as ints in the underlying shared preferences under keys
 * KEY.hour and KEY.minute, where KEY is the preference's key.
 */
public class TimePreference extends DialogPreference {

	/** The widget for picking a time */
	private TimePicker timePicker;

	/** Default hour */
	private static int DEFAULT_HOUR = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

	/** Default minute */
	private static int DEFAULT_MINUTE = 0;

	/**
	 * Creates a preference for choosing a time based on its XML declaration.
	 * 
	 * @param context
	 * @param attributes
	 */
	public TimePreference(Context context, AttributeSet attributes) {
		super(context, attributes);
	}

	/**
	 * Initialize time picker to currently stored time preferences.
	 * 
	 * @param view
	 *            The dialog preference's host view
	 */
	@Override
	public void onBindDialogView(View view) {
		super.onBindDialogView(view);
		timePicker = (TimePicker) view.findViewById(R.id.prefTimePicker);
		int time = getSharedPreferences().getInt(getKey(), 0);
		if(time > 100) {
			try {
				DEFAULT_HOUR = time/100;
				DEFAULT_MINUTE = time%100;
			} catch(Exception e) {
				//Do nothing.
			}
		}
		timePicker.setCurrentHour(DEFAULT_HOUR);
		timePicker.setCurrentMinute(DEFAULT_MINUTE);
		timePicker.setIs24HourView(DateFormat.is24HourFormat(timePicker.getContext()));
	}

	/**
	 * Handles closing of dialog. If user intended to save the settings,
	 * selected hour and minute are stored in the preferences with keys KEY.hour
	 * and KEY.minute, where KEY is the preference's KEY.
	 * 
	 * @param okToSave
	 *            True if user wanted to save settings, false otherwise
	 */
	@Override
	protected void onDialogClosed(boolean okToSave) {
		super.onDialogClosed(okToSave);
		if (okToSave) {
			timePicker.clearFocus();
			SharedPreferences.Editor editor = getEditor();
			int setValue = timePicker.getCurrentHour()*100 + timePicker.getCurrentMinute();
			editor.putInt(getKey(), setValue);
			editor.commit();
			setSummary(getTimeString(setValue));
		}
	}
	
	public static String getTimeString(int time) {
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 12, 17, time/100, time%100, 0);
		return java.text.DateFormat.getTimeInstance().format(cal.getTime());
	}
}
