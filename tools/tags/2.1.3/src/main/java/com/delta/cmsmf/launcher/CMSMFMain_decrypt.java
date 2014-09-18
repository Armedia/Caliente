package com.delta.cmsmf.launcher;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.common.DfException;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_decrypt implements CMSMFMain {

	@Override
	public void run() throws CMSMFException {
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
			System.out.printf("%s%s%s%n", (console != null ? "The decrypted password is: [" : ""),
				RegistryPasswordUtils.decrypt(password), (console != null ? "]" : ""));
		} catch (DfException e) {
			throw new CMSMFException("Failed to decrypt the password", e);
		}
	}

	@Override
	public boolean requiresDataStore() {
		return false;
	}

	@Override
	public boolean requiresCleanData() {
		return false;
	}
}