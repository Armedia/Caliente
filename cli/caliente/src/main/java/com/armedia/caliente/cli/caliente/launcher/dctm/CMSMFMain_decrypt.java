package com.armedia.caliente.cli.caliente.launcher.dctm;

import com.armedia.caliente.cli.caliente.launcher.AbstractDecrypt;
import com.armedia.caliente.engine.documentum.DctmCrypto;

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