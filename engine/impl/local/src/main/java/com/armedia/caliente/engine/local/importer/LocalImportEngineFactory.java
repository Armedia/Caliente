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
package com.armedia.caliente.engine.local.importer;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportEngineFactory extends
	ImportEngineFactory<LocalRoot, CmfValue, LocalImportContext, LocalImportContextFactory, LocalImportDelegateFactory, LocalImportEngine> {

	public LocalImportEngineFactory() {
		super(false, CmfCrypt.DEFAULT);
	}

	@Override
	public LocalImportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) throws ImportException {
		return new LocalImportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return LocalCommon.TARGETS;
	}
}