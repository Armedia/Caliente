package com.armedia.caliente.tools.alfresco.bi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.Closer;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndex;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItemVersion;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.xml.XmlTools;

public final class BulkImportManager {

	private static Path BULK_IMPORT_ROOT = Paths.get("alfresco-bulk-import");
	private static Path CONTENT_MODEL_DIRECTORY = BulkImportManager.BULK_IMPORT_ROOT.resolve("content-models");
	private static Path INGESTION_MANIFEST = BulkImportManager.BULK_IMPORT_ROOT.resolve("CALIENTE_INGESTION_INDEX.txt");

	private static Path FILE_INDEX = BulkImportManager.BULK_IMPORT_ROOT.resolve("scan.files.xml");
	private static Path FOLDER_INDEX = BulkImportManager.BULK_IMPORT_ROOT.resolve("scan.folders.xml");

	private static final String METADATA_SUFFIX = ".xml";

	private final Path basePath;
	private final Path contentPath;
	private final Path unfiledPath;
	private final Path bulkImportRoot;
	private final Path modelDirectory;
	private final Path manifest;
	private final List<Path> fileIndexes;
	private final List<Path> folderIndexes;

	public BulkImportManager(Path basePath) throws IOException {
		this(basePath, null, null);
	}

	public BulkImportManager(Path basePath, Path contentPath) throws IOException {
		this(basePath, contentPath, null);
	}

	public BulkImportManager(Path basePath, String unfiledPath) throws IOException {
		this(basePath, null, unfiledPath);
	}

	public BulkImportManager(Path basePath, Path contentPath, String unfiledPath) throws IOException {
		this.basePath = Objects.requireNonNull(basePath).toRealPath();
		if (contentPath == null) {
			contentPath = this.basePath;
		} else {
			contentPath = contentPath.toRealPath();
		}
		this.contentPath = contentPath;

		this.bulkImportRoot = basePath.resolve(BulkImportManager.BULK_IMPORT_ROOT);
		this.modelDirectory = this.basePath.resolve(BulkImportManager.CONTENT_MODEL_DIRECTORY);
		this.manifest = this.basePath.resolve(BulkImportManager.INGESTION_MANIFEST);

		if (!StringUtils.isEmpty(unfiledPath)) {
			Path p = null;
			for (String s : Tools.splitEscaped('/', unfiledPath)) {
				if (p == null) {
					p = Paths.get(s);
				} else {
					p = p.resolve(s);
				}
			}
			this.unfiledPath = p;
		} else {
			this.unfiledPath = null;
		}

		List<Path> l = new ArrayList<>(2);
		l.add(this.basePath.resolve(BulkImportManager.FILE_INDEX));
		// For historical compatibility
		l.add(this.basePath.resolve(BulkImportManager.FILE_INDEX.getFileName()));
		this.fileIndexes = Tools.freezeList(l);
		l = new ArrayList<>(2);
		l.add(this.basePath.resolve(BulkImportManager.FOLDER_INDEX));
		// For historical compatibility
		l.add(this.basePath.resolve(BulkImportManager.FOLDER_INDEX.getFileName()));
		this.folderIndexes = Tools.freezeList(l);
	}

	private Path relativize(Path path) {
		return this.basePath.relativize(path);
	}

	private File toFile(Path indexFile) throws IOException {
		File f = indexFile.toFile();
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// Do nothing...
		} finally {
			f = f.getAbsoluteFile();
		}

		if (!f.exists() || !f.isFile() || !f.canRead()) { return null; }

