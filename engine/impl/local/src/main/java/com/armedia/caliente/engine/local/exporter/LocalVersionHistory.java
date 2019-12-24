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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.commons.utilities.Tools;

public final class LocalVersionHistory implements Iterable<LocalFile> {

	private final String historyId;
	private final LocalFile root;
	private final LocalFile current;

	private final Map<String, Integer> indexes;
	private final List<LocalFile> history;

	public LocalVersionHistory(LocalVersionPlan plan) throws IOException {
		LocalFile baseFile = Objects.requireNonNull(plan, "Must provide a LocalVersionPlan instance to follow")
			.getPatternFile();
		final Path baseFolder = baseFile.getAbsolute().getParentFile().toPath();
		final LocalRoot root = baseFile.getRootPath();
		this.historyId = baseFile.getHistoryId();

		final Path basePath = baseFile.getAbsolute().toPath();
		final Map<String, LocalFile> versions = new TreeMap<>(plan);
		final Predicate<Path> isBaseFile = (p) -> {
			try {
				return Files.isSameFile(basePath, p);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		final Function<Path, LocalFile> constructor = (p) -> {
			try {
				return new LocalFile(root, p.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};
		Collector<? super LocalFile, ?, Map<String, LocalFile>> collector = Collectors.toMap(LocalFile::getVersionTag,
			Function.identity(), (a, b) -> a, () -> versions);
		try {
			Files.walk(baseFolder, 1) //
				.filter(Files::exists) // Does the found path exist?
				.filter(isBaseFile.or(plan::isSibling)) // Same file or a sibling?
				.filter(plan::isSibling) // Is this a version sibling?
				.map(plan::convert) // Do I need to swap to another file?
				.filter(Objects::nonNull) // Is the converted path non-null?
				.filter(Files::exists) // Does the converted path exist?
				.filter(Files::isRegularFile) // Is the converted path a regular file?
				.map(constructor) // Turn it into a LocalFile instance
				.collect(collector)
			//
			;
		} catch (UncheckedIOException e) {
			throw e.getCause();
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
		this.history = Tools.freezeList(history);
		this.indexes = Tools.freezeMap(indexes);
		this.root = this.history.get(0);
		this.current = this.history.get(this.history.size() - 1);
	}

	public LocalFile getRootVersion() {
		return this.root;
	}

	public LocalFile getCurrentVersion() {
		return this.current;
	}

	public boolean isEmpty() {
		return this.history.isEmpty();
	}

	public int size() {
		return this.history.size();
	}

	@Override
	public Iterator<LocalFile> iterator() {
		return this.history.iterator();
	}

	public List<LocalFile> getVersions() {
		return this.history;
	}

	public String getHistoryId() {
		return this.historyId;
	}

	public LocalFile getVersion(String id) {
		Integer index = getIndexFor(id);
		if (index == null) { return null; }
		return this.history.get(index);
	}

	public LocalFile getAntecedent(LocalFile version) {
		return getAntecedent(Objects.requireNonNull(version, "Must provide a non-null version to check for").getId());
	}

	public LocalFile getAntecedent(String id) {
		Integer index = getIndexFor(id);
		if (index == null) { throw new IllegalArgumentException("This version does not belong to this history"); }
		if (index == 0) { return null; }
		return this.history.get(index - 1);
	}

	public Integer getIndexFor(String id) {
		return this.indexes.get(Objects.requireNonNull(id, "Must provide an ID to check for"));
	}

	public Map<String, Integer> getIndexes() {
		return this.indexes;
	}

	public int getCurrentIndex() {
		return (this.indexes.size() - 1);
	}
}