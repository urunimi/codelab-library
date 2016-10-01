package com.hovans.android.log;

import android.util.Log;
import com.hovans.android.constant.DebugConfig;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 정형화된 포맷으로 로그캣 출력을 하기 위한 래핑 메소드들.
 * 
 * @author Arngard
 *
 */
public class LogByCodeLab {

	
	public static String formatLog(String message) {
		return "[" + Thread.currentThread().getId() + "] " + message;
	}

	public static boolean d() {
		return DebugConfig.isShowLogCat();
	}

	public static void v(String message) {
		v(DebugConfig.LOG_TAG, message);
	}

	public static void v(String tag, String message) {
		if (d()) {
			Log.v(tag, formatLog(message));
		}
	}

	public static void d(String message) {
		d(DebugConfig.LOG_TAG, message);
	}

	public static void d(String tag, String message) {
		if (d()) {
			String text = formatLog(message);
			Log.d(tag, text);
//			writeLogToFile(text);
		}
	}

	public static void i(String tag, String message) {
		if (d()) {
			String text = formatLog(message);
			Log.i(tag, text);
			FileLogger.getInstance().write(text);
		}
	}
	public static void i(String message) {
		i(DebugConfig.LOG_TAG, message);
	}


	public static void w(String tag, String message) {
		if (d()) {
			String text = formatLog(message);
			Log.w(tag, text);
			FileLogger.getInstance().write(text);
		}
	}

	public static void w(String message) {
		w(DebugConfig.LOG_TAG, message);
	}


	public static void w(Throwable e) {
		w(getStringFromThrowable(e));
	}

	public static void e(String message) {
		if (d()) {
			String text = formatLog(message);
			Log.e(DebugConfig.LOG_TAG, text);
			FileLogger.getInstance().write(text);
		}
	}

	public static void e(Throwable e) {
		e(getStringFromThrowable(e));
	}
	
	public static void e(Throwable e, String message) {
		e(message +"\n" + getStringFromThrowable(e));
	}
	
	public static String getStringFromThrowable(Throwable e) {
		StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    return sw.toString();
	}
}
