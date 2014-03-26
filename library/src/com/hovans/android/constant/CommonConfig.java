package com.hovans.android.constant;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

import com.hovans.android.global.GlobalApplication;

/**
 * 설정 사항들을 모아둠.<br>
 * <br>
 * 이 클래스의 메소드는 GlobalApplication 을 사용할 때에만 동작한다.
 * @author Arngard
 *
 */
public class CommonConfig {
	
	/* 버전 판정 - STT */

	/** 베타 버전 */
	public static final int	VERSION_BETA			= -2;
	/** 개발 버전 */
	public static final int	VERSION_DEBUG			= -1;
	/** 버전 판정이 이루어지기 전의 기본값 */
	public static final int	VERSION_UNKNOWN			= 0;
	/** 정식 버전 */
	public static final int	VERSION_RELEASE			= 1;

	/** 디버그 키스토어의 해시 값 */
	private static final int	HASH_DEBUG				= -160;	// 사용한 키스토어에 맞는 값을 넣어줘야 한다.
	/** 배타 버전 키스토어의 해시 값 */
	private static final int	HASH_BETA				= 143;	// 사용한 키스토어에 맞는 값을 넣어줘야 한다.

	/** 버전 확인을 매번 다시 하지 않도록, 한 번 알아내면 저장해둔다. */
	private static int		calculatedVersionInfo	= VERSION_UNKNOWN;

	/**
	 * keystore를 통해 릴리즈버전인지 디버그 버전인지 자동으로 판단한 후 결과를 리턴. 혹은 이전에 판정이 이루어졌다면 그것을 리턴.
	 * @return {@link #VERSION_DEBUG}, {@link #VERSION_RELEASE} 등을 리턴해 줌.
	 * 0보다 크거나 같으면 public한 버전이라고 보고 로그 등을 숨기면 된다.
	 */
	synchronized public static int getVersionConfig() {
		if (calculatedVersionInfo != VERSION_UNKNOWN) {
			return calculatedVersionInfo;
		}
		try {
			Context context = GlobalApplication.getContext();
			if (context != null) {
				Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
				for (Signature sig : sigs) {
					MessageDigest sha = MessageDigest.getInstance("SHA-1");
					sha.update(sig.toByteArray());
					byte[] digest = sha.digest();
					int sum = 0;
					for (int di = 0; di < digest.length; ++di) {
						sum += digest[di];
					}
					switch (sum) {
					case HASH_DEBUG:
						System.out.println("com.codelab.library.constant.CommonConfig.isDebugVersion(): Debug Version.");// + sum);
						calculatedVersionInfo = VERSION_DEBUG;
						return calculatedVersionInfo;
					case HASH_BETA:
						System.out.println("com.codelab.library.constant.CommonConfig.isDebugVersion(): Beta Version.");// + sum);
						calculatedVersionInfo = VERSION_BETA;
						return calculatedVersionInfo;
					default:
						System.out.println("com.codelab.library.constant.CommonConfig.isDebugVersion(): Release Version.");// + sum);
						calculatedVersionInfo = VERSION_RELEASE;
						return calculatedVersionInfo;
					}
				}
			} else {
				// GlobalApplication 클래스를 사용하지 않기 때문에 Context 를 얻을 수 없는 경우.
				calculatedVersionInfo = VERSION_RELEASE;	// 무조건 릴리즈로 대응한다.
				return calculatedVersionInfo;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		System.out.println("com.codelab.library.constant.CommonConfig.isDebugVersion(): Version verification fail.");
		calculatedVersionInfo = VERSION_UNKNOWN;
		return calculatedVersionInfo;
	}
	
	/**
	 * 디버그 버전인지를 판단한다.
	 * @return {@link #getVersionConfig()} == {@link #VERSION_DEBUG};
	 */
	public static boolean isDebugVersion() {
		return getVersionConfig() == VERSION_DEBUG;
	}
	
	/**
	 * 베타 버전인지를 판단한다.
	 * @return {@link #getVersionConfig()} == {@link #VERSION_BETA};
	 */
	public static boolean isBetaVersion() {
		return getVersionConfig() == VERSION_BETA;
	}
	
	/**
	 * 릴리즈 버전인지를 판단한다.
	 * @return {@link #getVersionConfig()} == {@link #VERSION_RELEASE};
	 */
	public static boolean isReleaseVersion() {
		return getVersionConfig() == VERSION_RELEASE;
	}
	
	/* 버전 판정 - END */

}
