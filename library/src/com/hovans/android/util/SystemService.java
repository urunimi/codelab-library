package com.hovans.android.util;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import com.hovans.android.global.GlobalApplication;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: Hovan
 * Date: 13. 6. 22.
 * Time: PM 9:44
 * To change this template use File | Settings | File Templates.
 */
public class SystemService {

	static WeakReference<NotificationManager> sNotificationManager;

	public static NotificationManager getNotificationManager() {
		if(sNotificationManager == null || sNotificationManager.get() == null) {
			sNotificationManager = new WeakReference<>((NotificationManager) GlobalApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE));
		}
		return sNotificationManager.get();
	}

	static WeakReference<TelephonyManager> sTelephonyManager;
	public static TelephonyManager getTelephonyManager() {
		if(sTelephonyManager == null || sTelephonyManager.get() == null) {
			sTelephonyManager = new WeakReference<>((TelephonyManager) GlobalApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE));
		}
		return sTelephonyManager.get();
	}


	static WeakReference<AlarmManager> sAlarmManager;
	public static AlarmManager getAlarmManager() {
		if(sAlarmManager == null || sAlarmManager.get() == null) {
			sAlarmManager = new WeakReference<>((AlarmManager) GlobalApplication.getContext().getSystemService(Context.ALARM_SERVICE));
		}
		return sAlarmManager.get();
	}

	static WeakReference<PowerManager> sPowerManager;
	public static PowerManager getPowerManager() {
		if(sPowerManager == null || sPowerManager.get() == null) {
			sPowerManager = new WeakReference<>((PowerManager) GlobalApplication.getContext().getSystemService(Context.POWER_SERVICE));
		}
		return sPowerManager.get();
	}

	static WeakReference<ActivityManager> sActivityManager;
	public static ActivityManager getActivityManager() {
		if(sActivityManager == null || sActivityManager.get() == null) {
			sActivityManager = new WeakReference<>((ActivityManager) GlobalApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE));
		}
		return sActivityManager.get();
	}

	static WeakReference<WindowManager> sWindowManager;
	public static WindowManager getWindowManager() {
		if(sWindowManager == null || sWindowManager.get() == null) {
			sWindowManager = new WeakReference<>((WindowManager) GlobalApplication.getContext().getSystemService(Context.WINDOW_SERVICE));
		}
		return sWindowManager.get();
	}

	public static void setClipboardText(String text) {
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) GlobalApplication.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(text);
		} else {
			android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) GlobalApplication.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboardManager.setText(text);
		}
	}

	static WeakReference<AccountManager> sAccountManager;
	public static AccountManager getAccountManager() {
		if(sAccountManager == null || sAccountManager.get() == null) {
			sAccountManager = new WeakReference<>((AccountManager) GlobalApplication.getContext().getSystemService(Context.ACCOUNT_SERVICE));
		}
		return sAccountManager.get();
	}
}
