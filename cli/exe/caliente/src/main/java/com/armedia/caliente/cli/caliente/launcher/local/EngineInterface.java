package com.armedia.caliente.cli.caliente.launcher.local;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.local.exporter.LocalExportEngine;
import com.armedia.caliente.engine.local.importer.LocalImportEngine;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {

		return true;
	}

	public EngineInterface() {
	}

	@Override
	public String getName() {
		return "local";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return LocalExportEngine.getExportEngine();
	}

	@Override
	protected Exporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return LocalImportEngine.getImportEngine();
	}

	@Override
	protected Importer newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new Importer(engine);
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
			;
		}
	}

}