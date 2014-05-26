package com.hovans.android.concurrent;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.hovans.android.constant.DebugConfig;
import com.hovans.android.constant.ThreadConfig;
import com.hovans.android.log.LogByCodeLab;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * worker 스레드의 작업 환경을 제공한다.<br>
 * <br>
 * 환경에는 작업 공간과 대기열이 있다.
 * 대기열에 {@link ThreadGuest}가 들어오면 하나씩 작업 공간에 옮겨져 게스트 내부에 정의된 작업을 수행한다.<br>
 * <br>
 * 스레드 자원은 singleton 으로 사용된다.
 *
 * @author Arngard
 * @see ThreadGuest
 */
class ThreadHost {

    /*
     * ThreadHost 의 작업 스레드 내부에서 Looper 를 얻을 수 있게 만들어 보려고도 했다.
     * 결론적으로 말하자면 답이 안 나온다.
     *
     * Looper 를 사용하려면 스레드를 만들 때 HandlerThread 를 사용해야 한다.
     * ThreadFactory 를 경유하여 ThreadPoolExecutor 자원으로 HandlerThread 를
     * 넘겨주면, 스레딩은 된다. 그러나 어떻게 해도 Looper 를 돌릴 수가 없다.
     *
     * 그리고 HandlerThread 로 구현할 경우 ThreadGuest 에 우선순위를 부여할 수 없다.
     * 우선순위를 구현한다는 것이 ThreadGuest 설계의 중요한 요구사항이었기 때문에
     * 이 부분은 포기할 수 없다.
     *
     * 이에 대해 정보를 찾다가, 다음 내용을 얻었다.
     * (http://www.mail-archive.com/android-developers@googlegroups.com/msg62791.html)
     * "AsyncTask and Looper are two fundamentally different threading models,
     * which are not compatible"
     * "Actually it mean the two threading models are not compatible, so you can't
     * use these together.  Looper expects to to own the thread that you associate
     * it with, while AsyncTask owns the thread it creates for you to run in the
     * background.  They thus conflict with each other, and can't be used together."
     *
     * AsyncTask 는 내부적으로 ThreadPoolExecutor 를 사용한다.
     * ThreadHost 는 AsyncTask 와 같이 ThreadPoolExecutor 를 사용했다.
     * 결국 ThreadHost 역시 Looper 의 논리와는 호환성이 없다는 이야기다.
     *
     * 사실 Looper 가 필요한 경우에는 HandlerThread 를 사용해서 별도의 스레드를 만들면 된다.
     * ThreadHost 가 작업 우선순위를 포기하고 Looper 를 선택한다면, 그것은 단지
     * HandlerThread 를 래핑하는 것밖에 되지 않을 것이다.
     * 그런 것은 굳이 라이브러리로 만들어 놓을 필요가 없는 것 같다.
     * 따라서 여기에서는 Looper 를 포기하고 우선순위 구현을 선택하도록 한다.
     * */

    /**
     * 작업 공간
     */
    private static ThreadPoolExecutor executor = null;
    /**
     * 주 작업 대기열
     */
    private static BlockingQueue<Runnable> queue = null;

	/* 핸들러 내부에서 분기하는 시간조차 아깝다. 어차피 확장되는 분기가 아니니 핸들러 변수를 따로 두자. */
    /**
     * 메인 스레드 핸들러. {@link ThreadGuest#run(long)} 결과를 메인 스레드로 보낼 때 사용.
     */
    private static Handler resultHandler = null;
    /**
     * 메인 스레드 핸들러. 스레드 게스트 체인을 건너갈 때 사용.
     */
    private static Handler nextHandler = null;

    static {
        makeHandlers();
        threadingStart();
    }

    private ThreadHost() {
    }

