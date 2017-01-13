package com.hovans.network;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.Expose;

/**
 * DefaultHttpResponse.java
 * <p>
 * Created by Ben on 1/23/15.
 */
public class DefaultHttpResponse implements Parcelable {

	public static final int RES_SUCCESS = 0;
	public static final int RES_FAIL = 2;
	public static final int RES_TIMEOUT = 9999;

	@Expose
	int code;
	@Expose
	String message;

	@Expose
	String result;

	public String getResult() {
		return result;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "code: " + code + ", message: " + message + ", result: " + result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(code);
		dest.writeString(message);
		dest.writeString(result.toString());
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public DefaultHttpResponse createFromParcel(Parcel source) {
			DefaultHttpResponse response = new DefaultHttpResponse();
			response.code = source.readInt();
			response.message = source.readString();
			response.result = source.readString();
			return response;
		}

		public DefaultHttpResponse[] newArray(int size) {
			return new DefaultHttpResponse[size];
		}
	};
}
