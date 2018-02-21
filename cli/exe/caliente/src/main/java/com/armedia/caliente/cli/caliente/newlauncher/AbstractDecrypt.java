package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;

public abstract class AbstractDecrypt implements CalienteMain {

	protected final CmfCrypt crypto;

	protected AbstractDecrypt() {
		this(null);
	}

	protected AbstractDecrypt(CmfCrypt crypto) {
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
			System.out.printf("Enter the password that you would like to decrypt: ");
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			password = br.readLine();
		} catch (IOException e) {
			throw new CalienteException("IOException caught reading the password", e);
		}
		try {
			System.out.printf("%s%s%s%n", (console != null ? "The decrypted password is: [" : ""), decrypt(password),
				(console != null ? "]" : ""));
		} catch (Exception e) {
			throw new CalienteException("Failed to decrypt the password", e);
		}
	}

	protected final String decrypt(String password) {
		return this.crypto.decrypt(password);
	}
}