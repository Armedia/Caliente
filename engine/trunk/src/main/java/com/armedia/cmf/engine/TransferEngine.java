package com.armedia.cmf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStore;

public abstract class TransferEngine<L> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final List<L> listeners = new ArrayList<L>();

	private final int backlogSize;
	private final int threadCount;
	private final ObjectStore objectStore;
	private final ContentStreamStore contentStreamStore;
	private final Logger output;

	protected TransferEngine(ObjectStore objectStore, ContentStreamStore fileSystem) {
		this(objectStore, fileSystem, null);
	}

	protected TransferEngine(ObjectStore objectStore, ContentStreamStore fileSystem, int threadCount) {
		this(objectStore, fileSystem, null, threadCount);
	}

	protected TransferEngine(ObjectStore objectStore, ContentStreamStore fileSystem, int threadCount, int backlogSize) {
		this(objectStore, fileSystem, null, threadCount, backlogSize);
	}

	protected TransferEngine(ObjectStore objectStore, ContentStreamStore fileSystem, Logger output) {
		this(objectStore, fileSystem, output, TransferEngine.DEFAULT_THREAD_COUNT);
	}

	protected TransferEngine(ObjectStore objectStore, ContentStreamStore fileSystem, Logger output, int threadCount) {
		this(objectStore, fileSystem, output, threadCount, TransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	protected TransferEngine(ObjectStore objectStore, ContentStreamStore contentStreamStore, Logger output,
		int threadCount, int backlogSize) {
		if (threadCount <= 0) {
			threadCount = 1;
		}
		if (threadCount > TransferEngine.MAX_THREAD_COUNT) {
			threadCount = TransferEngine.MAX_THREAD_COUNT;
		}
		if (backlogSize <= 0) {
			backlogSize = 10;
		}
		if (backlogSize > TransferEngine.MAX_BACKLOG_SIZE) {
			backlogSize = TransferEngine.MAX_BACKLOG_SIZE;
		}
		this.threadCount = threadCount;
		this.backlogSize = backlogSize;
		this.objectStore = objectStore;
		this.contentStreamStore = contentStreamStore;
		this.output = output;
	}

	public final ObjectStore getObjectStore() {
		return this.objectStore;
	}

	public final ContentStreamStore getContentStreamStore() {
		return this.contentStreamStore;
	}

	public final Logger getOutput() {
		return this.output;
	}

	public final synchronized boolean addListener(L listener) {
		if (listener != null) { return this.listeners.add(listener); }
		return false;
	}

	public final synchronized boolean removeListener(L listener) {
		if (listener != null) { return this.listeners.remove(listener); }
		return false;
	}

	protected final synchronized Collection<L> getListeners() {
		return new ArrayList<L>(this.listeners);
	}

	public final int getBacklogSize() {
		return this.backlogSize;
	}

	public final int getThreadCount() {
		return this.threadCount;
	}
}