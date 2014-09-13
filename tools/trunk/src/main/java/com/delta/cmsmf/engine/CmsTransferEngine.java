package com.delta.cmsmf.engine;

import org.apache.log4j.Logger;

public abstract class CmsTransferEngine {

	protected static class SynchronizedCounter {
		private long value;

		public SynchronizedCounter() {
			this(0);
		}

		public SynchronizedCounter(long initialValue) {
			this.value = initialValue;
		}

		public synchronized long decrement() {
			return increment(-1);
		}

		public synchronized long decrement(long amount) {
			return increment(-amount);
		}

		public synchronized long increment() {
			return increment(1);
		}

		public synchronized long increment(long amount) {
			this.value += amount;
			notify();
			return this.value;
		}

		public synchronized long setValue(long value) {
			long prev = this.value;
			this.value = value;
			notify();
			return prev;
		}

		public synchronized long getValue() {
			return this.value;
		}

		public synchronized void waitUntilChange() throws InterruptedException {
			final long current = this.value;
			while (this.value == current) {
				wait();
			}
			notify();
		}

		public synchronized void waitForValue(long value) throws InterruptedException {
			while (this.value != value) {
				wait();
			}
			notify();
		}
	}

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