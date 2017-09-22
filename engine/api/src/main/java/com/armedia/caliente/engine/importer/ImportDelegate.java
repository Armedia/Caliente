package com.armedia.caliente.engine.importer;

import java.util.Collection;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegate;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;

public abstract class ImportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V, ?>, DF extends ImportDelegateFactory<S, W, V, C, E>, E extends ImportEngine<S, W, V, C, ?, DF>>
	extends TransferDelegate<T, S, V, C, DF, E> {

	protected final CmfObject<V> cmfObject;
	protected final ImportStrategy strategy;

	protected ImportDelegate(DF factory, Class<T> objectClass, CmfObject<V> storedObject) throws Exception {
		super(factory, objectClass);
		this.cmfObject = storedObject;
		this.strategy = factory.getEngine().getImportStrategy(storedObject.getType());
	}

	protected abstract Collection<ImportOutcome> importObject(TypeDescriptor targetType,
		CmfAttributeTranslator<V> translator, C ctx) throws ImportException, CmfStorageException;

}