package com.hovans.android.util;

/**
 * StringUtils.java
 *
 * @author Hovan Yoo
 */
public class StringUtils {
	public static boolean isEmpty(CharSequence string) {
		return string == null || string.equals("");
	}
}
