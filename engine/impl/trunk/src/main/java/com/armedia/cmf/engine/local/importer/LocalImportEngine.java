package com.armedia.cmf.engine.local.importer;

import java.text.ParseException;
import java.util.Set;

import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionFactory;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.LocalTranslator;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportEngine
	extends
	ImportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportContextFactory, LocalImportDelegateFactory> {

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
	};

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
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
	protected CmfValue getValue(CmfDataType type, Object value) {
		try {
			return new CmfValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Can't convert [%s] as a %s", value, type), e);
		}
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new LocalTranslator();
	}

	@Override
	protected LocalSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new LocalSessionFactory(cfg);
	}

	@Override
	protected LocalImportContextFactory newContextFactory(LocalRoot root, CfgTools cfg) throws Exception {
		return new LocalImportContextFactory(this, cfg);
	}

	@Override
	protected LocalImportDelegateFactory newDelegateFactory(LocalRoot root, CfgTools cfg) throws Exception {
		return new LocalImportDelegateFactory(this, cfg);
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(LocalCommon.TARGET_NAME);
	}

	@Override
	protected Set<String> getTargetNames() {
		return LocalCommon.TARGETS;
	}
}