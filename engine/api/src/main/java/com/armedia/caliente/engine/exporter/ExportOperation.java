package com.armedia.caliente.engine.exporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class ExportOperation {

	private static final AtomicLong COUNTER = new AtomicLong(0);

	final long objectNumber;
	final Thread creatorThread;
	final ExportTarget target;
	private final AtomicBoolean waiting = new AtomicBoolean(false);
	private boolean completed = false;
	private boolean success = false;

	ExportOperation(ExportTarget target) {
		this.creatorThread = Thread.currentThread();
		this.objectNumber = ExportOperation.COUNTER.getAndIncrement();
		this.target = target;
		this.completed = false;
	}

	Thread getCreatorThread() {
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
		if (Thread.currentThread() != this.creatorThread) {
			throw new IllegalStateException(
				"Can't mark an operation as completed from a thread that didn't create it!!!");
		}
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
		if (Thread.currentThread() == this.creatorThread) {
			throw new IllegalStateException("Can't wait on an operation from the thread that created it!!!");
		}
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