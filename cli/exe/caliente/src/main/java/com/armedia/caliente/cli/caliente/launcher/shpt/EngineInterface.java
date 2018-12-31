package com.armedia.caliente.cli.caliente.launcher.shpt;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.sharepoint.ShptCommon;
import com.armedia.caliente.engine.sharepoint.exporter.ShptExportEngineFactory;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	private final ShptExportEngineFactory exportFactory = new ShptExportEngineFactory();

	public EngineInterface() {
	}

	@Override
	public String getName() {
		return ShptCommon.TARGET_NAME;
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ShptExportEngineFactory getExportEngineFactory() {
		return this.exportFactory;
	}

	@Override
	protected Exporter newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected ImportEngineFactory<?, ?, ?, ?, ?, ?> getImportEngineFactory() {
		return null;
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

	@Override
	public void getDynamicOptions(CalienteCommand command, OptionScheme scheme) {
		if (command.isRequiresStorage()) {
			scheme //
				.addGroup(CLIGroup.STORE) //
				.addGroup(CLIGroup.MAIL) //
				.addGroup(CLIGroup.DOMAIN_CONNECTION) //
			;
		}
	}

}