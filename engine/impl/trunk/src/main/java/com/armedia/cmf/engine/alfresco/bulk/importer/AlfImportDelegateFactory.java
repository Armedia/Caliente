package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionFactory;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.XmlTools;

public class AlfImportDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine> {

	private static final String SCHEMA_NAME = "alfresco-model.xsd";

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(AlfImportDelegateFactory.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(String.format("Failed to load the required schema resource [%s]",
				AlfImportDelegateFactory.SCHEMA_NAME));
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
					AlfImportDelegateFactory.this.sequence.put(batchId, new AtomicInteger(-1));
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
					AlfImportDelegateFactory.this.sequence.remove(batchId);
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

	public AlfImportDelegateFactory(AlfImportEngine engine, CfgTools configuration) throws IOException {
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
	}

	String relativizeXmlLocation(String absolutePath) {
		String base = String.format("%s/", this.content.getAbsolutePath().replace(File.separatorChar, '/'));
		absolutePath = absolutePath.replace(File.separatorChar, '/');
		return absolutePath.substring(base.length());
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
	protected AlfImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case FOLDER:
				return new AlfFolderImportDelegate(this, storedObject);
			case DOCUMENT:
				// TODO: How to determine the minor counter
				return new AlfDocumentImportDelegate(this, storedObject);
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