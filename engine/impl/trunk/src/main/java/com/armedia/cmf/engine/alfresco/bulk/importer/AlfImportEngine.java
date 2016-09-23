package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfCommon;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfTranslator;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfrescoBaseBulkOrganizationStrategy;
import com.armedia.cmf.engine.importer.DefaultImportEngineListener;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.importer.ImportState;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfNameFixer;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class AlfImportEngine extends
	ImportEngine<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportContextFactory, AlfImportDelegateFactory> {

	private static final String MANIFEST_NAME = "CALIENTE_INGESTION_INDEX.txt";

	private static final class NameFixer implements CmfNameFixer<CmfValue> {

		private final String forbidden = "[\"*\\\\><?/:|]";

		private long renamed = 0;
		private long visited = 0;

		private final Logger output;

		private NameFixer(Logger output) {
			if (output == null) {
				output = LoggerFactory.getLogger(getClass());
			}
			this.output = output;
		}

		@Override
		public String fixName(CmfObject<CmfValue> dataObject) throws CmfStorageException {
			this.visited++;
			final String originalName = dataObject.getName();
			String newName = originalName;

			// File names may not contain any of the following characters: "*\><?/:|
			newName = newName.replaceAll(this.forbidden, "_");

			// File names may not end in one or more dots (.)
			newName = newName.replaceAll("\\.$", "_");

			// File names may not end in one or more spaces
			newName = newName.replaceAll("\\s$", "_");

			if (!Tools.equals(originalName, newName)) {
				this.renamed++;
			}

			if ((this.visited % 1000) == 0) {
				this.output.info("Analyzed {} names, fixed {} so far", this.visited, this.renamed);
			}
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

		@Override
		public void nameFixed(CmfObject<CmfValue> dataObject, String oldName, String newName) {
			this.output.info("Renamed {} with ID[{}] from [{}] to [{}]", dataObject.getType(), dataObject.getId(),
				oldName, newName);
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

	private final Map<UUID, ImportState> states = new ConcurrentHashMap<UUID, ImportState>();

	private final ImportEngineListener listener = new DefaultImportEngineListener() {

		private final PrintWriter nullWriter = new PrintWriter(new NullOutputStream());
		private final Map<UUID, PrintWriter> writers = new ConcurrentHashMap<UUID, PrintWriter>();

		@Override
		public void importStarted(UUID jobId, Map<CmfType, Integer> summary) {
			ImportState state = AlfImportEngine.this.states.get(jobId);
			if (state == null) {
				this.log.error("Failed to find the import state for job {}", jobId.toString());
				return;
			}

			File rootLocation = state.streamStore.getRootLocation();
			if (rootLocation != null) {
				// Initialize the manifest for this job
				File biRoot = new File(rootLocation, AlfrescoBaseBulkOrganizationStrategy.BASE_DIR);
				File manifest = new File(biRoot, AlfImportEngine.MANIFEST_NAME);
				try {
					manifest = manifest.getCanonicalFile();
				} catch (IOException e) {
					// Do nothing, stick with the old one
				}
				try {
					FileUtils.forceMkdir(biRoot);
					this.writers.put(jobId, new PrintWriter(manifest));
				} catch (IOException e) {
					// Log a warning
					this.log.error(String.format("Failed to initialize the output manifest for job %s at [%s]",
						jobId.toString(), manifest.getAbsolutePath()), e);
				}
			}
		}

		private PrintWriter getWriter(UUID jobId) {
			if (jobId == null) { return this.nullWriter; }
			return Tools.coalesce(this.writers.get(jobId), this.nullWriter);
		}

		@Override
		public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome) {
			switch (outcome.getResult()) {
				case CREATED:
				case UPDATED:
					// This ID we'll report...
					break;
				default:
					// Skip this one...
					return;
			}

			final PrintWriter writer = getWriter(jobId);
			try {
				switch (object.getType()) {
					case DOCUMENT:
						if (!object.isBatchHead()) {
							break;
						}
						// Fall-through
					case FOLDER:
						// output the r_object_id
						writer.printf("%s%n", object.getId());
						break;
					default:
						break;
				}
			} finally {
				writer.flush();
			}
		}

		@Override
		public void importFinished(UUID jobId, Map<ImportResult, Integer> counters) {
			PrintWriter w = getWriter(jobId);
			w.flush();
			IOUtils.closeQuietly(w);
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
	protected CmfNameFixer<CmfValue> getNameFixer(Logger output) {
		return new NameFixer(output);
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return ImportEngine.getImportEngine(AlfCommon.TARGET_NAME);
	}

	@Override
	protected Set<String> getTargetNames() {
		return AlfCommon.TARGETS;
	}

	@Override
	protected void prepareImport(ImportState importState) throws CmfStorageException, ImportException {
		super.prepareImport(importState);
		this.states.put(importState.jobId, importState);
	}

	@Override
	protected void importFinalized(ImportState importState) throws CmfStorageException, ImportException {
		this.states.remove(importState.jobId);
	}

	@Override
	protected void importFailed(ImportState importState) throws CmfStorageException, ImportException {
		this.states.remove(importState.jobId);
	}
}