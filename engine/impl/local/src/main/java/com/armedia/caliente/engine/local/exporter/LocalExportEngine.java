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
package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionFactory;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.common.LocalTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.alfresco.bi.BulkImportManager;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.StreamTools;

public class LocalExportEngine extends
	ExportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportContextFactory, LocalExportDelegateFactory, LocalExportEngineFactory> {

	public LocalExportEngine(LocalExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, false, SearchType.PATH);
	}

	@Override
	protected SearchType detectSearchType(String source) {
		if (source == null) { return null; }
		if (source.startsWith("@")) {
			// If the file exists and is a regular file, this is to be treated as a scan index
			Path p = new File(source.substring(1)).toPath();
			if (Files.exists(p) && Files.isRegularFile(p)) { return SearchType.QUERY; }
		}
		return SearchType.PATH;
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(LocalRoot session, CfgTools configuration,
		LocalExportDelegateFactory factory, String indexFile) throws Exception {
		// Skip the @ at the beginning...
		indexFile = indexFile.substring(1);
		Path path = new File(indexFile).toPath();
		Predicate<ScanIndexItem> p = ScanIndexItem::isDirectory;
		Stream<ScanIndexItem> directories = BulkImportManager.scanItems(path, p);
		Stream<ScanIndexItem> files = BulkImportManager.scanItems(path, p.negate());
		return Stream.concat(directories, files).flatMap(this::getExportTargets).filter(Objects::nonNull);
	}

	protected Stream<ExportTarget> getExportTargets(ScanIndexItem item) {
		String path = item.getSourcePath();
		String name = item.getSourceName();
		if (!StringUtils.isEmpty(path)) {
			name = path + "/" + name;
		}
		// Convert to a local path?
		final String portablePath = LocalCommon.getPortablePath(name);
		final Archetype type = (item.isDirectory() ? Archetype.FOLDER : Archetype.DOCUMENT);

		// The path from the XML is ALWAYS separated with forward slashes
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(name, '/')) {
			try {
				r.add(LocalFile.makeSafe(s));
			} catch (IOException e) {
				throw new UncheckedIOException(String.format("Failed to make safe the string [%s]", s), e);
			}
		}
		return Stream.of(new ExportTarget(type, LocalCommon.calculateId(portablePath),
			FileNameTools.reconstitute(r, false, false, '/')));
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(LocalRoot session, CfgTools configuration,
		LocalExportDelegateFactory factory, String searchKey) throws Exception {
		throw new Exception("Local Export doesn't support ID-based searches");
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(final LocalRoot session, CfgTools configuration,
		LocalExportDelegateFactory factory, String unused) throws Exception {
		File f = session.getFile();
		if (!f.exists()) {
			throw new FileNotFoundException(String.format("Failed to find a file or folder at [%s]", f));
		}
		if (f.isDirectory()) {
			return StreamTools
				.of(new LocalRecursiveIterator(session, configuration.getBoolean(LocalSetting.IGNORE_EMPTY_FOLDERS)))
				.map((p) -> toExportTarget(session, p));
		}

		return Stream.of(toExportTarget(session, f.toPath()));
	}

	protected static boolean isEmptyDirectory(Path p) {
		if (!Files.isDirectory(p)) { return false; }
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
			for (Path e : stream) {
				if (e == null) {
					// Just to get rid of a warning without annotations ;)
					continue;
				}
				return false;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
		return true;
	}

	protected ExportTarget toExportTarget(LocalRoot root, Path path) {
		final LocalFile localFile;
		try {
			localFile = getLocalFile(root, path.toString());
		} catch (IOException e) {
			throw new UncheckedIOException(
				String.format("Failed to calculate the path for [%s] relative to [%s]", path, root.getPath()), e);
		}
		final Archetype archetype = localFile.getAbsolute().isFile() ? CmfObject.Archetype.DOCUMENT
			: CmfObject.Archetype.FOLDER;
		return new ExportTarget(archetype, localFile.getId(), localFile.getSafePath());
	}

	@Override
	protected String findFolderName(LocalRoot session, String folderId, Object ecmObject) {
		LocalFile localFile = LocalFile.class.cast(ecmObject);
		String path = localFile.getPortableFullPath();
		if (!localFile.isFolder()) {
			// Remove the last component of the path
			path = FileNameTools.dirname(path, '/');
		}

		while (path.length() > 1) {
			String id = LocalCommon.calculateId(path);
			if (StringUtils.equals(id, folderId)) { return FileNameTools.basename(path, '/'); }
			// Move up one level...
			path = FileNameTools.dirname(path, '/');
		}
		return null;
	}

	protected LocalFile getLocalFile(LocalRoot session, String path) throws IOException {
		return LocalFile.getInstance(session, path.toString());
	}

	@Override
	protected CmfValue getValue(CmfValue.Type type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new LocalTranslator();
	}

	@Override
	protected LocalSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new LocalSessionFactory(cfg, crypto);
	}

	@Override
	protected LocalExportContextFactory newContextFactory(LocalRoot session, CfgTools cfg,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new LocalExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected LocalExportDelegateFactory newDelegateFactory(LocalRoot session, CfgTools cfg) throws Exception {
		return new LocalExportDelegateFactory(this, cfg);
	}
}