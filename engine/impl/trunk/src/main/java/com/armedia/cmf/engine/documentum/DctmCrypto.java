package com.armedia.cmf.engine.documentum;

import com.armedia.cmf.engine.CMFCrypto;
import com.documentum.fc.impl.util.RegistryPasswordUtils;

public class DctmCrypto extends CMFCrypto {

	public DctmCrypto() {
		super(1);
	}

	@Override
	protected String decryptPassword(int algorithm, String password) throws Exception {
		if (algorithm != 0) { throw new IllegalArgumentException(
			"Only a single supplementary algorithm is supported in this instance"); }
		return RegistryPasswordUtils.decrypt(password);
	}

	@Override
	protected String doEncryptPassword(String password) throws Exception {
		return RegistryPasswordUtils.encrypt(password);
	}
}