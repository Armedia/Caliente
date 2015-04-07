package com.delta.cmsmf.launcher.dctm;

import com.delta.cmsmf.launcher.AbstractDecrypt;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_decrypt extends AbstractDecrypt {

	@Override
	protected String decrypt(String password) throws Exception {
		// We do this to support old-version password encryption for old configurations/scripts
		// that may be out there, and avoid forcing cumbersome re-encryption.
		try {
			return super.decrypt(password);
		} catch (Exception e) {
			return RegistryPasswordUtils.decrypt(password);
		}
	}
}