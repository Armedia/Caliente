package com.armedia.caliente.engine.ucm.importer;

import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.engine.ucm.UcmCommon;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.UcmTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class UcmImportEngine extends
	ImportEngine<UcmSession, UcmSessionWrapper, CmfValue, UcmImportContext, UcmImportContextFactory, UcmImportDelegateFactory> {

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

	public UcmImportEngine() {
		super(new CmfCrypt());
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		switch (type) {
			case DOCUMENT:
				return UcmImportEngine.DOCUMENT_STRATEGY;
			case FOLDER:
				return UcmImportEngine.FOLDER_STRATEGY;
			default:
				break;
		}
		return UcmImportEngine.IGNORE_STRATEGY;
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new UcmTranslator();
	}

	@Override
	protected UcmSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new UcmSessionFactory(cfg, crypto);
	}

	@Override
	protected UcmImportContextFactory newContextFactory(UcmSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new UcmImportContextFactory(this, session, cfg, objectStore, streamStore, transformer, output,
			warningTracker);
	}

	@Override
	protected UcmImportDelegateFactory newDelegateFactory(UcmSession session, CfgTools cfg) throws Exception {
		return new UcmImportDelegateFactory(this, session, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return UcmCommon.TARGETS;
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(UcmCommon.TARGET_NAME);
	}
}