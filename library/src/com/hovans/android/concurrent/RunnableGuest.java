package com.hovans.android.concurrent;

/**
 * 우선순위 큐의 동작을 위한 Runnable 래핑.
 *
 * @author Arngard
 */
abstract class RunnableGuest implements Runnable, Comparable<RunnableGuest> {

    ThreadGuest mGuest;

    public RunnableGuest(ThreadGuest guest) {
        mGuest = guest;
    }

    @Override
    public int compareTo(RunnableGuest another) {
        return mGuest.compareTo(another.mGuest);
    }

}
