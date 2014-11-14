package com.armedia.cmf.engine;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

public abstract class ContextFactory<S, T, V, C extends TransferContext<S, T, V>> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private boolean open = false;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	protected ContextFactory() {
	}

	public final void init(CfgTools settings) throws Exception {
		if (settings == null) { throw new IllegalArgumentException("Must provide the settings to configure with"); }
		this.lock.writeLock().lock();
		boolean ok = false;
		try {
			if (this.open) {
				ok = true;
				throw new Exception("This pool is already open");
			}
			doInit(settings);
			ok = true;
		} finally {
			this.open = ok;
			this.lock.writeLock().unlock();
		}
	}

	protected void doInit(CfgTools settings) throws Exception {
	}

	private void doClose() {
	}

	public final void close() {
		this.lock.writeLock().lock();
		try {
			if (!this.open) { return; }
			doClose();
		} catch (Exception e) {
			if (this.log.isDebugEnabled()) {
				this.log.error("Exception caught closing this a factory", e);
			} else {
				this.log.error(String.format("Exception caught closing this a factory: %s", e.getMessage()));
			}
		} finally {
			this.open = false;
			this.lock.writeLock().unlock();
		}
	}

	public final C newContext(String rootId, StoredObjectType rootType, S session, Logger output,
		ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		this.lock.readLock().lock();
		try {
			return constructContext(rootId, rootType, session, output, objectStore, streamStore);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	protected abstract C constructContext(String rootId, StoredObjectType rootType, S session, Logger output,
		ObjectStore<?, ?> objectStore, ContentStore streamStore);

}