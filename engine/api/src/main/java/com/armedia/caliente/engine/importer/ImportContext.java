package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContext;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfTransformer;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportContext<S, V, CF extends ImportContextFactory<S, ?, V, ?, ?, ?>>
	extends TransferContext<S, V, CF> {

	private final ImportContextFactory<S, ?, V, ?, ?, ?> factory;
	private final CmfObjectStore<?, ?> cmfObjectStore;
	private final CmfAttributeTranslator<V> translator;
	private final CmfTransformer typeMapper;
	private final CmfContentStore<?, ?, ?> streamStore;
	private final int historyPosition;

	public <C extends ImportContext<S, V, CF>, W extends SessionWrapper<S>, E extends ImportEngine<S, W, V, C, ?, ?>, F extends ImportContextFactory<S, W, V, C, E, ?>> ImportContext(
		CF factory, CfgTools settings, String rootId, CmfType rootType, S session, Logger output,
		WarningTracker tracker, CmfTransformer typeMapper, CmfAttributeTranslator<V> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, int historyPosition) {
		super(factory, settings, rootId, rootType, session, output, tracker);
		this.factory = factory;
		this.translator = translator;
		this.cmfObjectStore = objectStore;
		this.streamStore = streamStore;
		this.typeMapper = typeMapper;
		this.historyPosition = historyPosition;
	}

	public final int getHistoryPosition() {
		return this.historyPosition;
	}

	public final CmfValueMapper getAttributeMapper() {
		return this.cmfObjectStore.getAttributeMapper();
	}

	public final int loadObjects(CmfType type, Set<String> ids, CmfObjectHandler<V> handler)
		throws CmfStorageException {
		return this.cmfObjectStore.loadObjects(this.typeMapper, this.translator, type, ids, handler);
	}

	public final CmfObject<V> getHeadObject(CmfObject<V> sample) throws CmfStorageException {
		return this.cmfObjectStore.loadHeadObject(this.typeMapper, this.translator, sample);
	}

	public final CmfContentStore<?, ?, ?> getContentStore() {
		return this.streamStore;
	}

	protected final CmfObjectStore<?, ?> getObjectStore() {
		return this.cmfObjectStore;
	}

	public final String getTargetPath(String sourcePath) throws ImportException {
		return this.factory.getTargetPath(sourcePath);
	}

	public final boolean isPathAltering() {
		return this.factory.isPathAltering();
	}

	public final List<CmfContentInfo> getContentInfo(CmfObject<V> object) throws ImportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose content to retrieve"); }
		try {
			return this.cmfObjectStore.getContentInfo(object);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the content info for %s", object.getDescription()),
				e);
		}
	}

	public Collection<CmfObjectRef> getContainers(CmfObject<V> object) throws ImportException {
		try {
			return this.cmfObjectStore.getContainers(object);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the containers for %s", object.getDescription()),
				e);
		}
	}

	public Collection<CmfObjectRef> getContainedObjects(CmfObject<V> object) throws ImportException {
		try {
			return this.cmfObjectStore.getContainedObjects(object);
		} catch (CmfStorageException e) {
			throw new ImportException(
				String.format("Failed to load the contained objects for %s", object.getDescription()), e);
		}
	}
}