		return f;
	}

	private Path resolve(String childPath) {
		if (StringUtils.isEmpty(childPath)) { return this.basePath; }
		// Split by forward slashes... this may be running on Windows!
		Path path = this.basePath;
		for (String s : Tools.splitEscapedStream('/', childPath).collect(Collectors.toCollection(ArrayList::new))) {
			path = path.resolve(s);
		}
		return path;
	}

	public Path getBasePath() {
		return this.basePath;
	}

	public Path getContentPath() {
		return this.contentPath;
	}

	public Path getBulkImportRoot() {
		return this.bulkImportRoot;
	}

	public Path getIndexFilePath(boolean directoryMode) {
		return (directoryMode ? this.folderIndexes : this.fileIndexes).get(0);
	}

	public Path getManifestPath(boolean relative) {
		if (!relative) { return this.manifest; }
		return this.basePath.relativize(this.manifest);
	}

	public Path getUnfiledPath() {
		return this.unfiledPath;
	}

	public Path getContentModelsPath() {
		return this.modelDirectory;
	}

	public Path resolveContentPath(ScanIndexItemVersion version) {
		return resolve(version.getContent());
	}

	// For now these are identical but they might change
	public Path calculateContentPath(Path actualPath) {
		return relativize(actualPath);
	}

	public Path getArtificialFolderPath(String actualPath) {
		return this.bulkImportRoot.resolve(actualPath);
	}

	public Path resolveMetadataPath(ScanIndexItemVersion version) {
		return resolve(version.getMetadata());
	}

	// For now these are identical but they might change
	public Path calculateMetadataPath(Path fullContentPath) {
		if (this.contentPath == null) {
			throw new IllegalStateException("No content path was set at manager creation");
		}
		// Calculate the path relative to the content root
		Path[] candidates = {
			this.contentPath, this.bulkImportRoot
		};
		Path metadataPath = null;
		for (Path p : candidates) {
			if (fullContentPath.startsWith(p)) {
				metadataPath = p.relativize(fullContentPath);
				break;
			}
		}
		// resolve this relative path relative to biRoot
		if (metadataPath != null) {
			metadataPath = this.bulkImportRoot.resolve(metadataPath);
		} else {
			metadataPath = fullContentPath;
		}
		return metadataPath
			.resolveSibling(String.format("%s%s", metadataPath.getFileName(), BulkImportManager.METADATA_SUFFIX));
	}

	public Writer openManifestWriter(Charset encoding, boolean createDirectories) throws IOException {
		Path manifest = getManifestPath(false);
		File f = manifest.toFile();
		if (createDirectories) {
			FileUtils.forceMkdir(f.getParentFile());
		}
		if (encoding == null) { return new FileWriter(f); }
		OutputStream fos = new FileOutputStream(f);
		return new OutputStreamWriter(fos, encoding);
	}

	public Writer openManifestWriter(boolean createDirectories) throws IOException {
		return openManifestWriter(null, createDirectories);
	}

	public Writer openManifestWriter(Charset charset) throws IOException {
		return openManifestWriter(charset, false);
	}

	public Reader openManifestReader(Charset encoding) throws IOException {
		Path manifest = this.bulkImportRoot.resolve(BulkImportManager.INGESTION_MANIFEST);
		File f = manifest.toFile();
		if (encoding == null) { return new FileReader(f); }
		InputStream fos = new FileInputStream(f);
		return new InputStreamReader(fos, encoding);
	}

	public Reader openManifestReader() throws IOException {
		return openManifestReader(null);
	}

	public Stream<ScanIndexItem> scanItems(final boolean directoryMode)
		throws IOException, JAXBException, XMLStreamException {
		File xmlFile = null;
		for (Path p : (directoryMode ? this.folderIndexes : this.fileIndexes)) {
			xmlFile = toFile(p);
			if (xmlFile != null) {
				break;
			}
		}
		if (xmlFile == null) { return Stream.empty(); }

		final InputStream in = new FileInputStream(xmlFile);
		final XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(in);
		if ((xml.nextTag() != XMLStreamConstants.START_ELEMENT) || !StringUtils.equals("scan", xml.getLocalName())) {
			// Empty document or no proper root tag?!?!?
			try {
				return Stream.empty();
			} finally {
				// Clean up resources...
				xml.close();
			}
		}

		final Unmarshaller u = XmlTools.getUnmarshaller(ScanIndex.class, ScanIndexItem.class,
			ScanIndexItemVersion.class);

		final CloseableIterator<ScanIndexItem> it = new CloseableIterator<ScanIndexItem>() {
			@Override
			protected CloseableIterator<ScanIndexItem>.Result findNext() throws Exception {
				while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
					final String elementName = xml.getLocalName();
					if (!elementName.equals("item")) {
						// This is garbage that can't be parsed, so skip it!
						XmlTools.skipBranch(xml);
						continue;
					}
					JAXBElement<ScanIndexItem> xmlItem = u.unmarshal(xml, ScanIndexItem.class);
					if (xmlItem != null) {
						final ScanIndexItem item = xmlItem.getValue();
						if (item.isDirectory() != directoryMode) {
							// Only return items of the type we're interested in
							continue;
						}
						return found(item);
					}
				}
				return null;
			}

			@Override
			protected void doClose() throws Exception {
				try {
					xml.close();
				} finally {
					Closer.closeQuietly(in);
				}
			}
		};
		return it.stream(Spliterator.IMMUTABLE | Spliterator.NONNULL);
	}
}