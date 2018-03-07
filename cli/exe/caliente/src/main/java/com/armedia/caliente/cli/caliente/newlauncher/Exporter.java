package com.armedia.caliente.cli.caliente.newlauncher;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportEngine;

public interface Exporter<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, CF>, CF extends ExportContextFactory<S, W, V, C, ?>, DF extends ExportDelegateFactory<S, W, V, C, ?>, E extends ExportEngine<S, W, V, C, CF, DF>>
	extends StorageClient {

	public E getExportEngine();

}