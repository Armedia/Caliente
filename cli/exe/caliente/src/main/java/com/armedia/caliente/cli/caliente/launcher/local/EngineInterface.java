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
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.exporter.LocalExportEngineFactory;
import com.armedia.caliente.engine.local.importer.LocalImportEngineFactory;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {

		return true;
	}

	private final LocalExportEngineFactory exportFactory = new LocalExportEngineFactory();
	private final LocalImportEngineFactory importFactory = new LocalImportEngineFactory();

	public EngineInterface() {
	}

	@Override
	public String getName() {
		return LocalCommon.TARGET_NAME;
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected LocalExportEngineFactory getExportEngine() {
		return this.exportFactory;
	}

	@Override
	protected Exporter newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected LocalImportEngineFactory getImportEngine() {
		return this.importFactory;
	}

	@Override
	protected Importer newImporter(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
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