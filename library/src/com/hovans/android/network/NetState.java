package com.hovans.android.network;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import com.hovans.android.concurrent.ThreadGuest;
import com.hovans.android.constant.CommonConfig;
import com.hovans.android.global.GlobalApplication;
import com.hovans.android.log.LogByCodeLab;

/**
 * 단말기의 네트워크 상태를 확인하기 위한 메소드를 제공한다.
 * @author Arngard, Hovan
 *
 */
public class NetState {

	/** 네트워크 상태 체크 실패 */
	@Deprecated
	public static final int UNCHECKED		= -2;
	/** 네트워크에 연결할 수 없음 */
	@Deprecated
	public static final int NOT_CONNECTED	= -1;
	/** Wi-Fi 네트워크 연결됨 */
	@Deprecated
	public static final int WIFI			= 0;
	/** 모바일 네트워크 연결됨 (3G...) */
	@Deprecated
	public static final int MOBILE		= 1;

	/**
	 * Network 상태를 체크한다.
	 * @param context Context 객체
	 * @return	접속된 네트워크 상태 값<br>
	 * {@link NetState#WIFI WIFI}, {@link NetState#MOBILE MOBILE},
	 * {@link NetState#NOT_CONNECTED NOT_CONNECTED}, {@link NetState#UNCHECKED UNCHECKED}
	 * @see NetworkInfo#getType()
	 */
	@Deprecated
	public static int getNetState(Context context) {
		int reVal = UNCHECKED;

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();

		if (ni == null) {
			reVal = NOT_CONNECTED;
		} else if(ni.isConnected() && ni.isAvailable()) {
			int type = ni.getType();	// 라이브러리에 getType() 함수의 리턴이 무슨 값인지에 대한 명세가 없다.
			switch (type) {	// android.net 라이브러리 변경에 대응하기 위한 변환
			case 0:
				reVal = WIFI;
				break;
			case 1:
				reVal = MOBILE;
				break;
			default:
				reVal = type;
				break;
			}
		}

		return reVal;
	}

	@Deprecated
	public static int getNetState(Context context, String address) {
		// 우선 물리적 연결을 검사
		int reVal = getNetState(context);
		if (reVal == NOT_CONNECTED)
			return reVal;

		// 호스트를 찾을 수 있는지 검사
		try {
			InetAddress.getAllByName(address);
		} catch(UnknownHostException uhe) {
			reVal = NOT_CONNECTED;
		}

		return reVal;
	}

