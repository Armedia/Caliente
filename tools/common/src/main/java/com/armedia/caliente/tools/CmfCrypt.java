/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.CheckedCodec;

public class CmfCrypt implements CheckedCodec<CharSequence, byte[], Exception> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	// This should never change... EVER...
	private static final Pattern BASE64_VALIDATOR = Pattern
		.compile("^[a-zA-Z0-9+/]+(?:[AEIMQUYcgkosw048]=|[AQgw]==)?$");
	private static final byte[] NO_BYTES = new byte[0];
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final String ENCODED_KEY = "6RBjZgfVO+KhuPU0qSqmdQ==";
	private static final SecretKey KEY;
	private static final String CIPHER_ALGORITHM = "AES";
	private static final String CIPHER_SPEC = String.format("%s/ECB/PKCS5Padding", CmfCrypt.CIPHER_ALGORITHM);

	protected static final Collection<Scheme> NO_SCHEMES = Collections.emptyList();

	static {
		try {
			KEY = new SecretKeySpec( //
				CmfCrypt.decodeBase64(CmfCrypt.ENCODED_KEY), //
				CmfCrypt.CIPHER_ALGORITHM //
			);
		} catch (CryptException e) {
			throw new RuntimeException("Failed to initialize the simple encryption engine", e);
		}
	}

	public static byte[] normalizeKey(byte[] key) {
		return CmfCrypt.normalizeKey(key, true);
	}

	public static byte[] normalizeKey(byte[] key, boolean strengthen) {
		if (key == null) {
			// Defend against null keys
			key = CmfCrypt.NO_BYTES;
		}

		if (key.length != 32) {
			byte[] newKey = new byte[32];
			if ((newKey.length > key.length) && strengthen) {
				// Fill in each byte with its offset, to make the key stronger. We don't use
				// random values b/c we want the result to be consistently reproducible
				for (int i = 0; i < newKey.length; i++) {
					newKey[i] = (byte) i;
				}
			}
			// We copy up to the first 32 bytes from the original key into the new key
			System.arraycopy(key, 0, newKey, 0, Math.min(key.length, newKey.length));

			// Swap the references
			key = newKey;
		}

		// Return the result
		return key;
	}

	public static byte[] decodeBase64(String value) throws CryptException {
		if (StringUtils.isEmpty(value)) { return CmfCrypt.NO_BYTES; }
		if ((value.length() % 4) != 0) {
			throw new CryptException(
				String.format("Bad Base64 value - its length should be a multiple of 4, but it's %,d", value.length()));
		}
		Matcher m = CmfCrypt.BASE64_VALIDATOR.matcher(value);
		if (!m.matches()) {
			throw new CryptException(String.format("Bad Base64 value - doesn't match the required syntax of <%s>",
				CmfCrypt.BASE64_VALIDATOR.pattern()));
		}
		return Base64.decodeBase64(value);
	}

	protected static interface Scheme {
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
				key = CmfCrypt.decodeBase64(CmfCrypt.ENCODED_KEY);
			} else {
				this.key = new SecretKeySpec(CmfCrypt.normalizeKey(key), CmfCrypt.CIPHER_ALGORITHM);
			}

			// Make sure we can work our magic both ways
			getCipher(this.key, false);
			getCipher(this.key, true);

			// Store the description, for future reference
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
			try {
				return new String( //
					getCipher(this.key, false) //
						.doFinal( //
							CmfCrypt.decodeBase64( //
								// Don't allow nulls to spoil our fun
								StringUtils.isNotEmpty(value) //
									? value //
									: StringUtils.EMPTY //
							) //
						), //
					CmfCrypt.CHARSET //
				);
			} catch (Exception e) {
				throw new CryptException("Failed to decrypt the given value", e);
			}
		}

		@Override
		public String encryptValue(String value) throws Exception {
			try {
				return Base64.encodeBase64String( //
					getCipher(this.key, true) //
						.doFinal( //
							// Don't allow nulls to spoil our fun
							Tools.coalesce(value, StringUtils.EMPTY) //
								.getBytes(CmfCrypt.CHARSET) //
						) //
				);
			} catch (Exception e) {
				throw new CryptException("Failed to encrypt the given value", e);
			}
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

	public static final CmfCrypt DEFAULT = new CmfCrypt();

	private final Collection<Scheme> alternateSchemes;
	private final Scheme encryptionScheme;

	private CmfCrypt() {
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

	@Override
	public byte[] encode(CharSequence str) throws Exception {
		return Base64.decodeBase64(encrypt(Tools.toString(str)));
	}

	@Override
	public CharSequence decode(byte[] data) throws Exception {
		return decrypt( //
			((data != null) && (data.length > 0)) //
				? Base64.encodeBase64String(data) //
				: StringUtils.EMPTY //
		);
	}
}