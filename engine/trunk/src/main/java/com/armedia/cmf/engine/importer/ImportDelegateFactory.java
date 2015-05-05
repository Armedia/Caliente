package com.armedia.cmf.engine.importer;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegateFactory;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportDelegateFactory<S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V>, E extends ImportEngine<S, W, V, C>>
	extends TransferDelegateFactory<S, V, C, E> {

	protected ImportDelegateFactory(E engine, CfgTools configuration) {
		super(engine, configuration);
	}

	/*
	protected abstract ImportDelegate<?, S, W, V, C, E> newImportDelegate(S session, StoredObject<V> storedObject)
		throws Exception;
	 */
}