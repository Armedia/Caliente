package com.armedia.cmf.engine;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public abstract class Crypt {

	// This should never change... EVER...
	private static final Pattern BASE64_VALIDATOR = Pattern.compile("^[a-zA-Z0-9+/](?:[AEIMQUYcgkosw048]=|[AQgw]==)?$");
	private static final byte[] NO_BYTES = new byte[0];
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

	public static byte[] decodeBase64(String value) throws CryptException {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to decode"); }
		value = value.trim();
		if (value.length() == 0) { return Crypt.NO_BYTES; }
		if ((value.length() % 4) != 0) { throw new CryptException(String.format(
			"Bad Base64 value - its length should be a multiple of 4, but it's %d", value.length())); }
		Matcher m = Crypt.BASE64_VALIDATOR.matcher(value);
		if (!m.matches()) { throw new CryptException(String.format(
			"Bad Base64 value - doesn't match the required syntax of <%s>", Crypt.BASE64_VALIDATOR.pattern())); }
		return DatatypeConverter.parseBase64Binary(value);
	}

	public static String decrypt(String value) throws CryptException {
		final byte[] data;
		try {
			data = Crypt.getCipher(false).doFinal(Crypt.decodeBase64(value));
		} catch (Exception e) {
			throw new CryptException("Failed to decrypt the given value", e);
		}
		return new String(data, Crypt.CHARSET);
	}
}