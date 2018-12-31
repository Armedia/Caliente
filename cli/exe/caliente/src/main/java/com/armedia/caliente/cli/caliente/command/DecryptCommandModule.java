package com.armedia.caliente.cli.caliente.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.Launcher;
import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.Tools;

public class DecryptCommandModule extends CommandModule<TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> {

	public DecryptCommandModule(TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?> engine) {
		super(CalienteCommand.DECRYPT, engine);
	}

	private final Collection<CmfCrypt> getCryptoOptions() {
		Collection<CmfCrypt> crypt = new ArrayList<>(2);
		CmfCrypt c = getCrypto();
		if (c != null) {
			crypt.add(c);
		}
		crypt.add(Launcher.CRYPTO);
		return Tools.freezeCollection(crypt);
	}

	protected final String decrypt(Collection<CmfCrypt> crypt, String password) throws CalienteException {
		List<Exception> exceptions = new ArrayList<>();
		for (CmfCrypt c : crypt) {
			try {
				return c.decrypt(password);
			} catch (Exception e) {
				// Ignore this one, try the next one...
				exceptions.add(e);
			}
		}

		CalienteException thrown = new CalienteException(String
			.format("Failed to decrypt the encrypted password [%s] with all available cryptography options", password));
		for (Exception e : exceptions) {
			thrown.addSuppressed(e);
		}
		throw thrown;
	}

	@Override
	protected int execute(CalienteState state, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		final Collection<CmfCrypt> crypt = getCryptoOptions();
		if (!positionals.isEmpty()) {
			for (String password : positionals) {
				try {
					System.out.printf("[%s]==[%s]%n", password, decrypt(crypt, password));
				} catch (Exception e) {
					System.err.printf("Failed to decrypt the encrypted password [%s]%n%s%n", password,
						Tools.dumpStackTrace(e));
					for (Throwable t : e.getSuppressed()) {
						System.err.printf("Suppressed Exception: %s%n", Tools.dumpStackTrace(t));
					}
				}
			}
		} else {
			// No positionals, so read from console
			String password = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				try {
					password = br.readLine();
				} catch (IOException e) {
					throw new CalienteException("IOException caught reading the password", e);
				}
				if (password == null) {
					break;
				}
				try {
					System.out.printf("[%s]==[%s]%n", password, decrypt(crypt, password));
				} catch (Exception e) {
					System.err.printf("Failed to decrypt the encrypted password [%s]%n%s%n", password,
						Tools.dumpStackTrace(e));
					for (Throwable t : e.getSuppressed()) {
						System.err.printf("Suppressed Exception: %s%n", Tools.dumpStackTrace(t));
					}
				}
			}
		}
		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}