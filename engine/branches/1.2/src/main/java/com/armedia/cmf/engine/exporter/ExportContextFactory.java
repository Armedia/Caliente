package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportContextFactory<S, W extends SessionWrapper<S>, T, V, C extends ExportContext<S, T, V>, E extends ExportEngine<S, W, T, V, C>>
	extends ContextFactory<S, T, V, C, E> {

	protected ExportContextFactory(E engine, CfgTools settings) {
		super(engine, settings);
	}
}