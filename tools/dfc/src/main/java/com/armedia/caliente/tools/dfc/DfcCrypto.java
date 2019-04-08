package com.armedia.caliente.tools.dfc;

import com.armedia.caliente.tools.CmfCrypt;
import com.documentum.fc.impl.util.RegistryPasswordUtils;

public class DfcCrypto extends CmfCrypt {

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

	public DfcCrypto() {
		super(DfcCrypto.DFC_SCHEME, CmfCrypt.DEFAULT_SCHEME);
	}
}