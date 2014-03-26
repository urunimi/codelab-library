package com.hovans.android.app;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.hovans.android.log.LogByCodeLab;

/**
 * WakeLock과 같은 Permission을 관리한다.
 * @author hovan, arngard
 *
 */
public class PermissionManager {
	private PermissionManager() {}
	
	private static Map<String, WakeLockWrapper> mWakeLockMap = new ConcurrentHashMap<String, WakeLockWrapper>();

	/**
	 * {@link PermissionManager#getWakeLockInstance(Context, int, String)
	 * getWakeLockInstance(context, WakeLockWrapper.PARTIAL_WAKE_LOCK, tag)}와 같다.
	 * @return 만들어진 Wrapper
	 */
	synchronized public static WakeLockWrapper getWakeLockInstance(Context context, String tag) {
		return getWakeLockInstance(context, WakeLockWrapper.PARTIAL_WAKE_LOCK, tag);
	}
	
	/**
	 * {@link PermissionManager#getWakeLockInstance(Context, int, String)
	 * getWakeLockInstance(context, WakeLockWrapper.PARTIAL_WAKE_LOCK, context.getApplicationInfo().packageName)}와 같다.
	 * @return 만들어진 Wrapper
	 */
	synchronized public static WakeLockWrapper getWakeLockInstance(Context context) {
		return getWakeLockInstance(context, WakeLockWrapper.PARTIAL_WAKE_LOCK, context.getApplicationInfo().packageName);
	}
	
	/**
	 * {@link PermissionManager#getWakeLockInstance(Context, int, String)
	 * getWakeLockInstance(context, flags, context.getApplicationInfo().packageName)}와 같다.
	 * @return 만들어진 Wrapper
	 */
	synchronized public static WakeLockWrapper getWakeLockInstance(Context context, int flags) {
		return getWakeLockInstance(context, flags, context.getApplicationInfo().packageName);
	}
	
	
	/**
	 * {@link WakeLockWrapper}를 리턴.<br/>
	 * 권한이 있는지, 객체의 존재유무를 미리 검사를 하므로 좀 더 자유롭게 사용할 수 있다.
	 * @param context
	 * @param tag 새로 만들어야 할 경우, WakeLock 에 사용할 태그.
	 * @param flags 새로 만들어야 할 경우, WakeLock 에 사용할 flags. {@link android.os.PowerManager}
	 * @return 만들어진 Wrapper
	 */
	synchronized public static WakeLockWrapper getWakeLockInstance(Context context, int flags, String tag) {
		LogByCodeLab.v("WakeLockWrapper.getWakeLockInstance(), tag: " + tag);
		WakeLockWrapper found = mWakeLockMap.get(tag);
		if (found == null) {
			found = new WakeLockWrapper(context, tag, flags);
			mWakeLockMap.put(tag, found);
		}
		return found;
	}
	
	/**
	 * 이 매니저가 관리하고 있던 모든 {@link WakeLockWrapper}에 대해 release()를 시도한다.
	 * @return release 가 이루어진 횟수.
	 */
	synchronized public static int releaseAllWakeLocks() {
		int reVal = 0;
		for (WakeLockWrapper locker : mWakeLockMap.values()) {
			if (locker.release()) {
				++reVal;
			}
		}
		return reVal;
	}

	/**
	 * 이 매니저가 관리하고 있던 모든 {@link WakeLockWrapper} 객체를 제거한다.
	 * 필요하다면 release 도 같이 이루어짐.
	 * @return 제거한 객체의 개수.
	 */
	synchronized public static int freeAllWakeLocks() {
		int reVal = mWakeLockMap.size();
		releaseAllWakeLocks();
		mWakeLockMap.clear();
		return reVal;
	}

	/**
	 * {@link android.os.PowerManager.WakeLock}를 감싸고 있는 Class
	 * @author hovan, arngard
	 *
	 */
	public static class WakeLockWrapper {

		private static boolean isPermissionChecked = false;
		private static boolean isPermissionGranted = false;

