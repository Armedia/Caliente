/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
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
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
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
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.alfresco.bi.BulkImportManager;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.xml.XmlTools;

public class AlfImportEngine extends
	ImportEngine<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportContextFactory, AlfImportDelegateFactory, AlfImportEngineFactory> {

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
		public boolean isFailBatchOnError() {
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
		public boolean isFailBatchOnError() {
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
		public boolean isFailBatchOnError() {
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
		public boolean isFailBatchOnError() {
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
			return false;
		}
	};

	private final ImportEngineListener listener = new DefaultImportEngineListener() {

		private final PrintWriter nullWriter = new PrintWriter(NullOutputStream.NULL_OUTPUT_STREAM);
		private final Map<UUID, PrintWriter> writers = new ConcurrentHashMap<>();

		@Override
		protected void importStartedImpl(ImportState importState, Map<CmfObject.Archetype, Long> summary) {
			File rootLocation = importState.baseData;
			if (rootLocation != null) {
				// Initialize the manifest for this job
				try {
					this.writers.put(importState.jobId,
						new PrintWriter(AlfImportEngine.this.biManager.openManifestWriter(true)));
				} catch (IOException e) {
					// Log a warning
					this.log.error("Failed to initialize the output manifest for job {}", importState.jobId.toString(),
						e);
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

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(AlfImportEngine.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(
				String.format("Failed to load the required schema resource [%s]", AlfImportEngine.SCHEMA_NAME));
		}
	}

	private final BulkImportManager biManager;

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;

	public AlfImportEngine(AlfImportEngineFactory factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings)
		throws ImportException, IOException, JAXBException {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings);

		if (settings.getBoolean(AlfSetting.GENERATE_INGESTION_INDEX)) {
			addListener(this.listener);
		}

		String content = settings.getString(AlfSetting.CONTENT);
		if (content == null) {
			throw new IOException("Can't proceed without a content directory to store artifacts in");
		}
		File contentFile = Tools.canonicalize(new File(content));
		if (!contentFile.exists()) {
			FileUtils.forceMkdir(contentFile);
		} else if (!contentFile.isDirectory()) {
			throw new IOException(String.format("The given content path of [%s] is not a valid directory",
				contentFile.getAbsolutePath()));
		}

		String unfiledPath = settings.getString(AlfSetting.UNFILED_PATH);
		unfiledPath = FilenameUtils.separatorsToUnix(unfiledPath);
		unfiledPath = FilenameUtils.normalizeNoEndSeparator(unfiledPath, true);

		this.biManager = new BulkImportManager(this.baseData, contentFile.toPath(), unfiledPath.replaceAll("^/+", ""));
		final File modelDir = this.biManager.getContentModelsPath().toFile();
		FileUtils.forceMkdir(modelDir);

		List<String> contentModels = settings.getStrings(AlfSetting.CONTENT_MODEL);
		if (contentModels == null) {
			throw new IllegalStateException("Must provide a valid set of content model XML files");
		}

		List<URI> modelUrls = new ArrayList<>();
		for (String s : contentModels) {
			File f = Tools.canonicalize(new File(s));
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
	}

	public final BulkImportManager getBulkImportManager() {
		return this.biManager;
	}

	public final AlfrescoSchema getSchema() {
		return this.schema;
	}

	public final Map<String, AlfrescoType> getDefaultTypes() {
		return this.defaultTypes;
	}

	@Override
	protected ImportStrategy getImportStrategy(CmfObject.Archetype type) {
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
	protected boolean checkSupported(Set<CmfObject.Archetype> excludes, CmfObject.Archetype type) {
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
	protected CmfValue getValue(CmfValue.Type type, Object value) {
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
	protected AlfImportContextFactory newContextFactory(AlfRoot session, CfgTools cfg, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
		return new AlfImportContextFactory(this, cfg, session, objectStore, streamStore, transformer, output,
			warningTracker);
	}

	@Override
	protected AlfImportDelegateFactory newDelegateFactory(AlfRoot session, CfgTools cfg) throws Exception {
		return new AlfImportDelegateFactory(this, cfg);
	}

	@Override
	protected AlfSchemaService newSchemaService(AlfRoot session) throws SchemaServiceException {
		return new AlfSchemaService(this.schema);
	}
}