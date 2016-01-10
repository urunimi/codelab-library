package com.hovans.network;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NetHttpTask.java
 *
 * @author Hovan Yoo
 */
public class NetHttpTask {

	static final String TAG = NetHttpTask.class.getSimpleName();

	static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'ZZZ").excludeFieldsWithoutExposeAnnotation().create();

	static final int REQUEST_TIMEOUT = 30, RESPONSE_OK = 200;

	@Expose
	final String url;
	final String waitString;
	@Expose
	final HashMap<String, String> params;

	static RequestQueue queue;

	//	final Context context;
	final boolean synchronousMode;
	final Activity activityForProgress;
	//	final SSLSocketFactory sslSocketFactory;
	ProgressDialog progressDialog;
	StringResponseHandler callbackString;
	ResponseHandler callbackObject;
	Handler handler;

	Class type;

	public <T> void post(Class<T> classOfT, final ResponseHandler<T> callback) {
		type = classOfT;
		this.callbackObject = callback;

		post(null);
	}

	public void post(final StringResponseHandler callback) {
		this.callbackString = callback;

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
			StringRequest stringRequest = new StringRequest(StringRequest.Method.POST, url, stringListener, errorListener) {
				@Override
				protected Map<String, String> getParams() throws AuthFailureError {
					return params;
				}
			};

			queue.add(stringRequest);
			handler = new Handler();
		} else {
			RequestFuture<String> future = RequestFuture.newFuture();
			StringRequest request = new StringRequest(StringRequest.Method.POST, url, future, errorListener);
			queue.add(request);
			try {
				String result = future.get(REQUEST_TIMEOUT, TimeUnit.SECONDS);
				stringListener.onResponse(result);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				Log.w(TAG, e);
				errorListener.onErrorResponse(new VolleyError(e));
			}
		}

//		httpClient.post(url, requestParams, new TextHttpResponseHandler() {
//			@Override
//			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
//				closeDialogIfItNeeds();
//				try {
//					callbackString.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class));
//				} catch (Exception e) {
//					callbackString.onFail(statusCode, null);
//				}
//			}
//
//			@Override
//			public void onSuccess(int statusCode, Header[] headers, String responseString) {
//				try {
//					JSONObject jsonObject = new JSONObject(responseString);
//
//					if(jsonObject.getInt("code") != 0) {
//						callbackString.onFail(statusCode, gson.fromJson(responseString, NetHttpResult.class));
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
//					callbackString.onSuccess(statusCode, result);
//				} catch (JSONException e) {
//					Log.e(TAG, e.getMessage());
//				}
//			}
//		});
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

	public String makeBackup() {
		return gson.toJson(this);
	}

	public static NetHttpTask restoreBackup(String gsonData) {
		NetHttpTask backup = gson.fromJson(gsonData, NetHttpTask.class);
		NetHttpTask task = new NetHttpTask(backup);
		return task;
	}

	void handleResponse(int statusCode, String responseString, Throwable e) {
		closeDialogIfItNeeds();
		switch(statusCode) {
			case RESPONSE_OK:
				try {
					JSONObject jsonObject = new JSONObject(responseString);

					if(jsonObject.getInt("code") != 0) {
						handleFailResponse(statusCode, gson.fromJson(responseString, NetHttpResult.class), e);
					} else {

						String resultString = null;
						if(jsonObject.has("result")) {
							resultString = jsonObject.getString("result");
						}

						handleSuccessResponse(statusCode, resultString);
					}
				} catch (Exception ex) {
					handleFailResponse(statusCode, null, ex);
				}

				break;
			default:
				try {
					handleFailResponse(statusCode, gson.fromJson(responseString, NetHttpResult.class), e);
				} catch (Exception ex) {
					handleFailResponse(statusCode, null, ex);
				}
				break;
		}
	}

	void handleSuccessResponse(int statusCode, String resultString) {
		if(callbackString != null) {
			callbackString.onSuccess(statusCode, resultString);
		} else if(callbackObject != null) {
			Object resultObject = gson.fromJson(resultString, type);
			callbackObject.onSuccess(statusCode, resultObject, resultString);
		}
	}

	void handleFailResponse(int statusCode, NetHttpResult result, Throwable e) {
		if(callbackString != null) {
			callbackString.onFail(statusCode, result, e);
		} else if(callbackObject != null) {
			callbackObject.onFail(statusCode, result, e);
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

	public interface StringResponseHandler {
		void onSuccess(int statusCode, String result);
		void onFail(int statusCode, NetHttpResult result, Throwable e);
	}

	public interface ResponseHandler<T> {
		void onSuccess(int statusCode, T result, String resultString);
		void onFail(int statusCode, NetHttpResult result, Throwable e);
	}

	public NetHttpTask(NetHttpTask backup) {
		waitString = backup.waitString;
		url = backup.url;
		params = backup.params;
//		sslSocketFactory = backup.sslSocketFactory;
		synchronousMode = true;
		activityForProgress = null;
	}

	private NetHttpTask(Context context, String url, HashMap<String, String> params, boolean syncronous, Activity activityForProgress, String waitString, SSLSocketFactory sslSocketFactory) {
		this.waitString = waitString;
		this.url = url;
		this.params = params;
//		this.sslSocketFactory = sslSocketFactory;
		if (queue == null) {
			queue = Volley.newRequestQueue(context);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
		}

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

		Context context;

		SSLSocketFactory sslSocketFactory;
		Activity activityForProgress;
		String waitString;

		public Builder(Context context) {
			this.context = context;
		}

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
			return new NetHttpTask(context, url, params, synchronousMode, activityForProgress, waitString, sslSocketFactory);
		}
	}


//	Thread worker = new Thread() {
//		@Override
//		public void run() {
//			try {
//				HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
////				if(url.startsWith("https")) {
////					if(sslSocketFactory != null) ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
////				}
//				urlConnection.setReadTimeout(10000);
//				urlConnection.setConnectTimeout(15000);
//				urlConnection.setRequestMethod("POST");
//				urlConnection.setDoInput(true);
//				urlConnection.setDoOutput(true);
//
////				List<NameValuePair> params = new ArrayList<NameValuePair>();
//
//				StringBuilder result = new StringBuilder();
//				boolean first = true;
//
//				for (String key : params.keySet()) {
//					String value = params.get(key);
//
//					if(value == null || value.equals("")) continue;
//					if (first)
//						first = false;
//					else
//						result.append("&");
//
//					result.append(URLEncoder.encode(key, "UTF-8"));
//					result.append("=");
//					result.append(URLEncoder.encode(value, "UTF-8"));
//				}
//
//				OutputStream os = urlConnection.getOutputStream();
//				BufferedWriter writer = new BufferedWriter(
//						new OutputStreamWriter(os, "UTF-8"));
//				writer.write(result.toString());
//				writer.flush();
//				writer.close();
//				os.close();
//
//				final int statusCode = urlConnection.getResponseCode();
//
//				BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
//				String inputLine;
//				StringBuilder response = new StringBuilder();
//
//				while ((inputLine = in.readLine()) != null) {
//					response.append(inputLine);
//				}
//				in.close();
//
//				final String responseString = response.toString();
//
//
//			} catch (Exception e) {
//				handleResponse(9999, null, e);
//			}
//		}
//	};

}