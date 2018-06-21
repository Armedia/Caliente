package com.armedia.caliente.cli.caliente.newlauncher.cmis;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.caliente.newlauncher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.cmis.exporter.CmisExportEngine;
import com.armedia.caliente.engine.cmis.importer.CmisImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;

public class CmisEngineProxy extends EngineInterface {

	static final String ID_PREFIX = "id:";

	public CmisEngineProxy() {
	}

	@Override
	public String getName() {
		return "cmis";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return CmisExportEngine.getExportEngine();
	}

	@Override
	protected CmisExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new CmisExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return CmisImportEngine.getImportEngine();
	}

	@Override
	protected CmisImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new CmisImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}