package com.armedia.caliente.cli.caliente.newlauncher.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.caliente.newlauncher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.xml.importer.XmlImportEngine;

public class XmlEngineProxy extends EngineInterface {

	public XmlEngineProxy() {
	}

	@Override
	public String getName() {
		return "xml";
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
		return XmlImportEngine.getImportEngine();
	}

	@Override
	protected XmlImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new XmlImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

}