package com.armedia.caliente.cli.caliente.launcher.dctm;

import com.armedia.caliente.cli.caliente.launcher.AbstractDecrypt;
import com.armedia.caliente.tools.dfc.DctmCrypto;

/**
 * The main method of this class is an entry point for the Caliente application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class Caliente_decrypt extends AbstractDecrypt {
	public Caliente_decrypt() {
		super(new DctmCrypto());
	}
}