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
package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.dfc.common.DctmCommon;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngineFactory;
import com.armedia.caliente.engine.dfc.importer.DctmImportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.tools.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.launcher.LaunchClasspathHelper;
import com.armedia.commons.utilities.cli.utils.DfcLaunchHelper;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static final DfcLaunchHelper DFC_HELPER = new DfcLaunchHelper(true);

	private static final OptionGroup DFC_OPTIONS = new OptionGroupImpl("DFC Configuration") //
		.add(DfcLaunchHelper.DFC_DOCUMENTUM) //
		.add(DfcLaunchHelper.DFC_LOCATION) //
	;

	private static final OptionGroup DFC_CONNECTION = new OptionGroupImpl("DFC Connection") //
		.addFrom(CLIGroup.CONNECTION) //
		.add(DfcLaunchHelper.DFC_UNIFIED) //
		.add(DfcLaunchHelper.DFC_PROPERTIES) //
	;

	private final DctmExportEngineFactory exportFactory = new DctmExportEngineFactory();
	private final DctmImportEngineFactory importFactory = new DctmImportEngineFactory();

	public EngineInterface() {
		super(DctmCommon.TARGET_NAME);
		// Load the logging-related patch classes
		// TODO: FIX THIS!!!
		// DfLoggerDisabled.class.hashCode();
		// LoggingConfigurator.class.hashCode();
	}

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		String server = commandValues.getString(CLIParam.server);
		String user = commandValues.getString(CLIParam.user);
		String password = commandValues.getString(CLIParam.password);

		if (commandValues.isPresent(DfcLaunchHelper.DFC_UNIFIED)) {
			// We don't use users/passwords for unified login
			user = null;
			password = null;
		} else {
			// If unified login is not in use, then user information (and a password?) is required
			if (StringUtils.isBlank(user)) {
				throw new CalienteException(
					String.format("A non-blank username (specified via --%s) is required when not using unified login",
						CLIParam.user.get().getLongOpt()));
			}
		}

		if (!StringUtils.isEmpty(server)) {
			settings.put(DfcSessionFactory.DOCBASE, server);
		}
		if (!StringUtils.isEmpty(user)) {
			settings.put(DfcSessionFactory.USERNAME, user);
		}
		if (!StringUtils.isEmpty(password)) {
			settings.put(DfcSessionFactory.PASSWORD, password);
		}
		return true;
	}

	@Override
	protected DctmExportEngineFactory getExportEngineFactory() {
		return this.exportFactory;
	}

	@Override
	protected Exporter newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected DctmImportEngineFactory getImportEngineFactory() {
		return this.importFactory;
	}

	@Override
	protected Importer newImporter(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Importer(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.singleton(EngineInterface.DFC_HELPER);
	}

	@Override
	public void getDynamicOptions(CalienteCommand command, OptionScheme scheme) {
		if (command.isRequiresStorage()) {
			scheme //
				.addGroup(CLIGroup.STORE) //
				.addGroup(CLIGroup.MAIL) //
				.addGroup(EngineInterface.DFC_CONNECTION) //
			;
		}

		scheme //
			.addGroup(EngineInterface.DFC_OPTIONS) //
		;
	}

}
