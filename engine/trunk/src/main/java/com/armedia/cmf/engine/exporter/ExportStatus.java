package com.armedia.cmf.engine.exporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class ExportStatus {

	private static final AtomicLong COUNTER = new AtomicLong(0);

	final long objectNumber;
	final String creatorThread;
	final ExportTarget target;
	private final AtomicBoolean waiting = new AtomicBoolean(false);
	private boolean exported = false;
	private boolean success = false;

	ExportStatus(ExportTarget target) {
		this.creatorThread = Thread.currentThread().getName();
		this.objectNumber = ExportStatus.COUNTER.getAndIncrement();
		this.target = target;
		this.exported = false;
	}

	String getCreatorThread() {
		return this.creatorThread;
	}

	long getObjectNumber() {
		return this.objectNumber;
	}

	boolean isWaiting() {
		return this.waiting.get();
	}

	void startWait() {
		this.waiting.set(true);
	}

	void endWait() {
		this.waiting.set(false);
	}

	synchronized boolean isSuccessful() {
		return this.success;
	}

	synchronized void markExported(boolean success) {
		this.exported = true;
		this.success |= success;
		notify();
	}

	synchronized long waitUntilExported() throws InterruptedException {
		return waitUntilExported(0);
	}

	synchronized long waitUntilExported(long timeout) throws InterruptedException {
		final long start = System.currentTimeMillis();
		while (!this.exported) {
			wait(timeout);
		}
		try {
			return (System.currentTimeMillis() - start);
		} finally {
			notify();
		}
	}
}