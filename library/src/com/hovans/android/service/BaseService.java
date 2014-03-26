package com.hovans.android.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.hovans.android.log.LogByCodeLab;

/**
 * 안드로이드 서비스 기반 구조에 대한 기본적인 부가 처리를 위한 래핑.<br>
 * <br>
 * 현재 이 클래스의 구현은 버전 호환성 문제와 작업 스레드 분리를 다룬다.
 * 
 * @author Arngard, Hovan
 */
public abstract class BaseService extends Service {

	/*	생명주기 대응 시작	*/

	@SuppressWarnings("rawtypes")
	private static final Class[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
	@SuppressWarnings("rawtypes")
	private static final Class[] mStopForegroundSignature = new Class[] {boolean.class};
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	/**
	 * {@link BaseService#startForegroundCompat(int, Notification) startForegroundCompat()} 또는
	 * {@link BaseService#stopForegroundCompat(int) stopForegroundCompat()}을 사용하기 위해서는
	 * 이 클래스를 상속받은 클래스의 onCreate()에서 반드시 super.onCreate()을 호출해야함.
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		try {
			mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
	}

	/**
	 * 가급적 이 메소드를 직접 이용하지 말고 {@link #handleStart(Intent, int)}를 이용할 것.<br>
	 * Android 2.0 미만 대응.
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		// 여기서 super.onStart()를 호출하면 안 됨.
		handleStart(intent, startId);
	}

	/**
	 * 가급적 이 메소드를 직접 이용하지 말고 {@link #handleStart(Intent, int)}를 이용할 것.<br>
	 * Android 2.0 이상 대응. onStart()를 대체하는 함수.<br>
	 * API에 예시된 코드 참고할 것.
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// 여기서 super.onStartCommand()를 호출하면 안 됨.
		handleStart(intent, startId);
		return START_STICKY;
	}

	/**
	 * 서비스 시작시 호출됨.<br>
	 * 즉, {@link BaseService#onStart(Intent, int) onStart(Intent, int)} 또는
	 * {@link BaseService#onStartCommand(Intent, int, int) onStartCommand(Intent, int, int)}에서 호출됨.
	 * 상속받은 클래스에서는 위 함수들을 직접 상속하지 말고 이 함수를 이용할 것.
	 */
	abstract protected void handleStart(Intent intent, int startId);

	/**
	 * This is a wrapper around the new startForeground method, using the older APIs if it is not available.
	 * @param id
	 * @param notification
	 * @see Service#startForeground(int, Notification)
	 * @see Service#setForeground(boolean)
	 */
	public final void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				LogByCodeLab.e(e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				LogByCodeLab.e(e);
			}
			return;
		}

		// Fall back on the old API.
		// Proguard compile 문제로 setForeground(true)대신 invoke를 사용한다.
		try {
			mStopForegroundArgs[0] = Boolean.TRUE;
			getClass().getMethod("setForeground",new Class[] {boolean.class}).invoke(this, mStopForegroundArgs);
		} catch (Throwable e) {
			// Should not happen.
			LogByCodeLab.e(e);
		}
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older APIs if it is not available.
	 * @param id
	 * @see Service#stopForeground(boolean)
	 * @see Service#setForeground(boolean)
	 */
	public final void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				LogByCodeLab.e(e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				LogByCodeLab.e(e);
			}
			return;
		}

		// Fall back on the old API.  Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		try {
			mStopForegroundArgs[0] = Boolean.FALSE;
			getClass().getMethod("setForeground",new Class[] {boolean.class}).invoke(this, mStopForegroundArgs);
		} catch (Throwable e) {
			// Should not happen.
			LogByCodeLab.e(e);
		}
	}

	/*	생명주기 대응 끝	*/

	/*	바인딩 시작	*/

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*	바인딩 끝	*/

	/*	Worker Thread 시작	*/

	/**
	 * Service 내부의 Worker Thread의 Looper와 통신하는 Handler.<br>
	 * 생성자에서 Worker thread 의 looper 를 넘긴다.
	 * @author namkhoh
	 */
	protected final class WorkerHandler extends Handler {
		public WorkerHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			onWorkerRequest(msg);
		}
	}

	protected HandlerThread mWorker = null;
	protected WorkerHandler mHandler = null;

	/**
	 * Handler Thread를 사용해 Service 내부적으로 동작하는 Worker Thread를 만든다.
	 * (HandlerThread는 내부적으로 Looper를 가지기 때문에 Handler를 해당 Thread에 대해서 사용할 수 있다.)
	 * @param tag 작업 스레드 이름
	 * @see #endWorker()
	 */
	protected void startWorker(String tag) {
		// 작업 스레드를 시작한다. 중복 호출을 허용하는 안전 시작.
		if (mWorker == null) {
			mWorker = null;
			mWorker = new HandlerThread(tag);
			mWorker.start();
			mHandler = null;
			mHandler = new WorkerHandler(mWorker.getLooper());
		} else if (mWorker.getState() == Thread.State.NEW) {
			mWorker.start();
			mHandler = null;
			mHandler = new WorkerHandler(mWorker.getLooper());
		} else if (mWorker.getState() == Thread.State.WAITING) {
			mHandler = null;
			mHandler = new WorkerHandler(mWorker.getLooper());
		} else if (mWorker.getState() == Thread.State.TERMINATED) {
			mWorker = null;
			mWorker = new HandlerThread(tag);
			mWorker.start();
			mHandler = null;
			mHandler = new WorkerHandler(mWorker.getLooper());
		}
	}

	/**
	 * Worker Thread를 종료한다.
	 * @see #startWorker(String)
	 */
	protected void endWorker() {
		// 작업 스레드 안전 종료.
		mHandler = null;
		HandlerThread snap = mWorker;
		mWorker = null;
		snap.quit();
		snap.interrupt();
	}

	/**
	 * 이 메소드는 작업 스레드가 생성된 후에야 의미있는 값을 리턴할 수 있음.
	 * @return 작업 스레드의 핸들러.
	 * @see #startWorker(String)
	 */
	protected WorkerHandler getWorkerHandler() {
		return mHandler;
	}

	/**
	 * worker thread에서 실제 작업이 일어나는 부분.
	 * Background Thread에서 동작한다.
	 */
	abstract protected void onWorkerRequest(Message msg);

	/*	Worker Thread 끝	*/
}
