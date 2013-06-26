package codelab.library.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;
import codelab.library.concurrent.ThreadGuest;
import codelab.library.constant.DebugConfig;
import codelab.library.global.GlobalApplication;

/**
 * 정형화된 포맷으로 로그캣 출력을 하기 위한 래핑 메소드들.
 * 
 * @author Arngard
 *
 */
public class LogByCodeLab {
	
	public static String formatLog(String message) {
		return new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS").format(new Date()) + " [" + Thread.currentThread().getId() + "] " + message;
	}

	public static boolean d() {
		return DebugConfig.isShowLogCat();
	}

	public static void v(String message) {
		if (d()) {
			Log.v(DebugConfig.LOG_TAG, formatLog(message));
		}
	}

	public static void d(String message) {
		if (d()) {
			String text = formatLog(message);
			Log.d(DebugConfig.LOG_TAG, text);
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
	
	static void writeLogToFile(final String message) {
		File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), DebugConfig.LOG_TAG);
		if(!root.canWrite())root.mkdirs();
		if (root.canWrite()) {
			final File logFile = new File(root, GlobalApplication.getContext().getPackageName() + new SimpleDateFormat("yyyy-MM-dd").format(new Date())+ ".txt");
			
			new ThreadGuest() {

				@Override
				public Object run(/*Handler initHandler, */long waitTimeMillis) {
					try {
						FileOutputStream fos = new FileOutputStream(logFile, true);
						fos.write((message+"\n").getBytes());
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
