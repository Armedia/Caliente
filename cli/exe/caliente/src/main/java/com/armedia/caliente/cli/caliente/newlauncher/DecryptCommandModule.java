package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.Tools;

public class DecryptCommandModule extends CommandModule {

	private static final Descriptor DESCRIPTOR = new Descriptor("decrypt", "Decrypt an encrypted password", "dec");

	public DecryptCommandModule() {
		super(null, false, false, DecryptCommandModule.DESCRIPTOR);
	}

	private final Collection<CmfCrypt> getCrypt(EngineProxy engineProxy) {
		Collection<CmfCrypt> crypt = new ArrayList<>(2);
		CmfCrypt c = engineProxy.getCrypt();
		if (c != null) {
			crypt.add(c);
		}
		crypt.add(Caliente.CRYPTO);
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
	protected int execute(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		final Collection<CmfCrypt> crypt = getCrypt(engineProxy);
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
			try {
				password = br.readLine();
			} catch (IOException e) {
				throw new CalienteException("IOException caught cryptoreading the password", e);
			}
			try {
				System.out.printf("[%s]==[%s]%n", password, decrypt(crypt, password));
			} catch (Exception e) {
				System.err.printf("Failed to decrypt the encrypted password [%s]%n%s%n", password,
					Tools.dumpStackTrace(e));
			}
		}
		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}