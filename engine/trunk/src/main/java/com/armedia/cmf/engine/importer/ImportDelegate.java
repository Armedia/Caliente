package com.armedia.cmf.engine.importer;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegate;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValueDecoderException;

public abstract class ImportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V>, F extends ImportDelegateFactory<S, W, V, C, E>, E extends ImportEngine<S, W, V, C, F>>
	extends TransferDelegate<T, S, V, C, F, E> {

	protected final StoredObject<V> storedObject;

	protected ImportDelegate(F factory, Class<T> objectClass, StoredObject<V> storedObject) throws Exception {
		super(factory, objectClass);
		this.storedObject = storedObject;
	}

	protected abstract ImportOutcome importObject(ObjectStorageTranslator<V> translator, C ctx) throws ImportException,
		StorageException, StoredValueDecoderException;

}