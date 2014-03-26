package com.hovans.android.database;

import java.lang.ref.WeakReference;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 *  Async 하게 ContentProvider 로 특정 Query 를 요청하고 등록한 Listener 로 
 *  return 을 받을 수 있게 하는 Handler 이다.
 *  
 *  <p>-Data 에 대한 Access 는 main thread 에서 하지 않는 것이 원칙이다. 모든 return 은 call back 으로 돌아간다.</p>
 *  
 *  Asynchronous Query Handle 은 {@link AsyncQueryHandler} 를 상속받아 구현 했다. 
 *  AsyncQueryHandler 는 Main Thread 에서 Handler 를 상속한 class 로
 *  내부적으로 Worker Thread 를 만들고 해당 Worker Thread 에 대한 Handler 를 구현한 뒤,
 *  특정 Action 을 요청하면 Worker Thread Handler 에게 넘기고, 작업이 완료되면 결과를
 *  Main Thread 의 Handler (자기 자신) 으로 넘겨 받는 방식을 사용한다. </p>
 *  Class Main Thread(UI Thread)에서 선언 해서 사용해야 한다. 
 *  </p>
 *  
 * @author namkhoh
 *
 */
public class NotifyingQueryHandler extends AsyncQueryHandler {

	/**
	 * Query 에 대한 Callback 을 받을 Listener 이다.
	 * Listener 가 선언된 View 가 사라질 경우를 대비해
	 * WeakReference 로 해당 Reference를 가지고 있는다.
	 */
	private WeakReference<AsyncQueryListener> mListener;
	
	/**
	 * NotifyingAsyncQueryHandler 를 통해 요청한 Query 에 대한 Call back 을 원할 경우 구현하는 interface
	 * 
	 * 
	 * @author namkhoh
	 *
	 */
	public interface AsyncQueryListener {
        void onQueryComplete(int token, Object cookie, Cursor cursor);
        void onInsertComplete(int token, Object cookie, Uri uri);
        void onUpdateComplete(int token, Object cookie, int result);
        void onDeleteComplete(int token, Object cookie, int result);
    }

	public NotifyingQueryHandler(ContentResolver resolver, AsyncQueryListener listener) {
        super(resolver);
        setQueryListener(listener);
    }

	
	/**
     * Assign the given {@link AsyncQueryListener} to receive query events from
     * asynchronous calls. Will replace any existing listener.
     */
	public void setQueryListener(AsyncQueryListener listener) {
        mListener = new WeakReference<AsyncQueryListener>(listener);
    }
	
	/**
     * Clear any {@link AsyncQueryListener} set through
     * {@link #setQueryListener(AsyncQueryListener)}
     */
    public void clearQueryListener() {
        mListener = null;
    }

    /**
     * Begin an asynchronous update with the given arguments.
     */
    public void startUpdate(Uri uri, ContentValues values) {
        startUpdate(-1, null, uri, values, null, null);
    }

    public void startInsert(Uri uri, ContentValues values) {
        startInsert(-1, null, uri, values);
    }

    public void startDelete(Uri uri) {
        startDelete(-1, null, uri, null, null);
    }
    
    public void startDelete(int token, Uri uri) {
        startDelete(token, null, uri, null, null);
    }

    /**
     * Begin an asynchronous query with the given arguments. When finished,
     * {@link AsyncQueryListener#onQueryComplete(int, Object, Cursor)} is
     * called if a valid {@link AsyncQueryListener} is present.
     */
    public void startQuery(Uri uri, String[] projection) {
        startQuery(-1, null, uri, projection, null, null, null);
    }

    /**
     * Begin an asynchronous query with the given arguments. When finished,
     * {@link AsyncQueryListener#onQueryComplete(int, Object, Cursor)} is called
     * if a valid {@link AsyncQueryListener} is present.
     *
     * @param token Unique identifier passed through to
     *            {@link AsyncQueryListener#onQueryComplete(int, Object, Cursor)}
     */
    public void startQuery(int token, Uri uri, String[] projection) {
        startQuery(token, null, uri, projection, null, null, null);
    }

    /**
     * Begin an asynchronous query with the given arguments. When finished,
     * {@link AsyncQueryListener#onQueryComplete(int, Object, Cursor)} is called
     * if a valid {@link AsyncQueryListener} is present.
     */
    public void startQuery(Uri uri, String[] projection, String sortOrder) {
        startQuery(-1, null, uri, projection, null, null, sortOrder);
    }

    /**
     * Begin an asynchronous query with the given arguments. When finished,
     * {@link AsyncQueryListener#onQueryComplete(int, Object, Cursor)} is called
     * if a valid {@link AsyncQueryListener} is present.
     */
    public void startQuery(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String orderBy) {
        startQuery(-1, null, uri, projection, selection, selectionArgs, orderBy);
    }
    
    /**
     * Main Thread 이므로 UI 작업을 할 수 있다. 
     * 
     */
    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
    	final AsyncQueryListener listener = mListener == null ? null : mListener.get();
        if (listener != null) {
            listener.onQueryComplete(token, cookie, cursor);
        } else if (cursor != null) {
            cursor.close();
        }
    }
    
    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
    	final AsyncQueryListener listener = mListener == null ? null : mListener.get();
    	if (listener != null) {
    		listener.onInsertComplete(token, cookie, uri);
    	}
    }
    
    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
    	final AsyncQueryListener listener = mListener == null ? null : mListener.get();
    	if (listener != null) {
    		listener.onUpdateComplete(token, cookie, result);
    	}
    }
    
    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
    	final AsyncQueryListener listener = mListener == null ? null : mListener.get();
    	if (listener != null) {
    		listener.onDeleteComplete(token, cookie, result);
    	}
    }
}
