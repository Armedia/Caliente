package com.armedia.caliente.engine.exporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.armedia.caliente.store.CmfObjectSearchSpec;

final class ExportOperation {

	private static final AtomicLong COUNTER = new AtomicLong(0);

	private final long objectNumber;
	private final Thread creatorThread;
	private final ExportTarget target;
	private final String targetLabel;
	private final CmfObjectSearchSpec referrent;
	private final String referrentLabel;
	private final AtomicBoolean waiting = new AtomicBoolean(false);
	private boolean completed = false;
	private boolean success = false;

	ExportOperation(ExportTarget target, String targetLabel, CmfObjectSearchSpec referrent, String referrentLabel) {
		this.creatorThread = Thread.currentThread();
		this.objectNumber = ExportOperation.COUNTER.getAndIncrement();
		this.target = target;
		this.targetLabel = targetLabel;
		this.referrent = referrent;
		this.referrentLabel = referrentLabel;
		this.completed = false;
		this.success = false;
	}

	Thread getCreatorThread() {
		return this.creatorThread;
	}

	long getObjectNumber() {
		return this.objectNumber;
	}

	ExportTarget getTarget() {
		return this.target;
	}

	String getTargetLabel() {
		return this.targetLabel;
	}

	CmfObjectSearchSpec getReferrent() {
		return this.referrent;
	}

	String getReferrentLabel() {
		return this.referrentLabel;
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

	synchronized long waitUntilCompleted(Runnable waitMessage) throws InterruptedException {
		return waitUntilCompleted(waitMessage, 0);
	}

	synchronized long waitUntilCompleted(Runnable waitMessage, long timeout) throws InterruptedException {
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
		boolean messageShown = (waitMessage == null);
		while (!this.completed) {
			if (!messageShown) {
				messageShown = true;
				try {
					waitMessage.run();
				} catch (Exception e) {
					// ignore...
				}
			}
			wait(timeout);
		}
		try {
			return (System.currentTimeMillis() - start);
		} finally {
			notify();
		}
	}
}