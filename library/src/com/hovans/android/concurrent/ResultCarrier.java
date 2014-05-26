package com.hovans.android.concurrent;

import android.os.Handler;
import android.os.Message;

/**
 * {@link ThreadHost}의 작업 스레드에서 {@link ThreadGuest#run(long)}의 수행이 끝나고 null 이 아닌 객체가 리턴된 경우,
 * 그 결과 객체를 {@link Handler}와 {@link Message}를 이용하여 메인스레드로 넘겨준다.
 * 이 때 {@link Message#obj}에 ThreadGuest 객체와 결과 객체를 담아 보내는 매개물로서 이 클래스가 존재한다.
 *
 * @author Arngard
 */
class ResultCarrier {

    /**
     * 현재 작업의 게스트
     */
    ThreadGuest guest;
    /**
     * {@link #guest}의 {@link ThreadGuest#run(long)}}이 리턴한 객체. {@link ThreadGuest#after(Object)}로 전달해야 한다.
     */
    Object result;

}
