package com.armedia.caliente.engine.xml.importer;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionFactory;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.engine.xml.common.XmlTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
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
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
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
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}
	};

	public XmlImportEngine(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings) {
		super(output, warningTracker, baseData, objectStore, contentStore, settings, new CmfCrypt(), false);
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
	protected XmlImportContextFactory newContextFactory(XmlRoot session, CfgTools cfg, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
		return new XmlImportContextFactory(this, cfg, session, objectStore, streamStore, transformer, output,
			warningTracker);
	}

	@Override
	protected XmlImportDelegateFactory newDelegateFactory(XmlRoot session, CfgTools cfg) throws Exception {
		return new XmlImportDelegateFactory(this, cfg);
	}
}