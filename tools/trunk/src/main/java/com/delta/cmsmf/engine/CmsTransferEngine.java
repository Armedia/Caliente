package com.delta.cmsmf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.storage.CmsObjectStore;

public abstract class CmsTransferEngine<T> {

	protected final Logger log = Logger.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final List<T> listeners = new ArrayList<T>();

	private final int backlogSize;
	private final int threadCount;
	private final CmsObjectStore objectStore;
	private final CmsFileSystem fileSystem;
	private final Logger output;

	public CmsTransferEngine(CmsObjectStore objectStore, CmsFileSystem fileSystem) {
		this(objectStore, fileSystem, null);
	}

	public CmsTransferEngine(CmsObjectStore objectStore, CmsFileSystem fileSystem, int threadCount) {
		this(objectStore, fileSystem, null, threadCount);
	}

	public CmsTransferEngine(CmsObjectStore objectStore, CmsFileSystem fileSystem, int threadCount, int backlogSize) {
		this(objectStore, fileSystem, null, threadCount, backlogSize);
	}

	public CmsTransferEngine(CmsObjectStore objectStore, CmsFileSystem fileSystem, Logger output) {
		this(objectStore, fileSystem, output, CmsTransferEngine.DEFAULT_THREAD_COUNT);
	}

	public CmsTransferEngine(CmsObjectStore objectStore, CmsFileSystem fileSystem, Logger output, int threadCount) {
		this(objectStore, fileSystem, output, threadCount, CmsTransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public CmsTransferEngine(CmsObjectStore objectStore, CmsFileSystem fileSystem, Logger output, int threadCount,
		int backlogSize) {
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