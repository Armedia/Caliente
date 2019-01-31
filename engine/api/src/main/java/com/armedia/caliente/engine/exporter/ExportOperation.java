package com.armedia.caliente.engine.exporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.armedia.caliente.store.CmfObjectSearchSpec;

final class ExportOperation {

	private static final AtomicLong COUNTER = new AtomicLong(0);

	private final long objectNumber;
	private final Thread creatorThread;
	private final ExportTarget target;
	private final CmfObjectSearchSpec referrent;
	private final AtomicBoolean waiting = new AtomicBoolean(false);
	private boolean completed = false;
	private boolean success = false;

	ExportOperation(ExportTarget target, CmfObjectSearchSpec referrent) {
		this.creatorThread = Thread.currentThread();
		this.objectNumber = ExportOperation.COUNTER.getAndIncrement();
		this.target = target;
		this.referrent = referrent;
		this.completed = false;
		this.success = false;
	}

	long getObjectNumber() {
		return this.objectNumber;
	}

	Thread getCreatorThread() {
		return this.creatorThread;
	}

	ExportTarget getTarget() {
		return this.target;
	}

	CmfObjectSearchSpec getReferrent() {
		return this.referrent;
	}

	String getReferrentDescription() {
		if (this.referrent == null) { return "direct search"; }
		return String.format("%s[%s]", this.referrent.getType().name(), this.referrent.getId());
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
		if (this.completed) {
			try {
				return 0;
			} finally {
				// Make sure we wake any waiters...
				notify();
			}
		}
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