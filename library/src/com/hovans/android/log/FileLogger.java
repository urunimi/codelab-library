package com.hovans.android.log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.hovans.android.constant.DebugConfig;
import com.hovans.android.global.GlobalAppHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileLogger {
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.US);

	static HandlerThread handlerThread;
	static Handler handler;

	public void write(String message) {
		File root = getLogFolder();
		if (!root.canWrite()) root.mkdirs();
		if (root.canWrite()) {
			Date today = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(today);
			calendar.add(Calendar.DATE, -5);    //5일 지난건 삭제

			final File oldFile = new File(root, dateFormat.format(calendar.getTime()) + ".txt");
			if (oldFile.exists()) {
				oldFile.delete();
			}

			calendar.setTime(today);

			final File logFile = new File(root, dateFormat.format(calendar.getTime()) + ".txt");

			final String messageWithTime = dateTimeFormat.format(today) + message;

			if (handlerThread == null) {
				handlerThread = new HandlerThread(DebugConfig.LOG_TAG);
				handlerThread.start();
				handler = new Handler(handlerThread.getLooper());
			}

			if (handler != null) {
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

	File getLogFolder() {
		return new File(GlobalAppHolder.get().getContext().getCacheDir(), DebugConfig.LOG_FOLDER);
	}

	public void send(Context context) {
		try {
			File outZipPath = new File(GlobalAppHolder.get().getContext().getCacheDir(), "Logs.zip");
			FileOutputStream fos = new FileOutputStream(outZipPath);
			ZipOutputStream zos = new ZipOutputStream(fos);
			File srcFile = getLogFolder();
			File[] files = srcFile.listFiles();
			Log.d("", "Zip directory: " + srcFile.getName());
			for (File file : files) {
				Log.d("", "Adding file: " + file.getName());
				byte[] buffer = new byte[1024];
				FileInputStream fis = new FileInputStream(file);
				zos.putNextEntry(new ZipEntry(file.getName()));
				int length;
				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}
				zos.closeEntry();
				fis.close();
			}
			zos.close();

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			Uri uri = Uri.fromFile(outZipPath);
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			context.startActivity(Intent.createChooser(intent, "Send email..."));
		} catch (IOException ioe) {
			Log.e("", ioe.getMessage());
		}
	}

	private static FileLogger ourInstance = new FileLogger();

	public static FileLogger getInstance() {
		return ourInstance;
	}

	private FileLogger() {
	}
}