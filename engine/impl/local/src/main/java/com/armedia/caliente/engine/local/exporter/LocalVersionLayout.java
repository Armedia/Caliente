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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.tools.VersionNumberScheme;
import com.armedia.commons.utilities.Tools;

public class LocalVersionLayout {

	protected static final class VersionInfo {
		private final Path path;
		private final Path radix;
		private final String historyId;
		private final String tag;

		VersionInfo(Path path, Path radix, String tag) {
			this.path = path;
			this.radix = radix;
			this.historyId = LocalCommon.calculateId(radix.toString());
			this.tag = (StringUtils.isBlank(tag) ? StringUtils.EMPTY : tag);
		}

		public Path getPath() {
			return this.path;
		}

		public Path getRadix() {
			return this.radix;
		}

		public String getHistoryId() {
			return this.historyId;
		}

		public String getTag() {
			return this.tag;
		}

		@Override
		public String toString() {
			return String.format("VersionInfo [path=%s, radix=%s, historyId=%s, tag=%s]", this.path, this.radix,
				this.historyId, this.tag);
		}
	}

	protected static final Predicate<Path> PATH_FALSE = (a) -> false;
	protected static final Function<Path, Path> IDENTITY = Function.identity();

	private final Function<Path, Path> converter;
	protected final VersionNumberScheme versionNumberScheme;

	public LocalVersionLayout(VersionNumberScheme numberScheme) {
		this(numberScheme, null);
	}

	public LocalVersionLayout(VersionNumberScheme numberScheme, Function<Path, Path> converter) {
		this.versionNumberScheme = Objects.requireNonNull(numberScheme,
			"Must provide a VersionNumberScheme to order tags with");
		this.converter = Tools.coalesce(converter, LocalVersionLayout.IDENTITY);
	}

	public VersionNumberScheme getVersionNumberScheme() {
		return this.versionNumberScheme;
	}

	protected final Predicate<VersionInfo> getSiblingCheck(final LocalFile baseFile) {
		return (p) -> isSibling(baseFile, p);
	}

	private boolean isSibling(LocalFile baseFile, VersionInfo candidate) {
		try {
			return (baseFile != null) && (candidate != null)
				&& (Files.isSameFile(baseFile.getAbsolute().toPath(), candidate.getPath())
					|| Objects.equals(baseFile.getHistoryId(), candidate.getHistoryId()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected Stream<Path> findSiblingCandidates(LocalRoot root, Path path) throws IOException {
		if (!path.isAbsolute()) {
			path = root.makeAbsolute(path);
		}
		final Path baseFolder = path.getParent();
		if (baseFolder == null) { return Stream.empty(); }
		return Files.list(baseFolder);
	}

	protected VersionInfo parseVersionInfo(LocalRoot root, Path p) {
		return null;
	}

	final LocalVersionHistory calculateFolderHistory(LocalRoot root, Path path) throws IOException {
		final Path truePath = root.makeAbsolute(path);
		Map<String, Integer> byPath = new HashMap<>();
		Map<String, Integer> byHistoryId = new HashMap<>();
		List<LocalFile> fullHistory = new ArrayList<>(1);
		VersionInfo thisInfo = new VersionInfo(truePath, root.relativize(truePath), "");
		byHistoryId.put("", 0);
		LocalFile lf = new LocalFile(root, thisInfo.getPath().toString(), thisInfo, true);
		byPath.put(lf.getFullPath(), 0);
		fullHistory.add(lf);
		byPath = Tools.freezeMap(byPath);
		fullHistory = Tools.freezeList(fullHistory);
		byHistoryId = Tools.freezeMap(byHistoryId);
		LocalFile rootVersion = lf;
		LocalFile currentVersion = lf;
		return new LocalVersionHistory(lf.getHistoryId(), rootVersion, currentVersion, byHistoryId, byPath,
			fullHistory);
	}

	final LocalVersionHistory calculateHistory(final LocalRoot root, final Path path) throws IOException {
		final Path truePath = root.makeAbsolute(path);
		if (Files.isDirectory(truePath)) { return calculateFolderHistory(root, path); }

		final VersionInfo info = parseVersionInfo(root, path);
		final String historyId = info.getHistoryId();
		final Map<String, VersionInfo> versions = new TreeMap<>(this.versionNumberScheme);
		final Collector<? super VersionInfo, ?, Map<String, VersionInfo>> collector = Collectors
			.toMap(VersionInfo::getTag, Function.identity(), (a, b) -> a, () -> versions);

		try (Stream<Path> candidates = findSiblingCandidates(root, path)) {
			candidates //
				.filter(Objects::nonNull) // Is the converted path non-null?
				.map(this.converter) // Do I need to swap to another file?
				.filter(Objects::nonNull) // Is the converted path non-null?
				.filter(Files::exists) // Does the converted path exist?
				.map((p) -> parseVersionInfo(root, p)) // parse the version info
				.filter(Objects::nonNull) // Again, avoid null values
				.filter((vi) -> historyId.equals(vi.getHistoryId())) // Same file or a sibling?
				.collect(collector) //
			//
			;
		} catch (UncheckedIOException e) {
			throw e.getCause();
		} catch (final IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(String.format("Failed to find the siblings for [%s]", path), t);
		}

		// Ok...we have the TreeMap containing the versions, properly organized from earliest
		// to latest, so now we convert that to indexes so we can have a properly ordered history
		Map<String, Integer> byPath = new HashMap<>();
		Map<String, Integer> byHistoryId = new HashMap<>();
		List<LocalFile> fullHistory = new ArrayList<>(versions.size());
		int i = 0;
		for (String tag : versions.keySet()) {
			VersionInfo thisInfo = versions.get(tag);
			byHistoryId.put(tag, i);
			LocalFile lf = new LocalFile(root, thisInfo.getPath().toString(), thisInfo, i == versions.size());
			byPath.put(lf.getFullPath(), i);
			fullHistory.add(lf);
			i++;
		}
		byPath = Tools.freezeMap(byPath);
		fullHistory = Tools.freezeList(fullHistory);
		byHistoryId = Tools.freezeMap(byHistoryId);
		LocalFile rootVersion = fullHistory.get(0);
		LocalFile currentVersion = fullHistory.get(fullHistory.size() - 1);
		return new LocalVersionHistory(historyId, rootVersion, currentVersion, byHistoryId, byPath, fullHistory);
	}
}