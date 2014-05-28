package com.hovans.android.constant;

import com.hovans.android.concurrent.ThreadGuest;

/**
 * 스레드의 동작 방식을 정의함.
 *
 * @author arngard
 */
public class ThreadConfig {

    /**
     * ThreadHost 대기열의 초기 크기
     */
    public static final int THREAD_BUCKET_SIZE = 32;    // 2^5
    /**
     * ThreadHost 대기열의 최대 크기. 이보다 커지는 경우에는 {@link ThreadGuest#offerFail()}이 호출된다.
     */
    public static final int THREAD_BUCKET_MAX_SIZE = 1048576;    // 2^20
    /**
     * ThreadHost 작업소의 크기
     */
    public static final int THREAD_CORE_SIZE = 1;
    /**
     * ThreadHost 작업소의 최대 크기
     */
    public static final int THREAD_MAX_SIZE = 1;
    /**
     * ThreadHost 작업소가 idle 상태로 생존할 수 있는 시간.
     * {@link #THREAD_CORE_SIZE}와 {@link #THREAD_MAX_SIZE}가 같은 경우에는 무의미하다.
     */
    public static final int THREAD_ALIVE_TIME = 1 * 1000;

}
