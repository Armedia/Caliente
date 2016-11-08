package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.ContextFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportContextFactory<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, ?>, E extends ExportEngine<S, W, V, C, ?, ?>>
	extends ContextFactory<S, V, C, E> {

	protected ExportContextFactory(E engine, CfgTools settings, S session) throws Exception {
		super(engine, settings, session);
	}
}