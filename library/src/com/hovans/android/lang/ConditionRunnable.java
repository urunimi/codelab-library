package com.hovans.android.lang;

/**
 * Represents a command that can be executed on condition.
 * 
 * @author Arngard
 *
 */
public abstract class ConditionRunnable {

	/**
	 * 조건에 따라 정의된 코드를 수행한다.
	 * @param condition
	 * @see #onTrue()
	 * @see #onFalse()
	 */
	public void run(final boolean condition) {
		if (condition) {
			onTrue();
		} else {
			onFalse();
		}
	}

	/**
	 * {@link #run(boolean)}이 인자 true로 수행될 때 호출됨.
	 */
	public abstract void onTrue();

	/**
	 * {@link #run(boolean)}이 인자 false로 수행될 때 호출됨.
	 */
	public abstract void onFalse();

}
