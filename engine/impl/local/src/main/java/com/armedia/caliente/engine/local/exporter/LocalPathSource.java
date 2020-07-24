package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Path;

public interface LocalPathSource {

	public LocalPathStore<Path> getDefaultStore();

	public LocalPathStore<String> getIdCapableStore();

}