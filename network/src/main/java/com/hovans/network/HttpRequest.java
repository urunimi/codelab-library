package com.hovans.network;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * HttpRequest.java
 *
 * @author Ben Yoo
 */
public class HttpRequest {

	static final String TAG = HttpRequest.class.getSimpleName();

	static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'ZZZ").create();

	static final int REQUEST_TIMEOUT = 10, RESPONSE_OK = 200, TIMEOUT = 10000;

	@Expose
	String url;
	String waitString;
	@Expose
	HashMap<String, String> params = new HashMap<>();

	protected static RequestQueue queue;

	boolean synchronousMode, useCache, alreadySent;
	String cachedResult;

	Activity activityForProgress;
	//	final SSLSocketFactory sslSocketFactory;
	ProgressDialog progressDialog;
	NetResponseHandler callbackNetResponse;
	StringResponseHandler callbackString;
	ResponseHandler callbackObject;
	Handler handler;

	Class type;

	public <T> void post(Class<T> classOfT, final ResponseHandler<T> callback) {
		type = classOfT;
		this.callbackObject = callback;
		request(StringRequest.Method.POST);
	}

	public void post(final NetResponseHandler callbackNetResponse) {
		this.callbackNetResponse = callbackNetResponse;
		request(StringRequest.Method.POST);
	}

	public void post(final StringResponseHandler callback) {
		this.callbackString = callback;
		request(StringRequest.Method.POST);
	}

	public <T> void get(Class<T> classOfT, final ResponseHandler<T> callback) {
		type = classOfT;
		this.callbackObject = callback;
		request(StringRequest.Method.GET);
	}

	public void get(final NetResponseHandler callbackNetResponse) {
		this.callbackNetResponse = callbackNetResponse;
		request(StringRequest.Method.GET);
	}

	public void get(final StringResponseHandler callback) {
		this.callbackString = callback;
		request(StringRequest.Method.GET);
	}

	public <T> void put(Class<T> classOfT, final ResponseHandler<T> callback) {
		type = classOfT;
		this.callbackObject = callback;
		request(StringRequest.Method.PUT);
	}

	public void put(final NetResponseHandler callbackNetResponse) {
		this.callbackNetResponse = callbackNetResponse;
		request(StringRequest.Method.PUT);
	}

	public void put(final StringResponseHandler callback) {
		this.callbackString = callback;
		request(StringRequest.Method.PUT);
	}

	public <T> void delete(Class<T> classOfT, final ResponseHandler<T> callback) {
		type = classOfT;
		this.callbackObject = callback;
		request(StringRequest.Method.DELETE);
	}

	public void delete(final NetResponseHandler callbackNetResponse) {
		this.callbackNetResponse = callbackNetResponse;
		request(StringRequest.Method.DELETE);
	}

	public void delete(final StringResponseHandler callback) {
		this.callbackString = callback;
		request(StringRequest.Method.DELETE);
	}

