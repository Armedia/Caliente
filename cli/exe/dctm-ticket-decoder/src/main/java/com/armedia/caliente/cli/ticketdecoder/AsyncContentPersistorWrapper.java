package com.armedia.caliente.cli.ticketdecoder;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;

public final class AsyncContentPersistorWrapper extends ContentPersistor {

	private final BlockingQueue<Content> queue = new LinkedBlockingQueue<>();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final String name;
	private final ContentPersistor persistor;
	private final ThreadGroup threadGroup;
	private Thread thread = null;

	public AsyncContentPersistorWrapper(String name, ContentPersistor persistor) {
		this(null, name, persistor);
	}

	public AsyncContentPersistorWrapper(ThreadGroup threadGroup, String name, ContentPersistor persistor) {
		super(null);
		this.name = Objects.requireNonNull(name, "Must give a name to this persistor");
		this.persistor = Objects.requireNonNull(persistor, "Must provide a content persistor to wrap");
		this.threadGroup = threadGroup;
	}

	@Override
	protected void startup() throws Exception {
		this.persistor.initialize();
		this.thread = (this.threadGroup != null //
			? new Thread(this.threadGroup, this::run, String.format("Persistor-%s", this.name)) //
			: new Thread(this::run, String.format("Persistor-%s", this.name)) //
		);
		this.thread.setDaemon(true);
		this.running.set(true);
		this.thread.start();
	}

	@Override
	protected void persistContent(Content content) throws Exception {
		try {
			this.queue.put(content);
		} catch (InterruptedException e) {
			// Should never happen - this is an unbounded queue
			this.error.error("Queue for {} failed to accept the content object {}", this.name, content, e);
			throw new RuntimeException(
				String.format("Failed to submit a content object to the unbounded queue for [%s]", this.name), e);
		}
	}

	protected void run() {
		while (true) {
			final Content c;
			try {
				if (this.running.get()) {
					c = this.queue.take();
				} else {
					c = this.queue.poll();
				}
				if (c == null) { return; }
			} catch (InterruptedException e) {
				continue;
			}

			try {
				this.persistor.persist(c);
			} catch (Exception e) {
				this.error.error("{} failed to output the content object {}", this.name, c, e);
			}
		}
	}

	@Override
	protected void cleanup() {
		this.running.set(false);
		if (this.thread != null) {
			if (this.thread.getState() == Thread.State.WAITING) {
				this.thread.interrupt();
			}
			try {
				this.thread.join();
			} catch (InterruptedException e) {
				this.error.error("{} was interrupted while waiting for the worker thread", this.name);
			} finally {
				this.thread = null;
			}
		}
		try {
			this.persistor.close();
		} catch (Exception e) {
			this.error.error("{} failed to close the wrapped persistor", this.name, e);
		}
	}

}