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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionWrapper;
import com.armedia.caliente.engine.alfresco.bi.AlfSetting;
import com.armedia.caliente.engine.alfresco.bi.AlfXmlIndex;
import com.armedia.caliente.engine.alfresco.bi.importer.ScanIndexItemMarker.MarkerType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dynamic.DynamicElementException;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.tools.PathTools;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.alfresco.bi.BulkImportManager;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndex;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItemVersion;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableSet;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.xml.XmlTools;

public class AlfImportDelegateFactory
	extends ImportDelegateFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine> {

	private final class VirtualDocument {
		private final String historyId;
		private ScanIndexItemMarker root = null;
		private final Map<String, ScanIndexItemMarker> versions = new TreeMap<>();
		private final Map<String, List<ScanIndexItemMarker>> members = new TreeMap<>();

		private VirtualDocument(String historyId) {
			this.historyId = historyId;
		}

		public void serialize() throws Exception {
			if (!isComplete()) {
				throw new Exception("This virtual document is not complete - can't serialize it yet");
			}

			synchronized (AlfImportDelegateFactory.this.folderIndex) {
				AlfImportDelegateFactory.this.folderIndex.marshal(this.root.getItem());
				for (String v : this.versions.keySet()) {
					AlfImportDelegateFactory.this.folderIndex.marshal(this.versions.get(v).getItem());
				}
			}

			synchronized (AlfImportDelegateFactory.this.fileIndex) {
				for (String v : this.members.keySet()) {
					for (ScanIndexItemMarker marker : this.members.get(v)) {
						(marker.isDirectory() ? AlfImportDelegateFactory.this.folderIndex
							: AlfImportDelegateFactory.this.fileIndex).marshal(marker.getItem());
					}
				}
			}
			clear();
		}

		public void clear() {
			this.root = null;
			this.versions.clear();
			this.members.clear();
		}

		public boolean isComplete() {
			if (this.root == null) { return false; }
			if (this.versions.isEmpty()) { return false; }
			if (this.versions.size() != this.members.size()) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("VirtualDocument [historyId=%s, root=%s, versions=%s, members=%s]", this.historyId,
				this.root, this.versions, this.members);
		}
	}

	private final class HistoryTracker {
		private final List<ScanIndexItemMarker> versions = new ArrayList<>();
		private final CmfObject<?> object;

		private HistoryTracker(CmfObject<?> object) {
			this.object = object;
		}

		private void reset() {
			this.versions.clear();
			AlfImportDelegateFactory.this.history.remove(this.object.getHistoryId());
		}
	}

	private static final BigDecimal LAST_INDEX = new BigDecimal(Long.MAX_VALUE);

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

	private final Path contentRoot;
	private final Path metadataRoot;
	private final BulkImportManager biManager;
	private final boolean renderManifest;

	private final Properties userLoginMap = new Properties();

	protected final AlfrescoSchema schema;
	private final Map<String, AlfrescoType> defaultTypes;

	private final ConcurrentMap<String, VirtualDocument> vdocs = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, HistoryTracker> history = new ConcurrentHashMap<>();

	private final AlfXmlIndex fileIndex;
	private final AlfXmlIndex folderIndex;
	private final AtomicBoolean manifestSerialized = new AtomicBoolean(false);
	private final AtomicReference<Boolean> initializedVdocs = new AtomicReference<>(null);

	private final Set<String> artificialFolders = new ShareableSet<>(new LinkedHashSet<>());

	public AlfImportDelegateFactory(AlfImportEngine engine, CfgTools configuration)
		throws IOException, JAXBException, XMLStreamException, DynamicElementException {
		super(engine, configuration);
		this.metadataRoot = engine.getBaseData();
		this.biManager = engine.getBulkImportManager();
		this.schema = engine.getSchema();
		this.defaultTypes = engine.getDefaultTypes();
		this.renderManifest = configuration.getBoolean(AlfSetting.GENERATE_INGESTION_INDEX);

		FileUtils.forceMkdir(this.biManager.getContentPath().toFile());
		this.contentRoot = this.biManager.getContentPath();

		final File modelDir = this.biManager.getContentModelsPath().toFile();
		FileUtils.forceMkdir(modelDir);

		Class<?>[] idxClasses = {
			ScanIndex.class, ScanIndexItem.class, ScanIndexItemVersion.class
		};
		this.fileIndex = new AlfXmlIndex(this.biManager.getIndexFilePath(false).toFile(), idxClasses);
		this.folderIndex = new AlfXmlIndex(this.biManager.getIndexFilePath(true).toFile(), idxClasses);

		if (!configuration.hasValue(AlfSetting.CONTENT_MODEL)) {
			throw new IllegalStateException("Must provide a valid set of content model XML files");
		}

		String pfx = configuration.getString(AlfSetting.RESIDUALS_PREFIX);
		pfx = StringUtils.strip(pfx);
		if (StringUtils.isEmpty(pfx)) {
			pfx = null;
		}
	}

	protected AlfrescoSchema getSchema() {
		return this.schema;
	}

	protected AlfrescoType getType(String name, String... aspects) {
		if ((aspects == null) || (aspects.length == 0)) { return this.defaultTypes.get(name); }
		return this.schema.buildType(name, aspects);
	}

	protected AlfrescoType getType(String name, Collection<String> aspects) {
		if ((aspects == null) || aspects.isEmpty()) { return this.defaultTypes.get(name); }
		return this.schema.buildType(name, aspects);
	}

	boolean initializeVdocSupport() {
		try (SharedAutoLock shared = sharedAutoLock()) {
			Boolean result = this.initializedVdocs.get();
			if (result == null) {
				try (MutexAutoLock mutex = shared.upgrade()) {
					result = this.initializedVdocs.get();
					if (result == null) {
						boolean ok = false;
						try {
							getType("cm:folder", "dctm:vdocRoot");
							getType("cm:folder", "dctm:vdocVersion");
							getType("dctm:vdocReference");
							ok = true;
						} catch (Exception e) {
							this.log.warn("VDoc support has been disabled", e);
						} finally {
							// Make sure we only do this once
							this.initializedVdocs.set(result = ok);
						}
					}
				}
			}
			return result;
		}
	}

	@Override
	protected AlfImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case USER:
				return new AlfImportUserDelegate(this, storedObject);
			case FOLDER:
				return new AlfImportFolderDelegate(this, storedObject);
			case DOCUMENT:
				// TODO: How to determine the minor counter
				return new AlfImportDocumentDelegate(this, storedObject);
			default:
				break;
		}
		return null;
	}

	boolean mapUserLogin(String userName, String login) {
		// Only add the mapping if the username is different from the login
		if ((userName == null) || Objects.equals(userName, login)) { return false; }
		if (login == null) {
			this.userLoginMap.remove(userName);
		} else {
			this.userLoginMap.setProperty(userName, login);
		}
		return true;
	}

	static final File normalizeAbsolute(File f) {
		if (f == null) { return null; }
		f = f.getAbsoluteFile();
		f = new File(FilenameUtils.normalize(f.getAbsolutePath()));
		return f.getAbsoluteFile();
	}

	private final void storeManifestToScanIndex() throws ImportException {
		if (!this.renderManifest || !this.manifestSerialized.compareAndSet(false, true)) {
			// This will only happen once
			return;
		}

		final Path contentPath = this.biManager.getManifestPath(true);
		ScanIndexItemMarker thisMarker = new ScanIndexItemMarker();
		thisMarker.setDirectory(false);
		thisMarker.setContent(contentPath);
		thisMarker.setMetadata(null);
		thisMarker.setSourcePath(contentPath.getParent());
		thisMarker.setSourceName(contentPath.getFileName().toString());
		thisMarker.setTargetPath("");
		thisMarker.setTargetName(contentPath.getFileName().toString());
		thisMarker.setNumber(BigDecimal.ONE);

		final ScanIndexItem item = thisMarker.getItem();
		try {
			this.fileIndex.marshal(item);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to serialize the file to XML: %s", item), e);
		}
	}

	protected Path getContentRelativePath(File contentFile) {
		return this.contentRoot.relativize(contentFile.toPath());
	}

	protected String getFinalName(final AlfImportContext ctx, CmfObject<CmfValue> cmfObject) {
		String finalName = null;

		if (StringUtils.isEmpty(finalName)) {
			CmfValue unfiled = getPropertyValue(cmfObject, IntermediateProperty.IS_UNFILED);
			if ((unfiled != null) && unfiled.isNotNull() && unfiled.asBoolean()) {
				// This helps protect against duplicate object names
				finalName = getUnfiledName(ctx, cmfObject);
			}
		}

		if (StringUtils.isEmpty(finalName)) {
			CmfValue fixedName = getPropertyValue(cmfObject, IntermediateProperty.FIXED_NAME);
			if ((fixedName != null) && fixedName.isNotNull()) {
				finalName = fixedName.asString();
			}
		}

		if (StringUtils.isEmpty(finalName)) {
			CmfObject<CmfValue> head = cmfObject;
			try {
				head = ctx.getHeadObject(cmfObject);
			} catch (CmfStorageException e) {
				this.log.warn("Failed to load the HEAD object for {} batch [{}]", cmfObject.getType().name(),
					cmfObject.getHistoryId(), e);
			}
			finalName = ctx.getObjectName(head);
		}

		if (StringUtils.isEmpty(finalName)) {
			finalName = cmfObject.getName();
		}

		return finalName;
	}

	protected final ScanIndexItemMarker generateItemMarker(final AlfImportContext ctx, final boolean folder,
		final CmfObject<CmfValue> cmfObject, CmfContentStream content, File contentFile, File metadataFile,
		MarkerType type) throws ImportException {

		String targetPath = getFixedPath(cmfObject, ctx);
		// If the path is null, we must skip this object
		if (targetPath == null) { return null; }

		final int head;
		final int count;
		final int current;
		String renditionRootPath = null;
		switch (type) {
			case RENDITION_ROOT:
				// Fall-through
			case RENDITION_TYPE:
				// Fall-through
			case RENDITION_ENTRY:
				renditionRootPath = String.format("%s-renditions", cmfObject.getId());
				// Fall-through

			case NORMAL:
			default:
				CmfProperty<CmfValue> vCounter = cmfObject.getProperty(IntermediateProperty.VERSION_COUNT);
				CmfProperty<CmfValue> vHeadIndex = cmfObject.getProperty(IntermediateProperty.VERSION_HEAD_INDEX);
				CmfProperty<CmfValue> vIndex = cmfObject.getProperty(IntermediateProperty.VERSION_INDEX);
				// These 3 must either all be present and have values, or none of them...
				if (((vCounter == null) || !vCounter.hasValues()) && //
					((vHeadIndex == null) || !vHeadIndex.hasValues()) && //
					((vIndex == null) || !vIndex.hasValues())) {
					// If none of them have values...
					head = count = current = 1;
				} else if ((vCounter != null) && vCounter.hasValues()
					&& ((vHeadIndex != null) && vHeadIndex.hasValues() && (vIndex != null) && vIndex.hasValues())) {
					// If all of them have values...
					head = vHeadIndex.getValue().asInteger();
					count = vCounter.getValue().asInteger();
					current = vIndex.getValue().asInteger();
				} else {
					// Only some have values, so only be lenient for directories...
					if (!folder) {
						// ERROR: insufficient data
						throw new ImportException(
							String.format("Incomplete version indexes found for %s", cmfObject.getDescription()));
					}
					// It's OK for directories...everything is 1
					head = count = current = 1;
				}
				break;
		}

		final boolean headVersion = (head == current);
		final boolean lastVersion = (count == current);

		// We don't use canonicalize() here because we want to be able to respect symlinks if
		// for whatever reason they need to be employed
		contentFile = AlfImportDelegateFactory.normalizeAbsolute(contentFile);
		metadataFile = AlfImportDelegateFactory.normalizeAbsolute(metadataFile);

		// basePath is the base path within which the entire import resides
		final Path relativeContentPath = getContentRelativePath(contentFile);
		final Path relativeMetadataPath = (metadataFile != null ? this.metadataRoot.relativize(metadataFile.toPath())
			: null);
		Path relativeMetadataParent = (metadataFile != null ? relativeMetadataPath.getParent() : null);
		if (relativeMetadataParent == null) {
			// if there's no metadata file, we fall back to the content file's parent...
			relativeMetadataParent = (relativeContentPath != null ? relativeContentPath.getParent() : null);
		}

		ScanIndexItemMarker thisMarker = new ScanIndexItemMarker();
		thisMarker.setDirectory(folder);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setMetadata(relativeMetadataPath);
		thisMarker.setSourcePath(relativeMetadataParent != null ? relativeMetadataParent : Paths.get(""));
		thisMarker.setSourceName(contentFile.getName());

		BigDecimal number = AlfImportDelegateFactory.LAST_INDEX;
		CmfProperty<CmfValue> versionCount = cmfObject.getProperty(IntermediateProperty.VERSION_COUNT);
		if ((versionCount != null) && versionCount.hasValues()) {
			number = new BigDecimal(versionCount.getValue().asLong());
		}
		if (!headVersion || !lastVersion) {
			number = new BigDecimal(current);
		}
		thisMarker.setNumber(number);

		final boolean unfiled;
		{
			CmfProperty<CmfValue> unfiledProp = cmfObject.getProperty(IntermediateProperty.IS_UNFILED);
			unfiled = (unfiledProp != null) && unfiledProp.hasValues() && unfiledProp.getValue().asBoolean();
		}

		if (unfiled) {
			List<String> paths = new ArrayList<>();
			CmfAttribute<CmfValue> unfiledFolderAtt = cmfObject.getAttribute(IntermediateAttribute.UNFILED_FOLDER);
			if ((unfiledFolderAtt != null) && unfiledFolderAtt.hasValues()) {
				for (CmfValue v : unfiledFolderAtt) {
					if ((v != null) && !v.isNull()) {
						paths.add(v.asString());
					}
				}
			} else {
				this.biManager.getUnfiledPath().forEach((p) -> paths.add(p.toString()));
				PathTools.addNumericPaths(paths, cmfObject.getNumber());
			}
			targetPath = Tools.joinEscaped('/', paths);
			storeArtificialFolderToIndex(targetPath);
		}

		final String renditionTypeStr;
		{
			String typeStr = content.getRenditionIdentifier();
			if (!StringUtils.isBlank(content.getModifier())) {
				typeStr = String.format("%s(%s)", typeStr, content.getModifier());
			}
			renditionTypeStr = typeStr;
		}

		// This is the base name, others may change it...
		thisMarker.setTargetName(getFinalName(ctx, cmfObject));
		switch (type) {
			case NORMAL:
				break;

			case RENDITION_ROOT:
				// Special case: the target name must be ${objectId}-renditions
				thisMarker.setTargetName(String.format("%s-renditions", cmfObject.getId()));
				thisMarker.setDirectory(true);
				thisMarker.setContent(null);
				break;

			case RENDITION_TYPE:
				targetPath = String.format("%s/%s", targetPath, renditionRootPath);
				thisMarker.setTargetName(renditionTypeStr);
				thisMarker.setDirectory(true);
				thisMarker.setContent(null);
				break;

			case RENDITION_ENTRY:
				// Add the rendition root path and type path
				targetPath = String.format("%s/%s/%s", targetPath, renditionRootPath, renditionTypeStr);
				thisMarker.setTargetName(
					String.format("%s.%s[%08x]", cmfObject.getId(), renditionTypeStr, content.getRenditionPage()));
				break;

			default:
				break;
		}

		if (unfiled) {
			thisMarker.setTargetName(getUnfiledName(ctx, cmfObject));
		}
		thisMarker.setTargetPath(targetPath);
		thisMarker.setIndex(current);
		thisMarker.setHeadIndex(head);
		thisMarker.setVersionCount(count);
		return thisMarker;
	}

	final File generateMetadataFile(final Properties p, final CmfObject<CmfValue> cmfObject, final File main)
		throws ImportException {
		Path target = this.biManager.calculateMetadataPath(main.toPath());

		final File meta = target.toFile();
		if (p == null) { return meta; }

		try {
			FileUtils.forceMkdirParent(meta);
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to create the directory at [%s]", target), e);
		}

		try (OutputStream out = new FileOutputStream(meta)) {
			XmlProperties.saveToXML(p, out, String.format("Properties for %s", cmfObject.getDescription()));
		} catch (FileNotFoundException e) {
			throw new ImportException(
				String.format("Failed to open the properties file at [%s]", main.getAbsolutePath()), e);
		} catch (IOException e) {
			meta.delete();
			throw new ImportException(
				String.format("Failed to write to the properties file at [%s]", main.getAbsolutePath()), e);
		}
		return meta;
	}

	protected final void storeArtificialFolderToIndex(String artificialFolder) throws ImportException {
		artificialFolder = FilenameUtils.normalize(artificialFolder, true);
		if (StringUtils.isEmpty(artificialFolder)) { return; }
		while (!Objects.equals(".", artificialFolder)) {
			this.artificialFolders.add(artificialFolder);
			artificialFolder = FileNameTools.dirname(artificialFolder, '/');
		}
	}

	private final void serializeArtificialFolders() throws XMLStreamException {
		if (this.artificialFolders.isEmpty()) { return; }
		this.folderIndex.writeComment(" Begin the artificial folders for unfiled objects ");
		final AtomicLong number = new AtomicLong(0);
		// Make sure we order them so they are sorted hierarchically
		for (final String mf : new TreeSet<>(this.artificialFolders)) {
			final File baseFile;
			{
				List<String> paths = new ArrayList<>();
				String name = PathTools.addNumericPaths(paths, number.getAndIncrement());
				name = String.format("folder-patch.%s", name);
				// Concatenate them into a path
				String path = FileNameTools.reconstitute(paths, false, false);
				// Now, create a file for them
				Path basePath = this.biManager.getArtificialFolderPath(path);
				baseFile = AlfImportDelegateFactory.normalizeAbsolute(basePath.resolve(name).toFile());
			}

			// Make sure nothing is there...
			if (baseFile.exists()) {
				try {
					FileUtils.forceDelete(baseFile);
				} catch (IOException e) {
					this.log.warn("Failed to delete the artificial folder [{}]", baseFile.getAbsolutePath(), e);
				}
			}

			// Generate the numeric base paths...

			final boolean root = (mf.indexOf('/') < 0);

			final String parent;
			final String name;
			if (root) {
				parent = "";
				name = mf;
			} else {
				parent = FileNameTools.dirname(mf, '/');
				name = FileNameTools.basename(mf, '/');
			}

			final ScanIndexItemMarker thisMarker = generateMissingFolderMarker(parent, name, baseFile);
			ScanIndexItem item = thisMarker.getItem();
			try {
				FileUtils.forceMkdir(baseFile);
				this.folderIndex.marshal(item);
			} catch (Exception e) {
				this.log.warn("Failed to create/serialize the artificial FOLDER to XML: {}", item, e);
			}
		}
		this.folderIndex.writeComment("  End the artificial folders for unfiled objects  ");
	}

	protected final ScanIndexItemMarker generateMissingFolderMarker(final String targetPath, final String folderName,
		File contentFile) {
		// We don't use canonicalize() here because we want to be able to respect symlinks if
		// for whatever reason they need to be employed
		contentFile = AlfImportDelegateFactory.normalizeAbsolute(contentFile);
		// basePath is the base path within which the entire import resides
		final Path relativeContentPath = this.biManager.calculateContentPath(contentFile.toPath());
		final Path relativeContentParent = (relativeContentPath != null ? relativeContentPath.getParent() : null);

		ScanIndexItemMarker thisMarker = new ScanIndexItemMarker();
		thisMarker.setDirectory(true);
		thisMarker.setContent(relativeContentPath);
		thisMarker.setSourcePath(relativeContentParent != null ? relativeContentParent : Paths.get(""));
		thisMarker.setSourceName(contentFile.getName());
		thisMarker.setNumber(AlfImportDelegateFactory.LAST_INDEX);
		thisMarker.setTargetName(folderName);
		thisMarker.setTargetPath(targetPath);
		thisMarker.setIndex(1);
		thisMarker.setHeadIndex(1);
		thisMarker.setVersionCount(1);
		return thisMarker;
	}

	protected final HistoryTracker getHistoryTracker(CmfObject<?> object) {
		return ConcurrentUtils.createIfAbsentUnchecked(this.history, object.getHistoryId(),
			() -> new HistoryTracker(object));
	}

	protected final void resetHistory(CmfObject<?> object) {
		HistoryTracker history = this.history.remove(object.getHistoryId());
		if (history != null) {
			history.reset();
		}
	}

	protected final void storeToIndex(final AlfImportContext ctx, final boolean folder,
		final CmfObject<CmfValue> cmfObject, CmfContentStream content, File contentFile, File metadataFile,
		MarkerType type) throws ImportException {

		storeManifestToScanIndex();

		final ScanIndexItemMarker thisMarker = generateItemMarker(ctx, folder, cmfObject, content, contentFile,
			metadataFile, type);

		// If this object must be skipped, skip it
		if (thisMarker == null) { return; }

		HistoryTracker history = null;
		switch (type) {
			case RENDITION_ROOT:
			case RENDITION_TYPE:
			case RENDITION_ENTRY:
				history = new HistoryTracker(cmfObject);
				break;

			case NORMAL:
				history = getHistoryTracker(cmfObject);
				if (!StringUtils.equals(history.object.getHistoryId(), cmfObject.getHistoryId())) {
					throw new ImportException(String.format(
						"History mixup!! Attempted to add %s [%s](%s) with history ID [%s] to history for %s [%s](%s) with history ID [%s]",
						cmfObject.getType(), cmfObject.getLabel(), cmfObject.getId(), cmfObject.getHistoryId(),
						history.object.getType(), history.object.getLabel(), history.object.getId(),
						history.object.getHistoryId()));
				}
				break;

			default:
				break;
		}

		history.versions.add(thisMarker);

		if (!thisMarker.isLastVersion()) {
			// more versions to come, so we simply keep going...
			// can't output the XML element just yet...
			return;
		}

		ScanIndexItemMarker headMarker = thisMarker;
		if (!thisMarker.isHeadVersion()) {
			// This is used when the head version and the last version are not the same,
			// thus a copy must be made and the version number adjusted so it's the last
			// version number (i.e. version count + 1) since we're appending a version
			// at the end of the version history
			headMarker = history.versions.get(thisMarker.getHeadIndex() - 1);
			headMarker = headMarker.clone();
			CmfProperty<CmfValue> versionCount = cmfObject.getProperty(IntermediateProperty.VERSION_COUNT);
			BigDecimal number = headMarker.getNumber();
			if ((versionCount != null) && versionCount.hasValues()) {
				number = new BigDecimal(versionCount.getValue().asLong() + 1);
			}
			headMarker.setNumber(number);
			headMarker.setContent(headMarker.getContent());
			headMarker.setMetadata(headMarker.getMetadata());
			history.versions.add(headMarker);
		}

		ScanIndexItem item = headMarker.getItem(history.versions);
		try {
			(item.isDirectory() ? this.folderIndex : this.fileIndex).marshal(item);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to serialize the %s to XML: %s", item.isDirectory() ? "folder" : "file", item),
				e);
		} finally {
			history.reset();
		}
	}

	final String getUnfiledName(final AlfImportContext ctx, final CmfObject<CmfValue> cmfObject) {
		// Generate a unique name using the history ID and the object's given name
		return String.format("%s-%s", cmfObject.getHistoryId(), ctx.getObjectName(cmfObject));
	}

	@Override
	public void close() {
		for (VirtualDocument vdoc : this.vdocs.values()) {
			try {
				vdoc.serialize();
			} catch (Exception e) {
				// This should never happen, but we still look out for it
				this.log.warn("Failed to marshal the VDoc XML for [{}]", vdoc, e);
			}
		}

		for (String historyId : this.history.keySet()) {
			HistoryTracker tracker = this.history.get(historyId);
			this.log.error("Unclosed history {} from {} [{}]({}) with {} entries", historyId, tracker.object.getType(),
				tracker.object.getLabel(), tracker.object.getId(), tracker.versions.size());
		}

		this.fileIndex.close();
		// Make sure we do this last
		try {
			serializeArtificialFolders();
		} catch (Exception e) {
			this.log.warn("Failed to serialize the artificial folders", e);
		}
		this.folderIndex.close();
		super.close();
	}
}