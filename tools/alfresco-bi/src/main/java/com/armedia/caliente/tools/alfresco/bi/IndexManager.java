package com.armedia.caliente.tools.alfresco.bi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Spliterator;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.tools.Closer;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItemVersion;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.StreamTools;

public class IndexManager {

	private static final Path METADATA_ROOT = Paths.get("alfresco-bulk-import");
	private static final Path MODEL_DIRECTORY = IndexManager.METADATA_ROOT.resolve("content-models");
	private static final Path MANIFEST = IndexManager.METADATA_ROOT.resolve("CALIENTE_INGESTION_INDEX.txt");
	private static final Path FILE_INDEX = IndexManager.METADATA_ROOT.resolve("scan.files.xml");
	private static final Path FOLDER_INDEX = IndexManager.METADATA_ROOT.resolve("scan.folders.xml");

	private static final File getIndexFile(Path rootDir, Path indexFile) throws IOException {
		final Path p = rootDir.resolve(indexFile);
		File f = p.toFile();
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// Do nothing...
		} finally {
			f = f.getAbsoluteFile();
		}

		if (!f.exists()) { throw new FileNotFoundException(f.getAbsolutePath()); }
		if (!f.isFile()) {
			throw new IOException(String.format("The path [%s] is not a regular file", f.getAbsolutePath()));
		}
		if (!f.canRead()) { throw new AccessDeniedException(f.getAbsolutePath()); }

		return f;
	}

	public static final Path getBulkImportRoot(final Path baseDirectory) {
		return (baseDirectory != null ? baseDirectory.resolve(IndexManager.METADATA_ROOT) : IndexManager.METADATA_ROOT);
	}

	public static final Path getIndexFilePath(final Path baseDirectory, boolean directoryMode) {
		Path biRoot = IndexManager.getBulkImportRoot(baseDirectory);
		return biRoot.resolve(directoryMode ? IndexManager.FILE_INDEX : IndexManager.FOLDER_INDEX);
	}

	public static final Path getManifestPath(final Path baseDirectory) {
		return (baseDirectory != null ? baseDirectory.resolve(IndexManager.MANIFEST) : IndexManager.MANIFEST);
	}

	public static final Writer openManifestWriter(final Path baseDirectory, Charset encoding, boolean createDirectories)
		throws IOException {
		Path biRoot = IndexManager.getBulkImportRoot(baseDirectory);
		Path manifest = biRoot.resolve(IndexManager.MANIFEST);
		File f = manifest.toFile();
		if (createDirectories) {
			FileUtils.forceMkdir(biRoot.toFile());
		}
		if (encoding == null) { return new FileWriter(f); }
		OutputStream fos = new FileOutputStream(f);
		return new OutputStreamWriter(fos, encoding);
	}

	public static final Writer openManifestWriter(final Path baseDirectory, boolean createDirectories)
		throws IOException {
		return IndexManager.openManifestWriter(baseDirectory, null, createDirectories);
	}

	public static final Writer openManifestWriter(final Path baseDirectory, Charset charset) throws IOException {
		return IndexManager.openManifestWriter(baseDirectory, charset, false);
	}

	public static final Reader openManifestReader(final Path baseDirectory, Charset encoding) throws IOException {
		Path biRoot = IndexManager.getBulkImportRoot(baseDirectory);
		Path manifest = biRoot.resolve(IndexManager.MANIFEST);
		File f = manifest.toFile();
		if (encoding == null) { return new FileReader(f); }
		InputStream fos = new FileInputStream(f);
		return new InputStreamReader(fos, encoding);
	}

	public static final Reader openManifestReader(final Path baseDirectory) throws IOException {
		return IndexManager.openManifestReader(baseDirectory, null);
	}

	public static final Path getContentModelsPath(final Path baseDirectory) {
		Path biRoot = IndexManager.getBulkImportRoot(baseDirectory);
		return biRoot.resolve(IndexManager.MODEL_DIRECTORY);
	}

	public static final Stream<ScanIndexItem> scanItems(final Path rootDirectory, boolean directoryMode)
		throws IOException, JAXBException, XMLStreamException {
		final Path cachePath = (directoryMode ? IndexManager.FILE_INDEX : IndexManager.FOLDER_INDEX);
		File xmlFile = IndexManager.getIndexFile(rootDirectory, cachePath);
		if (xmlFile == null) {
			xmlFile = IndexManager.getIndexFile(rootDirectory, cachePath.getFileName());
			if (xmlFile == null) { return Stream.empty(); }
		}

		final InputStream in = new FileInputStream(xmlFile);
		final XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(in);
		if (xml.nextTag() != XMLStreamConstants.START_ELEMENT) {
			// Empty document or no proper root tag?!?!?
			try {
				return Stream.empty();
			} finally {
				// Clean up resources...
				xml.close();
			}
		}

		final Unmarshaller u = JAXBContext.newInstance(ScanIndexItem.class, ScanIndexItemVersion.class)
			.createUnmarshaller();

		CloseableIterator<ScanIndexItem> it = new CloseableIterator<ScanIndexItem>() {
			@Override
			protected CloseableIterator<ScanIndexItem>.Result findNext() throws Exception {
				while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
					final String elementName = xml.getLocalName();
					if (!elementName.equals("item")) {
						// Bad element...skip it!
						continue;
					}

					JAXBElement<ScanIndexItem> xmlItem = u.unmarshal(xml, ScanIndexItem.class);
					if (xmlItem != null) {
						final ScanIndexItem item = xmlItem.getValue();
						if (item != null) { return found(item); }
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
		return StreamTools.of(it, Spliterator.IMMUTABLE | Spliterator.NONNULL).onClose(it::close);
	}
}