package com.hovans.android.global;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import com.hovans.android.log.LogByCodeLab;
import com.hovans.android.util.SimpleCrypto;

/**
 * GlobalPreferences.java
 * <p/>
 * Created by Hovan on 8/29/14.
 */
public class GlobalPreferences {

	public static final String KEY_VERSION_CODE = "KEY_VERSION_CODE";

	/** Multi Threading에 적합하도록 volatile로 선언 */
	protected volatile SharedPreferences preferenceInstance = null;

	final static Context appContext = GlobalApplication.getContext();
	final static String packageName = appContext.getPackageName();

	protected static GlobalPreferences globalPreferences;
	/**
	 * Preferences 객체를 가져가기 윈한 함수<br/>
	 * Singleton을 적용한다.
	 * @return
	 */
	public static SharedPreferences getInstance() {
		if(globalPreferences == null) {
			globalPreferences = new GlobalPreferences();
		}
		return globalPreferences.preferenceInstance;
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

	public synchronized static String getStringEncrypt(String seed, String key) {
		String encryptString = getInstance().getString(key, null);

		try {
			if(encryptString != null) {
				LogByCodeLab.d("seed=" + seed + ", key=" + key + ", encryptString=" + encryptString);
				encryptString = SimpleCrypto.decrypt(seed, encryptString);
			}
		} catch(Exception e) {
			LogByCodeLab.e(e, "key=" + key + ", encryptString=" + encryptString);
//			edit().putString(key, null).commit();
			encryptString = null;
		}

		return encryptString;
	}

	public synchronized static boolean setStringEncrypt(String seed, String key, String value) {
		try {
			if(value != null) {
				value = SimpleCrypto.encrypt(seed, value);
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

	protected GlobalPreferences() {
		preferenceInstance = PreferenceManager.getDefaultSharedPreferences(appContext);
	}

	static boolean alreadyChecked = false;

	protected static boolean refreshVersionCode() {
		if(alreadyChecked == true) return false;
		alreadyChecked = true;

		boolean isUpdated = false;
		try {
			PackageInfo packageInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);

			int newVersionCode = packageInfo.versionCode;
			int oldVersionCode = getInt(KEY_VERSION_CODE, 0);

			if(newVersionCode > oldVersionCode) {
				edit().putInt(KEY_VERSION_CODE, newVersionCode).apply();
				isUpdated = true;
			}
		} catch (PackageManager.NameNotFoundException e) {
			LogByCodeLab.e(e);
		}
		return isUpdated;
	}
}
