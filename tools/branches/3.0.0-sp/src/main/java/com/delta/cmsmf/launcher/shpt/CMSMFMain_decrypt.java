package com.delta.cmsmf.launcher.shpt;

import com.armedia.cmf.engine.sharepoint.ShptEncrypterTool;
import com.delta.cmsmf.launcher.AbstractDecrypt;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_decrypt extends AbstractDecrypt {

	@Override
	protected String decrypt(String password) throws Exception {
		return ShptEncrypterTool.decrypt(password);
	}
}