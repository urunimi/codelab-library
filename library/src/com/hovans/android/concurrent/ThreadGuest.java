package com.hovans.android.concurrent;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contents of method run() will be executed on background thread of ThreadHost.
 * And It will get back to main thread with after().
 * Offering interface of chaining several ThreadGuests, by addChain().<br/>
 * <br/>
 * Be aware of potential deadlock.<br/>
 * <br/>
 * This example shows how to make and run thread guest chain,
 * and passing any object with {@link #setObject(Object)} and {@link #getObject()}.<br/>
 * ex)<pre>
 * ThreadGuest.ChainBlocker blocker = new ThreadGuest.ChainBlocker();
 * blocker.block();
 * new ThreadGuest() {
 *     public void run(long waitTimeMillis) {
 *         setObject(somethingGreat());
 *     }
 * }
 * .addChain(blocker, new ThreadGuest() {
 *     public void run(long waitTimeMillis) {
 *         somethingAwesome();
 *     }
 * })
 * .addChain(200, new ThreadGuest() {
 *     public void run(long waitTimeMillis) {
 *         somethingPerfect(getObject());
 *     }
 * }).execute();
 * somethingOther();
 * blocker.unblock();</pre>
 * <p/>
 * The methods will be executed in this order.<br/>
 * somethingOther()<br/>
 * somethingGreat()	// on ThreadHost.<br/>
 * somethingAwesome()	// on ThreadHost, after blocker.unblock().<br/>
 * somethingPerfect()	// on ThreadHost.<br/>
 * <br/>
 *
 * @author Arngard
 */
public abstract class ThreadGuest implements Comparable<ThreadGuest> {

	/* 이하 우선순위 수치는 MS 윈도우의 프로세스 우선순위 값을 참고함. 참고로 윈도우의 System idle 은 0이다. */
    /**
     * Preset of priority. Prior to all another.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_GREEDY = 24;
    /**
     * Preset of priority.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_HIGH = 13;
    /**
     * Preset of priority.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_ABOVE_NORMAL = 10;
    /**
     * Preset of priority. Default.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_NORMAL = 8;
    /**
     * Preset of priority.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_BELOW_NORMAL = 6;
    /**
     * Preset of priority. Later to all another.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_IDLE = 4;

    /**
     * {@link #mSeqNum}의 값을 발급하는 데에 사용한다.
     */
    static final AtomicLong mSeqSource = new AtomicLong(Long.MIN_VALUE);
    /**
     * 이 스레드 게스트의 ID. 식별번호이면서 동시에 FIFO 를 지향하기 위해 사용된다.
     */
    final long mSeqNum;

    /**
     * 이 스레드 게스트의 우선순위.
     */
    final int mPriority;

	/* 스레드 체인 관련 변수 */
    /**
     * 시간 기반 체인에서 사용. 체인된 다음 게스트를 호스트에 등록할 때 사용하는 시간 지연
     */
    long mChainDelay;
    /**
     * 이벤트 기반 체인에서 사용.
     * 이것이 block()으로 세팅되어 있으면 다음 체인은 unblock()을 호출해주기 전까지 무조건 블락된다.
     */
    ChainBlocker mThreadChainBlocker;
    /**
     * 체인으로 연결하는 다음 게스트
     */
    ThreadGuest mChainNextGuest;
    /**
     * 체인을 통해 전달되는 임의의 객체
     */
    private Object mObjectHolder = null;

    /**
     * Initiate with default priority.
     *
     * @see ThreadGuest
     * @see #PRIORITY_NORMAL
     */
    public ThreadGuest() {
        this(PRIORITY_NORMAL);
    }

    /**
     * Initiate with defined priority.
     *
     * @param priority Bigger number is prior. Recommended to use pre-defined values.
     * @see ThreadGuest
     * @see #PRIORITY_NORMAL
     */
    public ThreadGuest(int priority) {
        this.mPriority = priority;
        mSeqNum = mSeqSource.getAndIncrement();    // 자신의 ID를 발급받는다.
        // 값의 범위를 봤을 때 오버플로우가 일어날 가능성은 사실상 없음.
    }

    /**
     * Compare to another ThreadGuest, by Priority and ID.<br/>
     * Result of comparison affects order in {@link PriorityBlockingQueue} on ThreadHost.
     *
     * @return -1 if prior to another. 1 if another is prior. 0 if same, but actually 0 will be returned if only if (another == this).
     * @see #getPriority()
     * @see #getId()
     */
    @Override
    public int compareTo(ThreadGuest another) {
        return compareToImpl(another);
    }

