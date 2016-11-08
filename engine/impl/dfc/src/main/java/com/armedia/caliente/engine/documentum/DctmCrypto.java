package com.armedia.caliente.engine.documentum;

import com.armedia.caliente.engine.CmfCrypt;
import com.documentum.fc.impl.util.RegistryPasswordUtils;

public class DctmCrypto extends CmfCrypt {

	private static final Scheme DFC_SCHEME = new Scheme() {

		@Override
		public String decryptValue(String password) throws Exception {
			return RegistryPasswordUtils.decrypt(password);
		}

		@Override
		public String encryptValue(String password) throws Exception {
			return RegistryPasswordUtils.encrypt(password);
		}

		@Override
		public String getDescription() {
			return "DFC-RegistryPasswordUtils";
		}

	};

	public DctmCrypto() {
		super(DctmCrypto.DFC_SCHEME, CmfCrypt.DEFAULT_SCHEME);
	}
}