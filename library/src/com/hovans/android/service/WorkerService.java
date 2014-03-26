package com.hovans.android.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Intent;
import android.os.Message;

import com.hovans.android.app.PermissionManager;
import com.hovans.android.log.LogByCodeLab;

/**
 * {@link BaseService}를 기반으로 작업 스레드의 시작과 종료를 래핑한다.
 * 작업 스레드에 onStart 이벤트를 전달하는 과정을 래핑한다.
 * @author Arngard
 *
 */
public abstract class WorkerService extends BaseService {

	private final Lock mWorkerLock = new ReentrantLock();

	/**
	 * @return 작업 스레드를 만들 때 사용할 태그.
	 */
	public abstract String getWorkerTag();

	/*	생명주기 대응 시작	*/

	@Override
	public void onCreate() {
		super.onCreate();

		startWorker(getWorkerTag());
	}

	/**
	 * 이 이벤트를 작업 스레드에서 받기를 원하면 {@link #onWorkerRequest(Intent, int)}를 사용하면 된다.
	 * @see com.hovans.android.service.BaseService#handleStart(android.content.Intent, int)
	 */
	@Override
	protected void handleStart(Intent intent, int startId) {
		// 이 메소드는 메인 스레드에서 동작한다.
		// 요청자를 block하지 않기 위해 작업의 내용을 Worker 스레드에 넘겨주자.
		// 넘겨준 정보는 Worker 스레드가 onWorkerRequest(Message)에서 받을 것이다.
		try {
			Message msg = getWorkerHandler().obtainMessage();
			msg.what = startId;
			msg.obj = intent;
			getWorkerHandler().dispatchMessage(msg);
		} catch (Exception e) {
			LogByCodeLab.e(e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		endWorker();
	}

	/*	생명주기 대응 끝	*/

	/*	Worker Thread 전달 시작	*/

	/**
	 * WorkerThread에서 수행되며 주 작업을 수행한다.
	 * @see com.hovans.android.service.BaseService#onWorkerRequest(android.os.Message)
	 */
	@Override
	protected void onWorkerRequest(Message msg) {
		mWorkerLock.lock();	// 동기화 락
		PermissionManager.getWakeLockInstance(this, getWorkerTag()).acquire();
		try {
			onWorkerRequest((Intent) msg.obj, msg.what);
		} catch (Exception e) {
			LogByCodeLab.e(e);
		} finally {
			PermissionManager.getWakeLockInstance(this, getWorkerTag()).release();
			mWorkerLock.unlock();
		}
	}

	/**
	 * 이 메소드는 작업 스레드로 호출이 전달된 onStart() 라고 보면 된다.
	 * 동기화와 WakeLock 처리가 되어 있다.
	 * @param intent
	 * @param startId
	 */
	public abstract void onWorkerRequest(Intent intent, int startId);

	/*	Worker Thread 전달 끝	*/

}
