/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.caliente.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.Entrypoint;
import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.OptionValues;

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
		crypt.add(Entrypoint.CRYPTO);
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
		exceptions.stream().forEach(thrown::addSuppressed);
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