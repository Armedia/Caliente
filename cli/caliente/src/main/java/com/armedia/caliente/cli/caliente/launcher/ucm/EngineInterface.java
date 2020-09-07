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
package com.armedia.caliente.cli.caliente.launcher.ucm;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.ucm.UcmCommon;
import com.armedia.caliente.engine.ucm.UcmSessionSetting;
import com.armedia.caliente.engine.ucm.UcmSessionSetting.SSLMode;
import com.armedia.caliente.engine.ucm.UcmSetting;
import com.armedia.caliente.engine.ucm.exporter.UcmExportEngine;
import com.armedia.caliente.engine.ucm.exporter.UcmExportEngineFactory;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.launcher.LaunchClasspathHelper;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {

		String server = commandValues.getString(CLIParam.server);

		URI baseUri = URI.create(server);
		baseUri = baseUri.normalize();

		if (baseUri.isOpaque()) {
			throw new CalienteException(String.format("Bad URL format for UCM connectivity: [%s]", baseUri));
		}

		String scheme = baseUri.getScheme().toLowerCase();
		UcmSessionSetting.SSLMode sslMode = UcmSessionSetting.SSLMode.NONE;
		if ("idc".equalsIgnoreCase(scheme)) {
			sslMode = UcmSessionSetting.SSLMode.NONE;
		} else if ("idcs".equalsIgnoreCase(scheme)) {
			sslMode = UcmSessionSetting.SSLMode.SERVER;
		} else {
			throw new CalienteException(String.format("Unknown URL scheme [%s] in [%s]", scheme, baseUri));
		}

		String minPingTime = commandValues.getString(Exporter.MIN_PING_TIME);
		if (!StringUtils.isEmpty(minPingTime)) {
			try {
				settings.put(UcmSessionSetting.MIN_PING_TIME.getLabel(), Integer.valueOf(minPingTime));
			} catch (NumberFormatException e) {
				throw new CalienteException(
					String.format("Bad value for min ping time: [%s] is not a valid integer", minPingTime));
			}
		}

		String socketTimeout = commandValues.getString(Exporter.SOCKET_TIMEOUT);
		if (!StringUtils.isEmpty(socketTimeout)) {
			try {
				settings.put(UcmSessionSetting.SOCKET_TIMEOUT.getLabel(), Integer.valueOf(socketTimeout));
			} catch (NumberFormatException e) {
				throw new CalienteException(
					String.format("Bad value for socket timeout: [%s] is not a valid integer", socketTimeout));
			}
		}

		String clientCert = commandValues.getString(Exporter.CLIENT_CERT);
		if (!StringUtils.isEmpty(clientCert)) {
			sslMode = UcmSessionSetting.SSLMode.CLIENT;
			settings.put(UcmSessionSetting.CLIENT_CERT_ALIAS.getLabel(), clientCert);

			String clientCertPass = commandValues.getString(Exporter.CLIENT_CERT_PASSWORD);
			if (!StringUtils.isEmpty(clientCertPass)) {
				settings.put(UcmSessionSetting.CLIENT_CERT_PASSWORD.getLabel(), clientCertPass);
			}
		}

		if (sslMode != UcmSessionSetting.SSLMode.NONE) {
			String truststore = commandValues.getString(Exporter.TRUSTSTORE);
			if (!StringUtils.isEmpty(truststore)) {
				settings.put(UcmSessionSetting.TRUSTSTORE.getLabel(), truststore);
				String truststorePass = commandValues.getString(Exporter.TRUSTSTORE_PASSWORD);
				if (!StringUtils.isEmpty(truststorePass)) {
					settings.put(UcmSessionSetting.TRUSTSTORE_PASSWORD.getLabel(), truststorePass);
				}
			}

			String keystore = commandValues.getString(Exporter.KEYSTORE);
			if (!StringUtils.isEmpty(keystore)) {
				settings.put(UcmSessionSetting.KEYSTORE.getLabel(), keystore);
				String keystorePass = commandValues.getString(Exporter.KEYSTORE_PASSWORD);
				if (!StringUtils.isEmpty(keystorePass)) {
					settings.put(UcmSessionSetting.KEYSTORE_PASSWORD.getLabel(), keystorePass);
				}
			}
		}

		String host = baseUri.getHost();
		if (host == null) {
			throw new CalienteException(String.format("Bad URL format for UCM connectivity: [%s]", baseUri));
		}
		settings.put(UcmSessionSetting.HOST.getLabel(), host);

		int port = baseUri.getPort();
		if (port > 0) {
			settings.put(UcmSessionSetting.PORT.getLabel(), port);
		}

		if (sslMode != SSLMode.NONE) {
			settings.put(UcmSessionSetting.SSL_MODE.getLabel(), sslMode.name());
		}

		List<String> paths = new ArrayList<>();

		for (String srcPath : commandValues.getStrings(CLIParam.from)) {
			if (StringUtils.isEmpty(srcPath)) { throw new CalienteException("Empty paths are not allowed"); }
			if (!srcPath.startsWith("/")) {
				srcPath = StringUtils.strip(srcPath);
			} else {
				srcPath = FilenameUtils.normalize(srcPath, true);
			}
			paths.add(srcPath);
		}

		settings.put(UcmSetting.SOURCE.getLabel(), UcmExportEngine.encodePathList(paths));

		String user = commandValues.getString(CLIParam.user);
		if (!StringUtils.isBlank(user)) {
			settings.put(UcmSessionSetting.USER.getLabel(), user);
		}

		String password = commandValues.getString(CLIParam.password);
		if (password != null) {
			settings.put(UcmSessionSetting.PASSWORD.getLabel(), password);
		}

		return true;
	}

	private final UcmExportEngineFactory exportFactory = new UcmExportEngineFactory();

	public EngineInterface() {
		super(UcmCommon.TARGET_NAME);
	}

	@Override
	protected UcmExportEngineFactory getExportEngineFactory() {
		return this.exportFactory;
	}

	@Override
	protected Exporter newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected ImportEngineFactory<?, ?, ?, ?, ?, ?> getImportEngineFactory() {
		return null;
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

	@Override
	public void getDynamicOptions(CalienteCommand command, OptionScheme scheme) {
		if (command.isRequiresStorage()) {
			scheme //
				.addGroup(CLIGroup.STORE) //
				.addGroup(CLIGroup.MAIL) //
				.addGroup(CLIGroup.CONNECTION) //
			;
		}
	}

}