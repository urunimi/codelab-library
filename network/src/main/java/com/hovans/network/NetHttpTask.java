package com.hovans.network;

import java.util.HashMap;
import java.util.Locale;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Looper;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

/**
 * NetHttpTask.java
 *
 * @author Hovan Yoo
 */
public class NetHttpTask {

	static final Gson gson = new Gson();

	final String url, waitString;
	final HashMap<String, String> params;
	final boolean synchronousMode;
	final Activity activityForProgress;
	ProgressDialog progressDialog;

	public void post(final ResponseHandler callback) {

		AsyncHttpClient httpClient = synchronousMode? new SyncHttpClient() : new AsyncHttpClient();

		RequestParams requestParams = new RequestParams();

		requestParams.put("locale", Locale.getDefault().toString());

		if(params != null) {
			for(String key : params.keySet()) {
				requestParams.put(key, params.get(key));
			}
		}

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
					e.printStackTrace();
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
