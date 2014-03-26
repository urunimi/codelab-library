package com.hovans.android.global;

import android.app.Activity;
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
public class GlobalApplication extends Application {

	/**
	 * {@link #onCreate()}에서 이 변수에 Application Context 를 저장한다.
	 */
	protected static Context sContext = null;

	protected static Handler sHandler = new Handler();

	/**
	 * {@link #isDebuggable()}이 리턴하는 결과값을 캐싱하기 위한 변수.
	 * 어차피 이 결과값은 바뀌는 값이 아니기 때문에 매번 새로 얻어올 필요가 없다.
	 * */
	private static Boolean sIsDebuggable = null;	// tri-state(nullable).

	/**
	 * 외부에 출력하는 사용 방법 경고
	 */
	private static final void usageWarning() {
		Log.w(DebugConfig.LOG_TAG, "com.codelab.library.global.GlobalApplication: Wrong usage.");
	} 

	/**
	 * 인자로 준 액티비티가 종속되어 있는 Application 객체를 얻어온다.
	 * 이 때 얻어오는 객체는 어플리케이션의 모든 실행 요소가
	 * 제거(Task kill 등)되지 않는 한 계속 살아있는 객체이다.<br>
	 * @param activity 생성이 완료되지 않은 액티비티를 사용하지 않도록 주의한다.
	 * 그런 경우에는 내부 요소가 모두 구성되었으리라고 보장할 수 없다.
	 * onCreate() 등이 호출된 이후의 객체를 사용하도록 한다.
	 * @return activity.getApplication()
	 */
	public static GlobalApplication of(Activity activity) {
		return (GlobalApplication)(activity.getApplication());
	}

	/**
	 * {@link #onCreate()}에서 저장해둔 어플리케이션 Context를 얻는다.
	 * @return Application Context
	 */
	public static Context getContext() {
		return sContext;
	}

	/**
	 * 어플리케이션 리소스를 얻는다.
	 * @return 리소스.
	 */
	public static Resources getResource() {
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
	public static boolean isDebuggable() {
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

	public static void runOnUiThread(Runnable runnable) {
		sHandler.post(runnable);
	}

	public static void runOnUiThread(Runnable runnable, long millis) {
		sHandler.postDelayed(runnable, millis);
	}

	public static void removeCallbacks(Runnable runnable) {
		sHandler.removeCallbacks(runnable);
	}

	public static Handler getHandler() {
		return sHandler;
	}

	/**
	 * 이 메소드를 상속할 경우 super 호출을 빠뜨리지 말 것.
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		sContext = null;
		sContext = getApplicationContext();
	}

}
