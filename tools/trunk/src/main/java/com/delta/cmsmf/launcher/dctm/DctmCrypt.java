package com.delta.cmsmf.launcher.dctm;

import com.armedia.cmf.engine.Crypt;
import com.armedia.cmf.engine.CryptException;
import com.documentum.fc.common.DfException;
import com.documentum.fc.tools.RegistryPasswordUtils;

class DctmCrypt {

	private static final String NULL_ENC;
	private static final String NULL_DEC;

	static {
		try {
			NULL_ENC = Crypt.encrypt("");
			NULL_DEC = Crypt.decrypt(DctmCrypt.NULL_ENC);
		} catch (CryptException e) {
			throw new RuntimeException("Failed to initialize base encryption values");
		}
	}

	static String encrypt(String str) {
		if (str == null) { return DctmCrypt.NULL_ENC; }
		try {
			return Crypt.encrypt(str);
		} catch (CryptException e) {
			// Can't encrypt?!? How?!?!
			return str;
		}
	}

	static String decrypt(String str) {
		if (str == null) { return DctmCrypt.NULL_DEC; }
		try {
			return Crypt.decrypt(str);
		} catch (CryptException e) {
			try {
				return RegistryPasswordUtils.decrypt(str);
			} catch (DfException e2) {
				return str;
			}
		}
	}
}