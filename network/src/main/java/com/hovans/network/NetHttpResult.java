package com.hovans.network;

import com.google.gson.annotations.Expose;

/**
 * NetResult.java
 *
 * Created by Hovan on 1/23/15.
 */
public class NetHttpResult {
	@Expose
	int code;
	@Expose
	String message, result;

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
}
