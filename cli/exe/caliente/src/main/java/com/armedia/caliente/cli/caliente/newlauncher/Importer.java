package com.armedia.caliente.cli.caliente.newlauncher;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportEngine;

public interface Importer<S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V, CF>, CF extends ImportContextFactory<S, W, V, C, ?, ?>, DF extends ImportDelegateFactory<S, W, V, C, ?>, E extends ImportEngine<S, W, V, C, CF, DF>>
	extends StorageClient {

	public E getImportEngine();

}