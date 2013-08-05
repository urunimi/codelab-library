package codelab.library.util;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

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
}