package com.armedia.cmf.engine.importer;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.CmfAttributeMapper;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportContext<S, V, CF extends ImportContextFactory<S, ?, V, ?, ?, ?>>
	extends TransferContext<S, V, CF> {

	private final ImportContextFactory<S, ?, V, ?, ?, ?> factory;
	private final CmfObjectStore<?, ?> cmfObjectStore;
	private final CmfAttributeTranslator<V> translator;
	private final CmfTypeMapper typeMapper;
	private final CmfContentStore<?, ?, ?> streamStore;
	private final int batchPosition;

	public <C extends ImportContext<S, V, CF>, W extends SessionWrapper<S>, E extends ImportEngine<S, W, V, C, ?, ?>, F extends ImportContextFactory<S, W, V, C, E, ?>> ImportContext(
		CF factory, CfgTools settings, String rootId, CmfType rootType, S session, Logger output,
		CmfTypeMapper typeMapper, CmfAttributeTranslator<V> translator, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output);
		this.factory = factory;
		this.translator = translator;
		this.cmfObjectStore = objectStore;
		this.streamStore = streamStore;
		this.typeMapper = typeMapper;
		this.batchPosition = batchPosition;
	}

	public final int getBatchPosition() {
		return this.batchPosition;
	}

	public final CmfAttributeMapper getAttributeMapper() {
		return this.cmfObjectStore.getAttributeMapper();
	}

	public final int loadObjects(CmfType type, Set<String> ids, CmfObjectHandler<V> handler)
		throws CmfStorageException {
		ImportStrategy strategy = this.factory.getEngine().getImportStrategy(type);
		return this.cmfObjectStore.loadObjects(this.typeMapper, this.translator, type, ids, handler,
			strategy.isBatchingSupported());
	}

	public final CmfContentStore<?, ?, ?> getContentStore() {
		return this.streamStore;
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
			throw new ImportException(String.format("Failed to load the content info for %s [%s](%s)", object.getType(),
				object.getLabel(), object.getId()), e);
		}
	}
}