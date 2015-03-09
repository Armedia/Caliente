package com.armedia.cmf.engine;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public abstract class Crypt {

	// This should never change... EVER...
	private static final String ENCODED_KEY = "6RBjZgfVO+KhuPU0qSqmdQ==";
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final SecretKey KEY;

	static {
		final byte[] key = DatatypeConverter.parseBase64Binary(Crypt.ENCODED_KEY);
		KEY = new SecretKeySpec(key, "AES");
		try {
			Crypt.getCipher(false);
			Crypt.getCipher(true);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No Such Algorithm", e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("Invalid Key", e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException("No Such Padding", e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Invalid Algorithm Parameter", e);
		}
	}

	private Crypt() {
	}

	private static Cipher getCipher(boolean encrypt) throws NoSuchAlgorithmException, NoSuchPaddingException,
		InvalidKeyException, InvalidAlgorithmParameterException {
		Cipher ret = Cipher.getInstance(Crypt.KEY.getAlgorithm());
		ret.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, Crypt.KEY);
		return ret;
	}

	public static String encrypt(String value) throws CryptException {
		final byte[] data;
		try {
			data = Crypt.getCipher(true).doFinal(value.getBytes(Crypt.CHARSET));
		} catch (Exception e) {
			throw new CryptException("Failed to encrypt the given value", e);
		}
		return DatatypeConverter.printBase64Binary(data);
	}

	public static String decrypt(String value) throws CryptException {
		final byte[] data;
		try {
			data = Crypt.getCipher(false).doFinal(DatatypeConverter.parseBase64Binary(value));
		} catch (Exception e) {
			throw new CryptException("Failed to decrypt the given value", e);
		}
		return new String(data, Crypt.CHARSET);
	}
}