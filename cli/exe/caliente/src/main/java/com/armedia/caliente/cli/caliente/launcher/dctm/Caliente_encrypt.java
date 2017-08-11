package com.armedia.caliente.cli.caliente.launcher.dctm;

import com.armedia.caliente.cli.caliente.launcher.AbstractEncrypt;
import com.armedia.caliente.tools.dfc.DctmCrypto;

/**
 * The main method of this class is an entry point for the Caliente application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class Caliente_encrypt extends AbstractEncrypt {
	public Caliente_encrypt() {
		super(new DctmCrypto());
	}
}