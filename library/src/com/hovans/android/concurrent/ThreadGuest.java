package com.hovans.android.concurrent;

import com.hovans.android.constant.ThreadConfig;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ThreadHost} 위에서 실행되는 코드를 정의하는 틀.
 * 비선형 체인을 구성할 때에는 주의깊게 사용해야 함.<br>
 * <br>
 * 아래 예제는 스레드 게스트의 체인을 만들고 실행하며,
 * {@link #setObject(Object)}와 {@link #getObject()}를 통해
 * 게스트 사이에서 임의의 객체를 전달하는 것을 보여준다.<br>
 * ex)<pre>
 * ThreadGuest.ChainBlocker blocker = new ThreadGuest.ChainBlocker();
 * blocker.block();
 * new ThreadGuest() {
 *     public void run(long waitTimeMillis) {
 *         setObject(somethingGreat());	// 객체를 set 해둔다.
 *     }
 * }
 * .addChain(blocker, new ThreadGuest() {
 *     public void run(long waitTimeMillis) {
 *         somethingAwesome();
 *     }
 * })
 * .addChain(200, new ThreadGuest() {
 *     public void run(long waitTimeMillis) {
 *         somethingPerfect(getObject());	// 체인의 앞쪽에서 마지막으로 set 했던 객체를 get.
 *     }
 * })
 * .execute();
 * somethingOther();
 * blocker.unblock();</pre>
 * <p/>
 * 위 예제에서 유저 메소드는 다음과 같은 순서로 수행될 것이다.<br>
 * somethingOther()<br>
 * somethingGreat()	// ThreadHost 에서 수행됨<br>
 * somethingAwesome()	// ThreadHost 에서 수행됨. blocker.unblock() 다음에야 수행될 수 있음.<br>
 * somethingPerfect()	// ThreadHost 에서 수행됨.<br>
 * <br>
 *
 * @author Arngard
 */
public abstract class ThreadGuest implements Comparable<ThreadGuest> {

	/* 이하 우선순위 수치는 MS 윈도우의 프로세스 우선순위 값을 참고함. 참고로 윈도우의 System idle 은 0이다. */
    /**
     * 게스트의 우선순위 프리셋. 가장 높은 우선순위.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_GREEDY = 24;
    /**
     * 게스트의 우선순위 프리셋.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_HIGH = 13;
    /**
     * 게스트의 우선순위 프리셋.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_ABOVE_NORMAL = 10;
    /**
     * 게스트의 우선순위 프리셋. 기본값이다.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_NORMAL = 8;
    /**
     * 게스트의 우선순위 프리셋.
     */
    @SuppressWarnings("unused")
    public static final int PRIORITY_BELOW_NORMAL = 6;
    /**
     * 게스트의 우선순위 프리셋. 가장 낮은 우선순위.
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
     * 기본 우선순위를 가진 객체를 만든다.
     *
     * @see ThreadGuest
     * @see #PRIORITY_NORMAL
     */
    public ThreadGuest() {
        this(PRIORITY_NORMAL);
    }

    /**
     * 지정된 우선순위를 가진 객체를 만든다.
     *
     * @param priority 우선순위. 큰 숫자일수록 우선.
     * @see ThreadGuest
     * @see #PRIORITY_NORMAL
     */
    public ThreadGuest(int priority) {
        this.mPriority = priority;
        mSeqNum = mSeqSource.getAndIncrement();    // 자신의 ID를 발급받는다.
        // 값의 범위를 봤을 때 오버플로우가 일어날 가능성은 사실상 없음.
    }

    /**
     * Priority 와 ID를 기준으로 다른 guest 와의 우선 관계를 비교한다.<br>
     * 결과의 정렬 방향은 {@link PriorityBlockingQueue}의 비교 연산과 관련되어 있다.
     *
     * @return 우선순위와 ID를 이용하여, 비교대상에 비해 우선권을 가지는 경우 -1 을 리턴.
     * 인자가 더 우선이면 1을 리턴한다. 같으면 0인데, 정상 작동시 이 경우는 나올 수 없다.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ThreadGuest another) {
        return compareToImpl(another);
    }

    /**
     * {@link #compareTo(ThreadGuest)}와 {@link #equals(Object)}의 대칭성을 보장하며,
     * 코드 중복을 피하고,
     * 동시에 Object 클래스를 거쳐 호출되는 것을 막기 위해 별도의 인라이닝 가능한 메소드로 분리.
     *
     * @param another 비교 대상
     * @return 비교 결과
     * @see #compareTo(ThreadGuest)
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
     * {@link #compareTo(ThreadGuest)}와 같은 논리로, 동일한 내용의 게스트인지 판단한다.
     *
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
        if (!(o instanceof ThreadGuest)) {//if (this.getClass() != o.getClass()) {
            return false;
        }
        // 내용 비교
        return compareToImpl((ThreadGuest) o) == 0;
    }

    /**
     * 이 게스트에 세팅된 우선순위를 얻는다.
     *
     * @return 이 게스트의 우선순위를 반환한다.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * 게스트의 ID는 전역 공간에서 유일하다. 게스트가 생성될 때 발급받는다.
     * 먼저 발급받은 게스트가 작은 값을 갖는다.
     *
     * @return 이 게스트의 ID.
     */
    public long getId() {
        return mSeqNum;
    }

