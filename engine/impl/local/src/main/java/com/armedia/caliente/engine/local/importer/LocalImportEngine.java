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
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionFactory;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportEngine extends
	ImportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportContextFactory, LocalImportDelegateFactory, LocalImportEngineFactory> {

	public LocalImportEngine(LocalImportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	private static final ImportStrategy IGNORE_STRATEGY = new ImportStrategy() {

		@Override
		public boolean isParallelCapable() {
			return false;
		}

		@Override
		public boolean isIgnored() {
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

	private static final ImportStrategy DOCUMENT_STRATEGY = new ImportStrategy() {

		@Override
		public boolean isParallelCapable() {
			return true;
		}

		@Override
		public boolean isIgnored() {
			return false;
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

	private static final ImportStrategy FOLDER_STRATEGY = new ImportStrategy() {

		@Override
		public boolean isParallelCapable() {
			return true;
		}

		@Override
		public boolean isIgnored() {
			return false;
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

	@Override
	protected ImportStrategy getImportStrategy(CmfObject.Archetype type) {
		switch (type) {
			case DOCUMENT:
				return LocalImportEngine.DOCUMENT_STRATEGY;
			case FOLDER:
				return LocalImportEngine.FOLDER_STRATEGY;
			default:
				return LocalImportEngine.IGNORE_STRATEGY;
		}
	}

	@Override
	protected CmfValue getValue(CmfValue.Type type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new LocalTranslator();
	}

	@Override
	protected LocalSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new LocalSessionFactory(cfg, crypto);
	}

	@Override
	protected LocalImportContextFactory newContextFactory(LocalRoot session, CfgTools cfg,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new LocalImportContextFactory(this, cfg, session, objectStore, streamStore, transformer, output,
			warningTracker);
	}

	@Override
	protected LocalImportDelegateFactory newDelegateFactory(LocalRoot session, CfgTools cfg) throws Exception {
		return new LocalImportDelegateFactory(this, cfg);
	}

	@Override
	protected SchemaService newSchemaService(LocalRoot session) throws SchemaServiceException {
		return null;
	}
}