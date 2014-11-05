package com.armedia.cmf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngine<S, T, V, L> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final int DEFAULT_THREAD_COUNT = 16;
	private static final int MIN_THREAD_COUNT = 1;
	private static final int MAX_THREAD_COUNT = 32;

	private static final int DEFAULT_BACKLOG_SIZE = 1000;
	private static final int MIN_BACKLOG_SIZE = 10;
	private static final int MAX_BACKLOG_SIZE = 100000;

	private final List<L> listeners = new ArrayList<L>();

	private int backlogSize = TransferEngine.DEFAULT_BACKLOG_SIZE;
	private int threadCount = TransferEngine.DEFAULT_THREAD_COUNT;

	public TransferEngine() {
		this(TransferEngine.DEFAULT_THREAD_COUNT, TransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public TransferEngine(int threadCount) {
		this(threadCount, TransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public TransferEngine(int threadCount, int backlogSize) {
		this.backlogSize = backlogSize;
		this.threadCount = threadCount;
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

	public final synchronized int setBacklogSize(int backlogSize) {
		int old = this.backlogSize;
		this.backlogSize = Tools.ensureBetween(TransferEngine.MIN_BACKLOG_SIZE, backlogSize,
			TransferEngine.MAX_BACKLOG_SIZE);
		return old;
	}

	public final int getThreadCount() {
		return this.threadCount;
	}

	public final synchronized int setThreadCount(int threadCount) {
		int old = this.threadCount;
		this.threadCount = Tools.ensureBetween(TransferEngine.MIN_THREAD_COUNT, threadCount,
			TransferEngine.MAX_THREAD_COUNT);
		return old;
	}

	protected abstract ObjectStorageTranslator<T, V> getTranslator();

	protected abstract SessionFactory<S> getSessionFactory();

}