    /**
     * For ensure semantic consistency of {@link #compareTo(ThreadGuest)} and {@link #equals(Object)}.
     * And avoid redundancy.
     * 또한 Object 클래스를 거쳐 호출되는 것을 막기 위해 별도의 인라이닝 가능한 메소드로 분리.
     *
     * @param another comparison target.
     * @return result of comparison.
     * @see #compareTo(ThreadGuest)
     * @see #equals(Object)
     */
    private int compareToImpl(ThreadGuest another) {
        /*
         * 결과의 정렬 방향은 {@link PriorityBlockingQueue}의 비교 연산과 관련되어 있다. 수정시 주의.
		 * */
        if (getPriority() == another.getPriority()) {    // 먼저 priority 비교해보자
            if (mSeqNum == another.mSeqNum)    // priority 에 차이가 없다면 seqNum 을 비교한다.
                return 0;
            else {
                return mSeqNum < another.mSeqNum ? -1 : 1;    // seqNum 은 숫자가 작으면 우선적이다.
            }
        } else {
            return getPriority() > another.getPriority() ? -1 : 1;    // priority 는 숫자가 크면 우선적이다.
        }
    }

    /**
     * Compares this instance with the specified object and indicates if they are equal,<br/>
     * with logic of {@link #compareTo(ThreadGuest)}.
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
        if (!(o instanceof ThreadGuest)) {//if (this.getClass() != o.getClass()) {
            return false;
        }
        // 내용 비교
        return compareToImpl((ThreadGuest) o) == 0;
    }

    /**
     * @return priority of this guest.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * ID of guest is unique in global-runtime space. Is assigned when creating new instance.
     * Value grows bigger for new instance.
     *
     * @return ID of this guest.
     */
    public long getId() {
        return mSeqNum;
    }

    /**
     * It will be invoked if this guest is failed to enter {@link ThreadHost}.
     * Will be invoked on the thread of {@link #execute()}.<br/>
     * <br/>
     * It will do nothing if you don't override this method.
     */
    public void offerFail() {
        // do nothing
    }

    /**
     * <font color=#FF0000>Deprecated. It will not be invoked anymore.</font><br/>
     * <br/>
     * Guest will not be invoked at direct time, it will be invoked when it can be.
     * On the thread of {@link ThreadHost}.
     * If you want to cancel task by checking timeout, you can use this guide.<br/>
     *
     * @param waitTimeMillis Wait time of this guest in queue.
     * @return Whether to continue.<br/>
     * If false, wait more and {@link #run(long)}.<br/>
     * If true, Terminate this guest and cancel the wait.<br/>
     * It will return false if you don't override this method.
     */
    @Deprecated
    public boolean waitTimeout(long waitTimeMillis) {
        return false;
    }

    /**
     * Run the task defined.<br/>
     * On the thread of {@link ThreadHost}.
     *
     * @param waitTimeMillis Wait time of this guest in queue.
     * @return If you return non-null object here, {@link #after(Object)} will be invoked.
     * If you return null, It will be finished.
     */
    public abstract Object run(long waitTimeMillis);

    /**
     * Invoked only if {@link #run(long)} returned non-null.<br/>
     * On the main thread. Not the thread of {@link #execute()}.
     * So it be invoked asynchronously with other guest.<br/>
     * <br/>
     * It will do nothing if you don't override this method.
     *
     * @param result Return value of {@link #run(long)}.
     */
    @SuppressWarnings("unused")
    public void after(Object result) {
        // do nothing
    }

    /**
     * Chaining guests and run sequentially.<br/>
     * nextGuest will be ready at the last of this guest's chain.<br/>
     *
     * @param delayMillis after invocation of {@link #run(long)} or {@link #after(Object)} of this guest,
     *                    delay to execute() nextGuest.
     *                    Considered as 0 if smaller than 0.
     * @param nextGuest   will be executed after this guest's {@link #run(long)} or {@link #after(Object)}(if run() returns non-null).<br/>
     *                    If there is already a guest after this, then added at the last of chain.
     *                    If null, exact next reference will be un-set.
     * @return This instance. not the nextGuest.<br/>
     * Usage: {@code guest.addChain(0, other1).addChain(17000, other2).addChain(0, other3).execute()}
     */
    @SuppressWarnings("unused")
    public ThreadGuest addChain(long delayMillis, ThreadGuest nextGuest) {
        if (mChainNextGuest == null) {    // 이 객체에 체인이 없으면 체인을 추가한다.
            mChainDelay = delayMillis;
            mChainNextGuest = nextGuest;
        } else {    // 체인이 있으면 그 객체에 대하여 이 메소드를 부른다.
            mChainNextGuest.addChain(delayMillis, nextGuest);
        }
        return this;
    }

    /**
     * Support for Event-based ThreadGuest chain mechanism.
     * Argument type of {@link ThreadGuest#addChain(ChainBlocker, ThreadGuest)}.<br/>
     * <br/>
     * {@link #block()} makes blocker to blocking state.
     * Any chain passing through during blocking state,
     * will be stopped and collected into blocker's internal waiting queue.<br/>
     * <br/>
     * {@link #unblock()} makes blocker un-blocking state,
     * and invoke all waiting chains in the queue at that time.
     * this invocation flushes and empty the waiting queue.<br/>
     * <br/>
     * During un-blocking state, chains will just pass through, not collected.
     *
     * @author Arngard
     */
    public static class ChainBlocker {

        private ArrayList<ThreadGuest> waitingGuests = new ArrayList<ThreadGuest>();
        private boolean isBlocking = true;

