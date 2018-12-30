package com.armedia.caliente.engine.xml.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import com.armedia.caliente.engine.importer.DefaultImportEngineListener;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.schema.SchemaServiceException;
import com.armedia.caliente.engine.importer.schema.SchemaService;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.engine.xml.common.XmlSetting;
import com.armedia.caliente.engine.xml.importer.jaxb.AclsT;
import com.armedia.caliente.engine.xml.importer.jaxb.AggregatorBase;
import com.armedia.caliente.engine.xml.importer.jaxb.ContentStreamT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentIndexEntryT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentIndexT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentIndexVersionT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentVersionT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentsT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderIndexT;
import com.armedia.caliente.engine.xml.importer.jaxb.FoldersT;
import com.armedia.caliente.engine.xml.importer.jaxb.FormatsT;
import com.armedia.caliente.engine.xml.importer.jaxb.GroupsT;
import com.armedia.caliente.engine.xml.importer.jaxb.TypesT;
import com.armedia.caliente.engine.xml.importer.jaxb.UsersT;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.XmlTools;

public class XmlImportDelegateFactory
	extends ImportDelegateFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine> {

	private static final String SCHEMA_NAME = "caliente-engine-xml.xsd";

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(XmlImportDelegateFactory.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(String.format("Failed to load the required schema resource [%s]",
				XmlImportDelegateFactory.SCHEMA_NAME));
		}
	}

	static void marshalXml(Object target, OutputStream out) throws JAXBException {
		if (target == null) { throw new IllegalArgumentException("Must supply an object to marshal"); }
		if (out == null) { throw new IllegalArgumentException(
			String.format("Nowhere to write %s to", target.getClass().getName())); }

		Class<?> targetClass = target.getClass();
		Marshaller m = JAXBContext.newInstance(targetClass).createMarshaller();
		m.setSchema(XmlImportDelegateFactory.SCHEMA);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.marshal(target, out);
	}

	private final Map<CmfType, AggregatorBase<?>> xml;
	private final boolean aggregateFolders;
	private final boolean aggregateDocuments;
	private final File db;
	private final File content;

	private final ThreadLocal<List<DocumentVersionT>> threadedVersionList = new ThreadLocal<>();

	private final ImportEngineListener documentListener = new DefaultImportEngineListener() {

		private int filesWritten = 0;

		@Override
		public void objectHistoryImportStarted(UUID jobId, CmfType objectType, String batchId, int count) {
			if (objectType != CmfType.DOCUMENT) { return; }
			List<DocumentVersionT> l = XmlImportDelegateFactory.this.threadedVersionList.get();
			if (l == null) {
				l = new ArrayList<>();
				XmlImportDelegateFactory.this.threadedVersionList.set(l);
			}
			l.clear();
		}

		@Override
		public void objectHistoryImportFinished(UUID jobId, CmfType objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
			if (objectType != CmfType.DOCUMENT) { return; }
			if (failed) { return; }
			List<DocumentVersionT> l = XmlImportDelegateFactory.this.threadedVersionList.get();
			if ((l == null) || l.isEmpty()) { return; }

			try {
				DocumentT doc = new DocumentT();
				doc.getVersion().addAll(l);

				if (XmlImportDelegateFactory.this.aggregateDocuments) {
					DocumentsT.class.cast(XmlImportDelegateFactory.this.xml.get(CmfType.DOCUMENT)).add(doc);
				} else {
					// The content's location is in the first version
					DocumentVersionT first = l.get(0);
					ContentStreamT content = first.getContents().get(0);
					File tgt = new File(XmlImportDelegateFactory.this.content, content.getLocation());
					File dir = tgt.getParentFile();
					if (dir != null) {
						try {
							FileUtils.forceMkdir(dir);
						} catch (IOException e) {
							this.log.error(String.format("Failed to create the parent directory at [%s]", dir), e);
							return;
						}
					}
					tgt = new File(dir, String.format("%s-document.xml", tgt.getName()));

					final OutputStream out;
					try {
						out = new FileOutputStream(tgt);
					} catch (FileNotFoundException e) {
						this.log.error(String.format("Failed to open an output stream to [%s]", tgt), e);
						return;
					}

					boolean ok = false;
					try {
						XmlImportDelegateFactory.marshalXml(doc, out);
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
					List<DocumentIndexVersionT> entries = new ArrayList<>(l.size());
					for (DocumentVersionT v : l) {
						DocumentIndexVersionT idx = new DocumentIndexVersionT();
						idx.setId(v.getId());
						idx.setLocation(relativizeXmlLocation(tgt.getAbsolutePath()));
						idx.setName(v.getName());
						idx.setPath(v.getSourcePath());
						idx.setType(v.getType());
						idx.setVersion(v.getVersion());
						idx.setHistoryId(v.getHistoryId());
						idx.setCurrent(v.isCurrent());
						idx.setFormat(v.getFormat());
						long size = 0;
						if (!v.getContents().isEmpty()) {
							size = v.getContents().get(0).getSize();
						}
						idx.setSize(size);
						entries.add(idx);
					}
					// Make sure all entries go in one group, to ensure they're always together
					DocumentIndexEntryT entry = new DocumentIndexEntryT();
					entry.setHistoryId(first.getHistoryId());
					entry.add(entries);
					index.add(entry);
				}
			} catch (Throwable t) {
				this.log.error(String.format("Exception caught while closing out batch ID [%s]", batchId), t);
			} finally {
				// Some cleanup to facilitate garbage reclaiming
				l.clear();
			}
		}

		@Override
		public void objectTypeImportFinished(UUID jobId, CmfType cmfType, Map<ImportResult, Long> counters) {
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
				try {
					XmlImportDelegateFactory.marshalXml(root, out);
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
			} catch (Throwable t) {
				this.log.error(String.format("Exception caught while closing out objects of type [%s]", cmfType), t);
			} finally {
				// Help the GC out
				root.clear();
			}
		}

		private void writeSchema() {
			final InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(XmlImportDelegateFactory.SCHEMA_NAME);
			if (in == null) {
				this.log.warn(
					String.format("Failed to load the schema from the resource '%s'", XmlImportDelegateFactory.SCHEMA));
				return;
			}
			final File schemaFile = new File(XmlImportDelegateFactory.this.db, XmlImportDelegateFactory.SCHEMA_NAME);
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
		String db = configuration.getString(XmlSetting.DB);
		if (db != null) {
			this.db = new File(db).getCanonicalFile();
		} else {
			this.db = new File("caliente-data").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.db);
		String content = configuration.getString(XmlSetting.CONTENT);
		if (content != null) {
			this.content = new File(content).getCanonicalFile();
		} else {
			this.content = new File(db, "content").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.content);
		this.aggregateFolders = configuration.getBoolean(XmlSetting.AGGREGATE_FOLDERS);
		this.aggregateDocuments = configuration.getBoolean(XmlSetting.AGGREGATE_DOCUMENTS);

		Map<CmfType, AggregatorBase<?>> xml = new EnumMap<>(CmfType.class);
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
		List<DocumentVersionT> l = this.threadedVersionList.get();
		if (l == null) { throw new ImportException(
			String.format("Attempting to store version [%s] of history [%s], but no such history has been started: %s",
				v.getVersion(), v.getHistoryId(), v)); }
		DocumentT doc = new DocumentT();
		doc.getVersion().add(v);
		try {
			XmlImportDelegateFactory.marshalXml(doc, new NullOutputStream());
		} catch (JAXBException e) {
			throw new ImportException(
				String.format("Attempting to store version [%s] of history [%s], a marshalling error ocurred: %s",
					v.getVersion(), v.getHistoryId(), v),
				e);
		}
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
	protected SchemaService newSchemaService(XmlRoot session) throws SchemaServiceException {
		return null;
	}

	@Override
	public void close() {
		super.close();
	}
}
