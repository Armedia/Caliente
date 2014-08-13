/**
 *
 */

package com.delta.cmsmf.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author diego.rivera@armedia.com
 * 
 */
public class ProcessFuture {

	private final Process process;
	private boolean aborted = false;

	public ProcessFuture(Process process) {
		if (process == null) { throw new IllegalArgumentException("Must provide a non-null process"); }
		this.process = process;
	}

	private final Runnable waiter = new Runnable() {
		@Override
		public void run() {
			try {
				ProcessFuture.this.process.waitFor();
			} catch (InterruptedException e) {
				// Do nothing, but restore interrupted status
				Thread.currentThread().interrupt();
			}
		}
	};

	public synchronized boolean abort() {
		if (!isDone()) {
			this.aborted = true;
			this.process.destroy();
		}
		return this.aborted;
	}

	public synchronized boolean isAborted() {
		return this.aborted;
	}

	private Integer getExitStatus() {
		try {
			return this.process.exitValue();
		} catch (IllegalThreadStateException e) {
			return null;
		}
	}

	public boolean isDone() {
		return (getExitStatus() != null);
	}

	public Integer get() throws InterruptedException {
		return this.process.waitFor();
	}

	public Integer get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {

		final long waitInMillis = unit.convert(timeout, TimeUnit.MILLISECONDS);
		if (waitInMillis > 0) {
			final Thread t = new Thread(this.waiter);
			t.setDaemon(true);
			t.start();
			try {
				t.join(waitInMillis);
			} catch (final InterruptedException e) {
				// If we're interrupted, we interrupt the waiter thread as well
				t.interrupt();
				throw e;
			}
		}

		final Integer ret = getExitStatus();
		if (ret == null) { throw new TimeoutException("Timed out waiting for the child process to exit"); }
		return ret;
	}
}