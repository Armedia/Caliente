package com.armedia.caliente.cli.filenamemapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.commons.utilities.Tools;

public class FilenameDeduplicator {

	private static enum Canonicalizer {
		//
		// Same string, no change
		NO_CHANGE() {
		},
		// Case insensitive (fold to uppercase)
		CASE_INSENSITIVE() {
			@Override
			protected String doCanonicalize(String name) {
				return name.toUpperCase();
			}
		},
		//
		;

		protected String doCanonicalize(String name) {
			return name;
		}

		public final String canonicalize(String name) {
			if (name == null) { return null; }
			return doCanonicalize(name);
		}

		public final boolean equals(String a, String b) {
			return (compare(a, b) == 0);
		}

		public final int compare(String a, String b) {
			if (a == b) { return 0; }
			if (a == null) { return -1; }
			if (b == null) { return 1; }
			return canonicalize(a).compareTo(canonicalize(b));
		}
	}

	public static interface IdValidator {
		public boolean isValidId(CmfObjectRef id);
	}

	public static interface Renamer {
		public String getNewName(CmfObjectRef entryId, String currentName);
	}

	public static interface FilenameCollisionResolver {
		public String generateUniqueName(CmfObjectRef entryId, String currentName, long count);
	}

	public static interface RenamedEntryProcessor {
		public void processEntry(CmfObjectRef entryId, String entryName);
	}

	public class FSObject {
		protected final ReadWriteLock mainLock = new ReentrantReadWriteLock();
		protected final CmfObjectRef id;

		private FSObject(CmfObjectRef id) {
			if (id == null) { throw new IllegalArgumentException("Must provide a non-null id"); }
			this.id = id;
		}

		@Override
		public final int hashCode() {
			return Tools.hashTool(this, null, this.id);
		}

		@Override
		public final boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			FSObject other = FSObject.class.cast(obj);
			if (getOuterType() != other.getOuterType()) { return false; }
			if (!Tools.equals(this.id, other.id)) { return false; }
			return true;
		}

