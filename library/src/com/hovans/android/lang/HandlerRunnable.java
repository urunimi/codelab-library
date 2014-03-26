package com.hovans.android.lang;

import android.os.Handler;

/**
 * 생성시 지정한 핸들러에서 수행시킬 코드 블록을 정의한다.
 * 핸들러에 수행을 요청하는 환경을 제공한다.
 * 
 * @author Arngard
 *
 */
public abstract class HandlerRunnable implements Runnable {

	/** 생성자에서 대상으로 지정한 핸들러. */
	private Handler mHandler;

	/**
	 * 인자의 핸들러에서 실행시킬 코드 블록을 생성한다.
	 * @param handler
	 */
	public HandlerRunnable(Handler handler) {
		mHandler = handler;
	}

	/**
	 * 대상으로 지정된 핸들러를 얻는다.
	 * @return 생성자에서 지정했던 핸들러
	 */
	public Handler getHandler() {
		return mHandler;
	}

	/**
	 * {@link #run()}에 정의한 코드를 핸들러에 보내 실행시킨다.
	 * 만일 {@link #getHandler()}가 null 을 리턴한다면, 호출자의 스레드에서 수행된다.
	 */
	public void execute() {
		Handler target = getHandler();
		if (target == null) {
			run();
		} else {
			target.post(this);
		}
	}

	/**
	 * 지정된 핸들러의 스레드에서 돌아갈 코드를 여기에 정의한다.<br>
	 * <br>
	 * 핸들러를 통한 실행은 이 메소드를 직접 호출하는 것이 아님에 주의.
	 * {@link #execute()}를 사용하자.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public abstract void run();

}
