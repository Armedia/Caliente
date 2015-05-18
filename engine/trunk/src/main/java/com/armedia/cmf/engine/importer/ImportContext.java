package com.armedia.cmf.engine.importer;

import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.CmfAttributeMapper;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportContext<S, V> extends TransferContext<S, V> {

	private final ImportContextFactory<S, ?, V, ?, ?, ?> factory;
	private final CmfObjectStore<?, ?> cmfObjectStore;
	private final CmfAttributeTranslator<V> translator;
	private final CmfContentStore<?> streamStore;

	public <C extends ImportContext<S, V>, W extends SessionWrapper<S>, E extends ImportEngine<S, W, V, C, ?>, F extends ImportContextFactory<S, W, V, C, E, ?>> ImportContext(
		F factory, CfgTools settings, String rootId, CmfType rootType, S session, Logger output,
		CmfAttributeTranslator<V> translator, CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		super(factory, settings, rootId, rootType, session, output);
		this.factory = factory;
		this.translator = translator;
		this.cmfObjectStore = objectStore;
		this.streamStore = streamStore;
	}

	public final CmfAttributeMapper getAttributeMapper() {
		return this.cmfObjectStore.getAttributeMapper();
	}

	public final int loadObjects(CmfType type, Set<String> ids, CmfObjectHandler<V> handler)
		throws CmfStorageException, CmfValueDecoderException {
		// Only allow for dependent types (like ACL) to be loaded
		if (type.isIndependent()) { return 0; }
		return this.cmfObjectStore.loadObjects(this.translator, type, ids, handler);
	}

	public final CmfContentStore<?> getContentStore() {
		return this.streamStore;
	}

	public final void ensureTargetPath() throws ImportException {
		this.factory.ensureTargetPath(getSession());
	}

	public final String getTargetPath(String sourcePath) throws ImportException {
		return this.factory.getTargetPath(sourcePath);
	}

	public final boolean isPathAltering() {
		return this.factory.isPathAltering();
	}
}