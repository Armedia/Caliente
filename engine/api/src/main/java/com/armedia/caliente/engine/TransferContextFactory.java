package com.armedia.caliente.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.ArrayIterator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class TransferContextFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	ENGINE extends TransferEngine<SESSION, VALUE, CONTEXT, ?, ?, ?> //
> {

	private static CmfType decodeObjectType(Object o) {
		if (o == null) { return null; }
		if (o instanceof CmfType) { return CmfType.class.cast(o); }
		if (o instanceof String) {
			try {
				return CmfType.valueOf(String.valueOf(o));
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
					return new ArrayIterator<>(this.arr);
				}

			}; }
		}
		return Collections.emptyList();
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private boolean open = true;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final AtomicLong contextId = new AtomicLong(0);
	private CfgTools settings = CfgTools.EMPTY;
	private final ENGINE engine;
	private final Set<CmfType> excludes;
	private final String productName;
	private final String productVersion;
	private final CmfContentStore<?, ?, ?> contentStore;
	private final CmfObjectStore<?, ?> objectStore;
	private final Transformer transformer;
	private final Logger output;
	private final WarningTracker warningTracker;

	protected TransferContextFactory(ENGINE engine, CfgTools settings, SESSION session,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker tracker) throws Exception {
		if (engine == null) { throw new IllegalArgumentException(
			"Must provide an engine to which this factory is tied"); }
		this.engine = engine;
		this.settings = Tools.coalesce(settings, CfgTools.EMPTY);
		Set<CmfType> excludes = EnumSet.noneOf(CmfType.class);
		for (Object o : TransferContextFactory.getAsIterable(settings.getObject(TransferSetting.EXCLUDE_TYPES))) {
			CmfType t = TransferContextFactory.decodeObjectType(o);
			if (t != null) {
				excludes.add(t);
			}
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Excluded types for this context factory instance (%s): %s",
				getClass().getSimpleName(), excludes));
		}

		calculateExcludes(objectStore, excludes);

		this.excludes = Tools.freezeSet(excludes);
		this.productName = calculateProductName(session);
		this.productVersion = calculateProductVersion(session);
		this.objectStore = objectStore;
		this.contentStore = contentStore;
		this.transformer = transformer;
		this.output = output;
		this.warningTracker = tracker;
	}

	protected void calculateExcludes(CmfObjectStore<?, ?> objectStore, Set<CmfType> excludes)
		throws CmfStorageException {
		// do nothing
	}

	protected final CmfObjectStore<?, ?> getObjectStore() {
		return this.objectStore;
	}

	protected final CmfContentStore<?, ?, ?> getContentStore() {
		return this.contentStore;
	}

	protected final Transformer getTransformer() {
		return this.transformer;
	}

	protected final Logger getOutput() {
		return this.output;
	}

	protected final WarningTracker getWarningTracker() {
		return this.warningTracker;
	}

	public final boolean isSupported(CmfType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		return !this.excludes.contains(type) && this.engine.checkSupported(this.excludes, type);
	}

	public final CfgTools getSettings() {
		return this.settings;
	}

	public final ENGINE getEngine() {
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

	protected abstract String calculateProductName(SESSION session) throws Exception;

	protected abstract String calculateProductVersion(SESSION session) throws Exception;

	public final String getProductName() {
		return this.productName;
	}

	public final String getProductVersion() {
		return this.productVersion;
	}

	public final CONTEXT newContext(String rootId, CmfType rootType, SESSION session, int batchPosition) {
		this.lock.readLock().lock();
		try {
			if (!this.open) { throw new IllegalArgumentException("This context factory is not open"); }
			return constructContext(rootId, rootType, session, batchPosition);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	protected abstract CONTEXT constructContext(String rootId, CmfType rootType, SESSION session,
		int batchPosition);

	final String getNextContextId() {
		return String.format("%s-%016x", getContextLabel(), this.contextId.incrementAndGet());
	}

	protected abstract String getContextLabel();
}