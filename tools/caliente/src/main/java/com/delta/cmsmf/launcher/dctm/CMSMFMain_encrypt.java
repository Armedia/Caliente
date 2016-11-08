package com.delta.cmsmf.launcher.dctm;

import com.armedia.caliente.engine.documentum.DctmCrypto;
import com.delta.cmsmf.launcher.AbstractEncrypt;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_encrypt extends AbstractEncrypt {
	public CMSMFMain_encrypt() {
		super(new DctmCrypto());
	}
}