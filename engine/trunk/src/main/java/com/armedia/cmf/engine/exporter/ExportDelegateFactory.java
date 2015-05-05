package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegateFactory;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportDelegateFactory<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V>, E extends ExportEngine<S, W, V, C>>
	extends TransferDelegateFactory<S, V, C, E> {

	protected ExportDelegateFactory(E engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected abstract ExportDelegate<?, S, W, V, C, ?, E> newExportDelegate(S session, StoredObjectType type,
		String searchKey) throws Exception;
}