	/**
	 * 단말기의 네트워크 연결 상태를 얻는다.
	 * @param context
	 * @return 네트워크 연결이 되어 있으면 true, 그렇지 않으면 false
	 */
	public static boolean isAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] infos = cm.getAllNetworkInfo();
		for (NetworkInfo info : infos) {
			if (info != null) {
				if (info.isConnected() && info.isAvailable()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * DNS lookup을 수행한다.<br>
	 * <br>
	 * 기본적으로 무거운 작업이다.
	 * 특히 네트워크 상태가 좋지 않은 경우 이 메소드는 시간이 오래 걸리므로 주의깊게 사용할 것.<br>
	 * <br>
	 * <font color=#ff0000>Caution!</font>이 메소드는 ThreadHost의 스레드에 대하여 wait 를 사용하므로,
	 * ThreadGuest 내부에서 호출하면 안 된다.
	 * 또, 이 메소드는 호출자의 스레드를 block 하고 작업한다.<br>
	 * 
	 * @param context
	 * @param address 대상 주소
	 * @param timeout 강제 타임아웃. DNS lookup 결과를 받기 위해 100ms 부터 시작해서 배수 시간 대기를 하는데, 전체 대기 시간이 이 시간보다 길어지면 중단한다.
	 * @return 지정된 주소로 접속 경로를 찾을 수 있으면 true, 그렇지 않으면 false
	 * @see NetState#getLoggedConnectivity(String)
	 */
	synchronized public static boolean isConnectable(Context context, final String address, final long timeout) {
		if (! isAvailable(context)) {
			setLoggedConnectivity(address, false);
			return false;
		}
		// DNS 수준에서 호스트를 찾을 수 있는지 검사해야 한다.

		//InetAddress[] ips = InetAddress.getAllByName(address);
		//if (ips.length <=0) {
		//	return false;
		//}
		//return true;

		/*
		 * 위는 단순 루틴.
		 *
		 * [문제 상황]
		 * 
		 * InetAddress.getByName() 은 DNS lookup 이다.
		 * 자바 인터페이스는 이 과정에 대해 타임아웃을 제공하지 않는다. (nslookup 에는 옵션이 있음)
		 * 인자를 받지 않고 내부적으로 정의된 방법을 사용한다.
		 * 
		 * 정상적인 접속 환경이라면 이 시간은 3초 이내 수준의 아주 짧은 시간일 것이다.
		 * 그러나 안 되는 경우에는 20초 이상, 심지어 40초에 달하는 시간도 기다리게 되어 있다.
		 * 이렇게까지 오래 결과가 오지 않는 이유야 여러 가지 있을 수 있고, 대부분 네트워크 중간에서 발생하는 문제다.
		 * 
		 * 그러나 아무리 안드로이드 모바일 클라이언트라고 해도 이렇게 긴 타임아웃은 필요치 않다.
		 * 이 문제를 어떻게 해결할 수 있는가?
		 * 
		 * 이하에 강제 타임아웃 매커니즘을 구현함.
		 * */

		/*
		 * [현재의 논리]
		 * 
		 * Atomic한 체커를 두고, 별도의 스레드에서 DNS lookup을 수행한다.
		 * 현재 스레드는 체커를 지켜보면서 대기를 반복한다.
		 * 현재 스레드는 체커의 상태가 결과값으로 바뀌거나 지정된 타임아웃에 도달하면 더 이상 체커를 지켜보지 않고 결과를 리턴한다.
		 * 별도의 스레드에서 일어나고 있는 lookup 작업은 시간이 지나면 알아서 종료될 것이다.
		 * */

		// 이 메소드는 스레드 정지 대기를 할텐데, 메인 스레드라면 ANR의 원인이 된다.
		// 가능하다면 컴파일 타임에 해결해야 하고, 안 되면 어쩔 수 없는 문제다. 로그 모드일 때 경고만 찍어주자.
		if (! CommonConfig.isReleaseVersion()  && Thread.currentThread().getId() == 1) {
			LogByCodeLab.w("NetState.isConnectable(): DNS lookup on main thread.");
		}
		LogByCodeLab.v("NetState.isConnectable(): start DNS lookup. address="+address);

		// 체커 새로 생성
		final AtomicInteger checker = new AtomicInteger(0);
		
		/*
		 * [결함 또는 위험]
		 * 
		 * 타임아웃으로 현재의 스레드는 리턴되었다고 하자. 이 메소드에 대한 요청이 한 차례 끝난 상태.
		 * 만일 별도 스레드의 lookup이 아직 안 끝났는데, 이 상태에서 다시 이 메소드에 요청이 들어오면 어떻게 되는 것인가?
		 * 별도의 스레드에 다시 요청을 해야 하는데 그 스레드는 정지되어 있다.
		 * 체커는 거의 반드시 갱신되어야 한다. 특히 잘 안 되는 상황에서.
		 * 
		 * 체커의 갱신에 ThreadGuest 를 사용한다면 offerFail 이 나거나 또는 대기시간 경고 메소드인 ThreadGuest.waitTimeout()이 불릴 것이다.
		 * */
		
		// 네트워크 상태를 체커에 마킹 작업. 별도의 스레드에서 해야 함.
		// 반드시 별도의 스레드에서 체커를 갱신한 후 종료해야 한다.
		new ThreadGuest(ThreadGuest.PRIORITY_ABOVE_NORMAL) {

			@Override
			public void offerFail() {
				checker.set(-4);	// -4는 쥬금 ㅠㅜ
			}

			@Override
			public boolean waitTimeout(/*Handler initHandler, */long waitTimeMillis) {
				checker.set(-4);	// -4는 쥬금 ㅠㅜ
				return true;
			}

			@Override
			public Object run(/*Handler initHandler, */long waitTimeMillis) {
				try {
					InetAddress ip = InetAddress.getByName(address);
					if (ip == null) {
						checker.set(-4);	// -4는 쥬금 ㅠㅜ
					} else {
						checker.set(7);	// 7은 성공
					}
				} catch(UnknownHostException uhe) {
					checker.set(-4);	// -4는 쥬금 ㅠㅜ
				}
				return null;
			}
		}
		.execute();
		
		// 배수 시간 대기, 체커 점검 작업
		int totalTime = 0;
		for (int time = 100; totalTime < timeout; time *= 2) {	// 100ms부터 배수 시간 대기. 총 대기 시간은 타임아웃 이내.
			/*
			 * 이 루프는 실제로 잘 되는 상황에서는 1~2회 밖에 돌지 않으므로 별 문제가 되지 않는다.
			 * 안 될 때가 문제.
			 * */
			if (checker.get() > 0) {	// 7은 성공
				LogByCodeLab.v("NetState.isConnectable(): success. totalTime="+totalTime);
				setLoggedConnectivity(address, true);
				return true;
			} else if (checker.get() < 0) {	// -4는 쥬금 ㅠㅜ
				LogByCodeLab.v("NetState.isConnectable(): fail with DNS lookup fail. totalTime="+totalTime);
				setLoggedConnectivity(address, false);
				return false;
			} else {	// 아마 0
				try {
					Thread.sleep(time);	// 스레드 정지 대기.
					totalTime += time;	// 총 대기 시간 집계.
				} catch (InterruptedException e) {
					LogByCodeLab.e(e);
					LogByCodeLab.v("NetState.isConnectable(): fail with threading error.");
					setLoggedConnectivity(address, false);
					return false;
				}
			}
		}
		LogByCodeLab.v("NetState.isConnectable(): fail with forced timeout. totalTime="+totalTime);
		setLoggedConnectivity(address, false);
		return false;
	}

	/**
	 * DNS lookup 결과를 저장하는 정보 단위.
	 * @author arngard
	 */
	public static class ConnectivityLog {
		/**
		 * 이 로그의 저장이 이루어진 시점
		 */
		public long elapsedRealtime = 0;
		/**
		 * {@link NetState#isConnectable(Context, String, long)}이 리턴했던 값.
		 */
		public boolean isConnectable = false;
		public ConnectivityLog(long elapsedRealtime, boolean isConnectable) {
			this.elapsedRealtime = elapsedRealtime;
			this.isConnectable = isConnectable;
		}
	}
	
	/**
	 * {@link #isConnectable(Context, String)} 는 무겁다.
	 * 그리고 그 리턴은 물리적 갱신 시간과 연관되기 때문에 시간적으로 긴 유효성을 가진다.
	 * 따라서 그 결과를 어딘가에 저장하고 있다가 재활용하면 좋다.
	 * (주소, ConnectivityLog) 쌍을 저장해두자.
	 * */
	private static Hashtable<String, ConnectivityLog> loggedConnectivities = new Hashtable<String, ConnectivityLog>();
	
	/**
	 * {@link #getLoggedConnectivity(String)} 가 돌려주는 정보를 초기화한다.
	 */
	public static void resetLoggedConnectivity() {
		loggedConnectivities.clear();
	}
	
	/**
	 * {@link #isConnectable(Context, String)} 의 마지막 호출 결과와 시점을 기억해둔다.
	 * @param address
	 * @param isConnectable
	 */
	private static void setLoggedConnectivity(final String address, final boolean isConnectable) {
		loggedConnectivities.put(address, new ConnectivityLog(SystemClock.elapsedRealtime(), isConnectable));
	}
	
	/**
	 * {@link #isConnectable(Context, String, long)} 의 마지막 호출 결과와 시점을 기억하고 있다가 돌려준다.
	 * @param address
	 * @return 찾은 로그 정보
	 */
	public static ConnectivityLog getLoggedConnectivity(final String address) {
		return loggedConnectivities.get(address);
	}

	/**
	 * 지정된 기준 시간 내에 접속에 성공하거나 실패한 기록이 있으면 마지막 성패 여부를 알려준다.
	 * @param successLogTimeout 성공기록을 검색할 기준
	 * @param failLogTimeout 실패기록을 검색할 기준
	 * @return 기록이 없거나 기준에 미달이면 0. 기준에 합당한 성공기록이면 1. 기준에 합당한 실패기록이면 -1.
	 */
	public static int checkLastConnectivity(final String address, long successLogTimeout, long failLogTimeout) {
		ConnectivityLog connectivityLog = NetState.getLoggedConnectivity(address);
		long currentTime = SystemClock.elapsedRealtime();
		if (connectivityLog != null) {	// 기록이 있고
			if (! connectivityLog.isConnectable) {	// 실패기록이다.
				if (currentTime - connectivityLog.elapsedRealtime < failLogTimeout) {	// 너무 오래 전이 아니라면 유의미한 판단 요소로 사용한다.
					return -1;
				}
			} else {	// 성공기록이다
				if (currentTime - connectivityLog.elapsedRealtime < successLogTimeout) {	// 너무 오래 전이 아니라면 유의미한 판단 요소로 사용한다.
					return 1;
				}
			}
		}
		return 0;
	}

	/**
	 * 핑 테스트를 수행한다.<br>
	 * <br>
	 * 네트워크 상태가 좋지 않은 경우 이 메소드는 시간이 오래 걸리므로 주의깊게 사용할 것.<br>
	 * <br>
	 * {@link #isConnectable(Context, String, long)} 의 경우 타임아웃 루틴이 보완되어 있으며
	 * 문제를 완화할 수 있는 연관 메소드들이 있지만, 이 메소드는 그런 것이 없다.
	 * 그냥 있는 그대로의 핑 대기 동작을 수행한다.
	 * @param context
	 * @param address 대상 주소
	 * @return 핑 테스트의 결과. 성공이면 true.
	 */
	public static boolean pingTest(Context context, String address, int timeout) {
		if (! isAvailable(context)) {
			return false;
		}
		// 호스트를 찾을 수 있는지 검사
		try {
			InetAddress ip = InetAddress.getByName(address);
			if (! ip.isReachable(timeout)) {
				LogByCodeLab.i("NetState.pingTest(): Unreachable");
				return false;
			} else {
				LogByCodeLab.i("NetState.pingTest(): Reachable");
				return true;
			}
		} catch(UnknownHostException uhe) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 디바이스의 WIFI 주소를 얻는다.
	 * @return 디바이스의 WIFI 주소
	 */
	public static InetAddress getWifiIpAddress() {
		Context ctxt = GlobalApplication.getContext();
		WifiManager wifiManager = (WifiManager) ctxt.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();

		try {
			return (Inet4Address) Inet4Address.getByAddress(intToByteArray(ipAddress));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param i
	 * @return byte[4]. 인자의 정수를 낮은 자리부터 순서대로 분해한 바이트 배열.
	 * byte[0] 위치에 가장 낮은 자리 바이트가 들어 있다.
	 */
	private static byte[] intToByteArray(int i) {
		byte[] dword = new byte[4];
		dword[0] = (byte) (i & 0xFF);
		dword[1] = (byte) ((i >> 8) & 0xFF);
		dword[2] = (byte) ((i >> 16) & 0xFF);
		dword[3] = (byte) ((i >> 24) & 0xFF);
		return dword;
	}

}
