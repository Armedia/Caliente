package com.armedia.cmf.filenamemapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.LockDispenser;
import com.armedia.commons.utilities.Tools;

public class FilenameDeduplicator {

	public static interface IdValidator {
		public boolean isValidId(String id);
	}

	public static interface Renamer {
		public String getNewName(String entryId, String currentName);
	}

	public static interface ConflictResolver {
		public String resolveConflict(String entryId, String currentName, long count);
	}

	public static interface Processor {
		public void processEntry(String entryId, String entryName);
	}

	private class FSObject {
		protected final ReadWriteLock mainLock = new ReentrantReadWriteLock();
		protected final String id;

		private FSObject(String id) {
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

	private class FSEntry extends FSObject implements Comparable<FSEntry> {
		private final ReadWriteLock parentsLock = new ReentrantReadWriteLock();
		private final Map<String, FSEntryContainer> parents = new HashMap<String, FSEntryContainer>();
		private final String originalName;
		private String newName = null;

		private FSEntry(String id, String originalName) {
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

		private boolean setName(final String newName) {
			if (StringUtils.isEmpty(newName)) { return false; }
			if (Tools.equals(this.newName, newName)) { return false; }
			final String oldName;
			Lock l = this.mainLock.writeLock();
			l.lock();
			try {
				oldName = this.newName;
				this.newName = newName;
			} finally {
				l.unlock();
			}

			l = this.parentsLock.readLock();
			l.lock();
			try {
				for (FSEntryContainer c : this.parents.values()) {
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

			// First, sort by name
			int r = Tools.compare(this.newName, o.newName);
			if (r != 0) { return r; }

			// Then, the one with the fewest parents sorts first
			r = Tools.compare(this.parents.size(), o.parents.size());
			if (r != 0) { return r; }

			// Then, the one with the "newest" ID sorts first
			r = Tools.compare(this.id, o.id);
			if (r != 0) { return -r; }

			return 0;
		}
	}

	public class FSEntryContainer extends FSObject {

		private final ReadWriteLock childrenLock = new ReentrantReadWriteLock();
		private final Map<String, Map<String, FSEntry>> children = new HashMap<String, Map<String, FSEntry>>();

		private final ReadWriteLock conflictsLock = new ReentrantReadWriteLock();
		private final Set<String> conflicts = new HashSet<String>();

		private FSEntryContainer(String id) {
			super(id);
		}

		private void addChild(FSEntry child) {
			if (child == null) { throw new IllegalArgumentException("Must provide a non-null child"); }
			Map<String, FSEntry> namedChildren = null;
			final boolean newConflict;

			Lock l = this.childrenLock.writeLock();
			l.lock();
			try {
				namedChildren = this.children.get(child.newName);
				if (namedChildren == null) {
					// This is a new object name, so we create the map to contain
					// all entries with that name
					namedChildren = new HashMap<String, FSEntry>();
					this.children.put(child.newName, namedChildren);
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
					this.conflicts.add(child.newName);
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
			try {
				final Map<String, FSEntry> oldNamedChildren = this.children.get(oldName);
				if (oldNamedChildren == null) {
					// ERROR!! Parent link defect
					throw new IllegalStateException("An entry refers to a parent that knows nothing about it");
				}
				oldNamedChildren.remove(child.id);
				oldSize = oldNamedChildren.size();

				Map<String, FSEntry> newNamedChildren = this.children.get(child.newName);
				if (newNamedChildren == null) {
					newNamedChildren = new HashMap<String, FSEntry>();
					this.children.put(child.newName, newNamedChildren);
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
					this.conflicts.remove(oldName);
				}

				if (newSize > 1) {
					// The new name generates a conflict, so add it to the
					// conflict set
					this.conflicts.add(child.newName);
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
			if (Tools.equals(child.originalName, child.newName)) {
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

		@Override
		public String toString() {
			return String.format("FSEntryContainer [id=%s, childrenNames={%s}, conflicts={%s}]", this.id,
				this.children.keySet(), this.conflicts);
		}
	}

	private static final IdValidator DEFAULT_VALIDATOR = new IdValidator() {
		@Override
		public boolean isValidId(String id) {
			return (id != null);
		}
	};

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final LockDispenser<String, Object> containerLocks = LockDispenser.getBasic();
	private final Map<String, FSEntryContainer> containers = new ConcurrentHashMap<String, FSEntryContainer>();

	private final LockDispenser<String, Object> allEntriesLocks = LockDispenser.getBasic();
	private final Map<String, FSEntry> allEntries = new ConcurrentHashMap<String, FSEntry>();

	private final Map<String, FSEntryContainer> conflictContainers = new ConcurrentHashMap<String, FSEntryContainer>();

	private final Map<String, FSEntry> renamedEntries = new ConcurrentHashMap<String, FSEntry>();

	private final IdValidator idValidator;

	public FilenameDeduplicator() {
		this(null);
	}

	public FilenameDeduplicator(IdValidator idValidator) {
		this.idValidator = Tools.coalesce(idValidator, FilenameDeduplicator.DEFAULT_VALIDATOR);
	}

	private void conflictsDetected(FSEntryContainer container) {
		this.conflictContainers.put(container.id, container);
	}

	private void conflictsCleared(FSEntryContainer container) {
		this.conflictContainers.remove(container.id);
	}

	private FSEntryContainer getContainer(String id) {
		synchronized (this.containerLocks.getLock(id)) {
			FSEntryContainer ret = this.containers.get(id);
			if (ret == null) {
				ret = new FSEntryContainer(id);
				this.containers.put(id, ret);
			}
			return ret;
		}
	}

	public synchronized long renameEntries(Renamer renamer) {
		long count = 0;
		for (FSEntry e : this.allEntries.values()) {
			final String newName = renamer.getNewName(e.id, e.newName);
			if (e.setName(newName)) {
				count++;
			}
		}
		return count;
	}

	public synchronized long fixConflicts(ConflictResolver resolver) {
		long count = 0;
		nextConflict: while (!this.conflictContainers.isEmpty()) {
			int deltas = 0;
			for (FSEntryContainer c : this.conflictContainers.values()) {
				for (String s : c.conflicts) {
					Map<String, FSEntry> conflictEntries = c.children.get(s);
					for (FSEntry e : conflictEntries.values()) {
						final String oldName = e.newName;
						String newName = resolver.resolveConflict(e.id, e.newName, count);
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
				Map<String, FSEntry> conflictEntries = c.children.get(s);
				output.info("\t{} : {}", s, conflictEntries.size());
			}
		}
	}

	public synchronized void processRenamedEntries(Processor p) {
		if (p == null) { return; }
		for (FSEntry e : this.renamedEntries.values()) {
			p.processEntry(e.id, e.newName);
		}
	}

	public boolean addEntry(String containerId, String entryId, String name) {
		if (!this.idValidator.isValidId(containerId)) { return false; }
		if (!this.idValidator.isValidId(entryId)) { return false; }
		final FSEntryContainer container = getContainer(containerId);
		FSEntry entry = null;
		synchronized (this.allEntriesLocks.getLock(entryId)) {
			entry = this.allEntries.get(entryId);
			if (entry == null) {
				entry = new FSEntry(entryId, name);
				this.allEntries.put(entryId, entry);
			}
		}
		entry.addParent(container);
		container.addChild(entry);
		return true;
	}
}