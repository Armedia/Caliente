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
package com.armedia.caliente.engine.xml.importer;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.importer.DefaultImportEngineListener;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.xml.common.XmlCommon;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.engine.xml.common.XmlSetting;
import com.armedia.caliente.engine.xml.importer.jaxb.AclsT;
import com.armedia.caliente.engine.xml.importer.jaxb.AggregatorBase;
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
import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.xml.XmlTools;

public class XmlImportDelegateFactory
	extends ImportDelegateFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine> {

	private static final String SCHEMA_NAME = "caliente-engine-xml.xsd";
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	static final Schema SCHEMA;

	static {
		try {
			SCHEMA = XmlTools.loadSchema(XmlImportDelegateFactory.SCHEMA_NAME);
		} catch (JAXBException e) {
			throw new RuntimeException(String.format("Failed to load the required schema resource [%s]",
				XmlImportDelegateFactory.SCHEMA_NAME));
		}
	}

	void marshalXml(Object target, OutputStream out) throws JAXBException {
		if (target == null) { throw new IllegalArgumentException("Must supply an object to marshal"); }
		if (out == null) {
			throw new IllegalArgumentException(String.format("Nowhere to write %s to", target.getClass().getName()));
		}

		Class<?> targetClass = target.getClass();
		Marshaller m = XmlTools.getContext(targetClass).createMarshaller();
		m.setSchema(XmlImportDelegateFactory.SCHEMA);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.setProperty(Marshaller.JAXB_ENCODING, this.encoding.name());
		m.marshal(target, out);
	}

	private final Map<CmfObject.Archetype, AggregatorBase<?>> xml;
	private final boolean aggregateFolders;
	private final boolean aggregateDocuments;
	private final Path content;
	private final Path metadataRoot;
	private final Charset encoding;
	private volatile boolean schemaMissing = true;
	private final CmfContentOrganizer organizer;

	private final ThreadLocal<List<DocumentVersionT>> threadedVersionList = ThreadLocal.withInitial(ArrayList::new);

	private final ImportEngineListener documentListener = new DefaultImportEngineListener() {

		@Override
		public void objectHistoryImportStarted(UUID jobId, CmfObject.Archetype objectType, String batchId, int count) {
			if (objectType != CmfObject.Archetype.DOCUMENT) { return; }
			XmlImportDelegateFactory.this.threadedVersionList.remove();
		}

		@Override
		public void objectHistoryImportFinished(UUID jobId, CmfObject.Archetype objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
			if (objectType != CmfObject.Archetype.DOCUMENT) { return; }
			if (failed) { return; }
			List<DocumentVersionT> l = XmlImportDelegateFactory.this.threadedVersionList.get();
			if (l.isEmpty()) { return; }

			try {
				DocumentT doc = new DocumentT();
				doc.getVersion().addAll(l);

				if (XmlImportDelegateFactory.this.aggregateDocuments) {
					DocumentsT.class.cast(XmlImportDelegateFactory.this.xml.get(CmfObject.Archetype.DOCUMENT)).add(doc);
				} else {
					// The content's location is in the last version
					DocumentIndexT index = DocumentIndexT.class
						.cast(XmlImportDelegateFactory.this.xml.get(CmfObject.Archetype.DOCUMENT));
					List<DocumentIndexVersionT> entries = new ArrayList<>(l.size());
					final DocumentVersionT last = l.get(l.size() - 1);
					for (DocumentVersionT v : l) {
						String contentPath = v.getContentPath();
						Path tgt = XmlImportDelegateFactory.this.metadataRoot.resolve(contentPath);
						Path dir = tgt.getParent();
						if (dir != null) {
							try {
								FileUtils.forceMkdir(dir.toFile());
							} catch (IOException e) {
								this.log.error("Failed to create the parent directory at [{}]", dir, e);
								return;
							}
						}
						boolean ok = false;
						try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tgt.toFile()))) {
							marshalXml(doc, out);
							ok = true;
						} catch (FileNotFoundException e) {
							this.log.error("Failed to open an output stream to [{}]", tgt, e);
							return;
						} catch (IOException e) {
							this.log.error("IOException raised while writing to [{}]", tgt, e);
							return;
						} catch (JAXBException e) {
							this.log.error("Failed to marshal the XML for version [{}] for history [{}]({}) to [{}]",
								v.getVersion(), v.getHistoryId(), v.getId(), tgt, e);
							return;
						} finally {
							if (!ok) {
								FileUtils.deleteQuietly(tgt.toFile());
							}
						}

						DocumentIndexVersionT idx = new DocumentIndexVersionT();
						idx.setId(v.getId());
						idx.setLocation(relativizeXmlLocation(tgt));
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
					entry.setHistoryId(last.getHistoryId());
					entry.add(entries);
					index.add(entry);
				}
			} catch (Throwable t) {
				this.log.error("Exception caught while closing out batch ID [{}]", batchId, t);
			} finally {
				// Some cleanup to facilitate garbage reclaiming
				l.clear();
			}
		}

		@Override
		public void objectTypeImportFinished(UUID jobId, CmfObject.Archetype archetype,
			Map<ImportResult, Long> counters) {
			AggregatorBase<?> root = XmlImportDelegateFactory.this.xml.get(archetype);
			if ((root == null) || (root.getCount() == 0)) {
				// If there is no aggregator, or it's empty, skip it
				return;
			}
			try {
				// Write the schema out with the first non-empty XML file
				writeSchema();

				// There is an aggregator, so write out its file
				final Path p = calculateConsolidatedFile(archetype);
				boolean ok = false;
				try (OutputStream out = new BufferedOutputStream(new FileOutputStream(p.toFile()))) {
					marshalXml(root, out);
					ok = true;
				} catch (FileNotFoundException e) {
					this.log.error("Failed to open the output file for the aggregate XML for type {} at [{}]",
						archetype, p, e);
				} catch (IOException e) {
					this.log.error("IOException raised while writing the aggregate XML for type {} at [{}]", archetype,
						p, e);
				} catch (JAXBException e) {
					this.log.error("Failed to generate the XML for {}", archetype, e);
				} finally {
					if (!ok) {
						FileUtils.deleteQuietly(p.toFile());
					}
				}
			} finally {
				// Help the GC out
				root.clear();
			}
		}

		private void writeSchema() {
			try (SharedAutoLock shared = sharedAutoLock()) {
				if (!XmlImportDelegateFactory.this.schemaMissing) { return; }
				try (MutexAutoLock mutex = shared.upgrade()) {
					if (!XmlImportDelegateFactory.this.schemaMissing) { return; }
					try (InputStream in = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream(XmlImportDelegateFactory.SCHEMA_NAME)) {
						if (in == null) {
							this.log.warn("Failed to load the schema from the resource [{}]",
								XmlImportDelegateFactory.SCHEMA_NAME);
							return;
						}
						final Path schemaFile = XmlImportDelegateFactory.this.metadataRoot
							.resolve(XmlImportDelegateFactory.SCHEMA_NAME);

						try (OutputStream out = new BufferedOutputStream(new FileOutputStream(schemaFile.toFile()))) {
							IOUtils.copy(in, out);
							XmlImportDelegateFactory.this.schemaMissing = false;
						} catch (FileNotFoundException e) {
							if (this.log.isTraceEnabled()) {
								this.log.warn("Failed to create the schema file at [{}]", schemaFile, e);
							} else {
								this.log.warn("Failed to create the schema file at [{}]: {}", schemaFile,
									e.getMessage());
							}
						} catch (IOException e) {
							if (this.log.isTraceEnabled()) {
								this.log.warn("Failed to copy the schema into the file at [{}]", schemaFile, e);
							} else {
								this.log.warn("Failed to copy the schema into the file at [{}]: {}", schemaFile,
									e.getMessage());
							}
						}
					} catch (IOException e) {
						this.log.warn(XmlImportDelegateFactory.SCHEMA_NAME);
					}
				}
			}
		}
	};

	public XmlImportDelegateFactory(XmlImportEngine engine, CfgTools configuration)
		throws ImportException, IOException {
		super(engine, configuration);
		engine.addListener(this.documentListener);

		CmfContentStore<?, ?> contentStore = engine.getContentStore();
		if (contentStore.isSupportsFileAccess()) {
			// This is where the content is stored...
			this.content = contentStore.getRootLocation().toPath();
		} else {
			// It has no local storage, so we "make up" our own root...
			String root = configuration.getString(XmlSetting.ROOT);
			this.content = Tools.canonicalize(Paths.get(root, "streams"));
		}

		FileUtils.forceMkdir(this.content.toFile());
		this.metadataRoot = XmlCommon.getMetadataRoot(this.content);
		FileUtils.forceMkdir(this.metadataRoot.toFile());

		this.aggregateFolders = configuration.getBoolean(XmlSetting.AGGREGATE_FOLDERS);
		this.aggregateDocuments = configuration.getBoolean(XmlSetting.AGGREGATE_DOCUMENTS);

		final String encodingName = configuration.getString(XmlSetting.ENCODING);
		try {
			this.encoding = Charset.forName(encodingName);
		} catch (Exception e) {
			throw new ImportException(String.format("Illegal encoding name given [%s]", encodingName), e);
		}

		Map<CmfObject.Archetype, AggregatorBase<?>> xml = new EnumMap<>(CmfObject.Archetype.class);
		xml.put(CmfObject.Archetype.USER, new UsersT());
		xml.put(CmfObject.Archetype.GROUP, new GroupsT());
		xml.put(CmfObject.Archetype.ACL, new AclsT());
		xml.put(CmfObject.Archetype.TYPE, new TypesT());
		xml.put(CmfObject.Archetype.FORMAT, new FormatsT());
		xml.put(CmfObject.Archetype.FOLDER, (this.aggregateFolders ? new FoldersT() : new FolderIndexT()));
		xml.put(CmfObject.Archetype.DOCUMENT, (this.aggregateDocuments ? new DocumentsT() : new DocumentIndexT()));
		this.xml = xml;

		for (CmfObject.Archetype t : xml.keySet()) {
			Path p = calculateConsolidatedFile(t);
			try {
				FileUtils.forceDelete(p.toFile());
			} catch (FileNotFoundException e) {
				// Wasn't there...no problem!
			} catch (IOException e) {
				this.log.warn("Failed to delete the aggregate XML file at [{}]", p, e);
			}
		}

		String organizerName = configuration.getString(XmlSetting.ORGANIZER);
		if (StringUtils.isNotBlank(organizerName)) {
			this.organizer = CmfContentOrganizer.getOrganizer(organizerName);
			this.organizer.configure(configuration);
		} else {
			this.organizer = null;
		}
	}

	String relativizeXmlLocation(Path absolutePath) {
		return this.metadataRoot.relativize(absolutePath).toString();
	}

	String relativizeContentLocation(Path absolutePath) {
		return this.content.relativize(absolutePath).toString();
	}

	protected void storeDocumentVersion(DocumentVersionT v) throws ImportException {
		List<DocumentVersionT> l = this.threadedVersionList.get();
		DocumentT doc = new DocumentT();
		doc.getVersion().add(v);
		try {
			marshalXml(doc, NullOutputStream.NULL_OUTPUT_STREAM);
		} catch (JAXBException e) {
			throw new ImportException(
				String.format("Attempting to store version [%s] of history [%s], a marshalling error ocurred: %s",
					v.getVersion(), v.getHistoryId(), v),
				e);
		}
		l.add(v);
	}

	protected String renderAttributeName(String attributeName) {
		if (StringUtils.isBlank(attributeName)) {
			throw new IllegalArgumentException("Must provide a non-blank attribute name");
		}
		return String.format("caliente:%s", attributeName);
	}

	protected <T> T getXmlObject(CmfObject.Archetype t, Class<T> klazz) {
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

	protected Path calculateConsolidatedFile(CmfObject.Archetype t) {
		return this.metadataRoot.resolve(String.format("index.%ss.xml", t.name().toLowerCase()));
	}

	protected String renderXmlPath(XmlImportContext ctx, CmfObject<CmfValue> object) {
		final CmfContentStream stream = new CmfContentStream(object, 0);
		// If we have no organizer, we organize identically to the content store's approach
		if (this.organizer == null) { return ctx.getContentStore().renderContentPath(object, stream); }

		// If we do have an organizer, we use that instead
		CmfContentOrganizer.Location location = this.organizer.getLocation(object.getTranslator(), object, stream);
		List<String> path = new LinkedList<>(location.containerSpec);
		path.add(location.baseName);
		return FileNameTools.reconstitute(path, false, false, '/');
	}
}