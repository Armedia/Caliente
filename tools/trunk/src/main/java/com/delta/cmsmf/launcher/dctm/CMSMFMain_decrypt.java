package com.delta.cmsmf.launcher.dctm;

import com.armedia.cmf.engine.documentum.DctmCrypto;
import com.delta.cmsmf.launcher.AbstractDecrypt;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_decrypt extends AbstractDecrypt {
	public CMSMFMain_decrypt() {
		super(new DctmCrypto());
	}
}