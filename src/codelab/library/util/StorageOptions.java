package codelab.library.util;

import android.os.Build;
import android.os.Environment;
import codelab.library.log.LogByCodeLab;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class StorageOptions {

	static final String DEFAULT_SD_PATH = "/mnt/sdcard";
	static ArrayList<String> mMounts;
	static ArrayList<String> mVold;

	public static String[] labels;
	public static String[] paths;
	public static int count = 0;

	static {
		refresh();
	}
	
	public static void refresh() {
		mMounts = new ArrayList<String>();
		mVold = new ArrayList<String>();
		 
		readMountsFile();

		//http://stackoverflow.com/questions/18560192/get-a-list-of-external-storage-in-android-4-3
		if(Build.VERSION.SDK_INT < 18) {	//Build.VERSION_CODES.JELLY_BEAN_MR2
			readVoldFile();
			compareMountsWithVold();
		}

		removeSameStorage();

		testAndCleanMountsList();

		setProperties();
	}

	static void readMountsFile() {
		/*
		 * Scan the /proc/mounts file and look for lines like this:
		 * /dev/block/vold/179:1 /mnt/sdcard vfat
		 * rw,dirsync,nosuid,nodev,noexec,
		 * relatime,uid=1000,gid=1015,fmask=0602,dmask
		 * =0602,allow_utime=0020,codepage
		 * =cp437,iocharset=iso8859-1,shortname=mixed,utf8,errors=remount-ro 0 0
		 * 
		 * When one is found, split it into its elements and then pull out the
		 * path to the that mount point and add it to the arraylist
		 */

		// some mount files don't list the default
		// path first, so we add it here to
		// ensure that it is first in our list
		mMounts.add(DEFAULT_SD_PATH);

		try {
			Scanner scanner = new Scanner(new File("/proc/mounts"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (line.startsWith("/dev/block/vold/")) {
					String[] lineElements = line.split("\\s+");

					if(lineElements.length > 2) {
						String element = lineElements[1];
						// don't add the default mount path
						// it's already in the list.
						if ("vfat".equals(lineElements[2]) && !lineElements[1].equals(DEFAULT_SD_PATH)) {
							mMounts.add(element);
						}
					}
				}
			}
		} catch (Exception e) {
			LogByCodeLab.w(e);
		}
	}

	static void readVoldFile() {
		/*
		 * Scan the /system/etc/vold.fstab file and look for lines like this:
		 * dev_mount sdcard /mnt/sdcard 1
		 * /devices/platform/s3c-sdhci.0/mmc_host/mmc0
		 * 
		 * When one is found, split it into its elements and then pull out the
		 * path to the that mount point and add it to the arraylist
		 */

		// some devices are missing the vold file entirely
		// so we add a path here to make sure the list always
		// includes the path to the first sdcard, whether real
		// or emulated.
		mVold.add(DEFAULT_SD_PATH);

		try {
			Scanner scanner = new Scanner(new File("/system/etc/vold.fstab"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (line.startsWith("dev_mount")) {
					String[] lineElements = line.split("\\s+");
					String element = lineElements[2];

					if (element.contains(":"))
						element = element.substring(0, element.indexOf(":"));

					// don't add the default vold path
					// it's already in the list.
					if (!element.equals(DEFAULT_SD_PATH))
						mVold.add(element);
				}
			}
		} catch (Exception e) {
			LogByCodeLab.w(e);
		}
	}

	/**
	 * Sometimes the two lists of mount points will be different. We only  want those mount points that are in both list.
	 * Compare the two lists together and remove items that are not in both lists. */
	static void compareMountsWithVold() {
		for (int i = 0; i < mMounts.size(); i++) {
			String mount = mMounts.get(i);
			if (!mVold.contains(mount))
				mMounts.remove(i--);
		}

		// don't need this anymore, clear the vold list to reduce memory
		// use and to prepare it for the next time it's needed.
		mVold.clear();
	}

	/**
	 * GingerBread 이상부터는 용량계산이 가능하다. 같은 Storage 가 있다면 여기서 제거.
	 */
	static void removeSameStorage() {
		if(Build.VERSION.SDK_INT < 9) return;

		File defaultSd = new File(DEFAULT_SD_PATH);

		for(String path : mMounts) {
			if(DEFAULT_SD_PATH.equals(path)) continue;
			File file = new File(path);
			if(defaultSd.getFreeSpace() == file.getFreeSpace() && defaultSd.getTotalSpace() == file.getTotalSpace()) {
				mMounts.remove(path);
			}
		}
	}

	static void testAndCleanMountsList() {
		/*
		 * Now that we have a cleaned list of mount paths Test each one to make
		 * sure it's a valid and available path. If it is not, remove it from
		 * the list.
		 */

		for (int i = 0; i < mMounts.size(); i++) {
			String mount = mMounts.get(i);
			File root = new File(mount);
			if (!root.exists() || !root.isDirectory() || !root.canWrite())
				mMounts.remove(i--);
		}
	}

	static void setProperties() {
		/*
		 * At this point all the paths in the list should be valid. Build the
		 * public properties.
		 */

		ArrayList<String> mLabels = new ArrayList<String>();

		int j = 0;
		if (mMounts.size() > 0) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
				mLabels.add("Auto");
			else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				if (Environment.isExternalStorageRemovable()) {
					mLabels.add("External SD Card 1");
					j = 1;
				} else mLabels.add("Internal Storage");
			} else {
				if (!Environment.isExternalStorageRemovable() || Environment.isExternalStorageEmulated())
					mLabels.add("Internal Storage");
				else {
					mLabels.add("External SD Card 1");
					j = 1;
				}
			}

			if (mMounts.size() > 1) {
				for (int i = 1; i < mMounts.size(); i++) {
					mLabels.add("External SD Card " + (i + j));
				}
			}
		}

		labels = new String[mLabels.size()];
		mLabels.toArray(labels);

		paths = new String[mMounts.size()];
		mMounts.toArray(paths);

		count = Math.min(labels.length, paths.length);

		// don't need this anymore, clear the mounts list to reduce memory
		// use and to prepare it for the next time it's needed.
		mMounts.clear();
	}
}