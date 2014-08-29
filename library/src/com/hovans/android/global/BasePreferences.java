package com.hovans.android.global;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hovans.android.log.LogByCodeLab;
import com.hovans.android.util.SimpleCrypto;

/**
 * BasePreferences.java
 * <p/>
 * Created by Hovan on 8/29/14.
 */
public class BasePreferences {

	/** Multi Threading에 적합하도록 volatile로 선언 */
	private volatile static SharedPreferences sPreferenceInstance = null;

	private static String sPackageName;

	/**
	 * Preferences 객체를 가져가기 윈한 함수<br/>
	 * Singleton을 적용한다.
	 * @return
	 */
	public static SharedPreferences getInstance() {
		final Context appContext = GlobalApplication.getContext();
		if(sPreferenceInstance == null) {
			sPreferenceInstance = PreferenceManager.getDefaultSharedPreferences(appContext);
			sPackageName = appContext.getPackageName();
		}
		return sPreferenceInstance;
	}

	public synchronized static long getLong(String key) {
		return getInstance().getLong(key, -1);
	}

	public synchronized static int getIntFromString(String key, int defaultValue) {
		return Integer.parseInt(getInstance().getString(key, String.valueOf(defaultValue)));
	}

	public synchronized static int getInt(String key, int defaultValue) {
		return getInstance().getInt(key, defaultValue);
	}

	public synchronized static boolean setInt(String key, int value) {
		return getInstance().edit().putInt(key, value).commit();
	}

	public synchronized static boolean getBoolean(String key, boolean defaultValue) {
		return getInstance().getBoolean(key, defaultValue);
	}

	public synchronized static String getString(String key, String defaultValue) {
		return getInstance().getString(key, defaultValue);
	}

	public synchronized static String getString(String key) {
		return getString(key, null);
	}

	public synchronized static SharedPreferences.Editor edit() {
		return getInstance().edit();
	}


	public synchronized static String getStringEncrypt(String key) {
		String encryptString = getInstance().getString(key, null);

		try {
			if(encryptString != null) {
				encryptString = SimpleCrypto.decrypt(sPackageName, encryptString);
			}
		} catch(Exception e) {
			LogByCodeLab.e(e, "key=" + key + ", encryptString=" + encryptString);
			edit().putString(key, null).commit();
			encryptString = null;
		}

		return encryptString;
	}

	public synchronized static boolean setStringEncrypt(String key, String value) {
		try {
			if(value != null) {
				value = SimpleCrypto.encrypt(sPackageName, value);
			}
			getInstance().edit().putString(key, value).commit();
			return true;
		} catch(Exception e) {
			LogByCodeLab.e(e, "key=" + key + ", value=" + value);
			return false;
		}
	}

	public static SharedPreferences getSharedPreferences(String fileName) {
		return GlobalApplication.getContext().getSharedPreferences(fileName, Context.MODE_PRIVATE);
	}
}
