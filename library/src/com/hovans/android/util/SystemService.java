package com.hovans.android.util;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
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
			sNotificationManager = new WeakReference<NotificationManager>((NotificationManager) GlobalApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE));
		}
		return sNotificationManager.get();
	}

	static WeakReference<TelephonyManager> sTelephonyManager;
	public static TelephonyManager getTelephonyManager() {
		if(sTelephonyManager == null || sTelephonyManager.get() == null) {
			sTelephonyManager = new WeakReference<TelephonyManager>((TelephonyManager) GlobalApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE));
		}
		return sTelephonyManager.get();
	}


	static WeakReference<AlarmManager> sAlarmManager;
	public static AlarmManager getAlarmManager() {
		if(sAlarmManager == null || sAlarmManager.get() == null) {
			sAlarmManager = new WeakReference<AlarmManager>((AlarmManager) GlobalApplication.getContext().getSystemService(Context.ALARM_SERVICE));
		}
		return sAlarmManager.get();
	}

	static WeakReference<PowerManager> sPowerManager;
	public static PowerManager getPowerManager() {
		if(sPowerManager == null || sPowerManager.get() == null) {
			sPowerManager = new WeakReference<PowerManager>((PowerManager) GlobalApplication.getContext().getSystemService(Context.POWER_SERVICE));
		}
		return sPowerManager.get();
	}

	static WeakReference<WindowManager> sWindowManager;
	public static WindowManager getWindowManager() {
		if(sWindowManager == null || sWindowManager.get() == null) {
			sWindowManager = new WeakReference<WindowManager>((WindowManager) GlobalApplication.getContext().getSystemService(Context.WINDOW_SERVICE));
		}
		return sWindowManager.get();
	}
}
