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
package com.armedia.caliente.cli.caliente.launcher.alfresco;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.alfresco.bi.AlfCommon;
import com.armedia.caliente.engine.alfresco.bi.importer.AlfImportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	private final AlfImportEngineFactory importFactory = new AlfImportEngineFactory();

	public EngineInterface() {
		super(AlfCommon.TARGET_NAME);
	}

	@Override
	protected ExportEngineFactory<?, ?, ?, ?, ?, ?> getExportEngineFactory() {
		return null;
	}

	@Override
	protected AlfImportEngineFactory getImportEngineFactory() {
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