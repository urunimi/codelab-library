package com.hovans.network;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;

/**
 * NetHttpTask.java
 *
 * @author Hovan Yoo
 */
public class NetHttpTask {

	static final String TAG = NetHttpTask.class.getSimpleName();

	static final Gson gson = new Gson();

	final String url, waitString;
	final HashMap<String, String> params;
	final boolean synchronousMode;
	final Activity activityForProgress;
	ProgressDialog progressDialog;

	public void post(final ResponseHandler callback) {

		AsyncHttpClient httpClient = synchronousMode? new SyncHttpClient(getNewHttpSchemeRegistry()) : new AsyncHttpClient(getNewHttpSchemeRegistry());

		RequestParams requestParams = new RequestParams();
		requestParams.put("locale", Locale.getDefault().toString());

		if(params != null) {
			for(String key : params.keySet()) {
				requestParams.put(key, params.get(key));
			}
		}
//
//		try {
//			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//			trustStore.load(null, null);
//			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
//			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//			httpClient.setSSLSocketFactory(sf);
//		} catch (Exception e) {
//			Log.e(TAG, e.getMessage());
//		}

		if(activityForProgress != null) {
			activityForProgress.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					progressDialog = new ProgressDialog(activityForProgress);
					progressDialog.setMessage(waitString);
					progressDialog.setCancelable(false);
					progressDialog.show();
				}
			});
		}

		httpClient.post(url, requestParams, new TextHttpResponseHandler() {
			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				closeDialogIfItNeeds();
				try {
					callback.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class));
				} catch (Exception e) {
					callback.onFail(statusCode, null);
				}
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, String responseString) {
				try {
					JSONObject jsonObject = new JSONObject(responseString);

					if(jsonObject.getInt("code") != 0) {
						callback.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class));
					}

//					NetHttpResult httpResult = new NetHttpResult();
//					httpResult.code = jsonObject.getInt("code");
//					httpResult.message = jsonObject.getString("message");
//					httpResult.result = ;
					closeDialogIfItNeeds();
					String result = null;
					if(jsonObject.has("result")) {
						result = jsonObject.getString("result");
					}



					callback.onSuccess(statusCode, result);
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		});
	}

	void closeDialogIfItNeeds() {
		if(progressDialog != null && progressDialog.isShowing()) {
			activityForProgress.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						progressDialog.dismiss();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public SchemeRegistry getNewHttpSchemeRegistry() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			return registry;

		} catch (Exception e) {
			return null;
		}
	}

	public interface ResponseHandler {
		void onSuccess(int statusCode, String result);
		void onFail(int statusCode, NetHttpResult result);
	}



	private NetHttpTask(String url, HashMap<String, String> params, boolean syncronous, Activity activityForProgress, String waitString) {
		this.waitString = waitString;
		this.url = url;
		this.params = params;

		if(Looper.myLooper() == null) synchronousMode = true;
		else {
			this.synchronousMode = syncronous;
		}
		this.activityForProgress = activityForProgress;
	}

	public static class Builder {
		String url;
		HashMap<String, String> params = new HashMap<>();
		boolean synchronousMode;

		Activity activityForProgress;
		String waitString;

//		final String URL_BASE = "http://autoguard.hovans.com";

		public Builder setParams(HashMap<String, String> params) {
			this.params = params;
			return this;
		}

		public Builder addParam(String key, String value) {
			params.put(key, value);
			return this;
		}

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}

		public Builder showProgress(Activity activity, String waitString) {
			activityForProgress = activity;
			this.waitString = waitString;
			return this;
		}

//		public Builder setPath(String path) {
//			if(url != null) throw new UnsupportedOperationException("Only path or url should be set");
//
//			if(path.startsWith("/") == false) {
//				path = "/" + path;
//			}
//			this.url = URL_BASE + path;
//			return this;
//		}

		public Builder setSyncMode(boolean synchronousMode) {
			this.synchronousMode = synchronousMode;
			return this;
		}

		public NetHttpTask build() {
			return new NetHttpTask(url, params, synchronousMode, activityForProgress, waitString);
		}
	}


}

class MySSLSocketFactory extends SSLSocketFactory {
	SSLContext sslContext = SSLContext.getInstance("TLS");

	public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
		super(truststore);

		TrustManager tm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		sslContext.init(null, new TrustManager[] { tm }, null);
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	}

	@Override
	public Socket createSocket() throws IOException {
		return sslContext.getSocketFactory().createSocket();
	}
}