package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public class ExportCommandModule extends CommandModule {

	private static final Descriptor DESCRIPTOR = new Descriptor("export",
		"Extract content from an ECM or Local Filesystem", "exp", "ex");

	public ExportCommandModule(CalienteWarningTracker warningTracker) {
		super(warningTracker, true, true, ExportCommandModule.DESCRIPTOR);
	}

	@Override
	protected int execute(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		engineProxy.getExportEngine(commandValues, positionals);

		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}