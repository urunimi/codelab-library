package com.hovans.android.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * StringUtils.java
 *
 * @author Hovan Yoo
 */
public class StringUtils {
	public static boolean isEmpty(CharSequence string) {
		return string == null || string.equals("");
	}

	public static byte[] makeMD5(String param) {
		byte[] digest = null;
		try {
			digest = MessageDigest.getInstance("MD5").digest(
					param.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return digest;
	}

	public static String makeMD5Integer(String param) {
		byte[] bytes = makeMD5(param);
		StringBuffer hexPass = new StringBuffer();
        for (int i=0; i<bytes.length; i++) {
                String h = Integer.toHexString(0xFF & bytes[i]);
                while (h.length()<2) h = "0" + h;
                        hexPass.append(h);
        }
        return hexPass.toString();
	}
}
