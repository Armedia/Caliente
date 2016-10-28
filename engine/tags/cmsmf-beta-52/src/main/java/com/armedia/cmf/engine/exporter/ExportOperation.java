package com.armedia.cmf.engine.exporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class ExportOperation {

	private static final AtomicLong COUNTER = new AtomicLong(0);

	final long objectNumber;
	final String creatorThread;
	final ExportTarget target;
	private final AtomicBoolean waiting = new AtomicBoolean(false);
	private boolean completed = false;
	private boolean success = false;

	ExportOperation(ExportTarget target) {
		this.creatorThread = Thread.currentThread().getName();
		this.objectNumber = ExportOperation.COUNTER.getAndIncrement();
		this.target = target;
		this.completed = false;
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

	synchronized void setCompleted(boolean success) {
		this.completed = true;
		this.success |= success;
		notify();
	}

	synchronized boolean isCompleted() {
		return this.completed;
	}

	synchronized long waitUntilCompleted() throws InterruptedException {
		return waitUntilCompleted(0);
	}

	synchronized long waitUntilCompleted(long timeout) throws InterruptedException {
		final long start = System.currentTimeMillis();
		while (!this.completed) {
			wait(timeout);
		}
		try {
			return (System.currentTimeMillis() - start);
		} finally {
			notify();
		}
	}
}