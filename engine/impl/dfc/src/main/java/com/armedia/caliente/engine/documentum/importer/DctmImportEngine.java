/**
 *
 */

package com.armedia.caliente.engine.documentum.importer;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.documentum.DctmCrypto;
import com.armedia.caliente.engine.documentum.DctmObjectType;
import com.armedia.caliente.engine.documentum.DctmSessionFactory;
import com.armedia.caliente.engine.documentum.DctmSessionWrapper;
import com.armedia.caliente.engine.documentum.DctmTranslator;
import com.armedia.caliente.engine.documentum.common.DctmCommon;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportEngine extends
	ImportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportContextFactory, DctmImportDelegateFactory> {

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

	private static final Set<String> TARGETS = Collections.singleton(DctmCommon.TARGET_NAME);

	public DctmImportEngine() {
		super(new DctmCrypto(), true);
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		DctmObjectType dctmType = DctmObjectType.decodeType(type);
		if (dctmType == null) { return DctmImportEngine.NOT_SUPPORTED; }
		return dctmType.importStrategy;
	}

	@Override
	protected DctmImportContextFactory newContextFactory(IDfSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, CmfTypeMapper typeMapper, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new DctmImportContextFactory(this, cfg, session, objectStore, streamStore, typeMapper, output,
			warningTracker);
	}

	@Override
	protected IDfValue getValue(CmfDataType type, Object value) {
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
	protected Set<String> getTargetNames() {
		return DctmImportEngine.TARGETS;
	}

	@Override
	protected boolean abortImport(CmfType type, long errors) {
		if (type == CmfType.DATASTORE) {
			// We MUST have all datastores present
			return (errors > 0);
		}
		return super.abortImport(type, errors);
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(DctmCommon.TARGET_NAME);
	}
}