		volatile private WakeLock wakeLock = null;

		/** @see PowerManager#PARTIAL_WAKE_LOCK */
		public static final int PARTIAL_WAKE_LOCK = PowerManager.PARTIAL_WAKE_LOCK;
		/** @see PowerManager#SCREEN_DIM_WAKE_LOCK */
		@SuppressWarnings("deprecation")
		public static final int SCREEN_DIM_WAKE_LOCK = PowerManager.SCREEN_DIM_WAKE_LOCK;
		/** @see PowerManager#FULL_WAKE_LOCK */
		@SuppressWarnings("deprecation")
		public static final int FULL_WAKE_LOCK = PowerManager.FULL_WAKE_LOCK;

		WakeLockWrapper(Context context, String tag, int flags) {
			if (isAvailable(context)) {
				wakeLock = ((PowerManager)context.getSystemService(Context.POWER_SERVICE))
				.newWakeLock(flags, tag);
			}
		}
		
		/*
		 * 이하 논리에서,
		 * 같은 객체에 대해 요청을 하거나 상태에 맞지 않은 요청이 들어왔을 때,
		 * 요청을 실질적으로 처리하지 않았음에도 불구하고
		 * 아무런 제약 없이 메소드는 정상 종료를 하도록 구성되어 있다.
		 * 
		 * 물론 외부에서 이런 내용을 몰라도 되겠지만,
		 * 필요에 따라 알 수도 있어야 한다.
		 * 
		 * 예를 들어 비동기 루틴에서 각자의 필요에 따라 락을 잡았을 때,
		 * 한 쪽이 먼저 요청하고 먼저 종료해버린다고 하자.
		 * 이 경우 나중에 종료하는 쪽은 후반부에 락 없이 진행해야 한다.
		 * 최소한 각 루틴이 이러한 상황을 알 수는 있어야 한다.
		 * 
		 * 따라서 각 요청에 대해 리턴값을 이용하여 자신의 동작 여부를 보고하게 한다.
		 * */

		/**
		 * @return 이 호출에 의해 aquire 가 수행되었다면 true.
		 * @see WakeLock#acquire()
		 */
		public boolean acquire() {
			if (wakeLock != null && wakeLock.isHeld() == false) {
				LogByCodeLab.v("WakeLockWrapper.acquire()");
				wakeLock.acquire();
				return true;
			}
			return false;
		}

		/**
		 * @return 이 호출에 의해 aquire 가 수행되었다면 true.
		 * @see WakeLock#acquire(long)
		 */
		public boolean acquire(long timeout) {
			if (wakeLock != null /*&& wakeLock.isHeld() == false*/) {	//이 경우엔 isHeld를 확인하면 무조건 acquire인 상태로 리턴 된다.
				LogByCodeLab.v("WakeLockWrapper.acquire() timeout: "+timeout);
				wakeLock.acquire(timeout);
				return true;
			}
			return false;
		}

		/**
		 * @return 이 호출에 의해 release 가 수행되었다면 true.
		 * @see WakeLock#release()
		 */
		public boolean release() {
			if (wakeLock != null && wakeLock.isHeld() == true) {
				LogByCodeLab.v("WakeLockWrapper.release()");
				wakeLock.release();
				return true;
			}
			return false;
		}

		/**
		 * WakeLock에 관련된 permission이 있는지 체크
		 * @param context
		 * @return 권한이 있으면 true
		 */
		synchronized public static boolean isAvailable(Context context) {
			if (isPermissionChecked == false) {
				isPermissionChecked = true;
				PackageManager packageManager = context.getPackageManager();
				if (PackageManager.PERMISSION_GRANTED
						== packageManager.checkPermission(android.Manifest.permission.WAKE_LOCK, context.getPackageName())) {
					isPermissionGranted = true;
				}
			}
			LogByCodeLab.v("WakeLockWrapper.isAvailable() result: "+isPermissionGranted);
			return isPermissionGranted;
		}
	}
}
