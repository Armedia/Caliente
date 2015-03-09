package com.delta.cmsmf.launcher.shpt;

import com.armedia.cmf.engine.sharepoint.ShptEncrypterTool;
import com.delta.cmsmf.launcher.AbstractEncrypt;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_encrypt extends AbstractEncrypt {

	@Override
	protected String encrypt(String password) throws Exception {
		return ShptEncrypterTool.encrypt(password);
	}
}