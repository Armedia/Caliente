package com.armedia.cmf.engine;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMFCrypto {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final int algorithms;

	public CMFCrypto() {
		this(0);
	}

	protected CMFCrypto(int algorithms) {
		if (algorithms < 0) { throw new IllegalArgumentException(
			"Must provide a positive number of algorithms to use"); }
		this.algorithms = algorithms;
	}

	public final String decryptPassword(String password) {
		if (password == null) { throw new IllegalArgumentException(); }
		try {
			return Crypt.decrypt(password);
		} catch (CryptException e) {
			for (int i = 0; i < this.algorithms; i++) {
				try {
					return decryptPassword(i, password);
				} catch (Exception e2) {
					// Failed to decrypt, move on
					this.log.trace(
						String.format("Failed to encrypt the value [%s] using extra algorithm #%d", password, i + 1),
						e);
				}
			}
			return password;
		}
	}

	protected String decryptPassword(int algorithm, String password) throws Exception {
		throw new RuntimeException("This instance lacks any extra algorithms");
	}

	public final String encryptPassword(String password) {
		if (StringUtils.isEmpty(password)) {
			password = "";
		}
		try {
			return doEncryptPassword(password);
		} catch (Exception e) {
			// Can't encrypt?? HOW?!?
			this.log.trace(String.format("Failed to encrypt the value [%s]", password), e);
			return password;
		}
	}

	protected String doEncryptPassword(String password) throws Exception {
		return Crypt.encrypt(password);
	}
}