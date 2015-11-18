package com.delta.cmsmf.launcher;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import com.armedia.cmf.engine.Crypt;
import com.armedia.cmf.storage.CmfObjectStore;
import com.delta.cmsmf.exception.CMSMFException;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class AbstractEncrypt implements CMSMFMain {

	@Override
	public CmfObjectStore<?, ?> getObjectStore() {
		return null;
	}

	@Override
	public final void run() throws CMSMFException {
		final Console console = System.console();
		String password = null;
		if (console != null) {
			char[] pass = console
				.readPassword("Enter the password that you would like to encrypt (it will not be shown): ");
			password = new String(pass);
		} else {
			// Don't output a prompt
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				password = br.readLine();
			} catch (IOException e) {
				throw new CMSMFException("IOException caught reading the password", e);
			}
		}
		try {
			System.out.printf("%s%s%n", (console != null ? "The encrypted password is: " : ""), encrypt(password));
		} catch (Exception e) {
			throw new CMSMFException("Failed to decrypt the password", e);
		}
	}

	protected String encrypt(String password) throws Exception {
		return Crypt.encrypt(password);
	}
}