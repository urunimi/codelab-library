package com.hovans.android.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ThreadGuest} 에서 수행한 루틴을 동기적으로 얻어오는 과정을 가이드한다.<br>
 * <br>
 * 내부적으로 {@link ThreadGuest}를 만들어 수행하고 결과를 위해 대기한다.
 * 데드락이 발생하지 않도록 주의.
 * 다른 {@link FutureCaller} 또는 {@link ThreadGuest} 안에서 실행시키지 말 것.<br>
 * <br>
 * 사용 예:<pre>
 * Integer val = new FutureExecuter&lt;Integer&gt;() {
 *     public Integer call() throws Exception {
 *         // 이 위치의 작업은 ThreadHost에서 수행된다.
 *         return 10;
 *     }
 * }
 * .execute();    // call() 의 수행을 요청한 후 현재 스레드는 blocking 대기한다.
 * // 수행이 끝나면 val 에 10 이 할당된다.
 * </pre>
 *
 * @param <T> 실행 결과의 클래스
 * @author Arngard
 */
public abstract class FutureCaller<T> implements Callable<T> {

    private FutureTask<T> mFuture;

    /**
     * {@link ThreadHost}의 스레드에서 작업을 수행하고,
     * 그 결과를 동기적으로 얻어오기 위한 정의를 생성한다.
     */
    public FutureCaller() {
        mFuture = new FutureTask<T>(this);
    }

    private final void executeImple() {
        new ThreadGuest() {

            @Override
            public Object run(long waitTimeMillis) {
                mFuture.run();
                return null;
            }
        }
                .execute();
    }

    /**
     * {@link #call()}에 대한 수행을 하고 그 결과를 리턴한다.
     *
     * @return 결과. 문제가 발생하면 null.
     * @see #execute(long, TimeUnit)
     */
    public T execute() {
        try {
            executeImple();
            return mFuture.get();
        } catch (CancellationException e) {
            return null;
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    /**
     * {@link #call()}에 대한 수행을 하고 그 결과를 리턴한다.
     *
     * @param timeout
     * @param unit
     * @return 결과. 문제가 발생하면 null.
     * @see #execute()
     */
    public T execute(long timeout, TimeUnit unit) {
        try {
            executeImple();
            return mFuture.get(timeout, unit);
        } catch (CancellationException e) {
            return null;
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        } catch (TimeoutException e) {
            return null;
        }
    }

    /**
     * execute 를 했을 때 {@link ThreadHost}의 스레드에서 호출될 코드를 정의한다.
     * {@link ThreadHost}를 통한 실행은 이 메소드를 직접 호출하는 것이 아님에 주의.
     * {@link #execute()} 를 사용하자.
     *
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public abstract T call() throws Exception;

}
