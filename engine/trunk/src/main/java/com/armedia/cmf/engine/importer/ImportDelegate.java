package com.armedia.cmf.engine.importer;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegate;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValueDecoderException;

public abstract class ImportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V, ?>, DF extends ImportDelegateFactory<S, W, V, C, E>, E extends ImportEngine<S, W, V, C, ?, DF>>
extends TransferDelegate<T, S, V, C, DF, E> {

	protected final CmfObject<V> cmfObject;

	protected ImportDelegate(DF factory, Class<T> objectClass, CmfObject<V> storedObject) throws Exception {
		super(factory, objectClass);
		this.cmfObject = storedObject;
	}

	protected abstract ImportOutcome importObject(CmfAttributeTranslator<V> translator, C ctx) throws ImportException,
	CmfStorageException, CmfValueDecoderException;

}