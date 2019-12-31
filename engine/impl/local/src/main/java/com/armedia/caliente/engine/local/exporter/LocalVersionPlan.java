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

import com.armedia.caliente.tools.VersionNumberScheme;
import com.armedia.commons.utilities.Tools;

public class LocalVersionPlan {

	private static final Predicate<Path> NO_SIBLING = (a) -> false;

	private final VersionNumberScheme versionNumberScheme;
	private final Function<Path, Path> converter;

	public LocalVersionPlan(VersionNumberScheme numberScheme, Function<Path, Path> converter) {
		this.versionNumberScheme = Objects.requireNonNull(numberScheme,
			"Must provide a VersionNumberScheme to order tags with");
		this.converter = (converter != null ? converter : Function.identity());
	}

	protected Predicate<Path> getSiblingCheck(final LocalFile baseFile) {
		return LocalVersionPlan.NO_SIBLING;
	}

	protected Stream<Path> findSiblings(LocalFile baseFile) throws IOException {
		final Path baseFolder = baseFile.getAbsolute().getParentFile().toPath();
		return Files.walk(baseFolder, 1);
	}

	public final LocalVersionHistory getHistory(final LocalFile baseFile) throws IOException {
		String historyId = baseFile.getHistoryId();
		final Path basePath = baseFile.getAbsolute().toPath();
		final Map<String, LocalFile> versions = new TreeMap<>(this.versionNumberScheme);
		final Predicate<Path> isBaseFile = (p) -> {
			try {
				return Files.isSameFile(basePath, p);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		final Function<Path, LocalFile> constructor = (p) -> {
			try {
				return LocalFile.getInstance(baseFile.getRootPath(), p.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		Collector<? super LocalFile, ?, Map<String, LocalFile>> collector = Collectors.toMap(LocalFile::getVersionTag,
			Function.identity(), (a, b) -> a, () -> versions);
		try {
			findSiblings(baseFile) //
				.filter(Files::exists) // Does the found path exist?
				.filter(isBaseFile.or(getSiblingCheck(baseFile))) // Same file or a sibling?
				.map(this.converter) // Do I need to swap to another file?
				.filter(Objects::nonNull) // Is the converted path non-null?
				.filter(Files::exists) // Does the converted path exist?
				.filter(Files::isRegularFile) // Is the converted path a regular file?
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
		int i = 0;
		Map<String, Integer> indexes = new HashMap<>();
		List<LocalFile> history = new ArrayList<>(versions.size());
		for (String tag : versions.keySet()) {
			indexes.put(tag, i++);
			history.add(versions.get(tag));
		}
		history = Tools.freezeList(history);
		indexes = Tools.freezeMap(indexes);
		LocalFile root = history.get(0);
		LocalFile current = history.get(history.size() - 1);
		return new LocalVersionHistory(historyId, root, current, indexes, history);
	}

	public VersionNumberScheme getVersionNumberScheme() {
		return this.versionNumberScheme;
	}
}