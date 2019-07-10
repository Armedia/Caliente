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
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.common.DctmSpecialValues;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmImportContextFactory extends
	ImportContextFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportEngine, IDfFolder> {
	private final DctmSpecialValues specialValues;

	DctmImportContextFactory(DctmImportEngine engine, CfgTools cfg, IDfSession session, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
		super(engine, cfg, session, objectStore, contentStore, transformer, output, warningTracker);
		this.specialValues = new DctmSpecialValues(cfg);
	}

	@Override
	protected DctmImportContext constructContext(String rootId, CmfObject.Archetype rootType, IDfSession session,
		int historyPosition) {
		return new DctmImportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker(),
			getTransformer(), getEngine().getTranslator(), getObjectStore(), getContentStore(), historyPosition);
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}

	@Override
	protected IDfFolder locateFolder(IDfSession session, String path) throws Exception {
		return session.getFolderByPath(path);
	}

	@Override
	protected IDfFolder createFolder(IDfSession session, IDfFolder parent, String name) throws Exception {
		final String type = (parent != null ? DctmObjectType.FOLDER.getDmType() : "dm_cabinet");
		IDfFolder f = IDfFolder.class.cast(session.newObject(type));
		f.setObjectName(name);
		if (parent != null) {
			f.link(parent.getObjectId().getId());
		}
		f.save();
		return f;
	}

	@Override
	protected String calculateProductName(IDfSession session) {
		return "Documentum";
	}

	@Override
	protected String calculateProductVersion(IDfSession session) throws Exception {
		return session.getServerVersion();
	}
}