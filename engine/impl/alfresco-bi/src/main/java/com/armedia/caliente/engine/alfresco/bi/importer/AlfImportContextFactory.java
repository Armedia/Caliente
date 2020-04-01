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
package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionWrapper;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class AlfImportContextFactory
	extends ImportContextFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine, File> {

	protected AlfImportContextFactory(AlfImportEngine engine, CfgTools settings, AlfRoot root,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker tracker) throws Exception {
		super(engine, settings, root, objectStore, contentStore, transformer, output, tracker);
	}

	@Override
	protected File locateFolder(AlfRoot session, String path) throws Exception {
		File f = Tools.canonicalize(new File(session.getFile(), path));
		if (f.exists() && f.isDirectory()) { return f; }
		return null;
	}

	@Override
	protected File createFolder(AlfRoot session, File parent, String name) throws Exception {
		File f = new File(parent, name);
		f.mkdirs();
		if (!f.exists()) { throw new Exception(String.format("Could not create the directory at [%s]", name)); }
		if (!f.isDirectory()) { throw new Exception(String.format("A non-directory already exists at [%s]", name)); }
		return f;
	}

	@Override
	protected AlfImportContext constructContext(String rootId, CmfObject.Archetype rootType, AlfRoot session,
		int batchPosition) {
		final CmfObjectStore<?> store = getObjectStore();
		return new AlfImportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker(),
			getTransformer(), getEngine().getTranslator(), store, getContentStore(), batchPosition);
	}

	@Override
	protected String calculateProductName(AlfRoot session) throws Exception {
		return "AlfrescoBulkImport";
	}

	@Override
	protected String calculateProductVersion(AlfRoot session) throws Exception {
		return "1.0";
	}

	public final Map<CmfObjectRef, String> getObjectNames(Collection<CmfObjectRef> refs, boolean current)
		throws ImportException {
		try {
			return getObjectStore().getObjectNames(refs, current);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to resolve the object names for IDs %s", refs), e);
		}
	}
}