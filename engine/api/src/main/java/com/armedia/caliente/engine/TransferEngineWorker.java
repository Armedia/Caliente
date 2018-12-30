package com.armedia.caliente.engine;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngineWorker<LISTENER extends TransferListener, RESULT extends Enum<RESULT>, EXCEPTION extends TransferEngineException>
	implements AutoCloseable {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = this.lock.readLock();
	private final Lock write = this.lock.writeLock();
	private final Class<RESULT> resultClass;

	private boolean initialized = false;

	private Logger output = null;
	private WarningTracker warningTracker = null;
	private File baseData = null;
	private CmfObjectStore<?, ?> objectStore = null;
	private CmfContentStore<?, ?, ?> contentStore = null;
	private Map<String, ?> settings = null;

	protected TransferEngineWorker(Class<RESULT> resultClass) {
		this.resultClass = Objects.requireNonNull(resultClass, "Must provide a valid RESULT class");
	}

	public final void initialize(final Logger output, final WarningTracker warningTracker, final File baseData,
		final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws EXCEPTION, CmfStorageException {
		this.write.lock();
		try {
			if (!this.initialized) { throw newException("This worker is already initialized, close it first!"); }
			this.output = output;
			this.warningTracker = warningTracker;
			this.baseData = baseData;
			this.objectStore = objectStore;
			this.contentStore = contentStore;
			this.settings = Tools.freezeMap(new TreeMap<>(settings));
			this.initialized = true;
		} finally {
			if (!this.initialized) {
				try {
					doClose();
				} catch (Exception e) {
					// Do nothing...
				}
			}
			this.write.unlock();
		}
	}

	public final Logger getOutput() {
		this.read.lock();
		try {
			return this.output;
		} finally {
			this.read.unlock();
		}
	}

	public final WarningTracker getWarningTracker() {
		this.read.lock();
		try {
			return this.warningTracker;
		} finally {
			this.read.unlock();
		}
	}

	public final File getBaseData() {
		this.read.lock();
		try {
			return this.baseData;
		} finally {
			this.read.unlock();
		}
	}

	public final CmfObjectStore<?, ?> getObjectStore() {
		this.read.lock();
		try {
			return this.objectStore;
		} finally {
			this.read.unlock();
		}
	}

	public final CmfContentStore<?, ?, ?> getContentStore() {
		this.read.lock();
		try {
			return this.contentStore;
		} finally {
			this.read.unlock();
		}
	}

	public final Map<String, ?> getSettings() {
		this.read.lock();
		try {
			return this.settings;
		} finally {
			this.read.unlock();
		}
	}

	public final void run(LISTENER listener, CmfObjectCounter<RESULT> counter) throws EXCEPTION, CmfStorageException {
		this.read.lock();
		try {
			if (!this.initialized) { throw newException(
				"This worker is not yet initialized, call initialize() first!"); }
			if (counter == null) {
				counter = new CmfObjectCounter<>(this.resultClass);
			}
			work(listener, counter);
		} finally {
			this.read.unlock();
		}
	}

	protected final EXCEPTION newException(String message) {
		return newException(message, null);
	}

	protected abstract EXCEPTION newException(String message, Throwable cause);

	public CmfObjectCounter<RESULT> run(LISTENER listener) throws EXCEPTION, CmfStorageException {
		CmfObjectCounter<RESULT> counter = new CmfObjectCounter<>(this.resultClass);
		run(listener, counter);
		return counter;
	}

	protected abstract void work(LISTENER listener, CmfObjectCounter<RESULT> counter)
		throws EXCEPTION, CmfStorageException;

	@Override
	public final void close() throws EXCEPTION {
		this.write.lock();
		try {
			if (this.initialized) {
				doClose();
				this.initialized = false;
			}
		} finally {
			this.write.unlock();
		}
	}

	protected abstract void doClose() throws EXCEPTION;
}