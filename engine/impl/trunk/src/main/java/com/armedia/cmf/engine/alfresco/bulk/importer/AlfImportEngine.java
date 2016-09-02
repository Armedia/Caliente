package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfCommon;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfTranslator;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfNameFixer;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportEngine extends
	ImportEngine<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportContextFactory, AlfImportDelegateFactory> {

	private static final CmfNameFixer<CmfValue> NAME_FIXER = new CmfNameFixer<CmfValue>() {

		private final String forbidden = "[\"*\\\\><?/:|]";

		@Override
		public String fixName(CmfObject<CmfValue> dataObject) throws CmfStorageException {
			String newName = dataObject.getName();

			// File names may not contain any of the following characters: "*\><?/:|
			newName = newName.replaceAll(this.forbidden, "_");

			// File names may not end in one or more dots (.)
			newName = newName.replaceAll("\\.$", "_");

			// File names may not end in one or more spaces
			newName = newName.replaceAll("\\s$", "_");

			return newName;
		}

		@Override
		public boolean handleException(Exception e) {
			return false;
		}

		@Override
		public boolean supportsType(CmfType type) {
			switch (type) {
				case DOCUMENT:
				case FOLDER:
					return true;
				default:
					return false;
			}
		}
	};

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

	private static final ImportStrategy USER_STRATEGY = new ImportStrategy() {

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

	private final ImportEngineListener listener = new ImportEngineListener() {

		private PrintWriter writer = null;

		@Override
		public void importStarted(UUID jobId, Map<CmfType, Integer> summary) {
		}

		@Override
		public void objectTypeImportStarted(UUID jobId, CmfType objectType, int totalObjects) {
		}

		@Override
		public void objectBatchImportStarted(UUID jobId, CmfType objectType, String batchId, int count) {
		}

		@Override
		public void objectImportStarted(UUID jobId, CmfObject<?> object) {
			if (this.writer == null) { return; }
			switch (object.getType()) {
				case DOCUMENT:
				case FOLDER:
					// output the r_object_id
					this.writer.printf("%s%n", object.getId());
					break;
				default:
					break;
			}
		}

		@Override
		public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome) {
		}

		@Override
		public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
		}

		@Override
		public void objectBatchImportFinished(UUID jobId, CmfType objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
		}

		@Override
		public void objectTypeImportFinished(UUID jobId, CmfType objectType, Map<ImportResult, Integer> counters) {
		}

		@Override
		public void importFinished(UUID jobId, Map<ImportResult, Integer> counters) {
		}
	};

	public AlfImportEngine() {
		super(new CmfCrypt());
		addListener(this.listener);
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfType type) {
		switch (type) {
			case USER:
				return AlfImportEngine.USER_STRATEGY;

			case FOLDER:
				return AlfImportEngine.FOLDER_STRATEGY;

			case DOCUMENT:
				return AlfImportEngine.DOCUMENT_STRATEGY;

			default:
				return AlfImportEngine.IGNORE_STRATEGY;
		}
	}

	@Override
	protected boolean checkSupported(Set<CmfType> excludes, CmfType type) {
		switch (type) {
			case USER:
			case FOLDER:
			case DOCUMENT:
				return super.checkSupported(excludes, type);
			default:
				break;
		}
		return false;
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

	@Override
	protected CmfNameFixer<CmfValue> getNameFixer() {
		return AlfImportEngine.NAME_FIXER;
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(AlfCommon.TARGET_NAME);
	}

	@Override
	protected Set<String> getTargetNames() {
		return AlfCommon.TARGETS;
	}

	@Override
	protected void prepareImport(UUID uuid, Map<String, ?> settings, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore) throws CmfStorageException, ImportException {
		// Set up the listener, and open the manifest file
	}
}