package com.hovans.android.util;

import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import com.hovans.android.global.GlobalApplication;

/**
 * DisplayUtils.java
 * <p/>
 * Created by Hovan on 9/11/14.
 */
public class DisplayUtils {

	static Point sScreenSize;
	public static Point getScreenSize() {
		if(sScreenSize == null) {
			sScreenSize = new Point();
			WindowManager w = SystemService.getWindowManager();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				w.getDefaultDisplay().getSize(sScreenSize);
			} else {
				Display d = w.getDefaultDisplay();
				sScreenSize.set(d.getWidth(), d.getHeight());
			}
		}
		return sScreenSize;
	}

	public static int dipToPixel(int dipValue) {
		Resources r = GlobalApplication.getResource();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, r.getDisplayMetrics());
	}
}
