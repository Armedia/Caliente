/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.cli.filenamemapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.FSEntry;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.FilenameCollisionResolver;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.commons.utilities.Tools;

public class NewFilenameMapper {

	private static final String DEFAULT_DEDUP_PATTERN = "${name}${fixChar}${id}";

	public static final class Entry implements Comparable<Entry>, Serializable {
		private static final long serialVersionUID = 1L;

		private final CmfObjectRef container;
		private final CmfObjectRef child;
		private final String name;

		public Entry(CmfObjectRef container, CmfObjectRef child, String childName) {
			this.container = container;
			this.child = child;
			this.name = childName;
		}

		public CmfObjectRef getContainer() {
			return this.container;
		}

		public CmfObjectRef getChild() {
			return this.child;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.container, this.name, this.child);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			Entry other = Entry.class.cast(obj);
			return (compareTo(other) == 0);
		}

		@Override
		public int compareTo(Entry o) {
			if (o == this) { return 0; }
			if (o == null) { return 1; }
			int r = Tools.compare(this.container.getType(), o.container.getType());
			if (r != 0) { return r; }
			r = Tools.compare(this.container.getId(), o.container.getId());
			if (r != 0) { return r; }
			r = Tools.compare(this.name, o.name);
			if (r != 0) { return r; }
			r = Tools.compare(this.child.getType(), o.child.getType());
			if (r != 0) { return r; }
			r = Tools.compare(this.child.getId(), o.child.getId());
			if (r != 0) { return r; }
			return 0;
		}
	}

	public static interface ProgressListener {

		public void processingStarted(long nanos);

		public void processingBenchmark(long totalDurationInNanos, long count, double rate);

		public void processingCompleted(long totalDurationInNanos, long count, double rate);

	}

	private static boolean checkDedupPattern(String pattern) {
		final Set<String> found = new HashSet<>();
		new StringSubstitutor((key) -> {
			found.add(key);
			return key;
		}).replace(pattern);
		return found.contains("id");
	}

	public static class SimpleConflictResolver implements FilenameCollisionResolver {

		private final FilenameFixer fixer;
		private final String resolverPattern;
		private final Map<String, Object> resolverMap = new HashMap<>();

		public SimpleConflictResolver(FilenameFixer fixer) {
			this(fixer, null);
		}

		public SimpleConflictResolver(String resolverPattern) {
			this(null, resolverPattern);
		}

		public SimpleConflictResolver(FilenameFixer fixer, String resolverPattern) {
			resolverPattern = Tools.coalesce(resolverPattern, NewFilenameMapper.DEFAULT_DEDUP_PATTERN);
			if (!NewFilenameMapper.checkDedupPattern(resolverPattern)) {
				throw new IllegalArgumentException(
					String.format("Illegal deduplication pattern - doesn't contain ${id}: [%s]", resolverPattern));
			}
			this.fixer = fixer;
			this.resolverPattern = resolverPattern;
			this.resolverMap.put("fixChar", Tools.coalesce(fixer.getFixChar(), FilenameFixer.DEFAULT_FIX_CHAR));
		}

		@Override
		public String generateUniqueName(CmfObjectRef entryId, String currentName, long count) {
			// Empty names get modified into their object IDs...
			if (StringUtils.isEmpty(currentName)) {
				currentName = entryId.getId();
			}
			this.resolverMap.put("typeName", entryId.getType().name());
			this.resolverMap.put("typeOrdinal", entryId.getType().ordinal());
			this.resolverMap.put("id", entryId.getId());
			this.resolverMap.put("name", currentName);
			this.resolverMap.put("count", count);
			String newName = StringSubstitutor.replace(this.resolverPattern, this.resolverMap);
			if (this.fixer != null) {
				// Make sure we use a clean name...
				newName = this.fixer.fixName(newName);
			}
			return newName;
		}
	}

	public static Map<CmfObjectRef, String> repairFilenames(Iterator<Entry> iterator, boolean dedupIgnoreCase,
		FilenameCollisionResolver resolver, FilenameFixer fixer,
		final BiConsumer<CmfObjectRef, String> renamedEntryProcessor, final ProgressListener listener)
		throws Exception {

		final FilenameDeduplicator deduplicator = new FilenameDeduplicator(dedupIgnoreCase);
		long count = 0;
		final long start = System.nanoTime();
		listener.processingStarted(start);
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			if (entry == null) {
				continue;
			}

			String name = entry.getName();
			FSEntry fsEntry = deduplicator.addEntry(entry.getContainer(), entry.getChild(), name);
			if (fixer != null) {
				final String oldName = name;
				// Empty names get modified into their object IDs...
				if (StringUtils.isEmpty(name)) {
					name = entry.getChild().getId();
				}
				name = Tools.coalesce(fixer.fixName(name), oldName);
				if (!Tools.equals(name, oldName)) {
					// Rename the entry so that it can be processed as a rename later on
					fsEntry.setName(name);
				}
			}

			if ((++count % 1000) == 0) {
				long duration = (System.nanoTime() - start);
				listener.processingBenchmark(duration, count, ((count * 1000) / duration));
			}
		}

		// If we're processing a remainder...
		if ((count % 1000) != 0) {
			long duration = (System.nanoTime() - start);
			listener.processingCompleted(duration, count, ((count * 1000) / duration));
		}

		if (resolver != null) {
			deduplicator.fixConflicts(resolver);
		}

		final Map<CmfObjectRef, String> renames = new LinkedHashMap<>();
		deduplicator.processRenamedEntries((entryId, entryName) -> {
			try {
				if (renamedEntryProcessor != null) {
					renamedEntryProcessor.accept(entryId, entryName);
				}
			} finally {
				renames.put(entryId, entryName);
			}
		});
		return renames;
	}
}