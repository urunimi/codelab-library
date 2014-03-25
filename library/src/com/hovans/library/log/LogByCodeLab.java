package com.hovans.library.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.util.Log;

import com.hovans.library.concurrent.ThreadGuest;
import com.hovans.library.constant.DebugConfig;
import com.hovans.library.global.GlobalApplication;

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
			writeLogToFile(text);
		}
	}

	public static void i(String message) {
		if (d()) {
			String text = formatLog(message);
			Log.i(DebugConfig.LOG_TAG, text);
			writeLogToFile(text);
		}
	}

	public static void w(String message) {
		if (d()) {
			String text = formatLog(message);
			Log.w(DebugConfig.LOG_TAG, text);
			writeLogToFile(text);
		}
	}
	
	public static void w(Throwable e) {
		w(getStringFromThrowable(e));
	}

	public static void e(String message) {
		if (d()) {
			String text = formatLog(message);
			Log.e(DebugConfig.LOG_TAG, text);
			writeLogToFile(text);
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
	
	static void writeLogToFile(String message) {
		File root = new File(GlobalApplication.getContext().getExternalCacheDir(), DebugConfig.LOG_FOLDER);
		if(!root.canWrite())root.mkdirs();
		if (root.canWrite()) {
			Date today = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(today);
			calendar.add(Calendar.DATE, -2);

			final File oldFile = new File(root, new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime())+ ".txt");
			if(oldFile.exists()) {
				oldFile.delete();
			}

			calendar.setTime(today);

			final File logFile = new File(root, new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime())+ ".txt");

			final String messageWithTime = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS").format(today) + message;

			new ThreadGuest(ThreadGuest.PRIORITY_IDLE) {

				@Override
				public Object run(/*Handler initHandler, */long waitTimeMillis) {
					try {
						FileOutputStream fos = new FileOutputStream(logFile, true);
						fos.write((messageWithTime + "\n").getBytes());
						fos.close();
						
					} catch (IOException ioe) {
						Log.e(DebugConfig.LOG_TAG, "Could not write filen", ioe);
					}
					return null;
				}
				
			}.execute();
		}
	}
}
