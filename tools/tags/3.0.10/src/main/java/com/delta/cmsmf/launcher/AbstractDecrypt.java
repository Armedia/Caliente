package com.delta.cmsmf.launcher;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import com.armedia.cmf.engine.Crypt;
import com.delta.cmsmf.exception.CMSMFException;

public abstract class AbstractDecrypt implements CMSMFMain {

	@Override
	public final void run() throws CMSMFException {
		final Console console = System.console();
		String password = null;
		if (console != null) {
			System.out.printf("Enter the password that you would like to decrypt: ");
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			password = br.readLine();
		} catch (IOException e) {
			throw new CMSMFException("IOException caught reading the password", e);
		}
		try {
			System.out.printf("%s%s%s%n", (console != null ? "The decrypted password is: [" : ""), decrypt(password),
				(console != null ? "]" : ""));
		} catch (Exception e) {
			throw new CMSMFException("Failed to decrypt the password", e);
		}
	}

	protected String decrypt(String password) throws Exception {
		return Crypt.decrypt(password);
	}

	@Override
	public final boolean requiresDataStore() {
		return false;
	}

	@Override
	public final boolean requiresCleanData() {
		return false;
	}
}