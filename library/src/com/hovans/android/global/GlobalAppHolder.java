package com.hovans.android.global;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import com.hovans.android.constant.DebugConfig;
import com.hovans.android.log.LogByCodeLab;

/**
 * Global Context와 Resource를 손쉽게 얻기 위한 래핑 메소드를 가지고 있다.<br>
 * <br>
 * 이 클래스의 메소드들이 정상 동작하기 위해서는,
 * manifest 의 application 태그의 android:name 에
 * 이 클래스(또는 상속받은 자식 클래스)를 지정해줘야 한다.
 * 또 상속을 했을 경우 super 를 불러줘야 한다.
 * 
 * @author Arngard
 *
 */
public class GlobalAppHolder {

	/**
	 * {@link #init(Application)}에서 이 변수에 Application Context 를 저장한다.
	 */
	protected Context sContext = null;
	protected Application sApp = null;
	protected Handler sHandler = new Handler();

	/**
	 * {@link #isDebuggable()}이 리턴하는 결과값을 캐싱하기 위한 변수.
	 * 어차피 이 결과값은 바뀌는 값이 아니기 때문에 매번 새로 얻어올 필요가 없다.
	 * */
	private Boolean sIsDebuggable = null;    // tri-state(nullable).

	/**
	 * 외부에 출력하는 사용 방법 경고
	 */
	private void usageWarning() {
		Log.w(DebugConfig.LOG_TAG, "com.codelab.library.global.GlobalApplication: Wrong usage.");
	}

	/**
	 * {@link #init(Application)}에서 저장해둔 어플리케이션 Context를 얻는다.
	 * @return Application Context
	 */
	public Context getContext() {
		return sContext;
	}

	/**
	 * 어플리케이션 리소스를 얻는다.
	 * @return 리소스.
	 */
	public Resources getResource() {
		if (sContext == null) {
			usageWarning();
			return null;
		}
		return sContext.getResources();
	}

	/**
	 * 이 메소드의 리턴값은 내부적으로 캐시된다.
	 * 정상적으로 값을 얻어왔다면 이후의 호출에서 연산이 절약될 것이다.
	 * 결과값을 호출자가 따로 캐시할 필요도 없다.
	 * @return masifest를 통해 인식한 android:debuggable 값.
	 */
	public boolean isDebuggable() {
		// 캐시된 값이 있으면 그냥 돌려준다.
		if (sIsDebuggable != null) {
			return sIsDebuggable;
		}
		// 없으면 새로 값을 얻고 캐싱 처리.
		// 값을 얻지 못한 경우 캐싱 처리가 일어나지 않는다.
		try {
			if (sContext == null) {
				usageWarning();
				return false;
			}
			sIsDebuggable = 0 != (sContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
			return sIsDebuggable;
		} catch (Exception e) {
			LogByCodeLab.e(e);
		}
		return false;
	}

	public void runOnUiThread(Runnable runnable) {
		sHandler.post(runnable);
	}

	public void runOnUiThread(Runnable runnable, long millis) {
		sHandler.postDelayed(runnable, millis);
	}

	public void removeCallbacks(Runnable runnable) {
		sHandler.removeCallbacks(runnable);
	}

	public Handler getHandler() {
		return sHandler;
	}

	protected static GlobalAppHolder sInstance;

	public static GlobalAppHolder get() {
		if (sInstance == null) {
			sInstance = new GlobalAppHolder();
		}
		return sInstance;
	}

	public void init(Application application) {
		sApp = application;
		sContext = application.getApplicationContext();
	}
}
