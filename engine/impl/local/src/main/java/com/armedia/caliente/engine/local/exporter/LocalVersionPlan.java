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

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.tools.VersionNumberScheme;
import com.armedia.commons.utilities.Tools;

public class LocalVersionPlan {

	protected static final class VersionInfo {
		private final Path path;
		private final Path radix;
		private final String historyId;
		private final String tag;

		public VersionInfo(Path path, Path radix, String tag) {
			this.path = path;
			this.radix = radix;
			this.historyId = LocalCommon.calculateId(radix.toString());
			this.tag = tag;
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
	}

	protected static final Predicate<Path> PATH_FALSE = (a) -> false;
	protected static final Function<Path, Path> IDENTITY = Function.identity();

	private final Function<Path, Path> converter;
	protected final VersionNumberScheme versionNumberScheme;
	protected final LocalRoot root;

	public LocalVersionPlan(LocalRoot root, VersionNumberScheme numberScheme) {
		this(root, numberScheme, null);
	}

	public LocalVersionPlan(LocalRoot root, VersionNumberScheme numberScheme, Function<Path, Path> converter) {
		this.versionNumberScheme = Objects.requireNonNull(numberScheme,
			"Must provide a VersionNumberScheme to order tags with");
		this.converter = Tools.coalesce(converter, LocalVersionPlan.IDENTITY);
		this.root = Objects.requireNonNull(root, "Must provide a LocalRoot instance");
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

	protected Stream<Path> findSiblingCandidates(LocalFile baseFile) throws IOException {
		final Path baseFolder = baseFile.getAbsolute().getParentFile().toPath();
		return Files.list(baseFolder);
	}

	protected VersionInfo parseVersionInfo(Path p) {
		return null;
	}

	public final LocalVersionHistory findHistory(final LocalRoot root, final LocalFile baseFile) throws IOException {
		String historyId = baseFile.getHistoryId();
		final Map<String, LocalFile> versions = new TreeMap<>(this.versionNumberScheme);
		final Function<VersionInfo, LocalFile> constructor = (vi) -> {
			try {
				return LocalFile.getInstance(baseFile.getRootPath(), vi.getPath().toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		Collector<? super LocalFile, ?, Map<String, LocalFile>> collector = Collectors.toMap(LocalFile::getVersionTag,
			Function.identity(), (a, b) -> a, () -> versions);
		try {
			findSiblingCandidates(baseFile) //
				.filter(Objects::nonNull) // Is the converted path non-null?
				.map(this.converter) // Do I need to swap to another file?
				.filter(Objects::nonNull) // Is the converted path non-null?
				.filter(Files::exists) // Does the converted path exist?
				.filter(Files::isRegularFile) // Is the converted path a regular file?
				.map(this::parseVersionInfo) //
				.filter(Objects::nonNull) // Again, avoid null values
				.filter(getSiblingCheck(baseFile)) // Same file or a sibling?
				.map(constructor) // Turn it into a LocalFile instance
				.collect(collector)
			//
			;
		} catch (UncheckedIOException e) {
			throw e.getCause();
		} catch (final IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(String.format("Failed to find the siblings for %s", baseFile), t);
		}

		// Ok...we have the TreeMap containing the versions, properly organized from earliest
		// to latest, so now we convert that to indexes so we can have a properly ordered history
		Map<String, Integer> versionIndexes = new HashMap<>();
		List<LocalFile> fullHistory = new ArrayList<>(versions.size());
		int i = 0;
		for (String tag : versions.keySet()) {
			versionIndexes.put(tag, i++);
			fullHistory.add(versions.get(tag));
		}
		fullHistory = Tools.freezeList(fullHistory);
		versionIndexes = Tools.freezeMap(versionIndexes);
		LocalFile rootVersion = fullHistory.get(0);
		LocalFile currentVersion = fullHistory.get(fullHistory.size() - 1);
		return new LocalVersionHistory(historyId, rootVersion, currentVersion, versionIndexes, fullHistory);
	}
}