package com.armedia.caliente.cli.caliente.launcher.alfresco;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.caliente.launcher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.alfresco.bi.importer.AlfImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;

public class AlfrescoEngineInterface extends EngineInterface {

	public AlfrescoEngineInterface() {
	}

	@Override
	public String getName() {
		return "alfrescobi";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return null;
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return AlfImportEngine.getImportEngine();
	}

	@Override
	protected AlfrescoImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new AlfrescoImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}