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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionFactory;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.local.common.LocalTranslator;
import com.armedia.caliente.engine.tools.PathTools;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.VersionNumberScheme;
import com.armedia.caliente.tools.alfresco.bi.BulkImportManager;
import com.armedia.caliente.tools.alfresco.bi.xml.ScanIndexItem;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;

public class LocalExportEngine extends
	ExportEngine<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportContextFactory, LocalExportDelegateFactory, LocalExportEngineFactory> {

	public static final String VERSION_SCHEME_NUMERIC = "num";
	public static final String VERSION_SCHEME_ALPHABETIC = "alpha";
	public static final String VERSION_SCHEME_ALPHANUMERIC = "alnum";
	public static final Set<String> VERSION_SCHEMES = Tools.freezeSet(new LinkedHashSet<>(new TreeSet<>(Arrays.asList( //
		LocalExportEngine.VERSION_SCHEME_NUMERIC, //
		LocalExportEngine.VERSION_SCHEME_ALPHABETIC, //
		LocalExportEngine.VERSION_SCHEME_ALPHANUMERIC //
	))));

	private static final Pattern QUERY_PREFIX_PARSER = Pattern.compile("^@([^:]+):(.+)$");

	// FLAT (a/b.v1, a/b.v2, a/b)
	public static final String VERSION_LAYOUT_FLAT = "flat";
	// HIERARCHICAL (a/b/v1[/stream] a/b/v2[/stream] a/b/v3[/stream])
	public static final String VERSION_LAYOUT_HIERARCHICAL = "hierarchical";
	private static final String VERSION_LAYOUT_DEFAULT = LocalExportEngine.VERSION_LAYOUT_FLAT;
	public static final Set<String> VERSION_LAYOUTS = Tools.freezeSet(new LinkedHashSet<>(new TreeSet<>(Arrays.asList( //
		LocalExportEngine.VERSION_LAYOUT_FLAT, //
		LocalExportEngine.VERSION_LAYOUT_HIERARCHICAL //
	))));

	private static enum QueryMode {
		//
		SCANXML, //
		JDBC, //
		//
		;
	}

	private interface LocalQuery extends Callable<Stream<ExportTarget>> {
	}

	private final class ScanXMLQuery implements LocalQuery {

		private final Path specFile;

		private ScanXMLQuery(Path specFile) {
			this.specFile = specFile;
		}

		@Override
		public Stream<ExportTarget> call() throws Exception {
			Predicate<ScanIndexItem> p = ScanIndexItem::isDirectory;
			Stream<ScanIndexItem> directories = BulkImportManager.scanItems(this.specFile, p);
			Stream<ScanIndexItem> files = BulkImportManager.scanItems(this.specFile, p.negate());
			return Stream.concat(directories, files).flatMap(LocalExportEngine.this::getExportTargets)
				.filter(Objects::nonNull);
		}
	}

	private final class JDBCQuery implements LocalQuery {
		private JDBCQuery(CfgTools configuration, Path specFile) throws Exception {
		}

		@Override
		public Stream<ExportTarget> call() throws Exception {
			// TODO: Not implemented yet
			return Stream.empty();
		}
	}

	private final LocalRoot root;
	private final LocalVersionLayout versionLayout;
	private final LocalVersionHistoryCache histories;

	public LocalExportEngine(LocalExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings)
		throws ExportException {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, false, SearchType.PATH);

		try {
			this.root = LocalCommon.getLocalRoot(settings);
		} catch (IOException e) {
			throw new ExportException("Failed to construct the root session", e);
		}

		final VersionNumberScheme scheme;
		final String schemeName = settings.getString(LocalSetting.VERSION_SCHEME);
		final String tagSeparator = settings.getString(LocalSetting.VERSION_TAG_SEPARATOR);
		if (!StringUtils.isBlank(schemeName)) {
			final boolean emptyIsRoot = Tools.coalesce(settings.getBoolean(LocalSetting.VERSION_SCHEME_EMPTY_IS_ROOT),
				Boolean.FALSE);
			// TODO: Make the separator configurable?
			final char sep = '.';
			switch (schemeName.toLowerCase()) {
				case VERSION_SCHEME_ALPHABETIC:
					scheme = VersionNumberScheme.getAlphabetic(sep, emptyIsRoot);
					break;
				case VERSION_SCHEME_ALPHANUMERIC:
					scheme = VersionNumberScheme.getAlphanumeric(null, emptyIsRoot);
					break;
				case VERSION_SCHEME_NUMERIC:
					scheme = VersionNumberScheme.getNumeric(sep, emptyIsRoot);
					break;
				default:
					throw new ExportException(
						String.format("Support for version scheme [%s] is not yet implemented", schemeName));
			}
		} else {
			scheme = null;
		}

		final String layoutName = Tools.coalesce(settings.getString(LocalSetting.VERSION_LAYOUT),
			LocalExportEngine.VERSION_LAYOUT_DEFAULT);
		// final String layoutStreamName =
		// settings.getString(LocalSetting.VERSION_LAYOUT_STREAM_NAME);
		switch (layoutName.toLowerCase()) {
			case VERSION_LAYOUT_HIERARCHICAL:
				// TODO: Implement this
			case VERSION_LAYOUT_FLAT:
				this.versionLayout = new SimpleVersionLayout(scheme, tagSeparator);
				break;

			default:
				throw new ExportException(
					String.format("Support for version scheme [%s] is not yet implemented", layoutName));
		}

		this.histories = new LocalVersionHistoryCache(this.root, this.versionLayout);
	}

	protected LocalRoot getRoot() {
		return this.root;
	}

	public LocalVersionLayout getVersionLayout() {
		return this.versionLayout;
	}

	public LocalVersionHistory getHistory(Path p) throws ExportException {
		try {
			return this.histories.getVersionHistory(p);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the version history for Path [%s]", p), e);
		}
	}

	public LocalVersionHistory getHistory(LocalFile f) throws ExportException {
		try {
			return this.histories.getVersionHistory(f);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the version history for ID [%s] (radix = [%s])",
				f.getHistoryId(), f.getHistoryRadix()), e);
		}
	}

	@Override
	protected SearchType detectSearchType(String source) {
		if (source == null) { return null; }
		Matcher m = LocalExportEngine.QUERY_PREFIX_PARSER.matcher(source);
		if (m.matches()) { return SearchType.QUERY; }
		return SearchType.PATH;
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(LocalRoot session, CfgTools configuration, String querySpec)
		throws Exception {
		Matcher m = LocalExportEngine.QUERY_PREFIX_PARSER.matcher(querySpec);
		if (!m.matches()) {
			throw new ExportException(
				"The string spec [" + querySpec + "] was identified as a query, but is not valid as one");
		}

		final String type = m.group(1);
		final String indexFile = m.group(2);

		final QueryMode mode;
		try {
			mode = QueryMode.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new ExportException("Unknown query mode [" + type + "] from query file spec [" + querySpec + "]");
		}

		final Path path = new File(indexFile).toPath();
		if (!Files.exists(path)) {
			throw new ExportException("No file found at [" + path + "] to execute the " + mode.name() + " query");
		}
		if (!Files.isRegularFile(path)) {
			throw new ExportException(
				"The path [" + path + "] is not a regular file for the " + mode.name() + " query");
		}
		if (!Files.isReadable(path)) {
			throw new ExportException("The file at [" + path + "] is not readable for the " + mode.name() + " query");
		}

		LocalQuery executor = null;
		switch (mode) {
			case JDBC:
				executor = new JDBCQuery(configuration, path);
				break;

			case SCANXML:
				executor = new ScanXMLQuery(path);
				break;

			default:
				break;
		}

		if (executor == null) { throw new Exception("Query type " + mode.name() + " is not implemented yet"); }

		return executor.call();
	}

	protected Stream<ExportTarget> getExportTargets(ScanIndexItem item) {
		String path = item.getSourcePath();
		String name = item.getSourceName();
		if (!StringUtils.isEmpty(path)) {
			name = path + "/" + name;
		}
		// Convert to a local path?
		final Archetype type = (item.isDirectory() ? Archetype.FOLDER : Archetype.DOCUMENT);

		// The path from the XML is ALWAYS separated with forward slashes
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(name, '/')) {
			r.add(PathTools.makeSafe(s));
		}
		return Stream.of(
			new ExportTarget(type, LocalCommon.calculateId(name), FileNameTools.reconstitute(r, false, false, '/')));
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(LocalRoot session, CfgTools configuration,
		String searchKey) throws Exception {
		throw new Exception("Local Export doesn't support ID-based searches");
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(final LocalRoot root, CfgTools configuration, String unused)
		throws Exception {
		Path p = root.getPath();
		if (!Files.exists(p)) {
			throw new FileNotFoundException(String.format("Failed to find a file or folder at [%s]", p));
		}
		final Stream<ExportTarget> stream;
		if (Files.isDirectory(p)) {
			stream = StreamTools //
				.of(new LocalRecursiveIterator(root, configuration.getBoolean(LocalSetting.IGNORE_EMPTY_FOLDERS))) //
				.filter(Files::exists) // Only include files/links that aren't broken
				.map(this::toExportTarget) // Convert to export targets
			;
		} else {
			stream = Stream.of(toExportTarget(p));
		}

		return stream.filter(Objects::nonNull);
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

	protected ExportTarget toExportTarget(Path path) {
		return LocalFile.toExportTarget(this.root, path);
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

	protected LocalFile getLocalFile(String path) throws IOException {
		return this.histories.getLocalFile(Paths.get(path));
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