package com.armedia.cmf.engine.alfresco.bulk.indexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoSchema;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class AlfIndexerDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfIndexerContext, AlfIndexerEngine> {

	private static final String SCHEMA_NAME = "alfresco-model.xsd";

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(AlfIndexerDelegateFactory.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(String.format("Failed to load the required schema resource [%s]",
				AlfIndexerDelegateFactory.SCHEMA_NAME));
		}
	}

	private final Map<String, AtomicInteger> sequence = new ConcurrentHashMap<String, AtomicInteger>();

	private final ImportEngineListener listener = new ImportEngineListener() {

		@Override
		public void importStarted(Map<CmfType, Integer> summary) {
		}

		@Override
		public void objectTypeImportStarted(CmfType objectType, int totalObjects) {
		}

		@Override
		public void objectBatchImportStarted(CmfType objectType, String batchId, int count) {
			switch (objectType) {
				case DOCUMENT:
					AlfIndexerDelegateFactory.this.sequence.put(batchId, new AtomicInteger(-1));
					return;
				default:
					return;
			}
		}

		@Override
		public void objectImportStarted(CmfObject<?> object) {
			getCounter(object).incrementAndGet();
		}

		@Override
		public void objectImportFailed(CmfObject<?> object, Throwable thrown) {
		}

		@Override
		public void objectImportCompleted(CmfObject<?> object, ImportOutcome outcome) {
		}

		@Override
		public void objectBatchImportFinished(CmfType objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
			switch (objectType) {
				case DOCUMENT:
					AlfIndexerDelegateFactory.this.sequence.remove(batchId);
					return;
				default:
					return;
			}
		}

		@Override
		public void objectTypeImportFinished(CmfType objectType, Map<ImportResult, Integer> counters) {
		}

		@Override
		public void importFinished(Map<ImportResult, Integer> counters) {
		}
	};

	private final File db;
	private final File content;

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;

	public AlfIndexerDelegateFactory(AlfIndexerEngine engine, CfgTools configuration) throws IOException, JAXBException {
		super(engine, configuration);
		engine.addListener(this.listener);
		String db = configuration.getString(AlfSessionFactory.DB);
		if (db != null) {
			this.db = new File(db).getCanonicalFile();
		} else {
			this.db = new File("cmsmf-xml").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.db);
		String content = configuration.getString(AlfSessionFactory.CONTENT);
		if (content != null) {
			this.content = new File(content).getCanonicalFile();
		} else {
			this.content = new File(db, "content").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.content);

		String contentModels = configuration.getString(AlfSessionFactory.CONTENT_MODEL);
		if (contentModels == null) { throw new IllegalStateException(
			"Must provide a valid set of content model XML files"); }

		List<URI> modelUrls = new ArrayList<URI>();
		for (String s : contentModels.split(",")) {
			File f = new File(s).getCanonicalFile();
			if (!f.exists()) { throw new FileNotFoundException(f.getAbsolutePath()); }
			if (!f.isFile()) { throw new IOException(
				String.format("File [%s] is not a regular file", f.getAbsolutePath())); }
			if (!f
				.canRead()) { throw new IOException(String.format("File [%s] is not readable", f.getAbsolutePath())); }
			modelUrls.add(f.toURI());
		}

		this.schema = new AlfrescoSchema(modelUrls);

		Map<String, AlfrescoType> m = new TreeMap<String, AlfrescoType>();
		for (String t : this.schema.getTypeNames()) {
			m.put(t, this.schema.buildType(t));
		}
		this.defaultTypes = Tools.freezeMap(new LinkedHashMap<String, AlfrescoType>(m));
	}

	protected AlfrescoType getType(String name, String... aspects) {
		if ((aspects == null) || (aspects.length == 0)) { return this.defaultTypes.get(name); }
		return this.schema.buildType(name, aspects);
	}

	protected AlfrescoType getType(String name, Collection<String> aspects) {
		if ((aspects == null) || aspects.isEmpty()) { return this.defaultTypes.get(name); }
		return this.schema.buildType(name, aspects);
	}

	String relativizeXmlLocation(String absolutePath) {
		String base = String.format("%s/", this.content.getAbsolutePath().replace(File.separatorChar, '/'));
		absolutePath = absolutePath.replace(File.separatorChar, '/');
		return absolutePath.substring(base.length());
	}

	File getLocation(String relativePath) {
		return new File(this.content, relativePath);
	}

	private final AtomicInteger getCounter(CmfObject<?> storedObject) {
		if (storedObject == null) { throw new IllegalArgumentException(
			"Must provide a CMF object to get the counter for"); }
		AtomicInteger counter = this.sequence.get(storedObject.getBatchId());
		if (counter == null) { throw new IllegalStateException(
			String.format("Failed to locate the counter for batch [%s] referenced by [%s](%s)",
				storedObject.getBatchId(), storedObject.getLabel(), storedObject.getId())); }
		return counter;
	}

	@Override
	protected AlfIndexerDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case FOLDER:
				return new AlfFolderIndexerDelegate(this, storedObject);
			case DOCUMENT:
				// TODO: How to determine the minor counter
				return new AlfDocumentIndexerDelegate(this, storedObject);
			default:
				break;
		}
		return null;
	}

	protected File calculateConsolidatedFile(CmfType t) {
		return new File(this.db, String.format("%ss.xml", t.name().toLowerCase()));
	}

	@Override
	public void close() {
		super.close();
	}
}