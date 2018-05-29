package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;
import java.util.Set;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.tools.CmfCrypt;

public interface EngineProxy {

	public String getName();

	public Set<String> getAliases();

	public CmfCrypt getCrypt();

	@SuppressWarnings("rawtypes")
	public ExportEngine getExportEngine(OptionValues commandValues, Collection<String> positionals);

	@SuppressWarnings("rawtypes")
	public ImportEngine getImportEngine(OptionValues commandValues, Collection<String> positionals);

	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers();

}