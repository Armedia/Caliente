package com.armedia.cmf.engine.importer;

import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;

public class ImportContext<S, T, V> extends TransferContext<S, T, V> {

	private final ObjectStore<?, ?> objectStore;
	private final ObjectStorageTranslator<T, V> translator;
	private final ContentStreamStore streamStore;

	ImportContext(String rootId, S session, Logger output, ObjectStorageTranslator<T, V> translator,
		ObjectStore<?, ?> objectStore, ContentStreamStore streamStore) {
		super(rootId, session, output);
		this.translator = translator;
		this.objectStore = objectStore;
		this.streamStore = streamStore;
	}

	public final StoredAttributeMapper getAttributeMapper() {
		return this.objectStore.getAttributeMapper();
	}

	public final int loadObjects(StoredObjectType type, Set<String> ids, StoredObjectHandler<V> handler)
		throws StorageException, StoredValueDecoderException {
		return this.objectStore.loadObjects(this.translator, type, ids, handler);
	}

	public final ContentStreamStore getContentStreamStore() {
		return this.streamStore;
	}
}