package com.armedia.cmf.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.ArrayIterator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class ContextFactory<S, V, C extends TransferContext<S, V>, E extends TransferEngine<S, V, C, ?, ?, ?>> {

	private static StoredObjectType decodeObjectType(Object o) {
		if (o == null) { return null; }
		if (o instanceof StoredObjectType) { return StoredObjectType.class.cast(o); }
		if (o instanceof String) {
			try {
				return StoredObjectType.decodeString(String.valueOf(o));
			} catch (IllegalArgumentException e) {
				// Do nothing...
			}
		}
		return null;
	}

	private static Iterable<?> getAsIterable(final Object o) {
		if (o == null) { return Collections.emptyList(); }
		if (o instanceof Iterable) { return Iterable.class.cast(o); }
		if (o instanceof String) { return new StrTokenizer(o.toString(), ',').getTokenList(); }
		if (o.getClass().isArray()) {
			if (!o.getClass().getComponentType().isPrimitive()) { return new Iterable<Object>() {
				private final Object[] arr = (Object[]) o;

				@Override
				public Iterator<Object> iterator() {
					return new ArrayIterator<Object>(this.arr);
				}

			}; }
		}
		return Collections.emptyList();
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private boolean open = true;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private CfgTools settings = CfgTools.EMPTY;
	private final E engine;
	private final Set<StoredObjectType> excludes;

	protected ContextFactory(E engine, CfgTools settings) {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to which this factory is tied"); }
		this.engine = engine;
		this.settings = Tools.coalesce(settings, CfgTools.EMPTY);
		Set<StoredObjectType> excludes = EnumSet.noneOf(StoredObjectType.class);
		for (Object o : ContextFactory.getAsIterable(settings.getObject(TransferSetting.EXCLUDE_TYPES))) {
			StoredObjectType t = ContextFactory.decodeObjectType(o);
			if (t != null) {
				excludes.add(t);
			}
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Excluded types for this context factory instance: %s", getClass()
				.getSimpleName(), excludes));
		}
		this.excludes = Tools.freezeSet(excludes);
	}

	public final boolean isSupported(StoredObjectType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		return !this.excludes.contains(type) && this.engine.checkSupported(this.excludes, type);
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
		ObjectStore<?, ?> objectStore, ContentStore<?> contentStore) {
		this.lock.readLock().lock();
		try {
			if (!this.open) { throw new IllegalArgumentException("This context factory is not open"); }
			return constructContext(rootId, rootType, session, output, objectStore, contentStore);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	protected abstract C constructContext(String rootId, StoredObjectType rootType, S session, Logger output,
		ObjectStore<?, ?> objectStore, ContentStore<?> contentStore);

}