    @SuppressWarnings("unused")
    public static ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * 대기열을 얻는다.
     *
     * @return 스레드 작업의 대기열 객체.
     */
    public static BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    synchronized private static void makeHandlers() {
        if (resultHandler == null) {
            resultHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(android.os.Message msg) {
                    // 여기는 메인스레드이다.
                    LogByCodeLab.v("ThreadHost.resultHandler.handleMessage(): ResultCarrier arrived.");
                    final ResultCarrier carrier = (ResultCarrier) msg.obj;    // 캐리어로 날아온 결과로 guest.after()를 수행해야 한다.
                    if (carrier == null) {
                        throw new NullPointerException("Failed to reference ThreadGuest Result");
                    }
                    carrier.guest.after(carrier.result);
                    processChain(carrier.guest);
                }
            };
        }
        if (nextHandler == null) {
            nextHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(android.os.Message msg) {
                    // 여기는 메인스레드이다.
                    LogByCodeLab.v("ThreadHost.nextHandler.handleMessage(): NextCarrier arrived.");
                    final NextCarrier carrier = (NextCarrier) msg.obj;    // 캐리어로 날아온 결과로 다음 체인을 등록해야 한다.
                    if (carrier == null) {
                        throw new NullPointerException("Failed to reference ThreadGuest Next");
                    }
                    carrier.nextGuest.execute();
                }
            };
        }
    }

	/*	큐잉관리 시작	*/

    /**
     * 스레드풀의 연산을 시작한다. 동기화에 안전하지 않으므로 너무 자주 호출하지 말 것.
     */
    synchronized protected static void threadingStart() {
        if (queue == null) {
            queue = new PriorityBlockingQueue<Runnable>(ThreadConfig.THREAD_BUCKET_SIZE);
        }
        if (executor == null) {
            executor = new ThreadPoolExecutor(
                    ThreadConfig.THREAD_CORE_SIZE, ThreadConfig.THREAD_MAX_SIZE,
                    ThreadConfig.THREAD_ALIVE_TIME, TimeUnit.MILLISECONDS,
                    queue);
        }
        if (!executor.prestartCoreThread()) {
            Log.w(DebugConfig.LOG_TAG, "Fail to pre-start core thread. It may already Started.");    // 공공연히 알려야 하므로 로그 래핑을 쓰지 않음.
        }
    }

    /**
     * 스레드풀의 연산을 종료한다. 동기화에 안전하지 않으므로 너무 자주 호출하지 말 것.
     */
    @SuppressWarnings("unused")
    synchronized protected static void threadingEnd() {
        executor.shutdown();
        executor = null;
    }

    /**
     * 대기열에 인자의 게스트를 대기시킨다.
     * 외부에서는 이 메소드 대신 {@link ThreadGuest#execute()}를 사용할 것을 권장함.<br>
     *
     * @param guest 대기시킬 대상
     */
    synchronized static void offer(final ThreadGuest guest) {
        if (guest == null) {
            throw new NullPointerException("guest is null.");
        }
        boolean offerSucceed = false;
        if (getQueue().size() < ThreadConfig.THREAD_BUCKET_MAX_SIZE) {
            offerSucceed = getQueue().offer(makeRunnable(android.os.SystemClock.elapsedRealtime(), guest));
        }
        if (!offerSucceed) {    // offer 실패했다면
            guest.offerFail();
        }
    }

    /**
     * 인자의 게스트에 정의된 작업을 수행하는 Runnable 을 만든다.
     *
     * @param offerTime 주 대기열에 대기시키기를 시도하는 시각
     * @param guest     작업이 정의된 대상
     * @return 주 대기열에 넣을 runnable 객체
     */
    private static RunnableGuest makeRunnable(final long offerTime, final ThreadGuest guest) {
        return new RunnableGuest(guest) {
            @Override
            public void run() {
                runGuest(offerTime, mGuest);
            }
        };
    }

    /**
     * {@link RunnableGuest} 위에서 수행되는 작업을 정의한 메소드.
     *
     * @param offerTime 주 대기열에 대기시키기를 시도하는 시각
     * @param guest     작업이 정의된 대상
     */
    private static void runGuest(final long offerTime, final ThreadGuest guest) {
        final long waitTime = android.os.SystemClock.elapsedRealtime() - offerTime;    // offer 이후 기다린 시각
        if (waitTime > ThreadConfig.THREAD_WAIT_TIME) {    // 너무 늦은 경우
            if (guest.waitTimeout(waitTime)) {    // 늦었는데 계속할까? 타임아웃 true 이면
                return;    // 리턴해버림.
            }
        }
        try {
            final Object runResult = guest.run(waitTime);    // 본문 실행
            handleResult(guest, runResult);
        } catch (Exception e) {
            Log.e(DebugConfig.LOG_TAG, "Exception occurred in ThreadGuest.run()", e);    // 이건 run 안에서 발생한 에러이다. 공공연히 알려야 하므로 로그 래핑을 쓰지 않음.
        }
    }

    /**
     * 게스트의 {@link ThreadGuest#run(long) run()}이 끝났을 때 호출.
     * 결과를 보고 게스트 체인을 진행하거나 {@link ThreadGuest#after(Object) after()}를 실행하거나 한다.
     *
     * @param guest  작업 대상
     * @param result guest 의 {@link ThreadGuest#run(long) run()}이 리턴한 객체.
     */
    private static void handleResult(final ThreadGuest guest, final Object result) {
        try {
            if (result == null) {    // null 이면 결과를 넘겨줄 필요가 없음.
                processChain(guest);
            } else {
                // guest.run()의 결과를 메인스레드로 넘겨주는 작업.
                final ResultCarrier carrier = new ResultCarrier();    // 캐리어에 담는다.
                carrier.guest = guest;
                carrier.result = result;
                final Message messageToMain = resultHandler.obtainMessage();    // 메인스레드로 가자.
                messageToMain.obj = carrier;
                LogByCodeLab.v("ThreadHost.handleResult(): ResultCarrier departed");
                resultHandler.dispatchMessage(messageToMain);
                // 간 후의 작업은 resultHandler 정의를 참고.
            }
        } catch (Exception e) {
            LogByCodeLab.e(e);
        }
    }

    /**
     * 인자의 스레드 게스트 안에 체인이 세팅되어 있다면, 다음 게스트를 offer 하는 작업이 수행될 것이다.
     * 체인이 없으면 그냥 종료함.
     *
     * @param guest 현재 수행중인 스레드 게스트.
     */
    synchronized static void processChain(final ThreadGuest guest) {
        if (guest.mChainNextGuest == null) {    // 체인이 없으면
            // 더 이상 할 일이 없다.
            return;
        }
        // 체인이 있다.
        if (guest.mThreadChainBlocker != null) {    // 체크아웃 이벤트 기반 체인인 경우
            if (guest.mThreadChainBlocker.isBlocking()) {    // 블락 상태라면
                guest.mThreadChainBlocker.waiting(guest);    // 아무것도 하지 않고 체크아웃을 기다려야 한다.
                return;
            }
        }
        // 시간 기반 체인. 혹은 이미 언블록된 이벤트 기반 체인.
        final NextCarrier carrier = new NextCarrier();    // 캐리어에 담는다.
        carrier.nextGuest = guest.mChainNextGuest;
        carrier.nextGuest.setObject(guest.getObject());    // 다음 게스트에게 오브젝트를 전달한다.
        final Message messageToMain = nextHandler.obtainMessage();    // 메인스레드로 가자.
        messageToMain.obj = carrier;
        LogByCodeLab.v("ThreadHost.processChain(): NextCarrier departed with time " + guest.mChainDelay);
        if (guest.mChainDelay <= 0) {
            nextHandler.dispatchMessage(messageToMain);
        } else {
            nextHandler.sendMessageDelayed(messageToMain, guest.mChainDelay);
        }
        // 간 후의 작업은 nextHandler 정의를 참고.
    }

	/*	큐잉관리 끝	*/

}
