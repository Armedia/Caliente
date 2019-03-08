package com.armedia.caliente.engine;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.tools.BaseReadWriteLockable;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class TransferContextFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	ENGINE extends TransferEngine<?, ?, ?, SESSION, VALUE, CONTEXT, ?, ?, ?> //
> extends BaseReadWriteLockable {

	private static CmfObject.Archetype decodeObjectType(Object o) {
		if (o == null) { return null; }
		if (o instanceof CmfObject.Archetype) { return CmfObject.Archetype.class.cast(o); }
		if (o instanceof String) {
			try {
				return CmfObject.Archetype.valueOf(String.valueOf(o));
			} catch (IllegalArgumentException e) {
				// Do nothing...
			}
		}
		return null;
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private boolean open = true;

	private final AtomicLong contextId = new AtomicLong(0);
	private CfgTools settings = CfgTools.EMPTY;
	private final ENGINE engine;
	private final Set<CmfObject.Archetype> excludes;
	private final String productName;
	private final String productVersion;
	private final CmfContentStore<?, ?> contentStore;
	private final CmfObjectStore<?> objectStore;
	private final Transformer transformer;
	private final Logger output;
	private final WarningTracker warningTracker;

	protected TransferContextFactory(ENGINE engine, CfgTools settings, SESSION session, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output, WarningTracker tracker)
		throws Exception {
		if (engine == null) {
			throw new IllegalArgumentException("Must provide an engine to which this factory is tied");
		}
		this.engine = engine;
		this.settings = Tools.coalesce(settings, CfgTools.EMPTY);
		Set<CmfObject.Archetype> excludes = EnumSet.noneOf(CmfObject.Archetype.class);
		for (Object o : settings.getObjects(TransferSetting.EXCLUDE_TYPES)) {
			CmfObject.Archetype t = TransferContextFactory.decodeObjectType(o);
			if (t != null) {
				excludes.add(t);
			}
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug("Excluded types for this context factory instance ({}): {}", getClass().getSimpleName(),
				excludes);
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

	protected void calculateExcludes(CmfObjectStore<?> objectStore, Set<CmfObject.Archetype> excludes)
		throws CmfStorageException {
		// do nothing
	}

	protected final CmfObjectStore<?> getObjectStore() {
		return this.objectStore;
	}

	protected final CmfContentStore<?, ?> getContentStore() {
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

	public final boolean isSupported(CmfObject.Archetype type) {
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
		writeLocked(() -> {
			try {
				if (!this.open) { return; }
				doClose();
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught closing this a factory", e);
				} else {
					this.log.error("Exception caught closing this a factory: {}", e.getMessage());
				}
			} finally {
				this.open = false;
			}
		});
	}

	protected abstract String calculateProductName(SESSION session) throws Exception;

	protected abstract String calculateProductVersion(SESSION session) throws Exception;

	public final String getProductName() {
		return this.productName;
	}

	public final String getProductVersion() {
		return this.productVersion;
	}

	public final CONTEXT newContext(String rootId, CmfObject.Archetype rootType, SESSION session, int batchPosition) {
		return readLocked(() -> {
			if (!this.open) { throw new IllegalArgumentException("This context factory is not open"); }
			return constructContext(rootId, rootType, session, batchPosition);
		});
	}

	protected abstract CONTEXT constructContext(String rootId, CmfObject.Archetype rootType, SESSION session,
		int batchPosition);

	final String getNextContextId() {
		return String.format("%s-%016x", getContextLabel(), this.contextId.incrementAndGet());
	}

	protected abstract String getContextLabel();
}