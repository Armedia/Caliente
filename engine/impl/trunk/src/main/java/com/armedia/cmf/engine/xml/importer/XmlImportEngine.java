package com.armedia.cmf.engine.xml.importer;

import java.util.Set;

import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.engine.xml.common.XmlCommon;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionFactory;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.common.XmlTranslator;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportEngine
	extends
	ImportEngine<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportContextFactory, XmlImportDelegateFactory> {

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

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		switch (type) {
			case DOCUMENT:
				return XmlImportEngine.DOCUMENT_STRATEGY;
			case FOLDER:
				return XmlImportEngine.FOLDER_STRATEGY;
			default:
				return XmlImportEngine.IGNORE_STRATEGY;
		}
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new XmlTranslator();
	}

	@Override
	protected XmlSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new XmlSessionFactory(cfg);
	}

	@Override
	protected XmlImportContextFactory newContextFactory(XmlRoot root, CfgTools cfg) throws Exception {
		return new XmlImportContextFactory(this, cfg, root);
	}

	@Override
	protected XmlImportDelegateFactory newDelegateFactory(XmlRoot root, CfgTools cfg) throws Exception {
		return new XmlImportDelegateFactory(this, cfg);
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(XmlCommon.TARGET_NAME);
	}

	@Override
	protected Set<String> getTargetNames() {
		return XmlCommon.TARGETS;
	}
}