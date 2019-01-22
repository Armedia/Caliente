package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.alfresco.bi.AlfCommon;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionFactory;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionWrapper;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.alfresco.bi.AlfTranslator;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.importer.DefaultImportEngineListener;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.ImportState;
import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class AlfImportEngine extends
	ImportEngine<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportContextFactory, AlfImportDelegateFactory, AlfImportEngineFactory> {

	static final String MANIFEST_NAME = "CALIENTE_INGESTION_INDEX.txt";

	private final class NameFixer implements CmfNameFixer<CmfValue> {

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
			final String originalName = getObjectName(dataObject);
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
		public boolean isBatchFailRemainder() {
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
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

	private final ImportEngineListener listener = new DefaultImportEngineListener() {

		private final PrintWriter nullWriter = new PrintWriter(new NullOutputStream());
		private final Map<UUID, PrintWriter> writers = new ConcurrentHashMap<>();

		@Override
		protected void importStartedImpl(ImportState importState, Map<CmfType, Long> summary) {
			File rootLocation = importState.baseData;
			if (rootLocation != null) {
				// Initialize the manifest for this job
				File biRoot = new File(rootLocation, AlfCommon.METADATA_ROOT);
				File manifest = new File(biRoot, AlfImportEngine.MANIFEST_NAME);
				try {
					manifest = manifest.getCanonicalFile();
				} catch (IOException e) {
					// Do nothing, stick with the old one
				}
				try {
					FileUtils.forceMkdir(biRoot);
					this.writers.put(importState.jobId, new PrintWriter(manifest));
				} catch (IOException e) {
					// Log a warning
					this.log.error(String.format("Failed to initialize the output manifest for job %s at [%s]",
						importState.jobId.toString(), manifest.getAbsolutePath()), e);
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
						if (!object.isHistoryCurrent()) {
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
		protected void importFinishedImpl(UUID jobId, Map<ImportResult, Long> counters) {
			PrintWriter w = getWriter(jobId);
			w.flush();
			w.close();
		}
	};

	private static final String SCHEMA_NAME = "alfresco-model.xsd";

	private static final String MODEL_DIR_NAME = "content-models";

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(AlfImportEngine.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(
				String.format("Failed to load the required schema resource [%s]", AlfImportEngine.SCHEMA_NAME));
		}
	}

	private final Path contentPath;
	private final Path biRootPath;
	private final String unfiledPath;

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;

	public AlfImportEngine(AlfImportEngineFactory factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings)
		throws ImportException, IOException, JAXBException {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings);
		addListener(this.listener);

		String content = settings.getString(AlfSetting.CONTENT);
		if (content == null) {
			throw new IOException("Can't proceed without a content directory to store artifacts in");
		}
		File contentFile = Tools.canonicalize(new File(content));
		FileUtils.forceMkdir(contentFile);
		this.contentPath = contentFile.toPath();

		this.biRootPath = this.baseData.resolve(AlfCommon.METADATA_ROOT);
		final File modelDir = this.biRootPath.resolve(AlfImportEngine.MODEL_DIR_NAME).toFile();
		FileUtils.forceMkdir(modelDir);

		List<String> contentModels = settings.getStrings(AlfSetting.CONTENT_MODEL);
		if (contentModels == null) {
			throw new IllegalStateException("Must provide a valid set of content model XML files");
		}

		List<URI> modelUrls = new ArrayList<>();
		for (String s : contentModels) {
			File f = new File(s).getCanonicalFile();
			if (!f.exists()) { throw new FileNotFoundException(f.getAbsolutePath()); }
			if (!f.isFile()) {
				throw new IOException(String.format("File [%s] is not a regular file", f.getAbsolutePath()));
			}
			if (!f.canRead()) {
				throw new IOException(String.format("File [%s] is not readable", f.getAbsolutePath()));
			}
			modelUrls.add(f.toURI());
			FileUtils.copyFile(f, new File(modelDir, f.getName()));
		}

		this.schema = new AlfrescoSchema(modelUrls);

		Map<String, AlfrescoType> m = new TreeMap<>();
		// First, we build all the base types, to have them cached and ready to go
		for (String t : this.schema.getTypeNames()) {
			m.put(t, this.schema.buildType(t));
		}
		this.defaultTypes = Tools.freezeMap(new LinkedHashMap<>(m));

		String unfiledPath = settings.getString(AlfSetting.UNFILED_PATH);
		unfiledPath = FilenameUtils.separatorsToUnix(unfiledPath);
		unfiledPath = FilenameUtils.normalizeNoEndSeparator(unfiledPath, true);
		this.unfiledPath = unfiledPath.replaceAll("^/+", "");
	}

	public final Path getContentPath() {
		return this.contentPath;
	}

	public final Path getBiRootPath() {
		return this.biRootPath;
	}

	public final String getUnfiledPath() {
		return this.unfiledPath;
	}

	public final AlfrescoSchema getSchema() {
		return this.schema;
	}

	public final Map<String, AlfrescoType> getDefaultTypes() {
		return this.defaultTypes;
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
	protected AlfImportContextFactory newContextFactory(AlfRoot session, CfgTools cfg, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
		return new AlfImportContextFactory(this, cfg, session, objectStore, streamStore, transformer, output,
			warningTracker);
	}

	@Override
	protected AlfImportDelegateFactory newDelegateFactory(AlfRoot session, CfgTools cfg) throws Exception {
		return new AlfImportDelegateFactory(this, cfg);
	}

	@Override
	protected CmfNameFixer<CmfValue> getNameFixer(Logger output) {
		return new NameFixer(output);
	}

	protected String getObjectName(CmfObject<CmfValue> object) {
		String finalName = object.getName();
		if (StringUtils.isBlank(finalName)) {
			finalName = object.getHistoryId();
		}
		return finalName;
	}

	@Override
	protected AlfSchemaService newSchemaService(AlfRoot session) throws SchemaServiceException {
		return new AlfSchemaService(this.schema);
	}
}