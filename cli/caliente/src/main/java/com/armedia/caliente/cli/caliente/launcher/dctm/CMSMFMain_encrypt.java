package com.armedia.caliente.cli.caliente.launcher.dctm;

import com.armedia.caliente.cli.caliente.launcher.AbstractEncrypt;
import com.armedia.caliente.engine.documentum.DctmCrypto;

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