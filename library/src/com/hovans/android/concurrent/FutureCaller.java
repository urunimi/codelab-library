package com.hovans.android.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * For get {@link ThreadGuest}'s result, synchronously.<br>
 * <br>
 * Make new {@link ThreadGuest}, run and wait result.
 * Warn to avoid potential deadlock.
 * Don't execute in another {@link FutureCaller} or {@link ThreadGuest}<br>
 * <br>
 * Ex)<pre>
 * Integer val = new FutureCaller&lt;Integer&gt;() {
 *     public Integer call() throws Exception {
 *         Integer great = somethingGreat(); // Running on ThreadHost
 *         return great;
 *     }
 * }.execute();    // caller thread will blocking-wait.
 * // 'val' will have 'great' value after execution.
 * </pre>
 *
 * @param <T> Result class of {@link #call()}
 * @author Arngard
 */
@SuppressWarnings("unused")
public abstract class FutureCaller<T> implements Callable<T> {

    private FutureTask<T> mFuture;

    /**
     * initialize new FutureCaller to run {@link #call()} on ThreadHost.
     */
    public FutureCaller() {
        mFuture = new FutureTask<T>(this);
    }

    private void executeImpl() {
        new ThreadGuest() {

            @Override
            public Object run(long waitTimeMillis) {
                mFuture.run();
                return null;
            }
        }.execute();
    }

    /**
     * Run {@link #call()}, and return the result.
     * It will block current thread.
     *
     * @return 결과.
     * @throws CancellationException if the computation was cancelled
     * @throws InterruptedException  if the current thread was interrupted while waiting
     * @throws ExecutionException    if the computation threw an exception
     * @see #execute(long, TimeUnit)
     */
    public T execute() throws CancellationException, InterruptedException, ExecutionException, TimeoutException {
        executeImpl();
        return mFuture.get();
    }

    /**
     * Run {@link #call()}, and return the result.
     * It will block current thread, until time-out.
     *
     * @param timeout {@link FutureTask#get(long, java.util.concurrent.TimeUnit)} 의 인자.
     * @param unit    {@link FutureTask#get(long, java.util.concurrent.TimeUnit)} 의 인자.
     * @return 결과.
     * @throws CancellationException if the computation was cancelled
     * @throws InterruptedException  if the current thread was interrupted while waiting
     * @throws ExecutionException    if the computation threw an exception
     * @throws TimeoutException      if the wait timed out
     * @see #execute()
     */
    public T execute(long timeout, TimeUnit unit) throws CancellationException, InterruptedException, ExecutionException, TimeoutException {
        executeImpl();
        return mFuture.get(timeout, unit);
    }

    /**
     * Definition of code to run on ThreadGuest, when execute().
     * Don't use this method directly. use {@link #execute()}.
     *
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public abstract T call() throws Exception;

}