	private void request(int method) {
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

		if (useCache && getPreferences().contains(url.toString())) {
			cachedResult = getPreferences().getString(url.toString(), null);
			if (cachedResult != null) {
				handleResponse(200, cachedResult, null);
			}
		}

		try {
			if (synchronousMode == false && Looper.myLooper() != null) {
				queue.add(getRequest(method, url, stringListener, errorListener));
				handler = new Handler();
			} else {
				RequestFuture<String> future = RequestFuture.newFuture();
				queue.add(getRequest(method, url, future, errorListener));
				String result = future.get(REQUEST_TIMEOUT, TimeUnit.SECONDS);
				stringListener.onResponse(result);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException | UnsupportedEncodingException e) {
			Log.w(TAG, e);
			errorListener.onErrorResponse(new VolleyError(e));
		}
	}

	StringRequest getRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) throws UnsupportedEncodingException {
		StringRequest stringRequest;
		switch (method) {
			case StringRequest.Method.GET:
				stringRequest = new StringRequest(method, url + "?" + getQuery(HttpRequest.this.getParams()), listener, errorListener);
				break;
			default:
				stringRequest = new StringRequest(method, url, listener, errorListener) {
					@Override
					protected Map<String, String> getParams() throws AuthFailureError {
						return HttpRequest.this.getParams();
					}
				};
				break;
		}

		stringRequest.setRetryPolicy(new DefaultRetryPolicy(
				TIMEOUT,
				DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		return stringRequest;
	}

	SharedPreferences getPreferences() {
		return context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
	}

	protected String getSuccessKey() {
		return "code";
	}

	protected Map<String, String> getParams() {
		return params;
	}

	Response.Listener<String> stringListener = new Response.Listener<String>() {
		@Override
		public void onResponse(final String response) {
			if (handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						handleResponse(RESPONSE_OK, response, null);
					}
				});
			} else {
				handleResponse(RESPONSE_OK, response, null);
			}
		}
	};

	Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			int statusCode = -1;
			if (error.networkResponse != null) {
				statusCode = error.networkResponse.statusCode;
			}
			handleResponse(statusCode, null, error.getCause());
		}
	};

	void handleResponse(int statusCode, String responseString, Throwable e) {
		closeDialogIfItNeeds();
		switch(statusCode) {
			case RESPONSE_OK:
				try {
					JSONObject jsonObject = new JSONObject(responseString);

					final String successKey = getSuccessKey();

					if (jsonObject.has(successKey) && jsonObject.getInt(successKey) != 0) {
						handleFailResponse(statusCode, gson.fromJson(responseString, DefaultHttpResponse.class), e);
					} else {
						String resultString;
						if(jsonObject.has("result")) {
							resultString = jsonObject.getString("result");
						} else {
							resultString = responseString;
						}

						if (alreadySent && useCache) {
							getPreferences().edit().putString(url, responseString).apply();
						}
						if (cachedResult == null || cachedResult.equals(responseString) == false) {
							handleSuccessResponse(statusCode, responseString, resultString);
						}
					}
				} catch (Exception ex) {
					handleFailResponse(statusCode, null, ex);
				}

				break;
			default:
				try {
					handleFailResponse(statusCode, gson.fromJson(responseString, DefaultHttpResponse.class), e);
				} catch (Exception ex) {
					handleFailResponse(statusCode, null, ex);
				}
				break;
		}
		alreadySent = true;
	}

	protected void handleSuccessResponse(int statusCode, String responseString, String resultString) {
		if(callbackString != null) {
			callbackString.onSuccess(statusCode, resultString);
		} else if(callbackObject != null) {
			Object resultObject = gson.fromJson(resultString, type);
			callbackObject.onSuccess(statusCode, resultObject, resultString);
		} else if (callbackNetResponse != null) {
			callbackNetResponse.onResponse(statusCode, gson.fromJson(responseString, DefaultHttpResponse.class));
		}
	}

	protected void handleFailResponse(int statusCode, DefaultHttpResponse httpResponse, Throwable e) {
		if(callbackString != null) {
			callbackString.onFail(statusCode, httpResponse, e);
		} else if(callbackObject != null) {
			callbackObject.onFail(statusCode, httpResponse, e);
		} else if (callbackNetResponse != null) {
			callbackNetResponse.onResponse(statusCode, httpResponse);
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

	public static String getQuery(Map<String, String> parameters) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (String key : parameters.keySet()) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(key, "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(parameters.get(key), "UTF-8"));
		}

		return result.toString();
	}

	public interface NetResponseHandler {
		void onResponse(int statusCode, DefaultHttpResponse response);
	}

	public interface StringResponseHandler {
		void onSuccess(int statusCode, String result);

		void onFail(int statusCode, DefaultHttpResponse response, Throwable e);
	}

	public interface ResponseHandler<T> {
		void onSuccess(int statusCode, T result, String resultString);

		void onFail(int statusCode, DefaultHttpResponse response, Throwable e);
	}

	final Context context;

	protected HttpRequest(Context context) {
		this.context = context;
	}

	public static class Builder {
		protected HttpRequest httpTask;

		public Builder(Context context) {
			httpTask = new HttpRequest(context);

			if (queue == null) {
				queue = Volley.newRequestQueue(context);
				queue.start();
			}
		}

//		final String URL_BASE = "http://autoguard.hovans.com";

		public Builder setParams(HashMap<String, String> params) {
			httpTask.params = params;
			return this;
		}

		public Builder addParam(String key, Object value) {
			httpTask.params.put(key, String.valueOf(value));
			return this;
		}

		public Builder setUrl(String url) {
			httpTask.url = url;
			return this;
		}

		public Builder addPath(String path) {
			httpTask.url += path;
			return this;
		}

		public Builder setUseCache(boolean useCache) {
			httpTask.useCache = useCache;
			return this;
		}

		public Builder showProgress(Activity activity, String waitString) {
			httpTask.activityForProgress = activity;
			httpTask.waitString = waitString;
			return this;
		}

		public Builder setSyncMode(boolean synchronousMode) {
			if (Looper.myLooper() == null) {
				httpTask.synchronousMode = true;
			} else {
				httpTask.synchronousMode = synchronousMode;
			}
			return this;
		}

		public HttpRequest build() {
			return httpTask;
		}
	}
}