package com.delta.cmsmf.launcher.dctm;

import com.delta.cmsmf.launcher.AbstractEncrypt;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_encrypt extends AbstractEncrypt {

	@Override
	protected String encrypt(String password) throws Exception {
		return RegistryPasswordUtils.encrypt(password);
	}
}