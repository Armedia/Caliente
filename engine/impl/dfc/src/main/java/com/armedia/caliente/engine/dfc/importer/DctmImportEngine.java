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
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionFactory;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.DctmTranslator;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmImportEngine extends
	ImportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportContextFactory, DctmImportDelegateFactory, DctmImportEngineFactory> {

	private static final ImportStrategy NOT_SUPPORTED = new ImportStrategy() {
		@Override
		public boolean isIgnored() {
			return true;
		}

		@Override
		public boolean isParallelCapable() {
			return true;
		}

		@Override
		public boolean isFailBatchOnError() {
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}
	};

	public DctmImportEngine(DctmImportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfObject.Archetype type) {
		DctmObjectType dctmType = DctmObjectType.decodeType(type);
		if (dctmType == null) { return DctmImportEngine.NOT_SUPPORTED; }
		return dctmType.importStrategy;
	}

	@Override
	protected DctmImportContextFactory newContextFactory(IDfSession session, CfgTools cfg,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new DctmImportContextFactory(this, cfg, session, objectStore, streamStore, transformer, output,
			warningTracker);
	}

	@Override
	protected IDfValue getValue(CmfValue.Type type, Object value) {
		return DfValueFactory.of(DctmTranslator.translateType(type).getDfConstant(), value);
	}

	@Override
	protected CmfAttributeTranslator<IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory(CfgTools config, CmfCrypt crypto) throws Exception {
		return new DctmSessionFactory(config, crypto);
	}

	@Override
	protected DctmImportDelegateFactory newDelegateFactory(IDfSession session, CfgTools cfg) throws Exception {
		return new DctmImportDelegateFactory(this, cfg);
	}

	@Override
	protected boolean abortImport(CmfObject.Archetype type, long errors) {
		if (type == CmfObject.Archetype.DATASTORE) {
			// We MUST have all datastores present
			return (errors > 0);
		}
		return super.abortImport(type, errors);
	}

	@Override
	protected DctmSchemaService newSchemaService(IDfSession session) throws SchemaServiceException {
		return new DctmSchemaService(session);
	}
}