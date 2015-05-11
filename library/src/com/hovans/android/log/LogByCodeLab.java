package com.hovans.android.log;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.hovans.android.constant.DebugConfig;
import com.hovans.android.global.GlobalApplication;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
			writeLogToFile(text);
		}
	}
	public static void i(String message) {
		i(DebugConfig.LOG_TAG, message);
	}


	public static void w(String tag, String message) {
		if (d()) {
			String text = formatLog(message);
			Log.w(tag, text);
			writeLogToFile(text);
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
		File root = new File(GlobalApplication.getContext().getCacheDir(), DebugConfig.LOG_FOLDER);
		if(!root.canWrite())root.mkdirs();
		if (root.canWrite()) {
			Date today = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(today);
			calendar.add(Calendar.DATE, -2);

			final File oldFile = new File(root, dateFormat.format(calendar.getTime())+ ".txt");
			if(oldFile.exists()) {
				oldFile.delete();
			}

			calendar.setTime(today);

			final File logFile = new File(root, dateFormat.format(calendar.getTime())+ ".txt");

			final String messageWithTime = dateTimeFormat.format(today) + message;

			if(handlerThread == null) {
				handlerThread = new HandlerThread(DebugConfig.LOG_TAG);
				handlerThread.start();
				handler = new Handler(handlerThread.getLooper());
			}

			if(handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							FileOutputStream fos = new FileOutputStream(logFile, true);
							fos.write((messageWithTime + "\n").getBytes());
							fos.close();

						} catch (IOException ioe) {
							Log.e(DebugConfig.LOG_TAG, "Could not write filen", ioe);
						}
					}
				});
			}
		}
	}

	static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");

	static HandlerThread handlerThread;
	static Handler handler;
}
