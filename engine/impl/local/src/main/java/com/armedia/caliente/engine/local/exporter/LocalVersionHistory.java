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
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LocalVersionHistory implements Iterable<LocalFile> {

	private final String historyId;
	private final String radix;
	private final LocalFile root;
	private final LocalFile current;

	private final Map<String, Integer> byIndex;
	private final List<LocalFile> history;
	private final Map<String, Integer> byPath;

	public LocalVersionHistory(String historyId, LocalFile root, LocalFile current, Map<String, Integer> byIndex,
		Map<String, Integer> byPath, List<LocalFile> history) {
		this.historyId = historyId;
		this.radix = root.getHistoryRadix();
		this.root = root;
		this.current = current;
		this.byIndex = byIndex;
		this.byPath = byPath;
		this.history = history;
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

	public LocalFile getByPath(Path path) throws IOException {
		// Convert to a relative, safe path
		path = this.root.getRootPath().relativize(path);
		String pathStr = path.toString();
		Integer idx = this.byPath.get(pathStr);
		if (idx == null) {
			throw new IllegalArgumentException(String.format("Path [%s] is not part of this history %s (radix = [%s])",
				path, this.historyId, this.radix));
		}
		return this.history.get(idx);
	}

	public Integer getIndexFor(String tag) {
		return this.byIndex.get(Objects.requireNonNull(tag, "Must provide a tag to check for"));
	}

	public Map<String, Integer> getIndexes() {
		return this.byIndex;
	}

	public int getCurrentIndex() {
		return (this.byIndex.size() - 1);
	}
}