package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.importer.DefaultImportEngineListener;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionFactory;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.importer.jaxb.AclsT;
import com.armedia.cmf.engine.xml.importer.jaxb.AggregatorBase;
import com.armedia.cmf.engine.xml.importer.jaxb.ContentInfoT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentIndexEntryT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentIndexT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentVersionT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentsT;
import com.armedia.cmf.engine.xml.importer.jaxb.FolderIndexT;
import com.armedia.cmf.engine.xml.importer.jaxb.FoldersT;
import com.armedia.cmf.engine.xml.importer.jaxb.FormatsT;
import com.armedia.cmf.engine.xml.importer.jaxb.GroupsT;
import com.armedia.cmf.engine.xml.importer.jaxb.TypesT;
import com.armedia.cmf.engine.xml.importer.jaxb.UsersT;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.LockDispenser;
import com.armedia.commons.utilities.XmlTools;

public class XmlImportDelegateFactory
	extends ImportDelegateFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine> {

	static final String SCHEMA = "import.xsd";

	private final Map<CmfType, AggregatorBase<?>> xml;
	private final boolean aggregateFolders;
	private final boolean aggregateDocuments;
	private final File db;
	private final File content;
	private final LockDispenser<String, Object> documentBatchLocks = LockDispenser.getBasic();
	private final Map<String, List<DocumentVersionT>> documentBatches = new ConcurrentHashMap<String, List<DocumentVersionT>>();

	private final ImportEngineListener documentListener = new DefaultImportEngineListener() {

		private int filesWritten = 0;

		@Override
		public void objectBatchImportStarted(CmfType objectType, String batchId, int count) {
			if (objectType != CmfType.DOCUMENT) { return; }
			List<DocumentVersionT> l = XmlImportDelegateFactory.this.documentBatches.get(batchId);
			if (l == null) {
				final Object lock = XmlImportDelegateFactory.this.documentBatchLocks.getLock(batchId);
				synchronized (lock) {
					l = XmlImportDelegateFactory.this.documentBatches.get(batchId);
					if (l == null) {
						l = Collections.synchronizedList(new ArrayList<DocumentVersionT>());
						XmlImportDelegateFactory.this.documentBatches.put(batchId, l);
					}
				}
			}
		}

		@Override
		public void objectBatchImportFinished(CmfType objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
			if (objectType != CmfType.DOCUMENT) { return; }
			if (failed) { return; }
			List<DocumentVersionT> l = XmlImportDelegateFactory.this.documentBatches.get(batchId);
			if ((l == null) || l.isEmpty()) { return; }

			try {
				DocumentT doc = new DocumentT();
				doc.getVersion().addAll(l);

				if (XmlImportDelegateFactory.this.aggregateDocuments) {
					DocumentsT.class.cast(XmlImportDelegateFactory.this.xml.get(CmfType.DOCUMENT)).add(doc);
				} else {
					// The content's location is in the first version
					DocumentVersionT first = l.get(0);
					ContentInfoT content = first.getContents().get(0);
					File tgt = new File(XmlImportDelegateFactory.this.content, content.getLocation());
					File dir = tgt.getParentFile();
					tgt = new File(dir, String.format("%s-document.xml", batchId));

					final OutputStream out;
					try {
						out = new FileOutputStream(tgt);
					} catch (FileNotFoundException e) {
						this.log.error(String.format("Failed to open an output stream to [%s]", tgt), e);
						return;
					}

					boolean ok = false;
					String xml = null;
					try {
						xml = XmlTools.marshal(doc, XmlImportDelegateFactory.SCHEMA, true);
						try {
							IOUtils.write(xml, out);
						} catch (IOException e) {
							this.log.error(String.format("Failed to write out the XML to [%s]:%n%s", tgt, xml), e);
							return;
						}
						this.filesWritten++;
						ok = true;
					} catch (JAXBException e) {
						this.log.error(String.format("Failed to marshal the XML for document [%s](%s) to [%s]",
							first.getSourcePath(), first.getId(), tgt), e);
						return;
					} finally {
						IOUtils.closeQuietly(out);
						if (!ok) {
							FileUtils.deleteQuietly(tgt);
						}
					}

					DocumentIndexT index = DocumentIndexT.class
						.cast(XmlImportDelegateFactory.this.xml.get(CmfType.DOCUMENT));
					for (DocumentVersionT v : l) {
						DocumentIndexEntryT idx = new DocumentIndexEntryT();
						idx.setId(v.getId());
						idx.setLocation(relativizeXmlLocation(tgt.getAbsolutePath()));
						idx.setName(v.getName());
						idx.setPath(v.getSourcePath());
						idx.setType(v.getType());
						idx.setVersion(v.getVersion());
						idx.setHistoryId(v.getHistoryId());
						idx.setCurrent(v.isCurrent());
						index.add(idx);
					}
				}
			} finally {
				// Some cleanup to facilitate garbage reclaiming
				l.clear();
				XmlImportDelegateFactory.this.documentBatches.remove(batchId);
			}
		}

		@Override
		public void objectTypeImportFinished(CmfType cmfType, Map<ImportResult, Integer> counters) {
			AggregatorBase<?> root = XmlImportDelegateFactory.this.xml.get(cmfType);
			if ((root == null) || (root.getCount() == 0)) {
				// If there is no aggregator, or it's empty, skip it
				return;
			}
			try {
				if (this.filesWritten == 0) {
					// Write the schema out with the first non-empty XML file
					writeSchema();
				}

				// There is an aggregator, so write out its file
				final File f = calculateConsolidatedFile(cmfType);
				final OutputStream out;
				try {
					out = new FileOutputStream(f);
				} catch (FileNotFoundException e) {
					this.log.error(String.format(
						"Failed to open the output file for the aggregate XML for type %s at [%s]", cmfType, f), e);
					return;
				}
				boolean ok = false;
				String xml = null;
				try {
					xml = XmlTools.marshal(root, XmlImportDelegateFactory.SCHEMA, true);
					try {
						IOUtils.write(xml, out);
					} catch (IOException e) {
						this.log.error(String.format("Failed to write the generated XML for type %s at [%s]:%n%s",
							cmfType, f, xml), e);
						return;
					}
					this.filesWritten++;
					ok = true;
				} catch (JAXBException e) {
					this.log.error(String.format("Failed to generate the XML for %s", cmfType), e);
				} finally {
					IOUtils.closeQuietly(out);
					if (!ok) {
						FileUtils.deleteQuietly(f);
					}
				}
			} finally {
				// Help the GC out
				root.clear();
			}
		}

		private void writeSchema() {
			final InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(XmlImportDelegateFactory.SCHEMA);
			if (in == null) {
				this.log.warn(
					String.format("Failed to load the schema from the resource '%s'", XmlImportDelegateFactory.SCHEMA));
				return;
			}
			final File schemaFile = new File(XmlImportDelegateFactory.this.db, XmlImportDelegateFactory.SCHEMA);
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(schemaFile);
			} catch (FileNotFoundException e) {
				IOUtils.closeQuietly(in);
				if (this.log.isTraceEnabled()) {
					this.log.warn(String.format("Failed to create the schema file at [%s]",
						XmlImportDelegateFactory.SCHEMA, schemaFile), e);
				} else {
					this.log.warn(String.format("Failed to create the schema file at [%s]: %s",
						XmlImportDelegateFactory.SCHEMA, schemaFile, e.getMessage()));
				}
			}

			try {
				IOUtils.copy(in, out);
			} catch (IOException e) {
				if (this.log.isTraceEnabled()) {
					this.log.warn(String.format("Failed to copy the schema into the file at [%s]",
						XmlImportDelegateFactory.SCHEMA, schemaFile), e);
				} else {
					this.log.warn(String.format("Failed to copy the schema into the file at [%s]: %s",
						XmlImportDelegateFactory.SCHEMA, schemaFile, e.getMessage()));
				}
			} finally {
				IOUtils.closeQuietly(out);
				IOUtils.closeQuietly(in);
			}
		}
	};

	public XmlImportDelegateFactory(XmlImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		engine.addListener(this.documentListener);
		String db = configuration.getString(XmlSessionFactory.DB);
		if (db != null) {
			this.db = new File(db).getCanonicalFile();
		} else {
			this.db = new File("cmsmf-xml").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.db);
		String content = configuration.getString(XmlSessionFactory.CONTENT);
		if (content != null) {
			this.content = new File(content).getCanonicalFile();
		} else {
			this.content = new File(db, "content").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.content);
		this.aggregateFolders = configuration.getBoolean(XmlSessionFactory.AGGREGATE_FOLDERS, false);
		this.aggregateDocuments = configuration.getBoolean(XmlSessionFactory.AGGREGATE_DOCUMENTS, false);

		Map<CmfType, AggregatorBase<?>> xml = new EnumMap<CmfType, AggregatorBase<?>>(CmfType.class);
		xml.put(CmfType.USER, new UsersT());
		xml.put(CmfType.GROUP, new GroupsT());
		xml.put(CmfType.ACL, new AclsT());
		xml.put(CmfType.TYPE, new TypesT());
		xml.put(CmfType.FORMAT, new FormatsT());
		xml.put(CmfType.FOLDER, (this.aggregateFolders ? new FoldersT() : new FolderIndexT()));
		xml.put(CmfType.DOCUMENT, (this.aggregateDocuments ? new DocumentsT() : new DocumentIndexT()));
		this.xml = xml;

		for (CmfType t : xml.keySet()) {
			File f = calculateConsolidatedFile(t);
			try {
				FileUtils.forceDelete(f);
			} catch (FileNotFoundException e) {
				// Wasn't there...no problem!
			} catch (IOException e) {
				this.log.warn(String.format("Failed to delete the aggregate XML file at [%s]", f.getAbsolutePath()), e);
			}
		}
	}

	String relativizeXmlLocation(String absolutePath) {
		String base = String.format("%s/", this.content.getAbsolutePath().replace(File.separatorChar, '/'));
		absolutePath = absolutePath.replace(File.separatorChar, '/');
		return absolutePath.substring(base.length());
	}

	protected void storeDocumentVersion(DocumentVersionT v) throws ImportException {
		List<DocumentVersionT> l = XmlImportDelegateFactory.this.documentBatches.get(v.getHistoryId());
		if (l == null) { throw new ImportException(
			String.format("Attempting to store version [%s] of history [%s], but no such history has been started: %s",
				v.getVersion(), v.getHistoryId(), v)); }
		l.add(v);
	}

	protected <T> T getXmlObject(CmfType t, Class<T> klazz) {
		return klazz.cast(this.xml.get(t));
	}

	@Override
	protected XmlImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case USER:
				return new XmlUserImportDelegate(this, storedObject);
			case GROUP:
				return new XmlGroupImportDelegate(this, storedObject);
			case ACL:
				return new XmlAclImportDelegate(this, storedObject);
			case TYPE:
				return new XmlTypeImportDelegate(this, storedObject);
			case FORMAT:
				return new XmlFormatImportDelegate(this, storedObject);
			case FOLDER:
				if (this.aggregateFolders) {
					return new XmlAggregateFoldersImportDelegate(this, storedObject);
				} else {
					return new XmlFolderImportDelegate(this, storedObject);
				}
			case DOCUMENT:
				return new XmlDocumentImportDelegate(this, storedObject);
			default:
				return null;
		}
	}

	protected File calculateConsolidatedFile(CmfType t) {
		return new File(this.db, String.format("%ss.xml", t.name().toLowerCase()));
	}

	@Override
	public void close() {
		super.close();
	}
}