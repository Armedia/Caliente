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

import java.util.Map;

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicCommandOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;

class Exporter extends ExportCommandModule implements DynamicCommandOptions {
	static final Option SOCKET_TIMEOUT = new OptionImpl() //
		.setLongOpt("socket-timeout") //
		.setArgumentLimits(1) //
		.setArgumentName("timeout") //
		.setDescription("The maximum number of seconds to wait on a socket connection to the RIDC backend") //
	;

	static final Option MIN_PING_TIME = new OptionImpl() //
		.setLongOpt("ping-time") //
		.setArgumentLimits(1) //
		.setArgumentName("time") //
		.setDescription(
			"The minimum number of seconds between ping attempts when validating pooled connections (0 = disable)") //
	;

	static final Option SSL_ALGORITHM = new OptionImpl() //
		.setLongOpt("ssl-algorithm") //
		.setArgumentLimits(1) //
		.setArgumentName("algorithm") //
		.setDescription("The SSL algorithm to use (only used if connecting via an idcs:// URL)") //
	;

	static final Option TRUSTSTORE = new OptionImpl() //
		.setLongOpt("truststore") //
		.setArgumentLimits(1) //
		.setArgumentName("path") //
		.setDescription("The keystore file to use as a trusted certificate store for SSL communication") //
	;

	static final Option TRUSTSTORE_PASSWORD = new OptionImpl() //
		.setLongOpt("truststore-pass") //
		.setArgumentLimits(1) //
		.setArgumentName("password") //
		.setDescription("The password required to unlock the given trusted store (can be an encrypted value)") //
	;

	static final Option KEYSTORE = new OptionImpl() //
		.setLongOpt("keystore") //
		.setArgumentLimits(1) //
		.setArgumentName("path") //
		.setDescription("The keystore file to use as a certificate store for SSL communication") //
	;

	static final Option KEYSTORE_PASSWORD = new OptionImpl() //
		.setLongOpt("keystoree-pass") //
		.setArgumentLimits(1) //
		.setArgumentName("password") //
		.setDescription("The password required to unlock the given keystore (can be an encrypted value)") //
	;

	static final Option CLIENT_CERT = new OptionImpl() //
		.setLongOpt("client-cert") //
		.setArgumentLimits(1) //
		.setArgumentName("time") //
		.setDescription("The name or alias of the client certificate to present, from within the given keystore") //
	;

	static final Option CLIENT_CERT_PASSWORD = new OptionImpl() //
		.setLongOpt("client-cert-pass") //
		.setArgumentLimits(1) //
		.setArgumentName("password") //
		.setDescription(
			"The password required to unlock the client certificate's private key (can be an encrypted value)") //
	;

	static final OptionGroup OPTIONS = new OptionGroupImpl("Oracle WebCenter RIDC Export") //
		.add(Exporter.SOCKET_TIMEOUT) //
		.add(Exporter.MIN_PING_TIME) //
		.add(Exporter.SSL_ALGORITHM) //
		.add(Exporter.TRUSTSTORE) //
		.add(Exporter.TRUSTSTORE_PASSWORD) //
		.add(Exporter.KEYSTORE) //
		.add(Exporter.KEYSTORE_PASSWORD) //
		.add(Exporter.CLIENT_CERT) //
		.add(Exporter.CLIENT_CERT_PASSWORD) //
	;

	Exporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		super(engine);
	}

	@Override
	protected boolean preInitialize(CalienteState state, Map<String, Object> settings) {
		return super.preInitialize(state, settings);
	}

	@Override
	protected boolean doInitialize(CalienteState state, Map<String, Object> settings) {
		return super.doInitialize(state, settings);
	}

	@Override
	protected boolean postInitialize(CalienteState state, Map<String, Object> settings) {
		return super.postInitialize(state, settings);
	}

	@Override
	protected void preValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
		super.preValidateSettings(state, settings);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return super.preConfigure(state, commandValues, settings);
	}

	@Override
	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.doConfigure(state, commandValues, settings)) { return false; }
		return EngineInterface.commonConfigure(commandValues, settings, getCrypto());
	}

	@Override
	protected void postConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		super.postConfigure(state, commandValues, settings);
	}

	@Override
	protected void postValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
		super.postValidateSettings(state, settings);
	}

	@Override
	public void getDynamicOptions(String engine, OptionScheme scheme) {
		scheme //
			.addGroup(CLIGroup.EXPORT_COMMON) //
			.addGroup(Exporter.OPTIONS) //
		;
	}
}