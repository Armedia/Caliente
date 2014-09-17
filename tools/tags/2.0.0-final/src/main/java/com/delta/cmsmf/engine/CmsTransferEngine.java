package com.delta.cmsmf.engine;

import org.apache.log4j.Logger;

public abstract class CmsTransferEngine {

	protected final Logger log = Logger.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final int backlogSize;
	private final int threadCount;

	public CmsTransferEngine() {
		this(CmsTransferEngine.DEFAULT_THREAD_COUNT);
	}

	public CmsTransferEngine(int threadCount) {
		this(threadCount, CmsTransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public CmsTransferEngine(int threadCount, int backlogSize) {
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
	}

	public int getBacklogSize() {
		return this.backlogSize;
	}

	public int getThreadCount() {
		return this.threadCount;
	}
}