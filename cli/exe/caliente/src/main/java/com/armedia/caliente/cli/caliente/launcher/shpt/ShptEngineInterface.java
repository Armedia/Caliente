package com.armedia.caliente.cli.caliente.launcher.shpt;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.launcher.DynamicOptions;
import com.armedia.caliente.cli.caliente.launcher.EngineInterface;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.sharepoint.exporter.ShptExportEngine;

public class ShptEngineInterface extends EngineInterface implements DynamicOptions {

	public ShptEngineInterface() {
	}

	@Override
	public String getName() {
		return "shpt";
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

	@Override
	public void getDynamicOptions(OptionScheme command) {
		command //
			.addGroup(CLIGroup.STORE) //
			.addGroup(CLIGroup.MAIL) //
			.addGroup(CLIGroup.DOMAIN_CONNECTION) //
		;
	}

}