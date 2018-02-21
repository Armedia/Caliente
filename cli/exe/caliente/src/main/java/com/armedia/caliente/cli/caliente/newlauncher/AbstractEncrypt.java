package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;

/**
 * The main method of this class is an entry point for the Caliente application.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class AbstractEncrypt implements CalienteMain {

	protected final CmfCrypt crypto;

	protected AbstractEncrypt() {
		this(null);
	}

	protected AbstractEncrypt(CmfCrypt crypto) {
		if (crypto == null) {
			crypto = new CmfCrypt();
		}
		this.crypto = crypto;
	}

	@Override
	public CmfObjectStore<?, ?> getObjectStore() {
		return null;
	}

	@Override
	public final void run() throws CalienteException {
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
				throw new CalienteException("IOException caught reading the password", e);
			}
		}
		try {
			System.out.printf("%s%s%n", (console != null ? "The encrypted password is: " : ""), encrypt(password));
		} catch (Exception e) {
			throw new CalienteException("Failed to decrypt the password", e);
		}
	}

	protected final String encrypt(String password) throws Exception {
		return this.crypto.encrypt(password);
	}
}