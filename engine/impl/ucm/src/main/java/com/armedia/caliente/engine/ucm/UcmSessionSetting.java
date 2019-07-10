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
package com.armedia.caliente.engine.ucm;

import java.util.concurrent.TimeUnit;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum UcmSessionSetting implements ConfigurationSetting {
	//
	USER, //
	PASSWORD, //
	HOST, //
	PORT(4444), //
	SSL_MODE(SSLMode.NONE.name()),
	SSL_ALGORITHM, //
	TRUSTSTORE(System.getProperty("javax.net.ssl.trustStore")), //
	TRUSTSTORE_PASSWORD(System.getProperty("javax.net.ssl.trustStorePassword")), //
	KEYSTORE(System.getProperty("javax.net.ssl.keyStore")), //
	KEYSTORE_PASSWORD(System.getProperty("javax.net.ssl.keyStorePassword")), //
	CLIENT_CERT_ALIAS, //
	CLIENT_CERT_PASSWORD, //
	MIN_PING_TIME(TimeUnit.MINUTES.toSeconds(1)), //
	SOCKET_TIMEOUT, //
	//
	;

	public static enum SSLMode {
		//
		NONE, // No SSL support
		SERVER, // Only server validation
		CLIENT, // Both server and client validation
		//
		;

		public static SSLMode decode(String str) {
			if (str == null) { return NONE; }
			return SSLMode.valueOf(str.toUpperCase());
		}
	}

	private final String label;
	private final Object defaultValue;

	private UcmSessionSetting() {
		this(null);
	}

	private UcmSessionSetting(Object defaultValue) {
		this.label = name().toLowerCase().replace('_', '.');
		this.defaultValue = defaultValue;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}
}