    /**
     * 이 게스트가 {@link ThreadHost}의 대기열에 진입하기를 실패한 경우 호출됨.
     * offer()의 스레드에서 수행된다.
     * 이 메소드를 오버라이드하지 않으면 아무 작업도 하지 않는다.
     */
    public void offerFail() {
        // do nothing
    }

    /**
     * {@link ThreadConfig#THREAD_WAIT_TIME}에 정의된 타임아웃 시간보다 작업 대기 시간이 더 오래된 경우.
     * 정확한 시간에 불리는 것이 아니라, 부를 수 있을 때 불림.
     * {@link ThreadHost}의 작업 스레드에서 호출된다.
     *
     * @param waitTimeMillis 이 게스트가 대기열에서 기다린 시간.
     * @return 작업을 계속할지 여부. false 이면 {@link #run(long)} 을 실행한다.
     * true 이면 그대로 종료한다.
     * 이 메소드를 오버라이드하지 않은 경우, false 를 리턴한다.
     */
    public boolean waitTimeout(long waitTimeMillis) {
        return false;
    }

    /**
     * 작업을 수행한다.<br>
     * 정상적으로 게스트의 차례가 돌아온 경우 호출된다.
     * {@link ThreadHost}의 작업 스레드에서 호출된다.
     *
     * @param waitTimeMillis 이 게스트가 대기열에서 기다린 시간.
     * @return 여기에서 null 이 아닌 것을 리턴하면, {@link #after(Object)}를 수행한다.
     * null 을 리턴하면 그대로 종료.
     */
    public abstract Object run(long waitTimeMillis);

    /**
     * {@link #run(long)}에서 null 이 아닌 객체를 리턴할 경우에만 호출된다.<br>
     * 이 메소드는 작업 스레드가 아닌 메인 스레드에서 호출된다.
     * {@link #execute()}한 스레드가 아님에 주의.
     * 다른 게스트와 비동기적으로 호출된다.<br>
     * 이 메소드를 오버라이드하지 않으면 아무 작업도 하지 않는다.
     *
     * @param result {@link #run(long)}에서 리턴했던 객체.
     */
    @SuppressWarnings("unused")
    public void after(Object result) {
        // do nothing
    }

    /**
     * 여러 개의 스레드 게스트를 연계시켜 실행하기 위한 메소드.<br>
     * 이 객체 혹은 이 객체에 연결된 체인의 수행이 끝난 후에,
     * 인자로 넘겨주는 게스트를 다시 대기시킨다.
     * 필요하다면 대기시키기 전에 지연 시간을 줄 수 있다.
     *
     * @param delay     이 객체의 {@link #run(long)} 또는 {@link #after(Object)}가 수행된 후,
     *                  nextGuest 가 대기열에 들어가기 전까지 줄 지연시간.
     *                  0보다 작으면 0으로 취급된다.
     * @param nextGuest 이 게스트 다음에 체인으로 연결시키는 게스트.
     *                  만일 이미 세팅된 체인이 있다면, 체인 구조의 맨 마지막에 추가된다.
     *                  null 을 주면 이 객체 바로 다음의 체인이 끊긴다.
     * @return 이 메소드의 작업을 수행한 후, 이 객체를 다시 리턴함.
     * 인자의 객체를 리턴하는 것이 아님에 주의.
     */
    @SuppressWarnings("unused")
    public ThreadGuest addChain(long delay, ThreadGuest nextGuest) {
        if (mChainNextGuest == null) {    // 이 객체에 체인이 없으면 체인을 추가한다.
            mChainDelay = delay;
            mChainNextGuest = nextGuest;
        } else {    // 체인이 있으면 그 객체에 대하여 이 메소드를 부른다.
            mChainNextGuest.addChain(delay, nextGuest);
        }
        return this;
    }

    /**
     * {@link ThreadGuest#addChain(ChainBlocker, ThreadGuest)}의 인자로 사용된다.
     * 스레드 체인의 지속을 시간 딜레이 기반이 아닌 이벤트 기반으로 수행하기 위한 매개체.<br>
     * <br>
     * {@link #block()}을 호출한 이후 그 blocker 객체를 통과하려고 시도하는 모든 체인은,
     * 그 blocker 가 관리하는 대기열에 수집된다.<br>
     * <br>
     * {@link #unblock()}을 호출하면
     * 그 시점에 해당 blocker 의 대기열에 수집되어 있는 모든 게스트를 다시 진행시킨다.
     * 이 때 실행된 게스트는 대기열에서 빠진다.
     * unblock 상태에서 진행되는 체인은 그냥 그대로 진행되며 대기열에 들어가지 않는다.
     *
     * @author arngard
     */
    public static final class ChainBlocker {

