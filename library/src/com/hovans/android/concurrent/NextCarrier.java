package com.hovans.android.concurrent;

import android.os.Handler;
import android.os.Message;

/**
 * 스레드 체인이 {@link Handler}를 통해 연계될 때, {@link Message#obj}에 이 객체를 담아 전달한다.
 *
 * @author Arngard
 */
class NextCarrier {

    /**
     * 다음 실행될 게스트
     */
    ThreadGuest nextGuest;

}
