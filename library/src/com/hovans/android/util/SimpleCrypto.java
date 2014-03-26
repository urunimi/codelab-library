package com.hovans.android.util;

import java.security.MessageDigest;
import java.util.Formatter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import android.util.Base64;

import com.hovans.android.log.LogByCodeLab;

/**
 * Usage:
 * <pre>
 * String crypto = SimpleCrypto.encrypt(masterpassword, cleartext)
 * ...
 * String cleartext = SimpleCrypto.decrypt(masterpassword, crypto)
 * </pre>
 * 
 * @author Hovan
 */
public class SimpleCrypto {
	static final String TRANSFORMATION = "DES";
	static final String ENCODING = "UTF8";

	public static synchronized String encrypt(String seed, String cleartext) throws Exception {
		Cipher cipher = Cipher.getInstance(TRANSFORMATION); // cipher is not thread safe
		cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(seed));
		return Base64.encodeToString(cipher.doFinal(cleartext.getBytes(ENCODING)), Base64.DEFAULT);
	}
	
	public static synchronized String decrypt(String seed, String encrypted) throws Exception {
		Cipher cipher = Cipher.getInstance(TRANSFORMATION); // cipher is not thread safe
		cipher.init(Cipher.DECRYPT_MODE, getKeySpec(seed));
		return new String(cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT)));
	}

	static SecretKey getKeySpec(String seed) throws Exception {
		DESKeySpec keySpec = new DESKeySpec(seed.getBytes(ENCODING));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(TRANSFORMATION);
		return keyFactory.generateSecret(keySpec);
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