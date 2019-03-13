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
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
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
		public boolean isBatchFailRemainder() {
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
		return DfValueFactory.newValue(DctmTranslator.translateType(type).getDfConstant(), value);
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