package com.hovans.android.widget;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.hovans.android.R;

/**
 * IconListPreference.java
 * List Preference with icons
 *
 * @author Hovan Yoo
 */
public class IconListPreference extends ListPreference {

	private LayoutInflater mInflater;
	private List<Drawable> mEntryIcons = null;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private String mKey;
	private int selectedEntry = -1;

	public IconListPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public IconListPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

		mInflater = LayoutInflater.from(context);
		mKey = getKey();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		editor = prefs.edit();
	}

	public void setEntriesAndValuesAndIcons(List<CharSequence> entries, List<CharSequence> entryValues, List<Drawable> entryIcons) {
		super.setEntries(entries.toArray(new CharSequence[entries.size()]));
		super.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
		mEntryIcons = entryIcons;
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);

		CharSequence[] entries = getEntries();
		CharSequence[] entryValues = getEntryValues();

		if (entries.length != entryValues.length) {
			throw new IllegalStateException("ListPreference requires an entries array and an entryValues array which are both the same length");
		}

		if (mEntryIcons != null && entries.length != mEntryIcons.size()) {
			throw new IllegalStateException("IconListPreference requires the icons entries array be the same length than entries or null");
		}

		if (mEntryIcons != null) {
			String selectedValue = prefs.getString(mKey, "");
			for (int i = 0; i < entryValues.length; i++) {
				if (selectedValue.compareTo((String) entryValues[i]) == 0) {
					selectedEntry = i;
					break;
				}
			}
			builder.setAdapter(new IconListPreferenceScreenAdapter(), null);
		}
	}

	private class IconListPreferenceScreenAdapter extends BaseAdapter {

		public int getCount() {
			return getEntries().length;
		}

		class CustomHolder {
			private TextView text = null;
			private RadioButton rButton = null;

			CustomHolder(View row, int position) {
				text = (TextView) row.findViewById(R.id.image_list_view_row_text_view);
				text.setText(getEntries()[position]);

				rButton = (RadioButton) row.findViewById(R.id.image_list_view_row_radio_button);
				rButton.setId(position);
				rButton.setClickable(false);
				rButton.setChecked(selectedEntry == position);

				if (mEntryIcons != null) {
					text.setText(" " + text.getText());
					text.setCompoundDrawables(mEntryIcons.get(position), null, null, null);
				}
			}
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			CustomHolder holder;
			final int p = position;
			row = mInflater.inflate(R.layout.preference_icon_row, parent, false);
			holder = new CustomHolder(row, position);

			row.setTag(holder);

			// row.setClickable(true);
			// row.setFocusable(true);
			// row.setFocusableInTouchMode(true);
			row.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					v.requestFocus();

					Dialog mDialog = getDialog();
					mDialog.dismiss();

					IconListPreference.this.callChangeListener(getEntryValues()[p]);
					editor.putString(mKey, getEntryValues()[p].toString());
					selectedEntry = p;
					editor.commit();
				}
			});

			return row;
		}

	}

}
