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

import java.io.File;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportContextFactory extends
	ImportContextFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportEngine, File> {

	protected LocalImportContextFactory(LocalImportEngine engine, CfgTools settings, LocalRoot root,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, root, objectStore, contentStore, transformer, output, warningTracker);
	}

	@Override
	protected File locateFolder(LocalRoot session, String path) throws Exception {
		File f = new File(session.getFile(), path).getCanonicalFile();
		if (f.exists() && f.isDirectory()) { return f; }
		return null;
	}

	@Override
	protected File createFolder(LocalRoot session, File parent, String name) throws Exception {
		File f = new File(parent, name);
		f.mkdirs();
		if (!f.exists()) { throw new Exception(String.format("Could not create the directory at [%s]", name)); }
		if (!f.isDirectory()) { throw new Exception(String.format("A non-directory already exists at [%s]", name)); }
		return f;
	}

	@Override
	protected LocalImportContext constructContext(String rootId, CmfObject.Archetype rootType, LocalRoot session,
		int historyPosition) {
		return new LocalImportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker(),
			getTransformer(), getEngine().getTranslator(), getObjectStore(), getContentStore(), historyPosition);
	}

	@Override
	protected String calculateProductName(LocalRoot session) throws Exception {
		return "LocalFilesystem";
	}

	@Override
	protected String calculateProductVersion(LocalRoot session) throws Exception {
		return "1.0";
	}
}