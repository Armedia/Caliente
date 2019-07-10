/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public abstract class TransferContextFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	ENGINE extends TransferEngine<?, ?, ?, SESSION, VALUE, CONTEXT, ?, ?, ?> //
> extends BaseShareableLockable {

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
		ConfigurationSetting excludeSetting = null;
		Set<CmfObject.Archetype> excludes = null;
		Consumer<CmfObject.Archetype> consumer = null;

		if (settings.hasValue(TransferSetting.ONLY_TYPES)) {
			excludeSetting = TransferSetting.ONLY_TYPES;
			excludes = EnumSet.allOf(CmfObject.Archetype.class);
			consumer = excludes::remove;
		} else if (settings.hasValue(TransferSetting.EXCEPT_TYPES)) {
			excludeSetting = TransferSetting.EXCEPT_TYPES;
			excludes = EnumSet.noneOf(CmfObject.Archetype.class);
			consumer = excludes::add;
		} else {
			excludes = EnumSet.noneOf(CmfObject.Archetype.class);
		}

		if ((excludeSetting != null) && (consumer != null)) {
			for (CmfObject.Archetype t : settings.getEnums(excludeSetting, CmfObject.Archetype.class, (o, e) -> null)) {
				if (t != null) {
					consumer.accept(t);
				}
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
		try (MutexAutoLock lock = autoMutexLock()) {
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

	public final CONTEXT newContext(String rootId, CmfObject.Archetype rootType, SESSION session, int batchPosition) {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (!this.open) { throw new IllegalArgumentException("This context factory is not open"); }
			return constructContext(rootId, rootType, session, batchPosition);
		}
	}

	protected abstract CONTEXT constructContext(String rootId, CmfObject.Archetype rootType, SESSION session,
		int batchPosition);

	final String getNextContextId() {
		return String.format("%s-%016x", getContextLabel(), this.contextId.incrementAndGet());
	}

	protected abstract String getContextLabel();
}