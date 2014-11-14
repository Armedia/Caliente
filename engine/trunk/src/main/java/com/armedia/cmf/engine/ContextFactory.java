package com.armedia.cmf.engine;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class ContextFactory<S, T, V, C extends TransferContext<S, T, V>, E extends TransferEngine<S, T, V, C, ?>> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private boolean open = false;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private CfgTools settings = CfgTools.EMPTY;
	private final E engine;

	protected ContextFactory(E engine, CfgTools settings) {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to which this factory is tied"); }
		this.engine = engine;
		this.settings = Tools.coalesce(settings, CfgTools.EMPTY);
	}

	public final CfgTools getSettings() {
		return this.settings;
	}

	public final E getEngine() {
		return this.engine;
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
		ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		this.lock.readLock().lock();
		try {
			if (!this.open) { throw new IllegalArgumentException("This context factory is not open"); }
			return constructContext(rootId, rootType, session, output, objectStore, contentStore);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	protected abstract C constructContext(String rootId, StoredObjectType rootType, S session, Logger output,
		ObjectStore<?, ?> objectStore, ContentStore contentStore);

}