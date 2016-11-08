package com.armedia.caliente.engine.xml.importer;

import java.util.Set;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.engine.xml.common.XmlCommon;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionFactory;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.engine.xml.common.XmlTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportEngine extends
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

	private static final ImportStrategy GROUP_STRATEGY = new ImportStrategy() {

		@Override
		public boolean isParallelCapable() {
			return false;
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

	private static final ImportStrategy AGGREGATE_STRATEGY = new ImportStrategy() {

		@Override
		public boolean isParallelCapable() {
			return false;
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

	public XmlImportEngine() {
		super(new CmfCrypt());
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		switch (type) {

			case USER:
			case TYPE:
			case ACL:
			case FORMAT:
				return XmlImportEngine.AGGREGATE_STRATEGY;

			case GROUP:
				return XmlImportEngine.GROUP_STRATEGY;

			case FOLDER:
				return XmlImportEngine.FOLDER_STRATEGY;

			case DOCUMENT:
				return XmlImportEngine.DOCUMENT_STRATEGY;

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
	protected XmlSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new XmlSessionFactory(cfg, crypto);
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