        /**
         * Enqueue the argument into this blocker's wait-queue.
         * It doesn't check any conditions include uniqueness.
         *
         * @param guest target.
         */
        synchronized final void addWait(final ThreadGuest guest) {
            waitingGuests.add(guest);
        }

        /**
         * @param guest target
         * @return If the target is waiting in this blocker's queue.
         */
        @SuppressWarnings("unused")
        synchronized public final boolean isWaiting(final ThreadGuest guest) {
            return waitingGuests.contains(guest);
        }

        /**
         * Start blocking.
         * From now on, until state change to unblock,
         * Every chain passing throw this blocker is stopped and entered to this blocker's wait-queue.
         */
        synchronized public void block() {
            isBlocking = true;
        }

        /**
         * @return True if it is blocking now.
         */
        synchronized public final boolean isBlocking() {
            return isBlocking;
        }

        /**
         * Similar to {@link #unblock(ThreadGuest)}, and setObject(setObject).
         *
         * @param guest     target. If it is not in this blocker's queue, nothing happens.
         * @param setObject {@link ThreadGuest#setObject(Object)} is invoked with this argument before continuation.
         *                  Nullable.
         * @return true if target is found and handled.
         * @see #setObject(Object)
         * @see #unblock(ThreadGuest)
         */
        @SuppressWarnings("unused")
        synchronized public boolean unblock(final ThreadGuest guest, final Object setObject) {
            isBlocking = false;
            if (waitingGuests.remove(guest)) {
                guest.setObject(setObject);
                ThreadHost.processChain(guest);
                return true;
            }
            return false;
        }

        /**
         * Continue the guest of argument if it's in this blocker's queue.
         *
         * @param guest target. If it is not in this blocker's queue, nothing happens.
         * @return true if target is found and handled.
         * @see #unblock(ThreadGuest, Object)
         */
        @SuppressWarnings("unused")
        synchronized public boolean unblock(final ThreadGuest guest) {
            isBlocking = false;
            if (waitingGuests.remove(guest)) {
                ThreadHost.processChain(guest);
                return true;
            }
            return false;
        }

        /**
         * Continue all the guests in this blocker's queue.
         *
         * @return Count of guests that continued.
         * @see #unblock(ThreadGuest, Object)
         * @see #unblock(ThreadGuest)
         */
        @SuppressWarnings("unused")
        synchronized public int unblock() {
            isBlocking = false;
            int reVal = waitingGuests.size();
            for (ThreadGuest waitingGuest : waitingGuests) {
                ThreadHost.processChain(waitingGuest);
            }
            waitingGuests.clear();
            return reVal;
        }

    }

    /**
     * Similar to {@link #addChain(long, ThreadGuest)}.
     * Chain blocking is not base on time, based on event.<br/>
     * <br/>
     * You can manage {@link ChainBlocker blocker} to control chain blocking.<br/>
     * <br/>
     * If it is unblocked before arriving, it will executed without block.
     *
     * @param blocker   You can manage this instance to block chain.
     * @param nextGuest will be executed after this guest's {@link #run(long)} or {@link #after(Object)}(if run() returns non-null).<br/>
     *                  If there is already a guest after this, then added at the last of chain.
     *                  If null, exact next reference will be un-set.
     * @return This instance. not the nextGuest.<br/>
     * Usage: {@code guest.addChain(blocker1, other1).addChain(0, other2).addChain(blocker2, other3).execute()}
     */
    @SuppressWarnings("unused")
    public ThreadGuest addChain(ChainBlocker blocker, ThreadGuest nextGuest) {
        if (blocker == null) {
            throw new NullPointerException("ChainBlocker is null.");
        }
        if (mChainNextGuest == null) {    // 이 객체에 체인이 없으면 체인을 추가한다.
            mThreadChainBlocker = blocker;
            mChainNextGuest = nextGuest;
        } else {    // 체인이 있으면 그 객체에 대하여 이 메소드를 부른다.
            mChainNextGuest.addChain(blocker, nextGuest);
        }
        return this;
    }

    /**
     * This method is to detect circular chaining of ThreadGuest.
     *
     * @return Be aware of logical infinite loop when it returns true.
     */
    @SuppressWarnings("unused")
    public boolean isCircularChain() {
        ThreadGuest pointer = this;
        while (pointer.mChainNextGuest != null) {
            pointer = pointer.mChainNextGuest;
            if (pointer == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enqueue this guest in {@link ThreadHost}.
     * {@link ThreadGuest#run(long)} will be invoked when it can be.
     */
    public void execute() {
        ThreadHost.offer(this);
    }

    /**
     * The argument will be passed to next guest in ThreadGuest chain.
     * If the next guest already has object, it will be overwritten when jumping next guest.
     *
     * @param obj object to carry on.
     * @see #addChain(long, ThreadGuest)
     * @see #getObject()
     */
    public void setObject(Object obj) {
        mObjectHolder = obj;
    }

    /**
     * @return Argument of last invocation of {@link #setObject(Object)}.
     * @see #setObject(Object)
     */
    public Object getObject() {
        return mObjectHolder;
    }

}
