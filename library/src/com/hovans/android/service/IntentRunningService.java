package com.hovans.android.service;

import java.util.Map;
import java.util.TreeMap;

import android.content.Intent;
import android.util.Log;

import com.hovans.android.constant.DebugConfig;

/**
 * 인텐트 액션에 따른 {@link IntentRunnable}을 등록해두면,
 * 인텐트를 받았을 때 그것의 수행을 시도한다.
 * 
 * @author Arngard
 *
 */
public abstract class IntentRunningService extends WorkerService {

	private Object mapLock = new Object();
	private IntentRunnable mForNullAction = null;
	private Map<String, IntentRunnable> mMap = new TreeMap<String, IntentRunnable>();

	/**
	 * {@link #processIntent(Intent)}에서 수행할 매핑을 추가한다.
	 * 맵의 관리는 동기화되어 있다.
	 * @param IntentRunnable 새로이 추가하거나 덮어 쓸 매핑.
	 * @return 이전에 이 매핑이 있었다면 그 값. 없었으면 null.
	 */
	public IntentRunnable addIntentRunnable(IntentRunnable IntentRunnable) {
		IntentRunnable reVal = null;
		synchronized (mapLock) {
			if (IntentRunnable.mAction == null) {
				reVal = mForNullAction;
				mForNullAction = IntentRunnable;
			} else {
				reVal = mMap.put(IntentRunnable.mAction, IntentRunnable);
			}
		}
		return reVal;
	}

	/**
	 * {@link #addIntentRunnable(IntentRunnable)}에서 추가했던 매핑을 제거한다.
	 * 맵의 관리는 동기화되어 있다.
	 * @param action 지울 매핑의 키
	 * @return 지울 매핑이 있었다면 그 값. 없었으면 null.
	 */
	public IntentRunnable removeIntentRunnable(String action) {
		IntentRunnable reVal = null;
		synchronized (mapLock) {
			if (action == null) {
				reVal = mForNullAction;
				mForNullAction = null;
			} else {
				reVal = mMap.remove(action);
			}
		}
		return reVal;
	}

	/**
	 * {@link #processIntent(Intent)}에서 처리하는 모든 매핑을 제거한다.
	 * 맵의 관리는 동기화되어 있다.
	 */
	public void resetIntentRunnables() {
		synchronized (mapLock) {
			mForNullAction = null;
			mMap.clear();
		}
	}

	/**
	 * 주어진 인텐트의 액션을 매핑에서 찾아 해당 동작을 수행한다.
	 * 맵의 관리는 동기화되어 있다.
	 * @param intent 매핑에서 찾아 처리할 인텐트. null 인 경우 아무 것도 하지 않는다. 
	 * @return 대응하는 {@link IntentRunnable}에 대한 수행이 이루어졌다면 true.
	 */
	public boolean processIntent(Intent intent) {
		try {
			if (intent == null) {
				return false;
			}
			String action = intent.getAction();
			synchronized (mapLock) {
				if (action == null && mForNullAction != null) {
					mForNullAction.run(intent);
					return true;
				} else {
					IntentRunnable IntentRunnable = mMap.get(action);
					if (IntentRunnable != null) {
						IntentRunnable.run(intent);
						return true;
					}
				}
			}
		} catch (Exception e) {
			Log.e(DebugConfig.LOG_TAG, "Fail on running IntentRunnable", e);
		}
		return false;
	}

	/**
	 * {@link #processIntent(Intent)}를 호출한다.
	 * @see com.hovans.android.service.WorkerService#onWorkerRequest(android.content.Intent, int)
	 */
	@Override
	public void onWorkerRequest(Intent intent, int startId) {
		processIntent(intent);
	}

}
