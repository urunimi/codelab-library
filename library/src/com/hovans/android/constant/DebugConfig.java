package com.hovans.android.constant;


import com.hovans.android.global.GlobalApplication;

/**
 * 디버깅 관련 상수 모음.

 * @author arngard
 *
 */
public class DebugConfig {
	
	/*
	 * 이 클래스의 정의를 할 때 {@link CommonConfig#getVersionConfig()}를 고려할 것.
	 * */

	/** 로그캣 태그 */
	public static final String	LOG_TAG						= GlobalApplication.getContext().getPackageName();
	public static final String	LOG_FOLDER					= "logs";

	/** 고의로 로딩 시간에 딜레이를 더 준다. */
	public static final int		LOAD_TIME_ADD_MILLIS		= CommonConfig.getVersionConfig() < 0 ? 500 : 0;

	public static Boolean DEBUG = null;

	/**
	 * 로그캣에 출력을 할지를 얻어온다.
	 * @return 로그캣에 출력을 할지
	 */
	public static boolean isShowLogCat() {
		if(DEBUG == null) {
			DEBUG = CommonConfig.getVersionConfig() < 0;
		}

		return DEBUG;
	}
}
