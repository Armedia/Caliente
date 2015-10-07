package com.armedia.cmf.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PooledWorkers<S, Q> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final BlockingQueue<Q> workQueue;
	private final List<Future<?>> futures;
	private final AtomicInteger activeCounter;

	private Q exitValue = null;
	private int threadCount = 0;
	private ThreadPoolExecutor executor = null;

	private final class Worker implements Runnable {
		private final Logger log = PooledWorkers.this.log;

		private final Q exitValue;

		private Worker(Q exitValue) {
			this.exitValue = exitValue;
		}

		@Override
		public void run() {
			final S state;
			try {
				state = prepare();
			} catch (Exception e) {
				this.log.error("Failed to prepare the worker for this thread", e);
				return;
			}
			PooledWorkers.this.activeCounter.incrementAndGet();
			try {
				while (!Thread.interrupted()) {
					if (this.log.isDebugEnabled()) {
						this.log.debug("Polling the queue...");
					}
					Q next = null;
					try {
						next = PooledWorkers.this.workQueue.take();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}

					// We compare instances, and not values, because we're interested
					// in seeing if the EXACT exit value flag is used, not one that
					// looks the same out of some unfortunate coincidence. By checking
					// instances, we ensure that we will not exit the loop prematurely
					// due to a value collision.
					if (next == this.exitValue) {
						// Work complete
						this.log.info("Exiting the export polling loop");
						return;
					}

					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Polled %s", next));
					}

					try {
						process(state, next);
					} catch (Exception e) {
						this.log.error(String.format("Failed to process item %s", next), e);
					}
				}
			} finally {
				PooledWorkers.this.activeCounter.decrementAndGet();
				cleanup(state);
			}
		}
	}

	public PooledWorkers(int backlogSize) {
		this.workQueue = new ArrayBlockingQueue<Q>(backlogSize);
		this.futures = new ArrayList<Future<?>>(this.threadCount);
		this.activeCounter = new AtomicInteger(0);
	}

	protected abstract S prepare() throws Exception;

	protected abstract void process(S state, Q item) throws Exception;

	protected abstract void cleanup(S state);

	public final void addWorkItem(Q item) throws InterruptedException {
		this.workQueue.put(item);
	}

	public final synchronized List<Q> clearWorkItems() {
		List<Q> ret = new ArrayList<Q>();
		this.workQueue.drainTo(ret);
		return ret;
	}

	public final synchronized boolean start(int threadCount, Q exitValue) {
		if (this.executor != null) { return false; }
		this.threadCount = threadCount;
		this.exitValue = exitValue;
		this.activeCounter.set(0);
		this.futures.clear();
		Worker worker = new Worker(exitValue);
		for (int i = 0; i < this.threadCount; i++) {
			this.futures.add(this.executor.submit(worker));
		}
		this.executor.shutdown();
		return true;
	}

	private final synchronized void waitCleanly() {
		this.log.info("Signaling work completion for the workers");
		boolean waitCleanly = true;
		for (int i = 0; i < this.threadCount; i++) {
			try {
				this.workQueue.put(this.exitValue);
			} catch (InterruptedException e) {
				waitCleanly = false;
				// Here we have a problem: we're timing out while adding the exit
				// values...
				this.log.warn("Interrupted while attempting to request executor thread termination", e);
				Thread.currentThread().interrupt();
				this.executor.shutdownNow();
				break;
			}
		}

		try {
			// We're done, we must wait until all workers are waiting
			if (waitCleanly) {
				this.log.info(String.format("Waiting for %d workers to finish processing", this.threadCount));
				for (Future<?> future : this.futures) {
					try {
						future.get();
					} catch (InterruptedException e) {
						this.log.warn("Interrupted while waiting for an executor thread to exit, forcing the shutdown",
							e);
						Thread.currentThread().interrupt();
						this.executor.shutdownNow();
						break;
					} catch (ExecutionException e) {
						this.log.warn("An executor thread raised an exception", e);
					} catch (CancellationException e) {
						this.log.warn("An executor thread was canceled!", e);
					}
				}
				this.log.info("All the export workers are done.");
			}
		} finally {
			List<Q> remaining = new ArrayList<Q>();
			this.workQueue.drainTo(remaining);
			for (Q v : remaining) {
				if (v == this.exitValue) {
					continue;
				}
				this.log.error(String.format("WORK LEFT PENDING IN THE QUEUE: %s", v));
			}
			remaining.clear();
		}

		// If there are still pending workers, then wait for them to finish for up to 5
		// minutes
		int pending = this.activeCounter.get();
		if (pending > 0) {
			this.log.info(String.format(
				"Waiting for pending workers to terminate (maximum 5 minutes, %d pending workers)", pending));
			try {
				this.executor.awaitTermination(5, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				this.log.warn("Interrupted while waiting for normal executor termination", e);
				Thread.currentThread().interrupt();
			}
		}
	}

	public final synchronized void waitForCompletion() {
		if (this.executor == null) { return; }
		try {
			waitCleanly();
		} finally {
			try {
				this.executor.shutdownNow();
				int pending = this.activeCounter.get();
				if (pending > 0) {
					try {
						this.log
							.info(String
								.format(
									"Waiting an additional 60 seconds for worker termination as a contingency (%d pending workers)",
									pending));
						this.executor.awaitTermination(1, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						this.log.warn("Interrupted while waiting for immediate executor termination", e);
						Thread.currentThread().interrupt();
					}
				}
			} finally {
				this.executor = null;
				this.threadCount = 0;
				this.exitValue = null;
			}
		}
	}
}