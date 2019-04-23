package com.armedia.caliente.tools;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public class CmfCrypt {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	// This should never change... EVER...
	private static final Pattern BASE64_VALIDATOR = Pattern
		.compile("^[a-zA-Z0-9+/]+(?:[AEIMQUYcgkosw048]=|[AQgw]==)?$");
	private static final byte[] NO_BYTES = new byte[0];
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final String ENCODED_KEY = "6RBjZgfVO+KhuPU0qSqmdQ==";
	private static final SecretKey KEY;
	private static final String CIPHER_ALGORITHM = "AES";
	private static final String CIPHER_SPEC = String.format("%s/ECB/PKCS5Padding", CmfCrypt.CIPHER_ALGORITHM);

	protected static final Collection<Scheme> NO_SCHEMES = Collections.emptyList();

	static {
		final byte[] key = DatatypeConverter.parseBase64Binary(CmfCrypt.ENCODED_KEY);
		KEY = new SecretKeySpec(key, CmfCrypt.CIPHER_ALGORITHM);
	}

	public static byte[] decodeBase64(String value) throws CryptException {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to decode"); }
		value = value.trim();
		if (value.length() == 0) { return CmfCrypt.NO_BYTES; }
		if ((value.length() % 4) != 0) {
			throw new CryptException(
				String.format("Bad Base64 value - its length should be a multiple of 4, but it's %d", value.length()));
		}
		Matcher m = CmfCrypt.BASE64_VALIDATOR.matcher(value);
		if (!m.matches()) {
			throw new CryptException(String.format("Bad Base64 value - doesn't match the required syntax of <%s>",
				CmfCrypt.BASE64_VALIDATOR.pattern()));
		}
		return DatatypeConverter.parseBase64Binary(value);
	}

	public static interface Scheme {
		public String decryptValue(String password) throws Exception;

		public String encryptValue(String password) throws Exception;

		public String getDescription();
	}

	public static class BasicAESScheme implements Scheme {

		private final SecretKey key;
		private final String description;

		public BasicAESScheme() throws Exception {
			this(null);
		}

		public BasicAESScheme(byte[] key) throws Exception {
			boolean pdk = false;
			if (key == null) {
				this.key = CmfCrypt.KEY;
				pdk = true;
				key = DatatypeConverter.parseBase64Binary(CmfCrypt.ENCODED_KEY);
			} else {
				if ((key.length != 16) && (key.length != 24) && (key.length != 32)) {
					throw new IllegalArgumentException("The key must be either 128, 192 or 256 bits");
				}
				this.key = new SecretKeySpec(key, CmfCrypt.CIPHER_ALGORITHM);
			}
			getCipher(this.key, false);
			getCipher(this.key, true);
			this.description = String.format("%s-%d-%s", CmfCrypt.CIPHER_SPEC, key.length * 8,
				(pdk ? "default" : DigestUtils.sha256Hex(key)));
		}

		protected Cipher getCipher(SecretKey key, boolean encrypt)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
			Cipher ret = Cipher.getInstance(CmfCrypt.CIPHER_SPEC);
			ret.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
			// , new IvParameterSpec(this.key.getEncoded(), 0, 16)
			return ret;
		}

		@Override
		public String decryptValue(String value) throws Exception {
			final byte[] data;
			try {
				data = getCipher(this.key, false).doFinal(CmfCrypt.decodeBase64(value));
			} catch (Exception e) {
				throw new CryptException("Failed to decrypt the given value", e);
			}
			return new String(data, CmfCrypt.CHARSET);
		}

		@Override
		public String encryptValue(String value) throws Exception {
			final byte[] data;
			try {
				data = getCipher(this.key, true).doFinal(value.getBytes(CmfCrypt.CHARSET));
			} catch (Exception e) {
				throw new CryptException("Failed to encrypt the given value", e);
			}
			return DatatypeConverter.printBase64Binary(data);
		}

		@Override
		public String getDescription() {
			return this.description;
		}
	}

	protected static final Scheme DEFAULT_SCHEME;

	static {
		try {
			DEFAULT_SCHEME = new BasicAESScheme();
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize the default encryption scheme", e);
		}
	}

	private final Collection<Scheme> alternateSchemes;
	private final Scheme encryptionScheme;

	public CmfCrypt() {
		this(CmfCrypt.DEFAULT_SCHEME, CmfCrypt.NO_SCHEMES);
	}

	public CmfCrypt(Scheme primaryScheme) {
		this(primaryScheme, CmfCrypt.NO_SCHEMES);
	}

	protected CmfCrypt(Collection<Scheme> alternateSchemes) {
		this(CmfCrypt.DEFAULT_SCHEME, alternateSchemes);
	}

	protected CmfCrypt(Scheme encryptionScheme, Scheme... schemes) {
		this(encryptionScheme,
			((schemes == null) || (schemes.length == 0) ? CmfCrypt.NO_SCHEMES : Arrays.asList(schemes)));
	}

	protected CmfCrypt(Scheme primaryScheme, Collection<Scheme> alternateSchemes) {
		if ((alternateSchemes == null) || alternateSchemes.isEmpty()) {
			this.alternateSchemes = Collections.emptyList();
		} else {
			this.alternateSchemes = Tools.freezeList(new ArrayList<>(alternateSchemes));
		}
		this.encryptionScheme = Tools.coalesce(primaryScheme, CmfCrypt.DEFAULT_SCHEME);
	}

	public final String decrypt(String value) {
		if (value == null) { throw new IllegalArgumentException(); }
		try {
			return this.encryptionScheme.decryptValue(value);
		} catch (Exception e) {
			this.log.trace("Failed to decrypt the value [{}] using the scheme [{}]", value,
				this.encryptionScheme.getDescription(), e);
			for (Scheme s : this.alternateSchemes) {
				if (s == null) {
					continue;
				}
				try {
					return s.decryptValue(value);
				} catch (Exception e2) {
					// Failed to decrypt, move on
					this.log.trace("Failed to decrypt the value [{}] using the scheme [{}]", value, s.getDescription(),
						e);
				}
			}
			return value;
		}
	}

	public final String encrypt(String value) throws Exception {
		if (StringUtils.isEmpty(value)) {
			value = "";
		}
		try {
			return this.encryptionScheme.encryptValue(value);
		} catch (Exception e) {
			// Can't encrypt?? HOW?!?
			this.log.trace("Failed to encrypt the value [{}] using scheme [{}]", value,
				this.encryptionScheme.getDescription(), e);
			for (Scheme s : this.alternateSchemes) {
				if (s == null) {
					continue;
				}
				try {
					return s.encryptValue(value);
				} catch (Exception e2) {
					// Failed to encrypt, move on
					this.log.trace("Failed to encrypt the value [{}] using the scheme [{}]", value, s.getDescription(),
						e);
				}
			}
			throw new Exception(
				String.format("Failed to encrypt the value [%s] using any of the available encryption schemes", value));
		}
	}
}