package com.hovans.network;

/**
 * NetResult.java
 *
 * Created by Hovan on 1/23/15.
 */
public class NetHttpResult {
	int code;
	String message;
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
}
