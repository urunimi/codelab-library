package com.hovans.network;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * NetHttpTask.java
 *
 * @author Hovan Yoo
 */
public class NetHttpTask {

	static final String TAG = NetHttpTask.class.getSimpleName();

	static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	@Expose
	final String url;
	final String waitString;
	@Expose
	final HashMap<String, String> params;

	final boolean synchronousMode;
	final Activity activityForProgress;
	final SSLSocketFactory sslSocketFactory;
	ProgressDialog progressDialog;
	ResponseHandler callback;
	Handler handler;


	public void post(final ResponseHandler callback) {
		this.callback = callback;

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

		if(synchronousMode == false && Looper.myLooper() != null) {
			worker.start();
			handler = new Handler();
		} else {
			worker.run();
		}

//		httpClient.post(url, requestParams, new TextHttpResponseHandler() {
//			@Override
//			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
//				closeDialogIfItNeeds();
//				try {
//					callback.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class));
//				} catch (Exception e) {
//					callback.onFail(statusCode, null);
//				}
//			}
//
//			@Override
//			public void onSuccess(int statusCode, Header[] headers, String responseString) {
//				try {
//					JSONObject jsonObject = new JSONObject(responseString);
//
//					if(jsonObject.getInt("code") != 0) {
//						callback.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class));
//					}
//
//					closeDialogIfItNeeds();
//					String result = null;
//					if(jsonObject.has("result")) {
//						result = jsonObject.getString("result");
//					}
//
//
//
//					callback.onSuccess(statusCode, result);
//				} catch (JSONException e) {
//					Log.e(TAG, e.getMessage());
//				}
//			}
//		});
	}

	public String makeBackup() {
		return gson.toJson(this);
	}

	public static NetHttpTask restoreBackup(String gsonData) {
		NetHttpTask backup = gson.fromJson(gsonData, NetHttpTask.class);
		NetHttpTask task = new NetHttpTask(backup);
		return task;
	}

	Thread worker = new Thread() {
		@Override
		public void run() {
			try {
				HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
				if(url.startsWith("https")) {
					if(sslSocketFactory != null) ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
				}
				urlConnection.setReadTimeout(10000);
				urlConnection.setConnectTimeout(15000);
				urlConnection.setRequestMethod("POST");
				urlConnection.setDoInput(true);
				urlConnection.setDoOutput(true);

//				List<NameValuePair> params = new ArrayList<NameValuePair>();

				StringBuilder result = new StringBuilder();
				boolean first = true;

				for (String key : params.keySet()) {
					String value = params.get(key);

					if(value == null || value.equals("")) continue;
					if (first)
						first = false;
					else
						result.append("&");

					result.append(URLEncoder.encode(key, "UTF-8"));
					result.append("=");
					result.append(URLEncoder.encode(value, "UTF-8"));
				}

				OutputStream os = urlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(os, "UTF-8"));
				writer.write(result.toString());
				writer.flush();
				writer.close();
				os.close();

				final int statusCode = urlConnection.getResponseCode();

				BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				String inputLine;
				StringBuilder response = new StringBuilder();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				final String responseString = response.toString();

				if(handler != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							handleResponse(statusCode, responseString, null);
						}
					});
				} else {
					handleResponse(statusCode, responseString, null);
				}

			} catch (Exception e) {
				handleResponse(9999, null, e);
			}
		}
	};

	void handleResponse(int statusCode, String responseString, Throwable th) {
		closeDialogIfItNeeds();
		switch(statusCode) {
			case 200:
				try {
					JSONObject jsonObject = new JSONObject(responseString);

					if(jsonObject.getInt("code") != 0) {
						callback.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class), null);
					} else {

						String resultString = null;
						if(jsonObject.has("result")) {
							resultString = jsonObject.getString("result");
						}

						callback.onSuccess(statusCode, resultString);
					}
				} catch (Exception e) {
					callback.onFail(statusCode, null, e);
				}

				break;
			default:
				try {
					callback.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class), th);
				} catch (Exception e) {
					callback.onFail(statusCode, null, e);
				}
				break;

		}
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

	public interface ResponseHandler {
		void onSuccess(int statusCode, String result);
		void onFail(int statusCode, NetHttpResult result, Throwable e);
	}

	public NetHttpTask(NetHttpTask backup) {
		waitString = backup.waitString;
		url = backup.url;
		params = backup.params;
		sslSocketFactory = backup.sslSocketFactory;
		synchronousMode = true;
		activityForProgress = null;
	}

	private NetHttpTask(String url, HashMap<String, String> params, boolean syncronous, Activity activityForProgress, String waitString, SSLSocketFactory sslSocketFactory) {
		this.waitString = waitString;
		this.url = url;
		this.params = params;
		this.sslSocketFactory = sslSocketFactory;

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

		SSLSocketFactory sslSocketFactory;
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

		public Builder setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
			this.sslSocketFactory = sslSocketFactory;
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
			return new NetHttpTask(url, params, synchronousMode, activityForProgress, waitString, sslSocketFactory);
		}
	}
}