		private FilenameDeduplicator getOuterType() {
			return FilenameDeduplicator.this;
		}
	}

	public class FSEntry extends FSObject implements Comparable<FSEntry> {
		private final ReadWriteLock parentsLock = new ReentrantReadWriteLock();
		private final Map<CmfObjectRef, FSEntryContainer> parents = new HashMap<>();
		private final String originalName;
		private String newName = null;

		private FSEntry(CmfObjectRef id, String originalName) {
			super(id);
			if (StringUtils
				.isEmpty(originalName)) { throw new IllegalArgumentException("Must provide a non-null original name"); }
			this.originalName = originalName;
			this.newName = originalName;
		}

		private void addParent(FSEntryContainer parent) {
			Lock l = this.parentsLock.writeLock();
			l.lock();
			try {
				this.parents.put(parent.id, parent);
			} finally {
				l.unlock();
			}
		}

		public String getOriginalName() {
			return this.originalName;
		}

		public String getCurrentName() {
			Lock l = this.mainLock.readLock();
			l.lock();
			try {
				return this.newName;
			} finally {
				l.unlock();
			}
		}

		public boolean setName(final String newName) {
			if (StringUtils.isEmpty(newName)) { return false; }
			final String oldName;
			Lock l = this.mainLock.writeLock();
			l.lock();
			try {
				if (FilenameDeduplicator.this.canonicalizer.equals(this.newName, newName)) { return false; }
				oldName = this.newName;
				this.newName = newName;
			} finally {
				l.unlock();
			}

			l = this.parentsLock.readLock();
			l.lock();
			try {
				for (FSEntryContainer c : this.parents.values()) {
					// TODO: use this to revert a rename if it would generate a new conflict
					c.entryRenamed(this, oldName);
				}
			} finally {
				l.unlock();
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("FSEntry [id=%s, originalName={%s}, newName={%s}]", this.id, this.originalName,
				this.newName);
		}

		@Override
		public int compareTo(FSEntry o) {
			if (o == null) { return 1; }

			// First, sort by name (take case sensitivity into account)
			int r = FilenameDeduplicator.this.canonicalizer.compare(this.newName, o.newName);
			if (r != 0) { return r; }

			// Then, the one with the fewest parents sorts first
			r = Tools.compare(this.parents.size(), o.parents.size());
			if (r != 0) { return r; }

			// Then, the one with the "newest" ID sorts first
			r = Tools.compare(this.id.getId(), o.id.getId());
			if (r != 0) { return -r; }

			return 0;
		}
	}

	public class FSEntryContainer extends FSObject {

		private final ReadWriteLock childrenLock = new ReentrantReadWriteLock();

		// Children, indexed by name...
		private final Map<String, Map<CmfObjectRef, FSEntry>> children = new HashMap<>();

		private final ReadWriteLock conflictsLock = new ReentrantReadWriteLock();

		// Names that are in conflict...
		private final Set<String> conflicts = new HashSet<>();

		private FSEntryContainer(CmfObjectRef id) {
			super(id);
		}

		private void addChild(FSEntry child) {
			if (child == null) { throw new IllegalArgumentException("Must provide a non-null child"); }
			Map<CmfObjectRef, FSEntry> namedChildren = null;
			final boolean newConflict;

			Lock l = this.childrenLock.writeLock();
			l.lock();
			final String childName = FilenameDeduplicator.this.canonicalizer.canonicalize(child.newName);
			try {
				namedChildren = this.children.get(childName);
				if (namedChildren == null) {
					// This is a new object name, so we create the map to contain
					// all entries with that name
					namedChildren = new HashMap<>();
					this.children.put(childName, namedChildren);
				}
				namedChildren.put(child.id, child);
				newConflict = (namedChildren.size() > 1);
			} finally {
				l.unlock();
			}

			if (newConflict) {
				l = this.conflictsLock.writeLock();
				l.lock();
				try {
					this.conflicts.add(childName);
					conflictsDetected(this);
				} finally {
					l.unlock();
				}
			}
		}

		private synchronized void entryRenamed(FSEntry child, String oldName) {
			final int oldSize;
			final int newSize;

			Lock l = this.childrenLock.writeLock();
			l.lock();
			final String canonicalOldName = FilenameDeduplicator.this.canonicalizer.canonicalize(oldName);
			final String canonicalNewName = FilenameDeduplicator.this.canonicalizer.canonicalize(child.newName);
			try {
				final Map<CmfObjectRef, FSEntry> oldNamedChildren = this.children.get(canonicalOldName);
				if (oldNamedChildren == null) {
					// ERROR!! Parent link defect
					throw new IllegalStateException("An entry refers to a parent that knows nothing about it");
				}
				oldNamedChildren.remove(child.id);
				oldSize = oldNamedChildren.size();

				Map<CmfObjectRef, FSEntry> newNamedChildren = this.children.get(canonicalNewName);
				if (newNamedChildren == null) {
					newNamedChildren = new HashMap<>();
					this.children.put(canonicalNewName, newNamedChildren);
				}
				newNamedChildren.put(child.id, child);
				newSize = newNamedChildren.size();
			} finally {
				l.unlock();
			}

			final boolean oldConflict;
			final boolean newConflict;
			l = this.conflictsLock.writeLock();
			l.lock();
			try {
				oldConflict = !this.conflicts.isEmpty();
				if (oldSize == 1) {
					// There is only one file left with the old name, so
					// we can remove the name from the conflict list
					this.conflicts.remove(canonicalOldName);
				}

				if (newSize > 1) {
					// The new name generates a conflict, so add it to the
					// conflict set
					this.conflicts.add(canonicalNewName);
				}
				newConflict = !this.conflicts.isEmpty();
			} finally {
				l.unlock();
			}

			// Check to see if our conflict status is different from before. If it's the same,
			// then we don't do anything since there's no need.
			if (oldConflict != newConflict) {
				if (newConflict) {
					// if we weren't in conflict, but are now
					conflictsDetected(this);
				} else {
					// If we were in conflict, but aren't anymore
					conflictsCleared(this);
				}
			}
			// Finally, mark the entry as renamed. But if it got renamed to its original
			// name, then unmark it as renamed so we don't do anything to it
			if (FilenameDeduplicator.this.canonicalizer.equals(child.originalName, child.newName)) {
				FilenameDeduplicator.this.renamedEntries.remove(child.id);
			} else {
				FilenameDeduplicator.this.renamedEntries.put(child.id, child);
			}
		}

		public boolean containsConflicts() {
			Lock l = this.conflictsLock.readLock();
			l.lock();
			try {
				return !this.conflicts.isEmpty();
			} finally {
				l.unlock();
			}
		}

		public Set<String> getConflicts() {
			Lock l = this.conflictsLock.readLock();
			l.lock();
			try {
				return new HashSet<>(this.conflicts);
			} finally {
				l.unlock();
			}
		}

		@Override
		public String toString() {
			return String.format("FSEntryContainer [id=%s, childrenNames={%s}, conflicts={%s}]", this.id,
				this.children.keySet(), this.conflicts);
		}
	}

	protected static final IdValidator DEFAULT_ID_VALIDATOR = new IdValidator() {
		@Override
		public boolean isValidId(CmfObjectRef id) {
			return (id != null) && (id.getType() != null) && (id.getId() != null);
		}
	};

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<CmfObjectRef, FSEntryContainer> containers = new ConcurrentHashMap<>();

	private final ConcurrentMap<CmfObjectRef, FSEntry> allEntries = new ConcurrentHashMap<>();

	private final ConcurrentMap<CmfObjectRef, FSEntryContainer> conflictContainers = new ConcurrentHashMap<>();

	private final ConcurrentMap<CmfObjectRef, FSEntry> renamedEntries = new ConcurrentHashMap<>();

	private final IdValidator idValidator;

	private final Canonicalizer canonicalizer;

	public FilenameDeduplicator(boolean ignoreCase) {
		this(null, ignoreCase);
	}

	public FilenameDeduplicator(IdValidator idValidator, boolean ignoreCase) {
		this.idValidator = Tools.coalesce(idValidator, FilenameDeduplicator.DEFAULT_ID_VALIDATOR);
		this.canonicalizer = (ignoreCase ? Canonicalizer.CASE_INSENSITIVE : Canonicalizer.NO_CHANGE);
	}

	private void conflictsDetected(FSEntryContainer container) {
		this.conflictContainers.put(container.id, container);
	}

	private void conflictsCleared(FSEntryContainer container) {
		this.conflictContainers.remove(container.id);
	}

	private FSEntryContainer getContainer(final CmfObjectRef id) {
		return ConcurrentUtils.createIfAbsentUnchecked(this.containers, id,
			new ConcurrentInitializer<FSEntryContainer>() {
				@Override
				public FSEntryContainer get() {
					return new FSEntryContainer(id);
				}
			});
	}

	public synchronized long renameAllEntries(Renamer renamer) {
		long count = 0;
		for (FSEntry e : this.allEntries.values()) {
			final String newName = renamer.getNewName(e.id, e.newName);
			if (e.setName(newName)) {
				count++;
			}
		}
		return count;
	}

	public synchronized long fixConflicts(FilenameCollisionResolver resolver) {
		return fixConflicts(resolver, null);
	}

	public synchronized long fixConflicts(FilenameCollisionResolver resolver, Comparator<FSEntry> comparator) {
		long count = 0;
		nextConflict: while (!this.conflictContainers.isEmpty()) {
			int deltas = 0;
			for (FSEntryContainer c : this.conflictContainers.values()) {
				for (String s : c.conflicts) {
					Map<CmfObjectRef, FSEntry> conflictEntries = c.children.get(s);
					// Sort the entries...this will only be slow when there are many, MANY
					// entries with a naming conflict. By taking this approach, we make it
					// so that we're now able to choose what order the entries will be renamed
					// in when fixing a conflict. The order should prioritize those who
					// have fewer parents, and thus those who cause the lowest impact.
					List<FSEntry> l = new ArrayList<>(conflictEntries.size());
					l.addAll(conflictEntries.values());
					if (comparator != null) {
						// If the user requested a specific ordering of entries, we use that.
						Collections.sort(l, comparator);
					} else {
						// The default ordering is by newName (asc), then # of parents (asc),
						// then ID (desc). This means that since all the entries in this list
						// share the same newName, then only the last two will be applied. Thus,
						// the first entry to be renamed will be the one with the fewest parents
						// and thus the (expected) smallest impact to the overall hierarchy.
						Collections.sort(l);
					}
					for (FSEntry e : l) {
						final String oldName = e.newName;
						String newName = resolver.generateUniqueName(e.id, e.newName, count);
						if (e.setName(newName)) {
							count++;
							deltas++;
							this.log.info("Renamed [{}]({}) to [{}]", oldName, e.id, newName);
							continue nextConflict;
						}
					}
				}
			}
			if (deltas == 0) {
				// Count how many conflicts were left
				long left = 0;
				for (FSEntryContainer c : this.conflictContainers.values()) {
					left += c.conflicts.size();
				}
				throw new IllegalStateException(
					String.format("Failed to resolve the remaining conflicts (%d left in %d containers)", left,
						this.conflictContainers.size()));
			}
		}
		return count;
	}

	public synchronized void showConflicts(Logger output) {
		for (FSEntryContainer c : this.conflictContainers.values()) {
			output.info("Container [{}]", c.id, c.conflicts);
			for (String s : c.conflicts) {
				Map<CmfObjectRef, FSEntry> conflictEntries = c.children.get(s);
				output.info("\t{} : {}", s, conflictEntries.size());
			}
		}
	}

	public synchronized void processRenamedEntries(RenamedEntryProcessor p) {
		if (p == null) { return; }
		for (FSEntry e : this.renamedEntries.values()) {
			p.processEntry(e.id, e.newName);
		}
	}

	public FSEntry addEntry(CmfObjectRef containerId, final CmfObjectRef entryId, final String name) {
		if (!this.idValidator.isValidId(containerId)) { return null; }
		if (!this.idValidator.isValidId(entryId)) { return null; }
		final FSEntryContainer container = getContainer(containerId);
		FSEntry entry = ConcurrentUtils.createIfAbsentUnchecked(this.allEntries, entryId,
			new ConcurrentInitializer<FSEntry>() {
				@Override
				public FSEntry get() {
					return new FSEntry(entryId, name);
				}
			});
		entry.addParent(container);
		container.addChild(entry);
		return entry;
	}
}