        private ArrayList<ThreadGuest> waitingGuests = new ArrayList<ThreadGuest>();
        private boolean isBlocking = true;

        /**
         * 인자의 게스트를 대기열에 등록한다.
         * 중복 호출해도 무시하지 않고 계속 대기열에 추가된다.
         *
         * @param guest 대상 게스트
         */
        synchronized final void waiting(final ThreadGuest guest) {
            waitingGuests.add(guest);
        }

        /**
         * @param guest 대상 게스트
         * @return 대상이 이 블로커의 대기열에 있으면 true.
         */
        @SuppressWarnings("unused")
        synchronized final boolean isWaiting(final ThreadGuest guest) {
            return waitingGuests.contains(guest);
        }

        /**
         * block 상태가 된다.
         * 이후 unblock 상태가 되기 전까지,
         * 이 블로커를 통과하려고 시도하는 모든 체인은 대기열에 수집된다.
         */
        synchronized public void block() {
            isBlocking = true;
        }

        /**
         * @return 블록 중이면 true.
         */
        synchronized public boolean isBlocking() {
            return isBlocking;
        }

        /**
         * {@link #unblock(ThreadGuest)}과 같고, 추가 인자에 대한 동작을 추가로 수행한다.
         *
         * @param guest     대상 게스트. 이 체인의 대기열에 이 게스트가 없으면 아무 동작도 하지 않음.
         * @param setObject 체인을 계속하기 전에 이 객체가
         *                  {@link ThreadGuest#setObject(Object)}를 통해 세팅된다.
         *                  nullable 임에 주의.
         * @return 대상 게스트가 처리(발견)되었으면 true.
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
         * block 상태를 해제하고 체인을 계속한다.
         *
         * @param guest 대상 게스트. 이 체인의 대기열에 이 게스트가 없으면 아무 동작도 하지 않음.
         * @return 대상 게스트가 처리되었으면 true.
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
         * 이 blocker 의 대기열에 등록된 모든 체인을 계속시킨다.
         *
         * @return 처리된 체인의 개수
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
     * {@link #addChain(long, ThreadGuest)}과 비슷한데,
     * 이 메소드를 통해 세팅된 체인은 이벤트 기반으로 연결된다.<br>
     * <br>
     * 인자로 넘겨주는 {@link ChainBlocker blocker}의 레퍼런스를 들고 있다가,
     * block 여부를 컨트롤해주면 된다.<br>
     * <br>
     * 만일 특정 체인에 대한 unblock 이 실행 시도 전에 수행되는 경우,
     * 그 체인은 blocking 없이 수행될 것이다.
     *
     * @param blocker   이 인자의 메소드를 통해 체인을 조절한다.
     * @param nextGuest 이 게스트 다음에 체인으로 연결시키는 게스트.
     *                  만일 이미 세팅된 체인이 있다면, 체인 구조의 맨 마지막에 추가된다.
     *                  null 을 주면 이 객체 바로 다음의 체인이 끊긴다.
     * @return 이 메소드의 작업을 수행한 후, 이 객체를 다시 리턴함.
     * 인자의 객체를 리턴하는 것이 아님에 주의.
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
     * 이 메소드는 일종의 race condition 체크를 하기 위해 존재한다.
     * true 를 리턴하는 경우, 해당 체인은 주의깊게 사용해야 한다.
     *
     * @return 이 게스트에 현재 세팅된 체인이, 순환 구조로 진입할 것이 예상되는 경우 true.
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
     * 이 객체를 {@link ThreadHost}의 작업 대기열에 대기시킨다.
     * 가능한 때에 {@link ThreadGuest#run(long)}의 수행이 시도된다.
     */
    public void execute() {
        ThreadHost.offer(this);
    }

    /**
     * 이 메소드의 인자를 통해 세팅된 객체는
     * 스레드 게스트 체인이 수행될 때 다음 게스트로 전달된다.
     * 만일 다음 게스트가 객체를 가지고 있었다면 덮어써버린다.
     *
     * @param obj 저장해 둘 객체
     * @see #addChain(long, ThreadGuest)
     * @see #getObject()
     */
    public void setObject(Object obj) {
        mObjectHolder = obj;
    }

    /**
     * {@link #setObject(Object)}로 세팅된 객체를 얻는다.
     *
     * @return 현재 이 게스트에 세팅되어 있는 객체.
     * @see #setObject(Object)
     */
    public Object getObject() {
        return mObjectHolder;
    }

}
