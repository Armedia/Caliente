package com.armedia.caliente.cli.caliente.newlauncher.shpt;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.caliente.newlauncher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.sharepoint.exporter.ShptExportEngine;

public class ShptEngineInterface extends EngineInterface {

	public ShptEngineInterface() {
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
		return ShptExportEngine.getExportEngine();
	}

	@Override
	protected ShptExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new ShptExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return null;
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}