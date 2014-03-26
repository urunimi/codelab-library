package com.hovans.android.service;

import android.content.Intent;

/**
 * {@link IntentRunningService}를 위한 환경.
 * 액션과 그에 대한 동작을 정의한다.
 * 
 * @author Arngard
 *
 */
public abstract class IntentRunnable implements Comparable<IntentRunnable> {

	String mAction = null;

	/**
	 * 인자의 액션에 대한 동작을 하는 객체를 생성한다.
	 * @param action 어느 액션에 대한 동작인지
	 */
	public IntentRunnable(String action) {
		mAction = action;
	}
	
	/**
	 * @return 이 객체의 생성시에 준 액션
	 */
	public String getAction() {
		return mAction;
	}

	/**
	 * action 을 비교한다.
	 * @throws NullPointerException On null action
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(IntentRunnable another) {
		return mAction.compareTo(another.mAction);
	}

	/**
	 * action 을 기준으로 비교.<br>
	 * <br>
	 * 만일 둘 다 action 이 null 이라면 같은 것(true)으로 처리한다.
	 * 비교 객체 자체가 null 이면 false임.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		// null 처리 규약
		if (o == null) {
			return false;
		}
		// 동일 객체 규약
		if (this == o) {
			return true;
		}
		// 타입 체크: 엄격한 타입 체크가 필요한가? 대칭성 규약 관련.
		if (! (o instanceof IntentRunnable)) {//if (this.getClass() != o.getClass()) {
			return false;
		}
		// 내용 비교
		String oAction = ((IntentRunnable) o).mAction;
		if (mAction == null) {
			if (oAction == null) {
				return true;
			} else {
				return false;
			}
		} else {
			if (oAction == null) {
				return false;
			} else {
				if (mAction.equals(oAction)) {
					return true;
				} else {
					return false;
				}
			}
		}
	}

	/**
	 * @param intent 처리할 인텐트.
	 */
	public abstract void run(Intent intent);

}
