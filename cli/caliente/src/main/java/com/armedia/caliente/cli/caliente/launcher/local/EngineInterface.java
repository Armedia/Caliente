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
package com.armedia.caliente.cli.caliente.launcher.local;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.exporter.LocalExportEngineFactory;
import com.armedia.caliente.engine.local.importer.LocalImportEngineFactory;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.launcher.LaunchClasspathHelper;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {

		return true;
	}

	private final LocalExportEngineFactory exportFactory = new LocalExportEngineFactory();
	private final LocalImportEngineFactory importFactory = new LocalImportEngineFactory();

	public EngineInterface() {
		super(LocalCommon.TARGET_NAME);
	}

	@Override
	protected LocalExportEngineFactory getExportEngineFactory() {
		return this.exportFactory;
	}

	@Override
	protected Exporter newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected LocalImportEngineFactory getImportEngineFactory() {
		return this.importFactory;
	}

	@Override
	protected Importer newImporter(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Importer(engine);
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
			;
		}
	}

}