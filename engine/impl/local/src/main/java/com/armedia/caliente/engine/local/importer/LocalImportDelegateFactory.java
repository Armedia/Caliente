/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.io.IOException;

import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory
	extends ImportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportEngine> {

	private final boolean includeAllVersions;
	private final boolean failOnCollisions;

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(LocalSetting.INCLUDE_ALL_VERSIONS);
		this.failOnCollisions = configuration.getBoolean(LocalSetting.FAIL_ON_COLLISIONS);
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	public final boolean isFailOnCollisions() {
		return this.failOnCollisions;
	}

	@Override
	protected LocalImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case DOCUMENT:
				return new LocalDocumentImportDelegate(this, storedObject);
			case FOLDER:
				return new LocalFolderImportDelegate(this, storedObject);
			default:
				return null;
		}
	}
}