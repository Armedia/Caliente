package com.armedia.caliente.cli.filenamemapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.FSEntry;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.FilenameCollisionResolver;
import com.armedia.caliente.cli.filenamemapper.FilenameDeduplicator.RenamedEntryProcessor;
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
		FilenameCollisionResolver resolver, FilenameFixer fixer, final RenamedEntryProcessor renamedEntryProcessor,
		final ProgressListener listener) throws Exception {

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
		deduplicator.processRenamedEntries(new RenamedEntryProcessor() {
			@Override
			public void processEntry(CmfObjectRef entryId, String entryName) {
				try {
					if (renamedEntryProcessor != null) {
						renamedEntryProcessor.processEntry(entryId, entryName);
					}
				} finally {
					renames.put(entryId, entryName);
				}
			}
		});
		return renames;
	}
}