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

	public static SharedPreferences getInstance(String fileName) {
		return appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE);
	}

	public synchronized static long getLong(String key) {
		return getLong(key, -1);
	}

	public synchronized static long getLong(String key, long defaultValue) {
		long value;
		try {
			value = getInstance().getLong(key, defaultValue);
		} catch (Throwable e) {
			value = defaultValue;
			getInstance().edit().remove(key).apply();
		}
		return value;
	}

	public synchronized static int getIntFromString(String key, int defaultValue) {
		return Integer.parseInt(getInstance().getString(key, String.valueOf(defaultValue)));
	}

	public synchronized static int getInt(String key, int defaultValue) {
		int value;
		try {
			value = getInstance().getInt(key, defaultValue);
		} catch (Throwable e) {
			value = defaultValue;
			getInstance().edit().remove(key).apply();
		}
		return value;
	}

	public synchronized static boolean setInt(String key, int value) {
		return getInstance().edit().putInt(key, value).commit();
	}

	public synchronized static boolean getBoolean(String key, boolean defaultValue) {
		boolean value;
		try {
			value = getInstance().getBoolean(key, defaultValue);
		} catch (Throwable e) {
			value = defaultValue;
			getInstance().edit().remove(key).apply();
		}
		return value;
	}

	public synchronized static String getString(String key, String defaultValue) {
		String value;
		try {
			value = getInstance().getString(key, defaultValue);
		} catch (Throwable e) {
			value = defaultValue;
			getInstance().edit().remove(key).apply();
		}
		return value;
	}

	public synchronized static String getString(String key) {
		return getString(key, null);
	}

	public synchronized static SharedPreferences.Editor edit() {
		return getInstance().edit();
	}

//	public synchronized static String getStringEncrypt(String key) {
//		return getStringEncrypt(globalPreferences.packageName, key);
//	}

	public synchronized static String getStringEncrypt(String seed, String key) {
		String encryptString = getInstance().getString(key, null);

		try {
			if(encryptString != null) {
				if(LogByCodeLab.d()) LogByCodeLab.d("seed=" + seed + ", key=" + key + ", encryptString=" + encryptString);
				encryptString = SimpleCrypto.decrypt(seed, encryptString);
			}
		} catch(Exception e) {
			if(LogByCodeLab.d()) LogByCodeLab.e(e, "key=" + key + ", encryptString=" + encryptString);
//			edit().putString(key, null).commit();
			encryptString = null;
		}

		return encryptString;
	}


//	public synchronized static boolean setStringEncrypt(String key, String value) {
//		return setStringEncrypt(globalPreferences.packageName, key, value);
//	}


	public synchronized static boolean setStringEncrypt(String seed, String key, String value) {
		try {
			if(value != null) {
				value = SimpleCrypto.encrypt(seed, value);
			}
			SharedPreferences.Editor editor = getInstance().edit();
			if(value == null) {
				editor.remove(key);
			} else {
				editor.putString(key, value);
			}
			editor.commit();
			return true;
		} catch(Exception e) {
			LogByCodeLab.e(e, "key=" + key + ", value=" + value);
			return false;
		}
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
