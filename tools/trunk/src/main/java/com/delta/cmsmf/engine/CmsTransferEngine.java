package com.delta.cmsmf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.storage.CmsObjectStore;

public abstract class CmsTransferEngine<T, O extends Enum<O>> {

	protected final Logger log = Logger.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final List<T> listeners = new ArrayList<T>();

	private final Set<CmsObjectType> manifestTypes;
	private final Set<O> manifestOutcomes;
	private final int backlogSize;
	private final int threadCount;
	private final CmsObjectStore objectStore;
	private final CmsFileSystem fileSystem;
	private final Logger output;

	public CmsTransferEngine(Class<O> outcomeClass, CmsObjectStore objectStore, CmsFileSystem fileSystem) {
		this(outcomeClass, objectStore, fileSystem, null);
	}

	public CmsTransferEngine(Class<O> outcomeClass, CmsObjectStore objectStore, CmsFileSystem fileSystem,
		int threadCount) {
		this(outcomeClass, objectStore, fileSystem, null, threadCount);
	}

	public CmsTransferEngine(Class<O> outcomeClass, CmsObjectStore objectStore, CmsFileSystem fileSystem,
		int threadCount, int backlogSize) {
		this(outcomeClass, objectStore, fileSystem, null, threadCount, backlogSize);
	}

	public CmsTransferEngine(Class<O> outcomeClass, CmsObjectStore objectStore, CmsFileSystem fileSystem, Logger output) {
		this(outcomeClass, objectStore, fileSystem, output, CmsTransferEngine.DEFAULT_THREAD_COUNT);
	}

	public CmsTransferEngine(Class<O> outcomeClass, CmsObjectStore objectStore, CmsFileSystem fileSystem,
		Logger output, int threadCount) {
		this(outcomeClass, objectStore, fileSystem, output, threadCount, CmsTransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public CmsTransferEngine(Class<O> outcomeClass, CmsObjectStore objectStore, CmsFileSystem fileSystem,
		Logger output, int threadCount, int backlogSize) {
		if (outcomeClass == null) { throw new IllegalArgumentException("Must provide an expected outcome class"); }
		if (threadCount <= 0) {
			threadCount = 1;
		}
		if (threadCount > CmsTransferEngine.MAX_THREAD_COUNT) {
			threadCount = CmsTransferEngine.MAX_THREAD_COUNT;
		}
		if (backlogSize <= 0) {
			backlogSize = 10;
		}
		if (backlogSize > CmsTransferEngine.MAX_BACKLOG_SIZE) {
			backlogSize = CmsTransferEngine.MAX_BACKLOG_SIZE;
		}
		this.threadCount = threadCount;
		this.backlogSize = backlogSize;
		this.objectStore = objectStore;
		this.fileSystem = fileSystem;
		this.output = output;
		Set<O> outcomes = EnumSet.noneOf(outcomeClass);
		StrTokenizer tok = StrTokenizer.getCSVInstance(Setting.MANIFEST_OUTCOMES.getString());
		for (String str : tok.getTokenList()) {
			try {
				outcomes.add(Enum.valueOf(outcomeClass, str.toUpperCase()));
			} catch (IllegalArgumentException e) {
				// Illegal outcome, not applicable
			}
		}
		this.manifestOutcomes = Tools.freezeSet(outcomes);

		Set<CmsObjectType> types = EnumSet.noneOf(CmsObjectType.class);
		tok = StrTokenizer.getCSVInstance(Setting.MANIFEST_TYPES.getString());
		for (String str : tok.getTokenList()) {
			try {
				types.add(Enum.valueOf(CmsObjectType.class, str.toUpperCase()));
			} catch (IllegalArgumentException e) {
				// Illegal outcome, not applicable
			}
		}
		this.manifestTypes = Tools.freezeSet(types);
	}

	protected final CmsObjectStore getObjectStore() {
		return this.objectStore;
	}

	protected final CmsFileSystem getFileSystem() {
		return this.fileSystem;
	}

	protected final Logger getOutput() {
		return this.output;
	}

	public final Set<CmsObjectType> getManifestTypes() {
		return this.manifestTypes;
	}

	public final Set<O> getManifestOutcomes() {
		return this.manifestOutcomes;
	}

	public final synchronized boolean addListener(T listener) {
		if (listener != null) { return this.listeners.add(listener); }
		return false;
	}

	public final synchronized boolean removeListener(T listener) {
		if (listener != null) { return this.listeners.remove(listener); }
		return false;
	}

	protected final Collection<T> getListeners() {
		return new ArrayList<T>(this.listeners);
	}

	public int getBacklogSize() {
		return this.backlogSize;
	}

	public int getThreadCount() {
		return this.threadCount;
	}
}