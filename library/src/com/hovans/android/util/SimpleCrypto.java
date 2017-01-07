package com.hovans.android.util;

import android.util.Base64;
import com.hovans.android.log.LogByCodeLab;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Formatter;

/**
 * Usage:
 * <pre>
 * String crypto = SimpleCrypto.encrypt(masterpassword, cleartext)
 * ...
 * String cleartext = SimpleCrypto.decrypt(masterpassword, crypto)
 * </pre>
 *
 * @author Ben Yoo
 */
public class SimpleCrypto {
	static final String TRANSFORMATION = "AES";
	static final String ENCODING = "UTF8";

	public static synchronized String encrypt(String seed, String cleartext, String transformation) throws Exception {
		if (StringUtils.isEmpty(transformation)) {
			transformation = TRANSFORMATION;
		}
		Cipher cipher = Cipher.getInstance(transformation); // cipher is not thread safe
		cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(seed, transformation));
		return Base64.encodeToString(cipher.doFinal(cleartext.getBytes(ENCODING)), Base64.DEFAULT);
	}

	public static synchronized String decrypt(String seed, String encrypted, String transformation) throws Exception {
		if (StringUtils.isEmpty(transformation)) {
			transformation = TRANSFORMATION;
		}
		Cipher cipher = Cipher.getInstance(transformation); // cipher is not thread safe
		cipher.init(Cipher.DECRYPT_MODE, getKeySpec(seed, transformation));
		return new String(cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT)));
	}

	static SecretKey getKeySpec(String seed, String transformation) throws Exception {
		if (transformation.equals("DES")) {
			DESKeySpec keySpec = new DESKeySpec(seed.getBytes(ENCODING));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(transformation);
			return keyFactory.generateSecret(keySpec);
		} else {
//		} else if (transformation.equals("AES")){
			SecretKeySpec keySpec = new SecretKeySpec(seed.getBytes(ENCODING), transformation);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(transformation);
			return keyFactory.generateSecret(keySpec);

//			return new SecretKeySpec(seed.getBytes(ENCODING), transformation);
		}
	}

	public static String encryptString(String string) {
		String sha1 = "";
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			crypt.reset();
			crypt.update(string.getBytes("UTF-8"));
			sha1 = byteToHex(crypt.digest());
		} catch(Throwable e) {
			LogByCodeLab.e(e);
		}
		return sha1;
	}

	static String byteToHex(final byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}
}