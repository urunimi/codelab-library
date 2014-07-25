package com.hovans.android.app;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.hovans.android.log.LogByCodeLab;

/**
 * 인텐트 송수신에서 도움이 될 만한 메소드들
 * 
 * @author Arngard, Hovan
 *
 */
public class IntentHelper {

	/**
	 * 인자의 인텐트를 수신할 수 있는 모든 컴포넌트들을 검색한다.
	 * @param context The application's environment.
	 * @param intent The Intent to check for availability.
	 * @return 인자의 인텐트를 수신 가능한 요소들의 목록
	 */
	private static final List<ResolveInfo> queryAll(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent,
				PackageManager.GET_INTENT_FILTERS |
				PackageManager.GET_RESOLVED_FILTER);
		list.addAll(packageManager.queryIntentActivities(intent,
				PackageManager.GET_INTENT_FILTERS |
				PackageManager.GET_RESOLVED_FILTER));
		list.addAll(packageManager.queryIntentServices(intent,
				PackageManager.GET_INTENT_FILTERS |
				PackageManager.GET_RESOLVED_FILTER));
		return list;
	}

	/**
	 * 인자의 인텐트를 수신할 수 있는 패키지가 있는지 확인한다.
	 * @param context The application's environment.
	 * @param intent The Intent to check for availability.
	 * @return True if the specified intent can be sent and
	 * responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, Intent intent) {
		return queryAll(context, intent).size() > 0;
	}

	/**
	 * 대상 패키지가 인자의 인텐트를 수신할 수 있을지 확인한다.
	 * @param context The application's environment.
	 * @param intent The Intent to check for availability.
	 * @param packageName limit information. Search target in this package.
	 * @return True if the specified intent can be sent and
	 * responded to by a component in the package, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, Intent intent, String packageName) {
		List<ResolveInfo> list = queryAll(context, intent);
		for (ResolveInfo resolveInfo : list) {
			if (resolveInfo.activityInfo == null) {
				continue;
			}
			if (resolveInfo.activityInfo.packageName == null) {
				continue;
			}
			if (resolveInfo.activityInfo.packageName.equals(packageName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 해당 패키지가 설치되어있는지 확인한다.
	 * @param context The application's environment.
	 * @param packageName limit information. Search target in this package. 
	 * @return True if the package is exists.
	 */
	public static boolean isPackageExists(Context context, String packageName) {
		try {
			List<ApplicationInfo> packages;
			PackageManager pm = context.getPackageManager();
			packages = pm.getInstalledApplications(0);
			for (ApplicationInfo packageInfo : packages) {
				if (packageInfo.packageName.equals(packageName))
					return true;
			}
		} catch(Exception e) {
			LogByCodeLab.e(e);
		}

		return false;
	}
}
