package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;

public class ImportCommandModule extends CommandModule {

	private static final Descriptor DESCRIPTOR = new Descriptor("import",
		"Import content from Caliente into an ECM or an intermediate format", "imp", "im");

	public ImportCommandModule(CalienteWarningTracker warningTracker) {
		super(warningTracker, true, false, ImportCommandModule.DESCRIPTOR);
	}

	@Override
	protected int execute(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}