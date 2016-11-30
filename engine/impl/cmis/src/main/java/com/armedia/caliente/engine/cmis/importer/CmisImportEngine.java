package com.armedia.caliente.engine.cmis.importer;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.cmis.CmisCommon;
import com.armedia.caliente.engine.cmis.CmisSessionFactory;
import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportEngine extends
	ImportEngine<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportContextFactory, CmisImportDelegateFactory> {

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
		public boolean isBatchFailRemainder() {
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
		public boolean isBatchFailRemainder() {
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
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}
	};

	public CmisImportEngine() {
		super(new CmfCrypt());
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		switch (type) {
			case DOCUMENT:
				return CmisImportEngine.DOCUMENT_STRATEGY;
			case FOLDER:
				return CmisImportEngine.FOLDER_STRATEGY;
			default:
				break;
		}
		return CmisImportEngine.IGNORE_STRATEGY;
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new CmisTranslator();
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new CmisSessionFactory(cfg, crypto);
	}

	@Override
	protected CmisImportContextFactory newContextFactory(Session session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, CmfTypeMapper typeMapper, Logger output)
		throws Exception {
		return new CmisImportContextFactory(this, session, cfg, objectStore, streamStore, typeMapper, output);
	}

	@Override
	protected CmisImportDelegateFactory newDelegateFactory(Session session, CfgTools cfg) throws Exception {
		return new CmisImportDelegateFactory(this, session, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(CmisCommon.TARGET_NAME);
	}
}