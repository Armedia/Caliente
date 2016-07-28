package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.Set;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfCommon;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfTranslator;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportEngine extends
	ImportEngine<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportContextFactory, AlfImportDelegateFactory> {

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
		public boolean isBatchIndependent() {
			return false;
		}

		@Override
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public BatchItemStrategy getBatchItemStrategy() {
			return BatchItemStrategy.ITEMS_SERIALIZED;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}

		@Override
		public boolean isBatchingSupported() {
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
		public boolean isBatchIndependent() {
			return true;
		}

		@Override
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public BatchItemStrategy getBatchItemStrategy() {
			return BatchItemStrategy.ITEMS_SERIALIZED;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}

		@Override
		public boolean isBatchingSupported() {
			return true;
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
		public boolean isBatchIndependent() {
			return false;
		}

		@Override
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public BatchItemStrategy getBatchItemStrategy() {
			return BatchItemStrategy.ITEMS_CONCURRENT;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}

		@Override
		public boolean isBatchingSupported() {
			return true;
		}
	};

	public AlfImportEngine() {
		super(new CmfCrypt());
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		switch (type) {
			case FOLDER:
				return AlfImportEngine.FOLDER_STRATEGY;

			case DOCUMENT:
				return AlfImportEngine.DOCUMENT_STRATEGY;

			default:
				return AlfImportEngine.IGNORE_STRATEGY;
		}
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new AlfTranslator();
	}

	@Override
	protected AlfSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new AlfSessionFactory(cfg, crypto);
	}

	@Override
	protected AlfImportContextFactory newContextFactory(AlfRoot root, CfgTools cfg) throws Exception {
		return new AlfImportContextFactory(this, cfg, root);
	}

	@Override
	protected AlfImportDelegateFactory newDelegateFactory(AlfRoot root, CfgTools cfg) throws Exception {
		return new AlfImportDelegateFactory(this, cfg);
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(AlfCommon.TARGET_NAME);
	}

	@Override
	protected Set<String> getTargetNames() {
		return AlfCommon.TARGETS;
	}
}