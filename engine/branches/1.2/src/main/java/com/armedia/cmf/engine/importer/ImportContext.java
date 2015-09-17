package com.armedia.cmf.engine.importer;

import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportContext<S, T, V> extends TransferContext<S, T, V> {

	private final ImportContextFactory<S, ?, T, V, ?, ?> factory;
	private final ObjectStore<?, ?> objectStore;
	private final ObjectStorageTranslator<T, V> translator;
	private final ContentStore streamStore;

	public <C extends ImportContext<S, T, V>, W extends SessionWrapper<S>, E extends ImportEngine<S, W, T, V, C>, F extends ImportContextFactory<S, W, T, V, C, E>> ImportContext(
		F factory, CfgTools settings, String rootId, StoredObjectType rootType, S session, Logger output,
		ObjectStorageTranslator<T, V> translator, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(factory, settings, rootId, rootType, session, output);
		this.factory = factory;
		this.translator = translator;
		this.objectStore = objectStore;
		this.streamStore = streamStore;
	}

	public final StoredAttributeMapper getAttributeMapper() {
		return this.objectStore.getAttributeMapper();
	}

	public final int loadObjects(StoredObjectType type, Set<String> ids, StoredObjectHandler<V> handler)
		throws StorageException, StoredValueDecoderException {
		if (isSurrogateType(getRootObjectType(), type)) {
			return this.objectStore.loadObjects(this.translator, type, ids, handler, this.factory.getEngine()
				.getImportStrategy(type).isBatchingSupported());
		} else {
			return 0;
		}
	}

	public final Handle getContentHandle(StoredObject<V> object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to inspect for a content URI"); }
		String qualifier = getContentQualifier(object);
		if (qualifier == null) { return null; }
		return this.streamStore.getHandle(object, qualifier);
	}

	protected boolean isSurrogateType(StoredObjectType rootType, StoredObjectType target) {
		return false;
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